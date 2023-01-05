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

import com.boy0000.modernlightapi.api.ResultCode
import com.boy0000.modernlightapi.api.engine.EditPolicy
import com.boy0000.modernlightapi.api.engine.LightFlag
import com.boy0000.modernlightapi.api.engine.SendPolicy
import com.boy0000.modernlightapi.api.engine.sched.ICallback
import com.boy0000.modernlightapi.api.extension.IExtension
import com.boy0000.modernlightapi.internal.IPlatformImpl
import com.boy0000.modernlightapi.internal.InternalCode
import com.boy0000.modernlightapi.internal.PlatformType
import com.boy0000.modernlightapi.internal.chunks.observer.IChunkObserver
import com.boy0000.modernlightapi.internal.engine.ILightEngine
import com.boy0000.modernlightapi.internal.engine.sched.RequestFlag
import com.boy0000.modernlightapi.internal.service.IBackgroundService
import java.util.*

/**
 * Main class for all platforms. Contains basic methods for all implementations.
 *
 * @author BeYkeRYkt
 */
open class ModernLightAPI private constructor(internal: IPlatformImpl) {
    private val mInternal: IPlatformImpl

    init {
        if (singleton != null) {
            throw RuntimeException("Use get() method to get the single instance of this class.")
        }
        mInternal = internal
    }

    val isInitialized: Boolean
        get() = pluginImpl.isInitialized
    private val pluginImpl: IPlatformImpl
        get() {
            checkNotNull(get().mInternal) { "ModernLightAPI not yet initialized! Use prepare() !" }
            return get().mInternal
        }
    private val lightEngine: ILightEngine
        get() {
            checkNotNull(pluginImpl.lightEngine) { "LightEngine not yet initialized!" }
            return pluginImpl.lightEngine!!
        }
    private val chunkObserver: IChunkObserver
        get() {
            checkNotNull(pluginImpl.chunkObserver) { "ChunkObserver not yet initialized!" }
            return pluginImpl.chunkObserver!!
        }
    private val backgroundService: IBackgroundService
        get() {
            checkNotNull(pluginImpl.backgroundService) { "BackgroundService not yet initialized!" }
            return pluginImpl.backgroundService!!
        }
    val extension: IExtension?
        get() = pluginImpl.extension

    protected fun log(msg: String?) {
        pluginImpl.info(msg)
    }

    val platformType: PlatformType
        /**
         * Platform that is being used
         *
         * @return One of the proposed options from [PlatformType]
         */
        get() = if (singleton == null) {
            PlatformType.UNKNOWN
        } else pluginImpl.platformType!!

    /**
     * Gets the level of light from given coordinates with specific flags.
     */
    fun getLightLevel(worldName: String?, blockX: Int, blockY: Int, blockZ: Int): Int {
        return getLightLevel(worldName, blockX, blockY, blockZ, LightFlag.BLOCK_LIGHTING)
    }

    /**
     * Gets the level of light from given coordinates with specific flags.
     */
    fun getLightLevel(worldName: String?, blockX: Int, blockY: Int, blockZ: Int, lightFlags: Int): Int {
        return lightEngine.getLightLevel(worldName, blockX, blockY, blockZ, lightFlags)
    }

    /**
     * Placement of a specific type of light with a given level of illumination in the named world in
     * certain coordinates with the return code result.
     */
    fun setLightLevel(worldName: String?, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int): Int {
        return setLightLevel(
            worldName, blockX, blockY, blockZ, lightLevel, LightFlag.BLOCK_LIGHTING,
            EditPolicy.DEFERRED, SendPolicy.DEFERRED, null
        )
    }

    /**
     * Placement of a specific type of light with a given level of illumination in the named world in
     * certain coordinates with the return code result.
     */
    fun setLightLevel(
        worldName: String?,
        blockX: Int,
        blockY: Int,
        blockZ: Int,
        lightLevel: Int,
        lightFlags: Int
    ): Int {
        return setLightLevel(
            worldName, blockX, blockY, blockZ, lightLevel, lightFlags, EditPolicy.DEFERRED,
            SendPolicy.DEFERRED, null
        )
    }

    /**
     * Placement of a specific type of light with a given level of illumination in the named world in
     * certain coordinates with the return code result.
     */
    fun setLightLevel(
        worldName: String?, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int, lightFlags: Int,
        callback: ICallback
    ): Int {
        return setLightLevel(
            worldName, blockX, blockY, blockZ, lightLevel, lightFlags, EditPolicy.DEFERRED,
            SendPolicy.DEFERRED, callback
        )
    }

