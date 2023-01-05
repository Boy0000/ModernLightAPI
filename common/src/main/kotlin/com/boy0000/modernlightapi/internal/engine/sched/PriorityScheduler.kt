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
import com.boy0000.modernlightapi.internal.chunks.data.IChunkData
import com.boy0000.modernlightapi.internal.chunks.observer.sched.IScheduledChunkObserver
import com.boy0000.modernlightapi.internal.service.IBackgroundService
import com.boy0000.modernlightapi.internal.utils.FlagUtils

/**
 * A scheduler based on a priority system
 */
class PriorityScheduler<IChunkData>(
    private val lightEngine: IScheduledLightEngine, chunkObserver: IScheduledChunkObserver,
    backgroundService: IBackgroundService, maxTimeMsPerTick: Long
) : IScheduler {
    private val mChunkObserver: IScheduledChunkObserver
    private val mBackgroundService: IBackgroundService
    private val mMaxTimeMsPerTick: Long

    init {
        mChunkObserver = chunkObserver
        mBackgroundService = backgroundService
        mMaxTimeMsPerTick = maxTimeMsPerTick
    }

    private val chunkObserver: IScheduledChunkObserver
        private get() = mChunkObserver
    private val backgroundService: IBackgroundService
        private get() = mBackgroundService

    override fun canExecute(): Boolean {
        return !chunkObserver.isBusy
    }

    override fun createEmptyRequest(
        worldName: String, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int,
        lightFlags: Int, editPolicy: EditPolicy?, sendPolicy: SendPolicy?, callback: ICallback
    ): Request {
        // keep information about old light level
        val oldLightLevel: Int = lightEngine.getLightLevel(worldName, blockX, blockY, blockZ, lightFlags)
        return Request(
            Request.Companion.DEFAULT_PRIORITY, 0, worldName, blockX, blockY, blockZ, oldLightLevel, lightLevel,
            lightFlags, callback
        )
    }

    override fun createRequest(
        defaultFlag: Int, worldName: String, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int,
        lightFlags: Int, editPolicy: EditPolicy?, sendPolicy: SendPolicy, callback: ICallback
    ): Request {
        val request = createEmptyRequest(
            worldName, blockX, blockY, blockZ, lightLevel, lightFlags, editPolicy,
            sendPolicy, callback
        )
        request.priority = Request.Companion.DEFAULT_PRIORITY
        request.requestFlags = defaultFlag
        when (editPolicy) {
            EditPolicy.FORCE_IMMEDIATE -> {
                request.addRequestFlag(RequestFlag.RECALCULATE)
                request.addRequestFlag(RequestFlag.SEPARATE_SEND)
                request.priority = Request.Companion.HIGH_PRIORITY
            }

            EditPolicy.IMMEDIATE -> {
                var newPriority = request.priority
                if (sendPolicy === SendPolicy.IMMEDIATE) {
                    request.addRequestFlag(RequestFlag.SEPARATE_SEND)
                    newPriority += 1
                } else if (sendPolicy === SendPolicy.DEFERRED) {
                    request.addRequestFlag(RequestFlag.COMBINED_SEND)
                }
                if (backgroundService.canExecuteSync(mMaxTimeMsPerTick)) {
                    if (lightEngine.relightPolicy === RelightPolicy.FORWARD) {
                        request.addRequestFlag(RequestFlag.RECALCULATE)
                        newPriority += 1
                    } else if (lightEngine.relightPolicy === RelightPolicy.DEFERRED) {
                        request.addRequestFlag(RequestFlag.DEFERRED_RECALCULATE)
                    }
                } else {
                    // move to queue
                    request.addRequestFlag(RequestFlag.DEFERRED_RECALCULATE)
                }
                request.priority = newPriority
            }

            EditPolicy.DEFERRED -> {
                var newPriority = request.priority
                request.addRequestFlag(RequestFlag.DEFERRED_RECALCULATE)
                if (sendPolicy === SendPolicy.IMMEDIATE) {
                    request.addRequestFlag(RequestFlag.SEPARATE_SEND)
                    newPriority += 1
                } else if (sendPolicy === SendPolicy.DEFERRED) {
                    request.addRequestFlag(RequestFlag.COMBINED_SEND)
                }
                request.priority = newPriority
            }

            else -> {}
        }
        return request
    }

    override fun handleLightRequest(request: Request?): Int {
        if (FlagUtils.isFlagSet(request?.requestFlags ?: 0, RequestFlag.EDIT)) {
            request!!.removeRequestFlag(RequestFlag.EDIT)
            val resultCode: Int = lightEngine.setRawLightLevel(
                request.worldName, request.blockX,
                request.blockY, request.blockZ, request.lightLevel, request.lightFlags
            )
            if (request.callback != null) {
                request.callback?.onResult(RequestFlag.EDIT, resultCode)
            }
            if (resultCode == ResultCode.SUCCESS) {
                if (request.lightLevel == 0) {
                    // HAX: If the light is successfully removed, then add an additional flag, since the
                    // return value of the recalculation may be equal to RECALCULATE_NO_CHANGES.
                    request.addRequestFlag(RequestFlag.FORCE_SEND)
                }
                handleRelightRequest(request)
            }
        }
        return ResultCode.SUCCESS
    }

    override fun handleRelightRequest(request: Request?): Int {
        if (FlagUtils.isFlagSet(request?.requestFlags ?: 0, RequestFlag.RECALCULATE)) {
            request!!.removeRequestFlag(RequestFlag.RECALCULATE)
            val resultCode: Int = lightEngine.recalculateLighting(
                request.worldName, request.blockX,
                request.blockY, request.blockZ, request.lightFlags
            )
            if (request.callback != null) {
                request.callback?.onResult(RequestFlag.RECALCULATE, resultCode)
            }
            if (resultCode == ResultCode.SUCCESS || FlagUtils.isFlagSet(
                    request.requestFlags,
                    RequestFlag.FORCE_SEND
                )
            ) {
                if (FlagUtils.isFlagSet(request.requestFlags, RequestFlag.COMBINED_SEND)) {
                    lightEngine.notifySend(request)
                } else if (FlagUtils.isFlagSet(request.requestFlags, RequestFlag.SEPARATE_SEND)) {
                    handleSendRequest(request)
                }
            }
        } else if (FlagUtils.isFlagSet(request?.requestFlags ?: 0, RequestFlag.DEFERRED_RECALCULATE)) {
            request!!.removeRequestFlag(RequestFlag.DEFERRED_RECALCULATE)
            request.addRequestFlag(RequestFlag.RECALCULATE)
            // HAX: Add an additional flag, since the return value of the recalculation can be equal to RECALCULATE_NO_CHANGES.
            request.addRequestFlag(RequestFlag.FORCE_SEND)
            val resultCode = lightEngine.notifyRecalculate(request)
            if (request.callback != null) {
                request.callback?.onResult(RequestFlag.DEFERRED_RECALCULATE, resultCode)
            }
        }
        return ResultCode.SUCCESS
    }

    override fun handleSendRequest(request: Request?): Int {
        if (FlagUtils.isFlagSet(request?.requestFlags ?: 0, RequestFlag.COMBINED_SEND)) {
            request!!.removeRequestFlag(RequestFlag.COMBINED_SEND)
            val resultCode: Int = chunkObserver.notifyUpdateChunks(
                request.worldName, request.blockX,
                request.blockY, request.blockZ,
                request.oldLightLevel.coerceAtLeast(request.lightLevel), request.lightFlags
            )
            if (request.callback != null) {
                request.callback?.onResult(RequestFlag.COMBINED_SEND, resultCode)
            }
        } else if (FlagUtils.isFlagSet(request?.requestFlags ?: 0, RequestFlag.SEPARATE_SEND)) {
            request!!.removeRequestFlag(RequestFlag.SEPARATE_SEND)
            // send updated chunks now
            val chunkDataList: List<com.boy0000.modernlightapi.internal.chunks.data.IChunkData?>? = chunkObserver.collectChunkSections(
                request.worldName,
                request.blockX, request.blockY, request.blockZ,
                request.oldLightLevel.coerceAtLeast(request.lightLevel), request.lightFlags
            )
            if (chunkDataList != null) {
                for (data in chunkDataList) {
                    chunkObserver.sendChunk(data)
                }
            }
            if (request.callback != null) {
                request.callback?.onResult(RequestFlag.SEPARATE_SEND, ResultCode.SUCCESS)
            }
        }
        return ResultCode.SUCCESS
    }
}
