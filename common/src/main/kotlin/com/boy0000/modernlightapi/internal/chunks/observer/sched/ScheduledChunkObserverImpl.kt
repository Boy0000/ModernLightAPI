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
package com.boy0000.modernlightapi.internal.chunks.observer.sched

import com.boy0000.modernlightapi.api.ResultCode
import com.boy0000.modernlightapi.internal.IPlatformImpl
import com.boy0000.modernlightapi.internal.chunks.data.IChunkData
import com.boy0000.modernlightapi.internal.service.IBackgroundService

abstract class ScheduledChunkObserverImpl(platform: IPlatformImpl, service: IBackgroundService) :
    IScheduledChunkObserver {
    private val mBackgroundService: IBackgroundService
    private val observedChunks: MutableMap<Long, IChunkData?> = HashMap<Long, IChunkData?>()
    private val mPlatformImpl: IPlatformImpl
    final override var isBusy = false
        private set

    init {
        mPlatformImpl = platform
        mBackgroundService = service
    }

    protected open val platformImpl: IPlatformImpl
        protected get() = mPlatformImpl
    protected val backgroundService: IBackgroundService
        protected get() = mBackgroundService

    override fun onStart() {
        platformImpl.debug(javaClass.name + " is started!")
    }

    override fun onShutdown() {
        platformImpl.debug(javaClass.name + " is shutdown!")
        handleChunksLocked()
        observedChunks.clear()
    }

    private fun getDeltaLight(x: Int, dx: Int): Int {
        return (x xor (-dx shr 4 and 15)) + 1 and -(dx and 1)
    }

    private fun chunkCoordToLong(chunkX: Int, chunkZ: Int): Long {
        var l = chunkX.toLong()
        l = l shl 32 or (chunkZ.toLong() and 0xFFFFFFFFL)
        return l
    }

    protected abstract fun createChunkData(worldName: String?, chunkX: Int, chunkZ: Int): IChunkData?
    protected abstract fun isValidChunkSection(worldName: String?, sectionY: Int): Boolean
    protected abstract fun isChunkLoaded(worldName: String?, chunkX: Int, chunkZ: Int): Boolean

    /* @hide */
    private fun notifyUpdateChunksLocked(
        worldName: String, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int,
        lightType: Int
    ): Int {
        if (!platformImpl.isWorldAvailable(worldName)) {
            return ResultCode.WORLD_NOT_AVAILABLE
        }
        val finalLightLevel = if (lightLevel < 0) 0 else Math.min(lightLevel, 15)

        // start watching chunks
        val CHUNK_RADIUS = 1
        for (dX in -CHUNK_RADIUS..CHUNK_RADIUS) {
            val lightLevelX = finalLightLevel - getDeltaLight(blockX and 15, dX)
            if (lightLevelX > 0) {
                for (dZ in -CHUNK_RADIUS..CHUNK_RADIUS) {
                    val lightLevelZ = lightLevelX - getDeltaLight(blockZ and 15, dZ)
                    if (lightLevelZ > 0) {
                        val chunkX = (blockX shr 4) + dX
                        val chunkZ = (blockZ shr 4) + dZ
                        if (!isChunkLoaded(worldName, chunkX, chunkZ)) {
                            continue
                        }
                        for (dY in -1..1) {
                            if (lightLevelZ > getDeltaLight(blockY and 15, dY)) {
                                val sectionY = (blockY shr 4) + dY
                                if (isValidChunkSection(worldName, sectionY)) {
                                    val chunkCoord = chunkCoordToLong(chunkX, chunkZ)
                                    var data: IChunkData?
                                    if (observedChunks.containsKey(chunkCoord)) {
                                        data = observedChunks[chunkCoord]
                                    } else {
                                        data = createChunkData(worldName, chunkX, chunkZ)
                                        // register new chunk data
                                        observedChunks[chunkCoord] = data
                                    }
                                    data?.markSectionForUpdate(lightType, sectionY)
                                }
                            }
                        }
                    }
                }
            }
        }
        return ResultCode.SUCCESS
    }

    override fun notifyUpdateChunks(
        worldName: String, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int,
        lightFlags: Int
    ): Int {
        if (backgroundService.isMainThread) {
            return notifyUpdateChunksLocked(worldName, blockX, blockY, blockZ, lightLevel, lightFlags)
        } else {
            synchronized(observedChunks) {
                return notifyUpdateChunksLocked(
                    worldName,
                    blockX,
                    blockY,
                    blockZ,
                    lightLevel,
                    lightFlags
                )
            }
        }
    }

    private fun handleChunksLocked() {
        isBusy = true
        val it: MutableIterator<*> = observedChunks.entries.iterator()
        if (observedChunks.size > 0) {
            platformImpl.debug("observedChunks size: " + observedChunks.size)
        }
        while (it.hasNext()) {
            val (_, data) = it.next() as Map.Entry<Long, IChunkData>
            sendChunk(data)
            data.clearUpdate()
            it.remove()
        }
        isBusy = false
    }

    override fun run() {
        synchronized(observedChunks) { handleChunksLocked() }
    }
}
