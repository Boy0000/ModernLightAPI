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
package com.boy0000.modernlightapi.bukkit.internal.handler

import com.boy0000.modernlightapi.bukkit.internal.BukkitPlatformImpl
import com.boy0000.modernlightapi.internal.PlatformType
import com.boy0000.modernlightapi.internal.chunks.data.IChunkData
import com.boy0000.modernlightapi.internal.engine.LightEngineType
import com.boy0000.modernlightapi.internal.engine.LightEngineVersion
import org.bukkit.World
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent

interface IHandler {
    /**
     * N/A
     */
    @Throws(Exception::class)
    fun onInitialization(impl: BukkitPlatformImpl?)

    /**
     * N/A
     */
    fun onShutdown(impl: BukkitPlatformImpl?)

    /**
     * Platform that is being used
     *
     * @return One of the proposed options from [PlatformType]
     */
    val platformType: PlatformType

    /**
     * N/A
     */
    val lightEngineType: LightEngineType

    /**
     * Used lighting engine version.
     *
     * @return One of the proposed options from [LightEngineVersion]
     */
    val lightEngineVersion: LightEngineVersion

    /**
     * N/A
     */
    val isMainThread: Boolean

    /**
     * N/A
     */
    fun onWorldLoad(event: WorldLoadEvent?)

    /**
     * N/A
     */
    fun onWorldUnload(event: WorldUnloadEvent?)

    /**
     * N/A
     */
    fun isLightingSupported(world: World?, lightFlags: Int): Boolean

    /**
     * Sets "directly" the level of light in given coordinates without additional processing.
     */
    fun setRawLightLevel(world: World, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int, lightFlags: Int): Int

    /**
     * Gets "directly" the level of light from given coordinates without additional processing.
     */
    fun getRawLightLevel(world: World, blockX: Int, blockY: Int, blockZ: Int, lightFlags: Int): Int

    /**
     * Performs re-illumination of the light in the given coordinates.
     */
    fun recalculateLighting(world: World?, blockX: Int, blockY: Int, blockZ: Int, lightFlags: Int): Int

    /**
     * N/A
     */
    fun createChunkData(worldName: String?, chunkX: Int, chunkZ: Int): IChunkData

    /**
     * Collects modified сhunks with sections around a given coordinate in the radius of the light
     * level. The light level is taken from the arguments.
     *
     * @return List changed chunk sections around the given coordinate.
     */
    fun collectChunkSections(
        world: World?, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int,
        lightFlags: Int
    ): List<IChunkData>

    /**
     * N/A
     */
    fun isValidChunkSection(world: World?, sectionY: Int): Boolean

    /**
     * N/A
     */
    fun sendChunk(data: IChunkData?): Int

    /**
     * Can be used for specific commands
     */
    fun sendCmd(cmdId: Int, vararg args: Any?): Int
}
