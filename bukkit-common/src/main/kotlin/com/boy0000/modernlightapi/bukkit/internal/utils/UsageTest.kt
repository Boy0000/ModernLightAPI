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
package com.boy0000.modernlightapi.bukkit.internal.utils

import com.boy0000.modernlightapi.ModernLightAPI
import com.boy0000.modernlightapi.api.ResultCode
import com.boy0000.modernlightapi.api.engine.EditPolicy
import com.boy0000.modernlightapi.api.engine.LightFlag
import com.boy0000.modernlightapi.api.engine.SendPolicy
import com.boy0000.modernlightapi.api.engine.sched.ICallback
import com.boy0000.modernlightapi.internal.engine.sched.RequestFlag


class UsageTest {
    private fun setLightLevel(world: String, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int) {
        val lightTypeFlags: Int = LightFlag.BLOCK_LIGHTING
        val oldBlockLight: Int = ModernLightAPI.get().getLightLevel(world, blockX, blockY, blockZ, lightTypeFlags)
        val editPolicy: EditPolicy = EditPolicy.DEFERRED
        val sendPolicy: SendPolicy = SendPolicy.DEFERRED
        ModernLightAPI.get().setLightLevel(world, blockX, blockY, blockZ, lightLevel, lightTypeFlags, editPolicy, sendPolicy,
            object : ICallback {
                override fun onResult(requestFlag: Int, resultCode: Int) {
                    when (requestFlag) {
                        RequestFlag.EDIT -> if (resultCode == ResultCode.SUCCESS) {
                            println("Light level was edited.")
                        } else {
                            println("request ($requestFlag): result code $resultCode")
                        }

                        RequestFlag.RECALCULATE -> if (resultCode == ResultCode.SUCCESS) {
                            val newBlockLight: Int = ModernLightAPI.get().getLightLevel(
                                world, blockX, blockY, blockZ,
                                lightTypeFlags
                            )
                            if (oldBlockLight != newBlockLight) {
                                println("Light level was recalculated.")
                            } else {
                                println("Light level was not recalculated.")
                            }
                        } else {
                            println("request ($requestFlag): result code $resultCode")
                        }

                        RequestFlag.SEPARATE_SEND -> println("request ($requestFlag): result code $resultCode")
                    }
                }
            })
    }
}
