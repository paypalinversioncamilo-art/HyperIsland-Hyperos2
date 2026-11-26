package io.github.hyperisland.hos2

import android.app.Notification
import android.app.PendingIntent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

/**
 * Safe HyperOS 2 fallback renderer.
 *
 * It uses the notification-listener and application-overlay APIs only. It does
 * not patch SystemUI, replace privileged APKs, or write to system partitions.
 * The user must explicitly grant notification access and overlay permission.
 */
class Hos2IslandListenerService : NotificationListenerService() {
    private val main = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var root: LinearLayout? = null
    private var titleView: TextView? = null
    private var detailView: TextView? = null
    private var kindView: TextView? = null
    private var progressView: View? = null
    private var actionsView: LinearLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var hideTask: Runnable? = null
    private var activeKey: String? = null
    private var activeIntent: PendingIntent? = null
    private var expanded = false
    private var downX = 0f
    private var downY = 0f

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        if (Settings.canDrawOverlays(this)) {
            ensureView()
            // Restore a currently active media/call/timer notification after service reconnect.
            activeNotifications.orEmpty()
                .filterNot { it.packageName == packageName }
                .filterNot { (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0 }
                .maxByOrNull { it.postTime }
                ?.let(::renderNotification)
        }
    }

    override fun onListenerDisconnected() {
        main.post { dismiss(animated = false) }
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName || !Settings.canDrawOverlays(this)) return
        val notification = sbn.notification ?: return
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        renderNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.key == activeKey) main.post { dismiss() }
    }

    private data class IslandContent(
        val kind: String,
        val title: String,
        val detail: String,
        val persistent: Boolean,
        val progress: Int?,
        val progressMax: Int,
        val intent: PendingIntent?,
        val actions: List<Notification.Action>,
    )

    private fun renderNotification(sbn: StatusBarNotification) {
        val n = sbn.notification
        val extras = n.extras
        val appName = runCatching {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString()
        }.getOrDefault(sbn.packageName)

        val category = n.category.orEmpty()
        val isCall = category == Notification.CATEGORY_CALL
        val isMedia = category == Notification.CATEGORY_TRANSPORT ||
            extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
        val isAlarm = category == Notification.CATEGORY_ALARM ||
            category == Notification.CATEGORY_EVENT
        val isProgress = category == Notification.CATEGORY_PROGRESS ||
            extras.containsKey(Notification.EXTRA_PROGRESS)

        val kind = when {
            isCall -> "CALL"
            isMedia -> "NOW PLAYING"
            isAlarm -> "TIMER"
            isProgress -> "ACTIVITY"
            sbn.isOngoing -> "LIVE"
            else -> appName.uppercase().take(22)
        }
        val heading = (extras.getCharSequence(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG))
            ?.toString()?.take(72) ?: appName
        val body = (extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
            ?.toString()?.take(110).orEmpty()

        val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0).coerceAtLeast(0)
        val rawProgress = extras.getInt(Notification.EXTRA_PROGRESS, -1)
        val progress = rawProgress.takeIf { isProgress && max > 0 && it >= 0 }
            ?.let { ((it.toFloat() / max) * 100f).toInt().coerceIn(0, 100) }

        val content = IslandContent(
            kind = kind,
            title = heading,
            detail = body.ifBlank { appName },
            // Calls and active timers should remain visible; ordinary alerts auto-dismiss.
            persistent = isCall || (isAlarm && sbn.isOngoing),
            progress = progress,
            progressMax = max,
            intent = n.contentIntent,
            actions = n.actions?.toList().orEmpty().take(3),
        )
        main.post { show(sbn.key, content) }
    }

    private fun ensureView() {
        if (root != null) return
        val wm = windowManager ?: return

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(15), dp(9), dp(15), dp(9))
            background = pillBackground()
            elevation = dp(16).toFloat()
            alpha = 0f
            scaleX = 0.82f
            scaleY = 0.82f
            visibility = View.GONE
            clipToPadding = false
            clipChildren = false
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val dot = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(95, 230, 165))
            }
        }
        header.addView(dot, LinearLayout.LayoutParams(dp(6), dp(6)))
        val kind = TextView(this).apply {
            setTextColor(Color.rgb(160, 160, 170))
            textSize = 9f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            maxLines = 1
        }
        val kindParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
            marginStart = dp(7)
        }
        header.addView(kind, kindParams)

        val heading = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            maxLines = 1
        }
        val detail = TextView(this).apply {
            setTextColor(Color.rgb(195, 195, 202))
            textSize = 11f
            maxLines = 2
            setLineSpacing(dp(1).toFloat(), 1f)
        }
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
        }
        val progress = View(this).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(2).toFloat()
                setColor(Color.rgb(105, 225, 170))
            }
            visibility = View.GONE
        }

        container.addView(header, LinearLayout.LayoutParams(-1, -2))
        container.addView(heading, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(4)
        })
        container.addView(detail, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(2)
        })
        container.addView(progress, LinearLayout.LayoutParams(dp(1), dp(3)).apply {
            topMargin = dp(7)
        })
        container.addView(actions, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(7)
        })

        val type = if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            dp(218), WindowManager.LayoutParams.WRAP_CONTENT, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(8)
        }

        container.setOnClickListener {
            val intent = activeIntent
            if (intent != null) runCatching { intent.send() } else toggleExpanded()
        }
        container.setOnLongClickListener {
            dismiss()
            true
        }
        container.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    false
                }
                MotionEvent.ACTION_UP -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (abs(dx) > dp(64) && abs(dx) > abs(dy)) {
                        dismiss()
                        true
                    } else false
                }
                else -> false
            }
        }

        runCatching { wm.addView(container, params) }.onSuccess {
            root = container
            titleView = heading
            detailView = detail
            kindView = kind
            progressView = progress
            actionsView = actions
            layoutParams = params
        }
    }

    private fun show(key: String, content: IslandContent) {
        ensureView()
        val view = root ?: return
        activeKey = key
        activeIntent = content.intent
        kindView?.text = content.kind
        titleView?.text = content.title
        detailView?.text = content.detail
        detailView?.visibility = if (expanded || content.detail.isNotBlank()) View.VISIBLE else View.GONE
        renderActions(content.actions)
        progressView?.let { bar ->
            val value = content.progress
            bar.visibility = if (value == null) View.GONE else View.VISIBLE
            val width = if (value == null) 1 else (dp(if (expanded) 290 else 188) * value / 100).coerceAtLeast(dp(2))
            (bar.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
                lp.width = width
                bar.layoutParams = lp
            }
        }
        updateSize()
        hideTask?.let(main::removeCallbacks)
        view.visibility = View.VISIBLE
        view.animate().cancel()
        view.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220L).start()
        if (!content.persistent) {
            hideTask = Runnable { dismiss() }
            main.postDelayed(hideTask!!, if (content.progress != null) 9000L else 5500L)
        }
    }

    private fun renderActions(actions: List<Notification.Action>) {
        val row = actionsView ?: return
        row.removeAllViews()
        val usable = actions.filter { !it.title.isNullOrBlank() && it.actionIntent != null }
        usable.forEachIndexed { index, action ->
            val button = TextView(this).apply {
                text = action.title.toString().take(18)
                setTextColor(Color.WHITE)
                textSize = 10f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(dp(10), dp(7), dp(10), dp(7))
                background = GradientDrawable().apply {
                    setColor(Color.rgb(38, 40, 46))
                    cornerRadius = dp(14).toFloat()
                    setStroke(dp(1), Color.argb(35, 255, 255, 255))
                }
                setOnClickListener {
                    runCatching { action.actionIntent.send() }
                    main.postDelayed({ dismiss() }, 1200L)
                }
            }
            row.addView(button, LinearLayout.LayoutParams(-2, -2).apply {
                if (index > 0) marginStart = dp(7)
            })
        }
        row.visibility = if (usable.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun toggleExpanded() {
        expanded = !expanded
        detailView?.maxLines = if (expanded) 4 else 2
        updateSize()
    }

    private fun updateSize() {
        val view = root ?: return
        val params = layoutParams ?: return
        params.width = dp(if (expanded) 320 else 218)
        runCatching { windowManager?.updateViewLayout(view, params) }
    }

    private fun dismiss(animated: Boolean = true) {
        hideTask?.let(main::removeCallbacks)
        hideTask = null
        val view = root ?: return
        activeKey = null
        activeIntent = null
        expanded = false
        if (!animated) {
            view.visibility = View.GONE
            return
        }
        view.animate().cancel()
        view.animate().alpha(0f).scaleX(0.84f).scaleY(0.84f)
            .setDuration(160L)
            .withEndAction { view.visibility = View.GONE }
            .start()
    }

    private fun pillBackground() = GradientDrawable().apply {
        setColor(Color.rgb(7, 8, 11))
        cornerRadius = dp(30).toFloat()
        setStroke(dp(1), Color.argb(52, 255, 255, 255))
    }

    override fun onDestroy() {
        hideTask?.let(main::removeCallbacks)
        root?.let { runCatching { windowManager?.removeView(it) } }
        root = null
        titleView = null
        detailView = null
        kindView = null
        progressView = null
        actionsView = null
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
