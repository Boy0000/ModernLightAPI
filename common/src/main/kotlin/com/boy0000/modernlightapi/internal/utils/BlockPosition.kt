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
package com.boy0000.modernlightapi.internal.utils

class BlockPosition(var blockX: Int, var blockY: Int, var blockZ: Int) {

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + blockX
        result = prime * result + blockY
        result = prime * result + blockZ
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj == null) {
            return false
        }
        if (javaClass != obj.javaClass) {
            return false
        }
        val other = obj as BlockPosition
        if (blockX != other.blockX) {
            return false
        }
        return if (blockY != other.blockY) {
            false
        } else blockZ == other.blockZ
    }

    override fun toString(): String {
        return "BlockPosition [blockX=$blockX, blockY=$blockY, blockZ=$blockZ]"
    }

    companion object {
        // from NMS 1.15.2
        private const val SIZE_BITS_X = 26
        private const val SIZE_BITS_Z = 26
        private const val SIZE_BITS_Y = 12
        private const val BITS_X: Long = 0x3FFFFFF // hex to dec: 67108863
        private const val BITS_Y: Long = 0xFFF // hex to dec: 4095
        private const val BITS_Z: Long = 0x3FFFFFF // hex to dec: 67108863
        private const val BIT_SHIFT_Z = 12
        private const val BIT_SHIFT_X = 38
        /*@JvmOverloads
        fun asLong(
            blockX: Int = blockX,
            blockY: Int = this.blockY(),
            blockZ: Int = this.blockZ()
        ): Long {
            var long4 = 0L
            long4 = long4 or (blockX.toLong() and BITS_X shl BIT_SHIFT_X)
            long4 = long4 or (blockY.toLong() and BITS_Y shl 0)
            long4 = long4 or (blockZ.toLong() and BITS_Z shl BIT_SHIFT_Z)
            return long4
        }*/

        fun unpackLongX(long1: Long): Int {
            return (long1 shl 64 - BIT_SHIFT_X - SIZE_BITS_X shr 64 - SIZE_BITS_X).toInt()
        }

        fun unpackLongY(long1: Long): Int {
            return (long1 shl 64 - SIZE_BITS_Y shr 64 - SIZE_BITS_Y).toInt()
        }

        fun unpackLongZ(long1: Long): Int {
            return (long1 shl 64 - BIT_SHIFT_Z - SIZE_BITS_Z shr 64 - SIZE_BITS_Z).toInt()
        }

        fun fromLong(value: Long): BlockPosition {
            return BlockPosition(unpackLongX(value), unpackLongY(value), unpackLongZ(value))
        }
    }
}
