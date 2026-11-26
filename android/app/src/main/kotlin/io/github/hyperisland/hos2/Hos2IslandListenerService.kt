package io.github.hyperisland.hos2

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.view.View

/**
 * HyperOS 2 fallback island. It deliberately uses public Android APIs only:
 * NotificationListenerService + TYPE_APPLICATION_OVERLAY. It never hooks or
 * replaces SystemUI classes, so a failure here cannot crash SystemUI.
 * User must grant Notification access and Display over other apps.
 */
class Hos2IslandListenerService : NotificationListenerService() {
    private val main = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var pill: LinearLayout? = null
    private var title: TextView? = null
    private var body: TextView? = null
    private var hideTask: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        if (Settings.canDrawOverlays(this)) ensureView()
    }

    override fun onListenerDisconnected() {
        // Do not leave a stale floating window if Android revokes listener access.
        main.post { pill?.visibility = View.GONE }
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        if (!Settings.canDrawOverlays(this)) return
        val n = sbn.notification ?: return
        // Group summaries duplicate the child notifications and create visual spam.
        if ((n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        val extras = n.extras
        val app = runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString() }.getOrDefault(sbn.packageName)
        val heading = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.take(80) ?: app
        val text = (extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString()?.take(140).orEmpty()
        main.post { show(heading, text.ifBlank { app }, 5000L) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Keep the island visible until its short timeout; avoid flicker on grouped notifications.
    }

    private fun ensureView() {
        if (pill != null) return
        val wm = windowManager ?: return
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(9), dp(18), dp(9))
            background = GradientDrawable().apply {
                setColor(Color.rgb(8, 8, 10))
                cornerRadius = dp(28).toFloat()
                setStroke(dp(1), Color.argb(45, 255, 255, 255))
            }
            elevation = dp(12).toFloat()
        }
        val heading = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            maxLines = 1
        }
        val detail = TextView(this).apply {
            setTextColor(Color.LTGRAY); textSize = 10f; maxLines = 2
        }
        container.addView(heading, LinearLayout.LayoutParams(-1, -2))
        container.addView(detail, LinearLayout.LayoutParams(-1, -2))
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            dp(230), WindowManager.LayoutParams.WRAP_CONTENT, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(10)
        }
        runCatching { wm.addView(container, params) }.onSuccess {
            pill = container; title = heading; body = detail
            container.visibility = View.GONE
        }
    }

    private fun show(head: String, detail: String, duration: Long) {
        ensureView()
        val v = pill ?: return
        title?.text = head
        body?.text = detail
        v.visibility = View.VISIBLE
        hideTask?.let(main::removeCallbacks)
        hideTask = Runnable { v.visibility = View.GONE }
        main.postDelayed(hideTask!!, duration)
    }

    override fun onDestroy() {
        hideTask?.let(main::removeCallbacks)
        pill?.let { runCatching { windowManager?.removeView(it) } }
        pill = null; title = null; body = null
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
}
