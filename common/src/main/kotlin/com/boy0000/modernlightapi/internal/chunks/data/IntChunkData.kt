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


open class IntChunkData(
    worldName: String,
    chunkX: Int,
    chunkZ: Int,
    var skyLightUpdateBits: Int,
    var blockLightUpdateBits: Int
) : ChunkData(worldName, chunkX, chunkZ) {
    override fun markSectionForUpdate(lightFlags: Int, sectionY: Int) {
        if (FlagUtils.isFlagSet(lightFlags, LightFlag.SKY_LIGHTING)) {
            skyLightUpdateBits = skyLightUpdateBits or (1 shl sectionY + 1)
        }
        if (FlagUtils.isFlagSet(lightFlags, LightFlag.BLOCK_LIGHTING)) {
            blockLightUpdateBits = blockLightUpdateBits or (1 shl sectionY + 1)
        }
    }

    override fun clearUpdate() {
        skyLightUpdateBits = 0
        blockLightUpdateBits = 0
    }

    override fun setFullSections() {
        skyLightUpdateBits = FULL_MASK
        blockLightUpdateBits = FULL_MASK
    }

    override fun toString(): String {
        return ("IntChunkData{" + "worldName=" + worldName + ", chunkX=" + chunkX + ", chunkZ=" + chunkZ
                + ", skyLightUpdateBits=" + skyLightUpdateBits + ", blockLightUpdateBits=" + blockLightUpdateBits + '}')
    }

    companion object {
        const val FULL_MASK = 0x1ffff
    }
}