    /**
     * Placement of a specific type of light with a given level of illumination in the named world in
     * certain coordinates with the return code result.
     */
    fun setLightLevel(
        worldName: String?, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int, lightFlags: Int,
        editPolicy: EditPolicy?, sendPolicy: SendPolicy?, callback: ICallback?
    ): Int {
        return lightEngine.setLightLevel(
            worldName, blockX, blockY, blockZ, lightLevel, lightFlags, editPolicy,
            sendPolicy, callback
        )
    }

    /**
     * Checks the light level and restores it if available.
     */
    @Deprecated(
        "", ReplaceWith(
            "checkLight(worldName, blockX, blockY, blockZ, LightFlag.BLOCK_LIGHTING)",
            "com.boy0000.modernlightapi.api.engine.LightFlag"
        )
    )
    fun checkLight(worldName: String?, blockX: Int, blockY: Int, blockZ: Int): Int {
        return checkLight(worldName, blockX, blockY, blockZ, LightFlag.BLOCK_LIGHTING)
    }

    /**
     * Checks the light level and restores it if available.
     */
    @Deprecated("")
    fun checkLight(worldName: String?, blockX: Int, blockY: Int, blockZ: Int, lightFlags: Int): Int {
        return lightEngine.checkLight(worldName, blockX, blockY, blockZ, lightFlags)
    }

    /**
     * Send specific commands for implementation
     */
    fun sendCmd(cmdId: Int, vararg args: Any?): Int {
        if (cmdId <= InternalCode.RESERVED_LENGTH) {
            pluginImpl.error("$cmdId is reserved for internal use.")
            return ResultCode.FAILED
        }
        return pluginImpl.sendCmd(cmdId, args)
    }

