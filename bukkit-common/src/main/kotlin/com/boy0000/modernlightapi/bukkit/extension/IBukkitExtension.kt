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
package com.boy0000.modernlightapi.bukkit.extension

import com.boy0000.modernlightapi.api.engine.EditPolicy
import com.boy0000.modernlightapi.api.engine.SendPolicy
import com.boy0000.modernlightapi.api.engine.sched.ICallback
import com.boy0000.modernlightapi.api.extension.IExtension
import com.boy0000.modernlightapi.bukkit.internal.handler.IHandler
import org.bukkit.World

interface IBukkitExtension : IExtension {
    /**
     * Gets the level of light from given coordinates with specific flags.
     */
    fun getLightLevel(world: World?, blockX: Int, blockY: Int, blockZ: Int): Int

    /**
     * Gets the level of light from given coordinates with specific flags.
     */
    fun getLightLevel(world: World?, blockX: Int, blockY: Int, blockZ: Int, lightFlags: Int): Int

    /**
     * Placement of a specific type of light with a given level of illumination in the named world in
     * certain coordinates with the return code result.
     */
    fun setLightLevel(world: World?, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int): Int

    /**
     * Placement of a specific type of light with a given level of illumination in the named world in
     * certain coordinates with the return code result.
     */
    fun setLightLevel(world: World?, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int, lightFlags: Int): Int

    /**
     * Placement of a specific type of light with a given level of illumination in the named world in
     * certain coordinates with the return code result.
     */
    fun setLightLevel(
        world: World?, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int, lightFlags: Int,
        callback: ICallback?
    ): Int

    /**
     * Placement of a specific type of light with a given level of illumination in the named world in
     * certain coordinates with the return code result.
     */
    fun setLightLevel(
        world: World?, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int, lightFlags: Int,
        editPolicy: EditPolicy?, sendPolicy: SendPolicy?, callback: ICallback?
    ): Int

    /**
     * N/A
     */
    val isBackwardAvailable: Boolean

    /**
     * N/A
     */
    val isCompatibilityMode: Boolean

    /**
     * N/A
     */
    val handler: IHandler?
}
