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
package com.boy0000.modernlightapi.bukkit.internal.engine.sched

import com.boy0000.modernlightapi.api.ResultCode
import com.boy0000.modernlightapi.api.engine.RelightPolicy
import com.boy0000.modernlightapi.bukkit.internal.BukkitPlatformImpl
import com.boy0000.modernlightapi.bukkit.internal.handler.IHandler
import com.boy0000.modernlightapi.internal.chunks.observer.sched.IScheduledChunkObserver
import com.boy0000.modernlightapi.internal.engine.ILightEngine
import com.boy0000.modernlightapi.internal.engine.LightEngineType
import com.boy0000.modernlightapi.internal.engine.LightEngineVersion
import com.boy0000.modernlightapi.internal.engine.sched.PriorityScheduler
import com.boy0000.modernlightapi.internal.engine.sched.ScheduledLightEngineImpl
import com.boy0000.modernlightapi.internal.service.IBackgroundService
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.configuration.file.FileConfiguration
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

open class BukkitScheduledLightEngineImpl<IScheduler>(
    pluginImpl: BukkitPlatformImpl?, service: IBackgroundService?,
    strategy: RelightPolicy?, handler: IHandler?, maxRequestCount: Int, maxTimeMsPerTick: Int
) : ScheduledLightEngineImpl(pluginImpl!!, service!!, strategy!!, maxRequestCount, maxTimeMsPerTick), ILightEngine {
    /**
     * CONFIG
     */
    private val CONFIG_TITLE = javaClass.simpleName
    private val CONFIG_RELIGHT_STRATEGY = "$CONFIG_TITLE.relight-strategy"
    private val CONFIG_TICK_PERIOD = "$CONFIG_TITLE.tick-period"
    private val CONFIG_MAX_TIME_MS_IN_PER_TICK = "$CONFIG_TITLE.max-time-ms-in-per-tick"
    private val CONFIG_MAX_ITERATIONS_IN_PER_TICK = "$CONFIG_TITLE.max-iterations-in-per-tick"
    private var mHandler: IHandler? = null
    private var mScheduledFuture: ScheduledFuture<*>? = null
    private var mTaskId = -1

    /**
     * @hide
     */
    constructor(pluginImpl: BukkitPlatformImpl?, service: IBackgroundService?, handler: IHandler?) : this(
        pluginImpl,
        service,
        RelightPolicy.DEFERRED,
        handler,
        250,
        250
    )

    init {
        if (handler != null) {
            mHandler = handler
        }
    }

    private val handler: IHandler?
        get() = mHandler
    override val platformImpl: BukkitPlatformImpl
        get() = super.platformImpl as BukkitPlatformImpl

    private fun checkAndSetDefaults() {
        var needSave = false
        val fc: FileConfiguration = platformImpl.plugin.config
        if (!fc.isSet(CONFIG_RELIGHT_STRATEGY)) {
            fc.set(CONFIG_RELIGHT_STRATEGY, RelightPolicy.DEFERRED.name)
            needSave = true
        }
        if (!fc.isSet(CONFIG_TICK_PERIOD)) {
            fc.set(CONFIG_TICK_PERIOD, 1)
            needSave = true
        }
        if (!fc.isSet(CONFIG_MAX_TIME_MS_IN_PER_TICK)) {
            fc.set(CONFIG_MAX_TIME_MS_IN_PER_TICK, 50)
            needSave = true
        }
        if (!fc.isSet(CONFIG_MAX_ITERATIONS_IN_PER_TICK)) {
            fc.set(CONFIG_MAX_ITERATIONS_IN_PER_TICK, 256)
            needSave = true
        }
        if (needSave) {
            platformImpl.plugin.saveConfig()
        }
    }

    private fun configure() {
        checkAndSetDefaults()
        // load config
        val fc: FileConfiguration = platformImpl.plugin.config
        val relightStrategyName: String = fc.getString(CONFIG_RELIGHT_STRATEGY).toString()
        try {
            // TODO: move to throw exception
            val relightPolicy: RelightPolicy = RelightPolicy.valueOf(relightStrategyName)
            mRelightPolicy = relightPolicy
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        maxRequestCount = fc.getInt(CONFIG_MAX_ITERATIONS_IN_PER_TICK)
        maxTimeMsPerTick = fc.getInt(CONFIG_MAX_TIME_MS_IN_PER_TICK).toLong()
        mTaskId = platformImpl.plugin.server.scheduler.runTaskTimer(
            platformImpl.plugin, this::onTickPenaltyTime, 0, 1
        ).taskId

        // scheduler
        // TODO: Make config (?)
        var scheduler: PriorityScheduler<Any?> = PriorityScheduler(
            this, platformImpl.chunkObserver as IScheduledChunkObserver,
            backgroundService, maxTimeMsPerTick
        )
        scheduler = scheduler
        val period: Int = fc.getInt(CONFIG_TICK_PERIOD)
        mScheduledFuture = backgroundService.scheduleWithFixedDelay(this, 0, 50 * period, TimeUnit.MILLISECONDS)
    }

    override fun onStart() {
        configure()
        super.onStart()
    }

    override fun onShutdown() {
        if (mTaskId != -1) {
            platformImpl.plugin.server.scheduler.cancelTask(mTaskId)
        }
        if (mScheduledFuture != null) {
            mScheduledFuture!!.cancel(true)
        }
        super.onShutdown()
    }

    override val lightEngineType: LightEngineType
        get() = handler!!.lightEngineType
    override val lightEngineVersion: LightEngineVersion
        get() = handler!!.lightEngineVersion
    override val relightPolicy: RelightPolicy
        get() = TODO("Not yet implemented")

    @Deprecated("")
    override fun checkLight(worldName: String?, blockX: Int, blockY: Int, blockZ: Int, lightFlags: Int): Int {
        TODO("Not yet implemented")
    }

    /* @hide */
    private fun getLightLevelLocked(worldName: String?, blockX: Int, blockY: Int, blockZ: Int, lightFlags: Int): Int {
        if (!platformImpl.isWorldAvailable(worldName)) {
            return ResultCode.WORLD_NOT_AVAILABLE
        }
        val world: World = Bukkit.getWorld(worldName.toString()) ?: return 0
        return handler!!.getRawLightLevel(world, blockX, blockY, blockZ, lightFlags)
    }

    override fun getLightLevel(worldName: String?, blockX: Int, blockY: Int, blockZ: Int, lightFlags: Int): Int {
        if (handler!!.isMainThread) {
            return getLightLevelLocked(worldName, blockX, blockY, blockZ, lightFlags)
        } else {
            synchronized(relightQueue) { return getLightLevelLocked(worldName, blockX, blockY, blockZ, lightFlags) }
        }
    }

    /* @hide */
    open fun setRawLightLevelLocked(
        worldName: String, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int,
        lightFlags: Int
    ): Int {
        if (!platformImpl.isWorldAvailable(worldName)) {
            return ResultCode.WORLD_NOT_AVAILABLE
        }
        val world = Bukkit.getWorld(worldName)
        return handler?.setRawLightLevel(world!!, blockX, blockY, blockZ, lightLevel, lightFlags) ?: 0
    }

    override fun setRawLightLevel(
        worldName: String?,
        blockX: Int,
        blockY: Int,
        blockZ: Int,
        lightLevel: Int,
        lightFlags: Int
    ): Int {
        if (handler?.isMainThread == true) {
            return setRawLightLevelLocked(worldName.toString(), blockX, blockY, blockZ, lightLevel, lightFlags)
        } else {
            synchronized(lightQueue) {
                return setRawLightLevelLocked(worldName.toString(), blockX, blockY, blockZ, lightLevel, lightFlags)
            }
        }
    }



    /* @hide */
    private fun recalculateLightingLocked(
        worldName: String,
        blockX: Int,
        blockY: Int,
        blockZ: Int,
        lightFlags: Int
    ): Int {
        if (!platformImpl.isWorldAvailable(worldName)) {
            return ResultCode.WORLD_NOT_AVAILABLE
        }
        val world: World = Bukkit.getWorld(worldName.toString()) ?: return 0
        return handler!!.recalculateLighting(world, blockX, blockY, blockZ, lightFlags)
    }

    override fun recalculateLighting(worldName: String?, blockX: Int, blockY: Int, blockZ: Int, lightFlags: Int): Int {
        if (handler!!.isMainThread) {
            return recalculateLightingLocked(worldName.toString(), blockX, blockY, blockZ, lightFlags)
        } else {
            synchronized(relightQueue) {
                return recalculateLightingLocked(worldName.toString(), blockX, blockY, blockZ, lightFlags)
            }
        }
    }
}
