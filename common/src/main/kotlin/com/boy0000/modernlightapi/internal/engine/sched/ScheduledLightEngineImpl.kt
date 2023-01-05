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
package com.boy0000.modernlightapi.internal.engine.sched

import com.boy0000.modernlightapi.api.ResultCode
import com.boy0000.modernlightapi.api.engine.EditPolicy
import com.boy0000.modernlightapi.api.engine.RelightPolicy
import com.boy0000.modernlightapi.api.engine.SendPolicy
import com.boy0000.modernlightapi.api.engine.sched.ICallback
import com.boy0000.modernlightapi.internal.IPlatformImpl
import com.boy0000.modernlightapi.internal.service.IBackgroundService
import java.util.*
import java.util.concurrent.PriorityBlockingQueue

/**
 * Abstract class for scheduled light engines
 */
abstract class ScheduledLightEngineImpl(
    platformImpl: IPlatformImpl, service: IBackgroundService, strategy: RelightPolicy,
    maxRequestCount: Int, maxTimeMsPerTick: Int
) : IScheduledLightEngine {
    protected val lightQueue: Queue<Request?> = PriorityBlockingQueue<Request>(20,
        Comparator { o1: Request, o2: Request -> o2.priority - o1.priority })
    protected val relightQueue: Queue<Request?> = PriorityBlockingQueue<Request>(20,
        Comparator { o1: Request, o2: Request -> o2.priority - o1.priority })
    protected val sendQueue: Queue<Request?> = PriorityBlockingQueue<Request>(20,
        Comparator { o1: Request, o2: Request -> o2.priority - o1.priority })
    private val mBackgroundService: IBackgroundService
    private val TICK_MS: Long = 50
    private val mPlatformImpl: IPlatformImpl
    protected var maxTimeMsPerTick: Long
    protected var maxRequestCount: Int
    protected var mRelightPolicy: RelightPolicy
    private var mScheduler: IScheduler? = null
    private var requestCount = 0
    private var penaltyTime: Long = 0

    init {
        mPlatformImpl = platformImpl
        mBackgroundService = service
        mRelightPolicy = strategy
        this.maxRequestCount = maxRequestCount
        this.maxTimeMsPerTick = maxTimeMsPerTick.toLong()
    }

    protected open val platformImpl: IPlatformImpl
        get() = mPlatformImpl
    protected val backgroundService: IBackgroundService
        get() = mBackgroundService

    private fun canExecuteSync(): Boolean {
        return (backgroundService.canExecuteSync(maxTimeMsPerTick) && penaltyTime < maxTimeMsPerTick
                && scheduler!!.canExecute())
    }

    override fun onStart() {
        if (scheduler != null) {
            platformImpl.debug(javaClass.name + " is started!")
        }
    }

    override fun onShutdown() {
        platformImpl.debug(javaClass.name + " is shutdown!")
        while (lightQueue.peek() != null) {
            val request = lightQueue.poll()
            handleLightRequest(request)
        }
        while (relightQueue.peek() != null) {
            val request = relightQueue.poll()
            handleRelightRequest(request)
        }
        while (sendQueue.peek() != null) {
            val request = sendQueue.poll()
            handleSendRequest(request)
        }
        lightQueue.clear()
        relightQueue.clear()
        sendQueue.clear()
    }

    override val relightPolicy: RelightPolicy
        get() = mRelightPolicy

    /* @hide */
    private fun checkLightLocked(worldName: String, blockX: Int, blockY: Int, blockZ: Int, lightFlags: Int): Int {
        return ResultCode.NOT_IMPLEMENTED
    }

    override fun checkLight(worldName: String?, blockX: Int, blockY: Int, blockZ: Int, lightFlags: Int): Int {
        if (backgroundService.isMainThread) {
            return checkLightLocked(worldName.toString(), blockX, blockY, blockZ, lightFlags)
        } else {
            synchronized(lightQueue) { return checkLightLocked(worldName.toString(), blockX, blockY, blockZ, lightFlags) }
        }
    }

    /* @hide */
    private fun setLightLevelLocked(
        worldName: String, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int, lightType: Int,
        editPolicy: EditPolicy, sendPolicy: SendPolicy, callback: ICallback
    ): Int {
        var resultCode: Int = ResultCode.SUCCESS
        val request = scheduler!!.createRequest(
            RequestFlag.EDIT, worldName, blockX, blockY, blockZ, lightLevel,
            lightType, editPolicy, sendPolicy, callback
        )
        when (editPolicy) {
            EditPolicy.FORCE_IMMEDIATE -> {

                // Execute request immediately
                handleLightRequest(request)
            }

            EditPolicy.IMMEDIATE -> {
                if (canExecuteSync()) {
                    // Execute the request only if we can provide it
                    val startTime = System.currentTimeMillis()
                    handleLightRequest(request)
                    val time = System.currentTimeMillis() - startTime
                    penaltyTime += time
                } else {
                    // add request to queue
                    val code = notifyChangeLightLevel(request)
                    if (code == ResultCode.SUCCESS) {
                        resultCode = ResultCode.MOVED_TO_DEFERRED
                    }
                }
            }

            EditPolicy.DEFERRED -> {

                // add request to queue
                val code = notifyChangeLightLevel(request)
                if (code == ResultCode.SUCCESS) {
                    resultCode = ResultCode.MOVED_TO_DEFERRED
                }
            }

            else -> throw IllegalArgumentException("Not supported strategy: " + editPolicy.name)
        }
        return resultCode
    }

    override fun setLightLevel(
        worldName: String?,
        blockX: Int,
        blockY: Int,
        blockZ: Int,
        lightLevel: Int,
        lightFlags: Int,
        editPolicy: EditPolicy?,
        sendPolicy: SendPolicy?,
        callback: ICallback?
    ): Int {
        if (!platformImpl.isWorldAvailable(worldName)) {
            return ResultCode.WORLD_NOT_AVAILABLE
        }
        if (backgroundService.isMainThread) {
            return setLightLevelLocked(
                worldName.toString(), blockX, blockY, blockZ, lightLevel, lightFlags, editPolicy!!,
                sendPolicy!!,
                callback!!
            )
        } else {
            synchronized(lightQueue) {
                return setLightLevelLocked(
                    worldName.toString(), blockX, blockY, blockZ, lightLevel, lightFlags, editPolicy!!,
                    sendPolicy!!,
                    callback!!
                )
            }
        }
    }

    override var scheduler: IScheduler?
        get() {
            if (mScheduler == null) {
                throw NullPointerException("Scheduler is null!")
            }
            return mScheduler
        }
        set(scheduler) {
            mScheduler = scheduler
        }

    /* @hide */
    private fun notifyChangeLightLevelLocked(request: Request?): Int {
        if (request != null) {
            lightQueue.add(request)
        }
        return ResultCode.SUCCESS
    }

    override fun notifyChangeLightLevel(request: Request?): Int {
        if (backgroundService.isMainThread) {
            return notifyChangeLightLevelLocked(request)
        } else {
            synchronized(lightQueue) { return notifyChangeLightLevelLocked(request) }
        }
    }

    /* @hide */
    private fun notifyRecalculateLocked(request: Request?): Int {
        if (request != null) {
            relightQueue.add(request)
        }
        return ResultCode.SUCCESS
    }

    override fun notifyRecalculate(request: Request?): Int {
        if (backgroundService.isMainThread) {
            return notifyRecalculateLocked(request)
        } else {
            synchronized(relightQueue) { return notifyRecalculateLocked(request) }
        }
    }

    /* @hide */
    private fun notifySendLocked(request: Request?): Int {
        if (request != null) {
            sendQueue.add(request)
        }
        return ResultCode.SUCCESS
    }

    override fun notifySend(request: Request?): Int {
        if (backgroundService.isMainThread) {
            return notifySendLocked(request)
        } else {
            synchronized(sendQueue) { return notifySendLocked(request) }
        }
    }

    private fun handleLightRequest(request: Request?) {
        if (backgroundService.isMainThread) {
            scheduler!!.handleLightRequest(request)
        } else {
            synchronized(request!!) { scheduler!!.handleLightRequest(request) }
        }
    }

    private fun handleRelightRequest(request: Request?) {
        if (backgroundService.isMainThread) {
            scheduler!!.handleRelightRequest(request)
        } else {
            synchronized(request!!) { scheduler!!.handleRelightRequest(request) }
        }
    }

    private fun handleSendRequest(request: Request?) {
        if (backgroundService.isMainThread) {
            scheduler!!.handleSendRequest(request)
        } else {
            synchronized(request!!) { scheduler!!.handleSendRequest(request) }
        }
    }

    private fun handleLightQueueLocked() {
        if (!scheduler!!.canExecute()) {
            return
        }
        val startTime = System.currentTimeMillis()
        requestCount = 0
        while (lightQueue.peek() != null) {
            platformImpl.debug("handleLightQueueLocked()")
            val time = System.currentTimeMillis() - startTime
            if (time > maxTimeMsPerTick) {
                platformImpl.debug("handleLightQueueLocked: maxRelightTimePerTick is reached ($time ms)")
                break
            }
            if (requestCount > maxRequestCount) {
                platformImpl.debug("handleLightQueueLocked: maxRequestCount is reached ($requestCount)")
                break
            }
            val request = lightQueue.poll()
            handleLightRequest(request)
            requestCount++
        }
    }

    private fun handleRelightQueueLocked() {
        if (!scheduler!!.canExecute()) {
            return
        }
        val startTime = System.currentTimeMillis()
        requestCount = 0
        while (relightQueue.peek() != null) {
            platformImpl.debug("handleRelightQueueLocked()")
            val time = System.currentTimeMillis() - startTime
            if (time > maxTimeMsPerTick) {
                platformImpl.debug("handleRelightQueueLocked: maxRelightTimePerTick is reached ($time ms)")
                break
            }
            if (requestCount > maxRequestCount) {
                platformImpl.debug("handleRelightQueueLocked: maxRequestCount is reached ($requestCount)")
                break
            }
            val request = relightQueue.poll()
            handleRelightRequest(request)
            requestCount++
        }
    }

    private fun handleSendQueueLocked() {
        if (!scheduler!!.canExecute()) {
            return
        }
        val startTime = System.currentTimeMillis()
        requestCount = 0
        while (sendQueue.peek() != null) {
            platformImpl.debug("handleSendQueueLocked()")
            val time = System.currentTimeMillis() - startTime
            if (time > maxTimeMsPerTick) {
                platformImpl.debug("handleSendQueueLocked: maxRelightTimePerTick is reached ($time ms)")
                break
            }
            if (requestCount > maxRequestCount) {
                platformImpl.debug("handleSendQueueLocked: maxRequestCount is reached ($requestCount)")
                break
            }
            val request = sendQueue.poll()
            handleSendRequest(request)
            requestCount++
        }
    }

    override fun run() {
        synchronized(lightQueue) { handleLightQueueLocked() }
        synchronized(relightQueue) { handleRelightQueueLocked() }
        synchronized(sendQueue) { handleSendQueueLocked() }
    }

    protected fun onTickPenaltyTime() {
        if (penaltyTime > 0) {
            penaltyTime -= TICK_MS
        } else if (penaltyTime < 0) {
            penaltyTime = 0
        }
    }
}
