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
package com.boy0000.modernlightapi.internal

import com.boy0000.modernlightapi.api.extension.IExtension
import com.boy0000.modernlightapi.internal.chunks.observer.IChunkObserver
import com.boy0000.modernlightapi.internal.engine.ILightEngine
import com.boy0000.modernlightapi.internal.service.IBackgroundService
import java.util.*

interface IPlatformImpl {
    /**
     * N/A
     */
    fun prepare(): Int

    /**
     * N/A
     */
    fun initialization(): Int

    /**
     * N/A
     */
    fun shutdown()

    /**
     * N/A
     */
    val isInitialized: Boolean

    /**
     * Log message in console
     *
     * @param msg - message
     */
    fun log(msg: String?)

    /**
     * Info message in console
     *
     * @param msg - message
     */
    fun info(msg: String?)

    /**
     * Debug message in console
     *
     * @param msg - message
     */
    fun debug(msg: String?)

    /**
     * Error message in console
     *
     * @param msg - message
     */
    fun error(msg: String?)

    /**
     * N/A
     */
    val uuid: UUID

    /**
     * Platform that is being used
     *
     * @return One of the proposed options from [PlatformType]
     */
    val platformType: PlatformType?

    /**
     * N/A
     */
    val lightEngine: ILightEngine?

    /**
     * N/A
     */
    val chunkObserver: IChunkObserver?

    /**
     * N/A
     */
    val backgroundService: IBackgroundService?

    /**
     * N/A
     */
    val extension: IExtension?

    /**
     * N/A
     */
    fun isWorldAvailable(worldName: String?): Boolean

    /**
     * Can be used for specific commands
     */
    fun sendCmd(cmdId: Int, vararg args: Any?): Int
}
