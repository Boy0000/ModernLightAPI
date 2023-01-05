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

import com.boy0000.modernlightapi.api.ResultCode
import com.boy0000.modernlightapi.api.engine.LightFlag
import com.boy0000.modernlightapi.bukkit.internal.BukkitPlatformImpl
import com.boy0000.modernlightapi.internal.PlatformType
import com.boy0000.modernlightapi.internal.chunks.data.BitChunkData
import com.boy0000.modernlightapi.internal.chunks.data.IChunkData
import com.boy0000.modernlightapi.internal.engine.LightEngineType
import com.boy0000.modernlightapi.internal.engine.LightEngineVersion
import com.boy0000.modernlightapi.internal.utils.FlagUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Levelled
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent

class CompatibilityHandler : IHandler {
    private var mPlatform: BukkitPlatformImpl? = null
    private val platformImpl: BukkitPlatformImpl?
        get() = mPlatform

    @Throws(Exception::class)
    override fun onInitialization(impl: BukkitPlatformImpl?) {
        mPlatform = impl
    }

    override fun onShutdown(impl: BukkitPlatformImpl?) {}
    override val platformType: PlatformType
        get() = PlatformType.BUKKIT
    override val lightEngineType: LightEngineType
        get() = LightEngineType.COMPATIBILITY
    override val lightEngineVersion: LightEngineVersion
        get() = LightEngineVersion.UNKNOWN
    override val isMainThread: Boolean
        get() = Bukkit.isPrimaryThread()

    override fun onWorldLoad(event: WorldLoadEvent?) {}
    override fun onWorldUnload(event: WorldUnloadEvent?) {}
    override fun isLightingSupported(world: World?, lightFlags: Int): Boolean {
        return FlagUtils.isFlagSet(lightFlags, LightFlag.BLOCK_LIGHTING)
    }

    private fun setLightBlock(block: Block, finalLightLevel: Int) {
        val newMaterial = if (finalLightLevel > 0) Material.LIGHT else Material.AIR
        block.type = newMaterial
        if (newMaterial == Material.LIGHT) {
            val level: Levelled = Material.LIGHT.createBlockData() as Levelled
            level.level = finalLightLevel
            block.setBlockData(level, true)
        }
    }

    private fun setRawLightLevelLocked(
        world: World, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int,
        lightFlags: Int
    ): Int {
        if (!isLightingSupported(world, lightFlags)) {
            return ResultCode.NOT_IMPLEMENTED
        }
        val finalLightLevel = if (lightLevel < 0) 0 else Math.min(lightLevel, 15)
        val block: Block = world.getBlockAt(blockX, blockY, blockZ)
        val material = block.type
        if (material.isAir || material == Material.LIGHT) {
            setLightBlock(block, finalLightLevel)
        } else {
            for (side in SIDES) {
                val sideBlock = block.getRelative(side)
                if (sideBlock.type.isAir || sideBlock.type == Material.LIGHT) {
                    setLightBlock(sideBlock, Math.max(finalLightLevel - 1, 0))
                }
            }
        }
        return ResultCode.SUCCESS
    }

    override fun setRawLightLevel(
        world: World,
        blockX: Int,
        blockY: Int,
        blockZ: Int,
        lightLevel: Int,
        lightFlags: Int
    ): Int {
        return if (isMainThread) {
            setRawLightLevelLocked(world, blockX, blockY, blockZ, lightLevel, lightFlags)
        } else {
            platformImpl?.plugin?.let {
                Bukkit.getScheduler().runTask(it,
                    Runnable { setRawLightLevelLocked(world, blockX, blockY, blockZ, lightLevel, lightFlags) })
            }
            ResultCode.SUCCESS
        }
    }

    private fun getLightFromBlock(block: Block, lightFlags: Int): Int {
        val lightLevel = -1
        if (FlagUtils.isFlagSet(lightFlags, LightFlag.BLOCK_LIGHTING) && FlagUtils.isFlagSet(
                lightFlags,
                LightFlag.SKY_LIGHTING
            )
        ) {
            return block.lightLevel.toInt()
        } else if (FlagUtils.isFlagSet(lightFlags, LightFlag.BLOCK_LIGHTING)) {
            return block.lightFromBlocks.toInt()
        } else if (FlagUtils.isFlagSet(lightFlags, LightFlag.SKY_LIGHTING)) {
            return block.lightFromSky.toInt()
        }
        return lightLevel
    }

    override fun getRawLightLevel(world: World, blockX: Int, blockY: Int, blockZ: Int, lightFlags: Int): Int {
        val block: Block = world.getBlockAt(blockX, blockY, blockZ)
        val material = block.type
        var lightLevel = 0
        if (material.isAir || material == Material.LIGHT) {
            lightLevel = getLightFromBlock(block, lightFlags)
        } else {
            for (side in SIDES) {
                val sideBlock = block.getRelative(side)
                if (sideBlock.type.isAir || sideBlock.type == Material.LIGHT) {
                    val blockLightLevel = getLightFromBlock(sideBlock, lightFlags)
                    if (blockLightLevel > lightLevel) {
                        lightLevel = blockLightLevel
                    }
                }
            }
        }
        return lightLevel
    }

    override fun recalculateLighting(world: World?, blockX: Int, blockY: Int, blockZ: Int, lightFlags: Int): Int {
        return ResultCode.NOT_IMPLEMENTED
    }

    override fun createChunkData(worldName: String?, chunkX: Int, chunkZ: Int): IChunkData {
        return BitChunkData(worldName.toString(), chunkX, chunkZ, 0, 0)
    }

    override fun collectChunkSections(
        world: World?, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int,
        lightFlags: Int
    ): List<IChunkData> {
        return ArrayList()
    }

    override fun isValidChunkSection(world: World?, sectionY: Int): Boolean {
        return true
    }

    override fun sendChunk(data: IChunkData?): Int {
        platformImpl?.debug("sendChunk: Not implemented for compatibility mode")
        return ResultCode.NOT_IMPLEMENTED
    }

    override fun sendCmd(cmdId: Int, vararg args: Any?): Int {
        platformImpl?.debug("sendCmd: Not implemented for compatibility mode")
        return ResultCode.NOT_IMPLEMENTED
    }

    companion object {
        private val SIDES: Array<BlockFace> = arrayOf<BlockFace>(
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST
        )
    }
}
