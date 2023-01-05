package com.boy0000.modernlightapi.craftbukkit

import com.boy0000.modernlightapi.bukkit.internal.BukkitPlatformImpl
import com.boy0000.modernlightapi.internal.PlatformType
import org.bukkit.Bukkit

abstract class BaseNMSHandler : IHandler {
    private var mPlatformImpl: BukkitPlatformImpl? = null

    @Throws(Exception::class)
    override fun onInitialization(impl: BukkitPlatformImpl) {
        mPlatformImpl = impl
    }

    protected val platformImpl: BukkitPlatformImpl?
        get() = mPlatformImpl

    override fun getPlatformType(): PlatformType {
        return PlatformType.CRAFTBUKKIT
    }

    override fun isMainThread(): Boolean {
        return Bukkit.isPrimaryThread()
    }
}