    companion object {
        @Volatile
        private var singleton: ModernLightAPI? = null

        /**
         * Must be called in onLoad();
         */
        @Throws(Exception::class)
        fun prepare(impl: IPlatformImpl) {
            if (singleton == null || !get().isInitialized) {
                impl.info("Preparing ModernLightAPI...")
                synchronized(ModernLightAPI::class.java) {
                    if (singleton == null) {
                        val initCode: Int = impl.prepare()
                        if (initCode == ResultCode.SUCCESS) {
                            singleton = ModernLightAPI(impl)
                            impl.info("Preparing done!")
                        } else {
                            throw IllegalStateException("Preparing failed! Code: $initCode")
                        }
                    }
                }
            }
        }

        /**
         * Must be called in onEnable();
         */
        @Throws(Exception::class)
        fun initialization() {
            if (!get().isInitialized) {
                get().log("Initializing ModernLightAPI...")
                synchronized(ModernLightAPI::class.java) {
                    val initCode: Int = get().pluginImpl.initialization()
                    if (initCode == ResultCode.SUCCESS) {
                        // send random generated uuid
                        val uuid = UUID.randomUUID()
                        get().pluginImpl.sendCmd(InternalCode.UPDATE_UUID, uuid)
                        get().log("ModernLightAPI initialized!")
                    } else {
                        // Initialization failed
                        throw IllegalStateException("Initialization failed! Code: $initCode")
                    }
                }
            }
        }

        /**
         * Must be called in onDisable();
         */
        fun shutdown(impl: IPlatformImpl) {
            if (!get().isInitialized || get().isInitialized && get().pluginImpl.uuid == impl.uuid
            ) {
                get().log("Shutdown ModernLightAPI...")
                synchronized(ModernLightAPI::class.java) { get().pluginImpl.shutdown() }
            } else {
                get().log("Disabling the plugin is allowed only to the implementation class")
            }
        }

        /**
         * The global [ModernLightAPI] instance.
         */
        fun get(): ModernLightAPI {
            checkNotNull(singleton) { "Singleton not yet initialized! Use prepare() !" }
            return singleton as ModernLightAPI
        }

        @Deprecated("")
        fun createLight(
            worldName: String?, type: LightType,
            blockX: Int, blockY: Int, blockZ: Int, lightlevel: Int
        ): Boolean {
            return createLight(worldName, type, blockX, blockY, blockZ, lightlevel, null)
        }

        @Deprecated("")
        fun createLight(
            worldName: String?, type: LightType,
            blockX: Int, blockY: Int, blockZ: Int, lightlevel: Int, callback: LCallback?
        ): Boolean {
            var lightFlags: Int = LightFlag.BLOCK_LIGHTING
            if (type === LightType.SKY) {
                lightFlags = LightFlag.SKY_LIGHTING
            }
            val resultCode = get()
                .setLightLevel(worldName, blockX, blockY, blockZ, lightlevel, lightFlags,
                    EditPolicy.DEFERRED, SendPolicy.DEFERRED, object : ICallback {
                        override fun onResult(requestFlag: Int, resultCode: Int) {
                            if (callback != null) {
                                var stage = LStage.CREATING
                                when (requestFlag) {
                                    RequestFlag.EDIT -> stage = LStage.WRITTING
                                    RequestFlag.RECALCULATE -> stage = LStage.RECALCULATING
                                }
                                if (resultCode == ResultCode.SUCCESS || resultCode == ResultCode.MOVED_TO_DEFERRED) {
                                    callback.onSuccess(worldName, type, blockX, blockY, blockZ, lightlevel, stage)
                                } else {
                                    var reason = LReason.UNKNOWN
                                    when (resultCode) {
                                        ResultCode.RECALCULATE_NO_CHANGES -> reason = LReason.NO_LIGHT_CHANGES
                                    }
                                    callback.onFailed(
                                        worldName,
                                        type,
                                        blockX,
                                        blockY,
                                        blockZ,
                                        lightlevel,
                                        stage,
                                        reason
                                    )
                                }
                            }
                        }
                    })
            return resultCode == ResultCode.SUCCESS
        }

        @Deprecated("", ReplaceWith(
            "deleteLight(worldName, type, blockX, blockY, blockZ, null)",
            "com.boy0000.modernlightapi.ModernLightAPI.Companion.deleteLight"
        )
        )
        fun deleteLight(
            worldName: String?, type: LightType,
            blockX: Int, blockY: Int, blockZ: Int
        ): Boolean {
            return deleteLight(worldName, type, blockX, blockY, blockZ, null)
        }

        @Deprecated("")
        fun deleteLight(
            worldName: String?, type: LightType,
            blockX: Int, blockY: Int, blockZ: Int, callback: LCallback?
        ): Boolean {
            var lightFlags: Int = LightFlag.BLOCK_LIGHTING
            if (type === LightType.SKY) {
                lightFlags = LightFlag.SKY_LIGHTING
            }
            val resultCode = get()
                .setLightLevel(worldName, blockX, blockY, blockZ, 0, lightFlags, EditPolicy.DEFERRED,
                    SendPolicy.DEFERRED, object : ICallback {
                        override fun onResult(requestFlag: Int, resultCode: Int) {
                            if (callback != null) {
                                var stage = LStage.DELETING
                                when (requestFlag) {
                                    RequestFlag.EDIT -> stage = LStage.WRITTING
                                    RequestFlag.RECALCULATE -> stage = LStage.RECALCULATING
                                }
                                if (resultCode == ResultCode.SUCCESS || resultCode == ResultCode.MOVED_TO_DEFERRED) {
                                    callback.onSuccess(worldName, type, blockX, blockY, blockZ, 0, stage)
                                } else {
                                    var reason = LReason.UNKNOWN
                                    when (resultCode) {
                                        ResultCode.RECALCULATE_NO_CHANGES -> reason = LReason.NO_LIGHT_CHANGES
                                    }
                                    callback.onFailed(worldName, type, blockX, blockY, blockZ, 0, stage, reason)
                                }
                            }
                        }
                    })
            return resultCode == ResultCode.SUCCESS
        }

        @Deprecated("", ReplaceWith("ArrayList()", "java.util.ArrayList"))
        fun collectChunks(
            worldName: String?,
            blockX: Int,
            blockY: Int,
            blockZ: Int,
            lightlevel: Int
        ): List<IChunkData> {
            // Let's not do this , all right?
            return ArrayList()
        }

        @Deprecated("", ReplaceWith(
            "collectChunks(worldName, blockX, blockY, blockZ, 15)",
            "com.boy0000.modernlightapi.ModernLightAPI.Companion.collectChunks"
        )
        )
        fun collectChunks(worldName: String?, blockX: Int, blockY: Int, blockZ: Int): List<IChunkData> {
            return collectChunks(worldName, blockX, blockY, blockZ, 15)
        }

        @Deprecated("")
        fun sendChanges(worldName: String?, chunkX: Int, chunkZ: Int, playerName: String?) {
        }

        @Deprecated("")
        fun sendChanges(worldName: String?, chunkX: Int, blockY: Int, chunkZ: Int, playerName: String?) {
        }

        @Deprecated("")
        fun sendChanges(chunkData: IChunkData?, playerName: String?) {
        }

        @Deprecated("")
        fun sendChanges(worldName: String?, chunkX: Int, chunkZ: Int) {
        }

        @Deprecated("")
        fun sendChanges(worldName: String?, chunkX: Int, blockY: Int, chunkZ: Int) {
        }

        @Deprecated("")
        fun sendChanges(chunkData: IChunkData?) {
        }
    }
}
