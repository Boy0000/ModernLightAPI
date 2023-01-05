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
package com.boy0000.modernlightapi.api

/**
 * Result codes
 */
object ResultCode {
    /**
     * N/A
     */
    const val MOVED_TO_DEFERRED = 1

    /**
     * The task has been successfully completed
     */
    const val SUCCESS = 0

    /**
     * The task has been failed
     */
    const val FAILED = -1

    /**
     * Current world is not available
     */
    const val WORLD_NOT_AVAILABLE = -2

    /**
     * (1.14+) No lighting changes in current world
     */
    const val RECALCULATE_NO_CHANGES = -3

    /**
     * (1.14+) SkyLight data is not available in current world
     */
    const val SKYLIGHT_DATA_NOT_AVAILABLE = -4

    /**
     * (1.14+) BlockLight data is not available in current world
     */
    const val BLOCKLIGHT_DATA_NOT_AVAILABLE = -5

    /**
     * Current function is not implemented
     */
    const val NOT_IMPLEMENTED = -6

    /**
     * Chunk is not loaded
     */
    const val CHUNK_NOT_LOADED = -7
}
