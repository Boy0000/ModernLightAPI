/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Vladimir Mikhailov <beykerykt@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.boy0000.modernlightapi.bukkit.internal

import com.boy0000.modernlightapi.Build
import com.boy0000.modernlightapi.api.ResultCode
import com.boy0000.modernlightapi.api.engine.EditPolicy
import com.boy0000.modernlightapi.api.engine.LightFlag
import com.boy0000.modernlightapi.api.engine.SendPolicy
import com.boy0000.modernlightapi.api.engine.sched.ICallback
import com.boy0000.modernlightapi.api.extension.IExtension
import com.boy0000.modernlightapi.bukkit.BukkitPlugin
import com.boy0000.modernlightapi.bukkit.extension.IBukkitExtension
import com.boy0000.modernlightapi.bukkit.internal.chunks.observer.sched.BukkitScheduledChunkObserverImpl
import com.boy0000.modernlightapi.bukkit.internal.engine.sched.BukkitScheduledLightEngineImpl
import com.boy0000.modernlightapi.bukkit.internal.handler.CompatibilityHandler
import com.boy0000.modernlightapi.bukkit.internal.handler.IHandler
import com.boy0000.modernlightapi.bukkit.internal.handler.IHandlerFactory
import com.boy0000.modernlightapi.bukkit.internal.service.BukkitBackgroundServiceImpl
import com.boy0000.modernlightapi.bukkit.internal.utils.VersionUtil
import com.boy0000.modernlightapi.internal.IPlatformImpl
import com.boy0000.modernlightapi.internal.InternalCode
import com.boy0000.modernlightapi.internal.PlatformType
import com.boy0000.modernlightapi.internal.chunks.observer.IChunkObserver
import com.boy0000.modernlightapi.internal.engine.ILightEngine
import com.boy0000.modernlightapi.internal.service.IBackgroundService
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.World
import org.bukkit.configuration.file.FileConfiguration
import java.io.File
import java.util.*

class BukkitPlatformImpl(plugin: BukkitPlugin) : IPlatformImpl, IBukkitExtension, IExtension {
    /**
     * CONFIG
     */
    private val CONFIG_TITLE = "general"
    private val CONFIG_DEBUG = "$CONFIG_TITLE.debug"
    private val CONFIG_ENABLE_METRICS = "$CONFIG_TITLE.enable-metrics"
    private val CONFIG_ENABLE_COMPATIBILITY_MODE = "$CONFIG_TITLE.enable-compatibility-mode"
    private val CONFIG_FORCE_ENABLE_LEGACY = "$CONFIG_TITLE.force-enable-legacy"
    private val CONFIG_SPECIFIC_HANDLER_PATH = "$CONFIG_TITLE.specific-handler-path"
    private val CONFIG_HANDLERS_TITLE = "$CONFIG_TITLE.handlers"
    private val BSTATS_ID = 13051
    private val mPlugin: BukkitPlugin
    private var DEBUG = false
    final override var isInitialized = false
        private set
    private var forceLegacy = false
    final override var isCompatibilityMode = false
        private set
    private var mHandler: IHandler? = null
    private var mChunkObserver: IChunkObserver? = null
    private var mLightEngine: ILightEngine? = null
    private var mBackgroundService: IBackgroundService? = null
    private var mExtension: IExtension? = null
    private lateinit var mUUID: UUID

    init {
        mPlugin = plugin
    }

    fun toggleDebug() {
        DEBUG = !DEBUG
        log("Debug mode is " + (if (DEBUG) "en" else "dis") + "abled")
    }

    private val config: FileConfiguration
        get() = plugin.config

