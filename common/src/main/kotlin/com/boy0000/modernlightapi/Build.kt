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

import com.boy0000.modernlightapi.internal.PlatformType

/**
 * Information about the current ModernLightAPI build
 */
object Build {
    /**
     * ONLY FOR PREVIEW BUILD
     */
    @Deprecated("")
    val PREVIEW = 0

    /**
     * Public version for users. May change during any changes in the API. The string should change
     * when common 'api' package is changed in from release to release.
     */
    const val API_VERSION = 1

    /**
     * [Build.API_VERSION]
     */
    @Deprecated("")
    val CURRENT_VERSION = API_VERSION

    /**
     * Internal version. May change during any changes in the API. The string should change when
     * common 'internal' package is changed from release to release.
     */
    const val INTERNAL_VERSION = 4

    /**
     * Platform that is being used.
     */
    var CURRENT_IMPLEMENTATION: PlatformType = ModernLightAPI.Companion.get().platformType
}
