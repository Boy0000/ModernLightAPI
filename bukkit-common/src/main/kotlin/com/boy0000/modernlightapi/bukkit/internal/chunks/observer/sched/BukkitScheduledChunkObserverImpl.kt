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
package com.boy0000.modernlightapi.bukkit.internal.chunks.observer.sched

import com.boy0000.modernlightapi.api.ResultCode
import com.boy0000.modernlightapi.bukkit.internal.BukkitPlatformImpl
import com.boy0000.modernlightapi.bukkit.internal.handler.IHandler
import com.boy0000.modernlightapi.internal.chunks.data.IChunkData
import com.boy0000.modernlightapi.internal.chunks.observer.IChunkObserver
import com.boy0000.modernlightapi.internal.chunks.observer.sched.ScheduledChunkObserverImpl
import com.boy0000.modernlightapi.internal.service.IBackgroundService
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.configuration.file.FileConfiguration
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class BukkitScheduledChunkObserverImpl(platform: BukkitPlatformImpl?, service: IBackgroundService?, handler: IHandler?) :
    ScheduledChunkObserverImpl(platform!!, service!!), IChunkObserver {
    /**
     * CONFIG
     */
    private val CONFIG_TITLE = javaClass.simpleName
    private val CONFIG_TICK_PERIOD = "$CONFIG_TITLE.tick-period"
    private var mHandler: IHandler? = null
    private var mScheduledFuture: ScheduledFuture<*>? = null

    init {
        mHandler = handler
    }

    private val handler: IHandler?
        get() = mHandler
    override val platformImpl: BukkitPlatformImpl
        get() = super.platformImpl as BukkitPlatformImpl

    private fun checkAndSetDefaults() {
        var needSave = false
        val fc: FileConfiguration = platformImpl.plugin.config
        if (!fc.isSet(CONFIG_TICK_PERIOD)) {
            fc.set(CONFIG_TICK_PERIOD, 2)
            needSave = true
        }
        if (needSave) {
            platformImpl.plugin.saveConfig()
        }
    }

    private fun configure() {
        checkAndSetDefaults()
        val fc: FileConfiguration = platformImpl.plugin.config
        val period: Int = fc.getInt(CONFIG_TICK_PERIOD)
        mScheduledFuture = backgroundService.scheduleWithFixedDelay(this, 0, 50 * period, TimeUnit.MILLISECONDS)
    }

    override fun onStart() {
        configure()
        super.onStart()
    }

    override fun onShutdown() {
        if (mScheduledFuture != null) {
            mScheduledFuture!!.cancel(true)
        }
        super.onShutdown()
    }

    override fun createChunkData(worldName: String?, chunkX: Int, chunkZ: Int): IChunkData? {
        return if (!platformImpl.isWorldAvailable(worldName)) {
            null
        } else handler?.createChunkData(worldName, chunkX, chunkZ)
    }

    override fun isValidChunkSection(worldName: String?, sectionY: Int): Boolean {
        if (!platformImpl.isWorldAvailable(worldName)) {
            return false
        }
        val world: World? = Bukkit.getWorld(worldName.toString())
        return handler?.isValidChunkSection(world, sectionY) == true
    }

    override fun isChunkLoaded(worldName: String?, chunkX: Int, chunkZ: Int): Boolean {
        if (!platformImpl.isWorldAvailable(worldName)) {
            return false
        }
        val world: World? = Bukkit.getWorld(worldName.toString())
        return world?.isChunkLoaded(chunkX, chunkZ) == true
    }

    override fun collectChunkSections(
        worldName: String?, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int,
        lightFlags: Int
    ): List<IChunkData> {
        if (!platformImpl.isWorldAvailable(worldName)) {
            return ArrayList<IChunkData>()
        }
        val world: World? = Bukkit.getWorld(worldName.toString())
        return handler?.collectChunkSections(world, blockX, blockY, blockZ, lightLevel, lightFlags) ?: emptyList()
    }

    override fun sendChunk(data: IChunkData?): Int {
        return if (!platformImpl.isWorldAvailable(data?.worldName)) {
            ResultCode.WORLD_NOT_AVAILABLE
        } else handler?.sendChunk(data) ?: 0
    }
}
