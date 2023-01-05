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
package com.boy0000.modernlightapi.internal.chunks.data

import com.boy0000.modernlightapi.api.engine.LightFlag
import com.boy0000.modernlightapi.internal.utils.FlagUtils
import java.util.*

/**
 * ChunkData with BitSet data
 */
class BitChunkData(
    worldName: String, chunkX: Int, chunkZ: Int,
    /**
     * Max chunk section
     */
    // getHeight() - this.world.countVerticalSections() + 2;
    // getTopY() - this.getBottomY() + this.getHeight();
    val topSection: Int,
    /**
     * Min chunk section
     */
    // getBottomY() - this.world.getBottomSectionCoord() - 1
    val bottomSection: Int
) : ChunkData(worldName, chunkX, chunkZ) {
    val skyLightUpdateBits: BitSet
    val blockLightUpdateBits: BitSet

    init {
        skyLightUpdateBits = BitSet(DEFAULT_SIZE)
        blockLightUpdateBits = BitSet(DEFAULT_SIZE)
    }

    override fun markSectionForUpdate(lightFlags: Int, sectionY: Int) {
        val minY = bottomSection
        val maxY = topSection
        if (sectionY < minY || sectionY > maxY) {
            return
        }
        val l = sectionY - minY
        if (FlagUtils.isFlagSet(lightFlags, LightFlag.SKY_LIGHTING)) {
            skyLightUpdateBits.set(l)
        }
        if (FlagUtils.isFlagSet(lightFlags, LightFlag.BLOCK_LIGHTING)) {
            blockLightUpdateBits.set(l)
        }
    }

    override fun clearUpdate() {
        skyLightUpdateBits.clear()
        blockLightUpdateBits.clear()
    }

    override fun setFullSections() {
        // TODO: Mark full sections
        for (i in bottomSection until topSection) {
            markSectionForUpdate(LightFlag.SKY_LIGHTING or LightFlag.BLOCK_LIGHTING, i)
        }
    }

    override fun toString(): String {
        return ("BitChunkData{" + "worldName=" + worldName + ", chunkX=" + chunkX + ", chunkZ=" + chunkZ
                + ", skyLightUpdateBits=" + skyLightUpdateBits + ", blockLightUpdateBits=" + blockLightUpdateBits + '}')
    }

    companion object {
        private const val DEFAULT_SIZE = 2048
    }
}