    private fun generateConfig() {
        // create config
        try {
            val file: File = File(plugin.getDataFolder(), "config.yml")
            if (!file.exists()) {
                config.set(CONFIG_DEBUG, false)
                config.set(CONFIG_ENABLE_METRICS, true)
                config.set(CONFIG_ENABLE_COMPATIBILITY_MODE, false)
                if (Build.API_VERSION === Build.PREVIEW) { // only for PREVIEW build
                    config.set(CONFIG_FORCE_ENABLE_LEGACY, true)
                } else {
                    config.set(CONFIG_FORCE_ENABLE_LEGACY, false)
                }
                plugin.saveConfig()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun upgradeConfig(): Boolean {
        var needSave = false
        if (config.isSet("general.specific-storage-provider")) {
            config.set("general.specific-storage-provider", null)
            needSave = true
        }
        if (config.isSet("handler.specific-handler-path")) {
            config.set("handler.specific-handler-path", null)
            needSave = true
        }
        if (config.isSet("handler.craftbukkit.factory-path")) {
            config.set("handler.craftbukkit.factory-path", null)
            needSave = true
        }
        if (needSave) {
            config.set("handler", null)
        }
        return needSave
    }

    private fun checkAndSetDefaults() {
        var needSave = upgradeConfig()
        if (!config.isSet(CONFIG_ENABLE_COMPATIBILITY_MODE)) {
            config.set(CONFIG_ENABLE_COMPATIBILITY_MODE, false)
            needSave = true
        }
        if (!config.isSet(CONFIG_SPECIFIC_HANDLER_PATH)) {
            config.set(CONFIG_SPECIFIC_HANDLER_PATH, "none")
            needSave = true
        }
        if (!config.isSet(CONFIG_HANDLERS_TITLE + "." + BukkitPlatformImpl.Companion.DEFAULT_IMPL_NAME + ".factory-path")) {
            config.set(
                CONFIG_HANDLERS_TITLE + "." + BukkitPlatformImpl.Companion.DEFAULT_IMPL_NAME + ".factory-path",
                "ru.beykerykt.minecraft.lightapi.bukkit.internal.handler." + BukkitPlatformImpl.Companion.DEFAULT_IMPL_NAME + ".HandlerFactory"
            )
            needSave = true
        }
        if (needSave) {
            plugin.saveConfig()
        }
    }

    @Throws(Exception::class)
    private fun initHandler() {
        checkAndSetDefaults()
        // load specific handler if available
        val specificPkg: String? = config.getString(CONFIG_SPECIFIC_HANDLER_PATH)
        if (specificPkg != null && !specificPkg.equals("none", ignoreCase = true)) {
            info("Initial load specific handler")
            mHandler = Class.forName(specificPkg).getConstructor().newInstance() as IHandler
            info("Custom handler is loaded: " + mHandler!!.javaClass.name)
            return
        }

        // compatibility mode (1.17+)
        isCompatibilityMode = config.getBoolean(CONFIG_ENABLE_COMPATIBILITY_MODE)
        if (isCompatibilityMode) {
            if (VersionUtil.compareBukkitVersionTo("1.17") >= 0) {
                info("Compatibility mode is enabled")
                mHandler = CompatibilityHandler()
                (mHandler as CompatibilityHandler).onInitialization(this)
                return
            } else {
                error("Compatibility mode can only work on versions > 1.17")
            }
        }

        // First, check Bukkit server implementation, since Bukkit is only an API, and there
        // may be several implementations (for example: Spigot, Paper, Glowstone and etc)
        var implName: String = Bukkit.getName().lowercase(Locale.getDefault())
        debug("Server implementation name: $implName")
        var modFactoryPath: String? = config.getString("$CONFIG_HANDLERS_TITLE.$implName.factory-path")
        try {
            Class.forName(modFactoryPath)
        } catch (ex: Exception) {
            debug(
                "Specific HandlerFactory for " + implName + " is not detected. Switch to default: "
                        + BukkitPlatformImpl.Companion.DEFAULT_IMPL_NAME
            )
            implName = BukkitPlatformImpl.Companion.DEFAULT_IMPL_NAME
            modFactoryPath = config.getString("$CONFIG_HANDLERS_TITLE.$implName.factory-path")
        }
        val factory: IHandlerFactory = Class.forName(modFactoryPath).getConstructor().newInstance() as IHandlerFactory
        mHandler = factory.createHandler(this)
        debug("Handler is loaded: " + mHandler?.javaClass?.name)
    }

    override fun prepare(): Int {
        // general default config
        generateConfig()

        // debug mode
        DEBUG = config.getBoolean(CONFIG_DEBUG)
        return ResultCode.SUCCESS
    }

    override fun initialization(): Int {
        // enable force legacy
        forceLegacy = config.getBoolean(CONFIG_FORCE_ENABLE_LEGACY)
        if (forceLegacy) {
            info("Force legacy is enabled")
        }

        // init handler
        try {
            initHandler()
            mHandler?.onInitialization(this) ?: return ResultCode.FAILED
        } catch (e: Exception) {
            e.printStackTrace()
            return ResultCode.FAILED
        }

        // init background service
        mBackgroundService = BukkitBackgroundServiceImpl(this, handler)
        (mBackgroundService as BukkitBackgroundServiceImpl).onStart()

        // init chunk observer
        mChunkObserver = BukkitScheduledChunkObserverImpl(this, backgroundService, handler)
        (mChunkObserver as BukkitScheduledChunkObserverImpl).onStart()

        // init light engine
        mLightEngine = BukkitScheduledLightEngineImpl<Any>(this, backgroundService, handler)
        (mLightEngine as BukkitScheduledLightEngineImpl<*>).onStart()

        // init extension
        mExtension = this
        this.isInitialized = true

        return ResultCode.SUCCESS
    }

    override fun shutdown() {
        mLightEngine?.onShutdown()
        mChunkObserver?.onShutdown()
        mBackgroundService?.onShutdown()
        mHandler?.onShutdown(this)

        mHandler = null
        this.isInitialized = false
    }

    override fun log(msg: String?) {
        val builder = StringBuilder(ChatColor.AQUA.toString() + "<LightAPI>: ")
        builder.append(ChatColor.WHITE).append(msg)
        mPlugin.server.consoleSender.sendMessage(builder.toString())
    }

    override fun info(msg: String?) {
        log("[INFO] $msg")
    }

    override val uuid: UUID
        get() = mUUID

    override fun debug(msg: String?) {
        if (DEBUG) {
            log(ChatColor.YELLOW.toString() + "[DEBUG] " + msg)
        }
    }

    override fun error(msg: String?) {
        log(ChatColor.RED.toString() + "[ERROR] " + msg)
    }

    override val platformType: PlatformType?
        get() = handler?.platformType
    override val lightEngine: ILightEngine?
        get() = mLightEngine
    override val chunkObserver: IChunkObserver?
        get() = mChunkObserver
    override val backgroundService: IBackgroundService?
        get() = mBackgroundService
    override val extension: IExtension?
        get() = mExtension

    override fun isWorldAvailable(worldName: String?): Boolean {
        return plugin.server.getWorld(worldName.toString()) != null
    }

    /* @hide */
    fun sendCmdLocked(cmdId: Int, vararg args: Any?): Int {
        var resultCode: Int = ResultCode.SUCCESS
        when (cmdId) {
            InternalCode.UPDATE_UUID -> mUUID = args[0] as UUID
            else -> resultCode = handler?.sendCmd(cmdId, args)!!
        }
        return resultCode
    }

    override fun sendCmd(cmdId: Int, vararg args: Any?): Int {
        return sendCmdLocked(cmdId, *args)
    }

    val plugin: BukkitPlugin
        get() = mPlugin
    override val handler: IHandler?
        get() = mHandler

    override fun getLightLevel(world: World?, blockX: Int, blockY: Int, blockZ: Int): Int {
        return getLightLevel(world, blockX, blockY, blockZ)
    }

    override fun getLightLevel(world: World?, blockX: Int, blockY: Int, blockZ: Int, lightFlags: Int): Int {
        return getLightLevel(world, blockX, blockY, blockZ, lightFlags)
    }

    override fun setLightLevel(world: World?, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int): Int {
        return getLightLevel(world, blockX, blockY, blockZ, lightLevel)
    }

    override fun setLightLevel(
        world: World?,
        blockX: Int,
        blockY: Int,
        blockZ: Int,
        lightLevel: Int,
        lightFlags: Int
    ): Int {
        return setLightLevel(world, blockX, blockY, blockZ, lightLevel, lightFlags)
    }

    override fun setLightLevel(
        world: World?,
        blockX: Int,
        blockY: Int,
        blockZ: Int,
        lightLevel: Int,
        lightFlags: Int,
        callback: ICallback?
    ): Int {
        return setLightLevel(world, blockX, blockY, blockZ, lightLevel, lightFlags, callback)
    }

    override fun setLightLevel(
        world: World?,
        blockX: Int,
        blockY: Int,
        blockZ: Int,
        lightLevel: Int,
        lightFlags: Int,
        editPolicy: EditPolicy?,
        sendPolicy: SendPolicy?,
        callback: ICallback?
    ): Int {
        return setLightLevel(world, blockX, blockY, blockZ, lightLevel, lightFlags, editPolicy, sendPolicy, callback)
    }

    override val isBackwardAvailable: Boolean
        get() {
            val flag = Build.API_VERSION === Build.PREVIEW
            return try {
                Class.forName("ru.beykerykt.lightapi.LightAPI")
                if (forceLegacy) true else flag and true
            } catch (ex: ClassNotFoundException) {
                false
            }
        }

    companion object {
        private const val DEFAULT_IMPL_NAME = "craftbukkit"
    }
}
