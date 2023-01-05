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

import com.boy0000.modernlightapi.api.engine.sched.ICallback
import com.boy0000.modernlightapi.internal.utils.FlagUtils


class Request(
    var priority: Int, var requestFlags: Int, val worldName: String, val blockX: Int, val blockY: Int, val blockZ: Int,
    val oldLightLevel: Int, val lightLevel: Int, val lightFlags: Int, callback: ICallback
) : Comparable<Request> {
    private val mCallback: ICallback

    init {
        mCallback = callback
    }

    fun addRequestFlag(targetFlag: Int) {
        requestFlags = FlagUtils.addFlag(requestFlags, targetFlag)
    }

    fun removeRequestFlag(targetFlag: Int) {
        requestFlags = FlagUtils.removeFlag(requestFlags, targetFlag)
    }

    val callback: ICallback?
        get() = mCallback

    override fun compareTo(o: Request): Int {
        return priority - o.priority
    }

    companion object {
        const val HIGH_PRIORITY = 10
        const val DEFAULT_PRIORITY = 5
        const val LOW_PRIORITY = 0
    }
}
