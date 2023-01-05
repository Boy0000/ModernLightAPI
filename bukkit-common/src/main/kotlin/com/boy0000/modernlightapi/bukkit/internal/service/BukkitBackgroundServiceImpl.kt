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
package com.boy0000.modernlightapi.bukkit.internal.service

import com.boy0000.modernlightapi.bukkit.internal.BukkitPlatformImpl
import com.boy0000.modernlightapi.bukkit.internal.handler.IHandler
import com.boy0000.modernlightapi.internal.service.BackgroundServiceImpl
import com.boy0000.modernlightapi.internal.service.IBackgroundService
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.bukkit.configuration.file.FileConfiguration
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit

class BukkitBackgroundServiceImpl(platform: BukkitPlatformImpl?, handler: IHandler?) : BackgroundServiceImpl(platform!!),
    IBackgroundService {
    /**
     * CONFIG
     */
    private val CONFIG_TITLE = javaClass.simpleName

    @Deprecated("")
    private val CONFIG_TICK_PERIOD = "$CONFIG_TITLE.tick-period"
    private val CONFIG_CORE_POOL_SIZE = "$CONFIG_TITLE.corePoolSize"
    private var mHandler: IHandler? = null
    private var taskId = -1
    private var lastAliveTime: Long = 0

    init {
        if (handler != null) {
            mHandler = handler
        }
    }

    override val platformImpl: BukkitPlatformImpl
        get() = super.platformImpl as BukkitPlatformImpl
    private val handler: IHandler
        get() = mHandler!!

    private fun upgradeConfig(): Boolean {
        var needSave = false
        val fc: FileConfiguration = platformImpl.plugin.config
        if (fc.isSet(CONFIG_TICK_PERIOD)) {
            fc.set(CONFIG_TICK_PERIOD, null)
            needSave = true
        }
        return needSave
    }

    private fun checkAndSetDefaults() {
        var needSave = upgradeConfig()
        val fc: FileConfiguration = platformImpl.plugin.config
        if (!fc.isSet(CONFIG_CORE_POOL_SIZE)) {
            fc.set(CONFIG_CORE_POOL_SIZE, 1)
            needSave = true
        }
        if (needSave) {
            platformImpl.plugin.saveConfig()
        }
    }

    override fun onStart() {
        checkAndSetDefaults()

        // executor service
        val namedThreadFactory: ThreadFactory = ThreadFactoryBuilder().setNameFormat(
            "lightapi-background-thread-%d"
        ).build()
        val fc: FileConfiguration = platformImpl.plugin.config
        val corePoolSize: Int = fc.getInt(CONFIG_CORE_POOL_SIZE)
        configureExecutorService(corePoolSize, namedThreadFactory)

        // heartbeat
        platformImpl.plugin.server.scheduler.runTaskTimer(
            platformImpl.plugin,
            Runnable { lastAliveTime = System.currentTimeMillis() }, 0, 1
        ).taskId.also { taskId = it }
    }

    override fun onShutdown() {
        if (taskId != -1) {
            platformImpl.plugin.server.scheduler.cancelTask(taskId)
        }
        super.onShutdown()
    }

    override fun canExecuteSync(maxTime: Long): Boolean {
        return System.currentTimeMillis() - lastAliveTime < maxTime
    }

    override val isMainThread: Boolean
        get() = handler.isMainThread

    override fun scheduleWithFixedDelay(
        runnable: Runnable?,
        initialDelay: Int,
        delay: Int,
        unit: TimeUnit?
    ): ScheduledFuture<*> {
        TODO("Not yet implemented")
    }
}
