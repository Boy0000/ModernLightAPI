/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Qveshn
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
package com.boy0000.modernlightapi.bukkit.internal.utils

import org.bukkit.Bukkit
import java.util.regex.Pattern

object VersionUtil {
    fun serverVersion(): String {
        return Bukkit.getServer().javaClass.getPackage().name.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()[3]
    }

    fun serverName(): String {
        return Bukkit.getVersion().split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
    }

    fun bukkitName(): String {
        return Bukkit.getName()
    }

    fun bukkitVersion(): String {
        return (Bukkit.getBukkitVersion() + "-").split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
    }

    private fun paddedVersion(value: String): String {
        return leftPad(value, "\\d+", '0', 8)
    }

    private fun paddedBukkitVersion(): String {
        return paddedVersion(bukkitVersion())
    }

    fun compareBukkitVersionTo(version: String): Int {
        return paddedBukkitVersion().compareTo(paddedVersion(version))
    }

    private fun leftPad(text: String, regex: String, padCharacter: Char, width: Int): String {
        val sb = StringBuilder()
        val m = Pattern.compile(regex).matcher(text)
        val chars = String.format("%" + width + "s", "").replace(' ', padCharacter)
        var last = 0
        while (m.find()) {
            val start = m.start()
            val n = m.end() - start
            if (n < width) {
                sb.append(text, last, start)
                sb.append(chars, n, width)
                last = start
            }
        }
        if (last == 0) {
            return text
        }
        if (last < text.length) {
            sb.append(text, last, text.length)
        }
        return sb.toString()
    }
}
