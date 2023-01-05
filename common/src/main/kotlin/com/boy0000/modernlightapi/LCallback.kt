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
package com.boy0000.modernlightapi

@Deprecated("")
interface LCallback {
    /**
     * Called after successful execution.
     *
     * @param worldName  - World name
     * @param type       - Light type
     * @param blockX     - Block X coordinate
     * @param blockY     - Block Y coordinate
     * @param blockZ     - Block Z coordinate
     * @param lightlevel - light level. Default range - 0 - 15
     */
    fun onSuccess(
        worldName: String?,
        type: LightType?,
        blockX: Int,
        blockY: Int,
        blockZ: Int,
        lightlevel: Int,
        stage: LStage?
    )

    /**
     * Called if something went wrong during execution.
     *
     * @param worldName - World name
     * @param type      - Light type
     * @param blockX    - Block X coordinate
     * @param blockY    - Block Y coordinate
     * @param blockZ    - Block Z coordinate
     * @param reason    - Reason
     */
    fun onFailed(
        worldName: String?, type: LightType?, blockX: Int, blockY: Int, blockZ: Int, lightlevel: Int, stage: LStage?,
        reason: LReason?
    )
}
