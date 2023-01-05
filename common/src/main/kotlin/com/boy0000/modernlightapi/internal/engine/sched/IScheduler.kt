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

import com.boy0000.modernlightapi.api.engine.EditPolicy
import com.boy0000.modernlightapi.api.engine.SendPolicy
import com.boy0000.modernlightapi.api.engine.sched.ICallback


/**
 * A scheduler interface for scheduled light engine logic
 */
interface IScheduler {
    /**
     * N/A
     */
    fun canExecute(): Boolean

    /**
     * Creates requests. The function must return only a request with specific flags.
     */
    fun createEmptyRequest(
        worldName: String, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int, lightFlags: Int,
        editPolicy: EditPolicy?, sendPolicy: SendPolicy?, callback: ICallback
    ): Request

    /**
     * Creates requests. The function must return only a request with specific flags.
     */
    fun createRequest(
        defaultFlag: Int, worldName: String, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int,
        lightFlags: Int, editPolicy: EditPolicy?, sendPolicy: SendPolicy, callback: ICallback
    ): Request

    /**
     * Processes light requests. The function should only process requests without changes in flags.
     */
    fun handleLightRequest(request: Request?): Int

    /**
     * Processes relight requests. The function should only process requests without changes in flags.
     */
    fun handleRelightRequest(request: Request?): Int

    /**
     * Processes send requests. The function should only process requests without changes in flags.
     */
    fun handleSendRequest(request: Request?): Int
}
