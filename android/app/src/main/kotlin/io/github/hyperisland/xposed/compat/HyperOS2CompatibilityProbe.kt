package io.github.hyperisland.xposed.compat

import io.github.hyperisland.xposed.logDebug
import io.github.libxposed.api.XposedModule
import java.util.Collections
import java.util.WeakHashMap

/**
 * Read-only capability probe for Xiaomi's native Dynamic Island implementation.
 *
 * This probe never installs hooks, changes resources, or mutates SystemUI. It
 * inspects both the SystemUI and MIUI plugin class loaders at runtime so the
 * port can distinguish a missing native implementation from a class-loader
 * mismatch before enabling compatibility hooks.
 */
object HyperOS2CompatibilityProbe {
    private const val TAG = "HyperIsland[HyperOS2Compat]"

    private val candidateClasses = linkedMapOf(
        "baseContent" to "miui.systemui.dynamicisland.window.content.DynamicIslandBaseContentView",
        "content" to "miui.systemui.dynamicisland.window.content.DynamicIslandContentView",
        "background" to "miui.systemui.dynamicisland.DynamicIslandBackgroundView",
        "coordinator" to "miui.systemui.dynamicisland.event.DynamicIslandEventCoordinator",
        "animation" to "miui.systemui.dynamicisland.anim.DynamicIslandAnimationController",
        "fakeContent" to "miui.systemui.dynamicisland.window.content.DynamicIslandContentFakeView",
        "animationDelegate" to "miui.systemui.dynamicisland.anim.DynamicIslandAnimationDelegate",
        "statusBarController" to "com.android.systemui.statusbar.StatusBarIslandControllerImpl",
        "dynamicController" to "com.android.systemui.statusbar.notification.DynamicIslandController",
    )

    private val inspected = Collections.synchronizedMap(
        WeakHashMap<ClassLoader, Boolean>()
    )

    fun inspect(module: XposedModule, loader: ClassLoader, source: String) {
        synchronized(inspected) {
            if (inspected.containsKey(loader) || inspected.size >= 256) return
            inspected[loader] = true
        }

        val results = candidateClasses.map { (key, className) ->
            key to inspectClass(loader, className)
        }
        val baseReady = results.firstOrNull { it.first == "baseContent" }
            ?.second?.startsWith("found") == true
        val backgroundReady = results.firstOrNull { it.first == "background" }
            ?.second?.startsWith("found") == true
        val summary = results.joinToString(" ") { (key, value) -> "$key=$value" }

        module.logDebug(TAG) {
            "source=$source loader=${loader.javaClass.name} " +
                "nativeIslandReady=${baseReady && backgroundReady} $summary"
        }
    }

    private fun inspectClass(loader: ClassLoader, name: String): String {
        return try {
            val type = Class.forName(name, false, loader)
            val methods = runCatching {
                type.declaredMethods
                    .take(10)
                    .joinToString(",") { method -> "${method.name}/${method.parameterCount}" }
            }.getOrDefault("unreadable")
            val fields = runCatching {
                type.declaredFields.take(8).joinToString(",") { field -> field.name }
            }.getOrDefault("unreadable")
            "found(methods=[$methods],fields=[$fields])"
        } catch (error: Throwable) {
            "missing(${error.javaClass.simpleName})"
        }
    }
}
