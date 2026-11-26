package io.github.hyperisland.xposed.compat

import io.github.hyperisland.xposed.logDebug
import io.github.libxposed.api.XposedModule
import java.util.Collections
import java.util.WeakHashMap

/**
 * Read-only HyperOS 2 capability probe.
 *
 * It deliberately does not install hooks or alter SystemUI. Native HyperOS 3
 * classes are inspected at runtime so compatibility decisions can be based on
 * the actual plugin class loader rather than a ROM version string.
 */
object HyperOS2CompatibilityProbe {
    private const val TAG = "HyperIsland[HyperOS2Compat]"
    private const val BASE_CONTENT =
        "miui.systemui.dynamicisland.window.content.DynamicIslandBaseContentView"
    private const val BACKGROUND =
        "miui.systemui.dynamicisland.DynamicIslandBackgroundView"

    private val inspected = Collections.synchronizedMap(
        WeakHashMap<ClassLoader, Boolean>()
    )

    fun inspect(module: XposedModule, loader: ClassLoader, source: String) {
        synchronized(inspected) {
            if (inspected.containsKey(loader) || inspected.size >= 256) return
            inspected[loader] = true
        }

        val base = inspectClass(loader, BASE_CONTENT)
        val background = inspectClass(loader, BACKGROUND)
        module.logDebug(TAG) {
            "source=" + source + " loader=" + loader.javaClass.name +
                " baseContent=" + base + " background=" + background +
                " nativeIslandReady=" + (base.startsWith("found") && background.startsWith("found"))
        }
    }

    private fun inspectClass(loader: ClassLoader, name: String): String {
        return try {
            val type = Class.forName(name, false, loader)
            val methods = runCatching { type.declaredMethods.size }.getOrDefault(-1)
            val fields = runCatching { type.declaredFields.size }.getOrDefault(-1)
            "found(methods=" + methods + ",fields=" + fields + ")"
        } catch (error: Throwable) {
            "missing(" + error.javaClass.simpleName + ")"
        }
    }
}
