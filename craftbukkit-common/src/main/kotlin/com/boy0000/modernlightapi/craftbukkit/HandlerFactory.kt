package com.boy0000.modernlightapi.craftbukkit

import com.boy0000.modernlightapi.bukkit.internal.BukkitPlatformImpl
import com.boy0000.modernlightapi.bukkit.internal.handler.IHandler
import com.boy0000.modernlightapi.bukkit.internal.handler.IHandlerFactory
import org.bukkit.Bukkit

class HandlerFactory : IHandlerFactory {
    private var mPlatformImpl: BukkitPlatformImpl? = null
    private val platformImpl: BukkitPlatformImpl?
        get() = mPlatformImpl
    private val isStarlight: Boolean
        get() {
            for (pkg in STARLIGHT_ENGINE_PKG) {
                try {
                    Class.forName(pkg)
                    return true
                } catch (e: ClassNotFoundException) {
                    //TODO Fix this
                //platformImpl?.debug("Class $pkg not found")
                }
            }
            return false
        }

    @Throws(Exception::class)
    override fun createHandler(impl: BukkitPlatformImpl?): IHandler {
        mPlatformImpl = impl
        var handler: IHandler? = null
        val serverImplPackage = Bukkit.getServer().javaClass.getPackage().name
        handler = if (serverImplPackage.startsWith(CRAFTBUKKIT_PKG)) { // make sure it's craftbukkit
            val line = serverImplPackage.replace(".", ",").split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val version = line[3]
            val handlerClassName = (if (isStarlight) "Starlight" else "Vanilla") + "NMSHandler"
            val handlerPath = javaClass.getPackage().name + ".nms." + version + "." + handlerClassName
            // start using nms handler
            Class.forName(handlerPath).getConstructor().newInstance() as IHandler
        } else { // something else
            error(Bukkit.getName() + " is currently not supported.")
        }
        return handler
    }

    companion object {
        private const val CRAFTBUKKIT_PKG = "org.bukkit.craftbukkit"
        private val STARLIGHT_ENGINE_PKG = arrayOf(
            "ca.spottedleaf.starlight.light.StarLightEngine",
            "ca.spottedleaf.starlight.common.light.StarLightEngine"
        )
    }
}
