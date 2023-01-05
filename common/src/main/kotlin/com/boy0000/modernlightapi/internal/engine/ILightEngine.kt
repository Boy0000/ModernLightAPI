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
package com.boy0000.modernlightapi.internal.engine

import com.boy0000.modernlightapi.api.engine.EditPolicy
import com.boy0000.modernlightapi.api.engine.RelightPolicy
import com.boy0000.modernlightapi.api.engine.SendPolicy
import com.boy0000.modernlightapi.api.engine.sched.ICallback


interface ILightEngine {
    /**
     * N/A
     */
    fun onStart()

    /**
     * N/A
     */
    fun onShutdown()

    /**
     * N/A
     */
    val lightEngineType: LightEngineType?

    /**
     * Used lighting engine version.
     *
     * @return One of the proposed options from [LightEngineVersion]
     */
    val lightEngineVersion: LightEngineVersion?

    /**
     * N/A
     */
    val relightPolicy: RelightPolicy?

    /**
     * Checks the light level and restores it if available.
     */
    @Deprecated("")
    fun checkLight(worldName: String?, blockX: Int, blockY: Int, blockZ: Int, lightFlags: Int): Int

    /**
     * Gets the level of light from given coordinates with specific flags.
     */
    fun getLightLevel(worldName: String?, blockX: Int, blockY: Int, blockZ: Int, lightFlags: Int): Int

    /**
     * Placement of a specific type of light with a given level of illumination in the named world in
     * certain coordinates with the return code result.
     */
    fun setLightLevel(
        worldName: String?, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int, lightFlags: Int,
        editPolicy: EditPolicy?, sendPolicy: SendPolicy?, callback: ICallback?
    ): Int

    /**
     * Sets "directly" the level of light in given coordinates without additional processing.
     */
    fun setRawLightLevel(
        worldName: String?,
        blockX: Int,
        blockY: Int,
        blockZ: Int,
        lightLevel: Int,
        lightFlags: Int
    ): Int

    /**
     * Performs re-illumination of the light in the given coordinates.
     */
    fun recalculateLighting(worldName: String?, blockX: Int, blockY: Int, blockZ: Int, lightFlags: Int): Int
}
