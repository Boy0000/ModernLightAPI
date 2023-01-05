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
package com.boy0000.modernlightapi.internal.service

import com.boy0000.modernlightapi.internal.IPlatformImpl
import java.util.concurrent.*

abstract class BackgroundServiceImpl(platform: IPlatformImpl) : IBackgroundService {
    private var executorService: ScheduledExecutorService? = null
    private val mPlatform: IPlatformImpl

    init {
        mPlatform = platform
    }

    protected open val platformImpl: IPlatformImpl
        get() = mPlatform

    protected fun configureExecutorService(corePoolSize: Int, namedThreadFactory: ThreadFactory?) {
        if (executorService == null) {
            executorService = Executors.newScheduledThreadPool(corePoolSize, namedThreadFactory)
        }
    }

    protected fun getExecutorService(): ScheduledExecutorService? {
        return executorService
    }

    override fun onShutdown() {
        if (getExecutorService() != null) {
            getExecutorService()?.shutdown()
            try {
                if (!getExecutorService()?.awaitTermination(10, TimeUnit.SECONDS)!!) {
                    platformImpl.info("Still waiting after 10 seconds: Shutdown now.")
                    getExecutorService()?.shutdownNow()
                }
            } catch (e: InterruptedException) {
                // (Re-)Cancel if current thread also interrupted
                getExecutorService()?.shutdownNow()
                // Preserve interrupt status
                Thread.currentThread().interrupt()
            }
        }
    }

    override fun scheduleWithFixedDelay(
        runnable: Runnable?,
        initialDelay: Int,
        delay: Int,
        unit: TimeUnit?
    ): ScheduledFuture<*>? {
        return getExecutorService()!!.scheduleWithFixedDelay(runnable, initialDelay.toLong(), delay.toLong(), unit)
    }
}
