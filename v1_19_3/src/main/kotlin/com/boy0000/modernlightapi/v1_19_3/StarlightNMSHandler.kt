package com.boy0000.modernlightapi.v1_19_3

import ca.spottedleaf.starlight.common.light.BlockStarLightEngine
import ca.spottedleaf.starlight.common.light.SkyStarLightEngine
import ca.spottedleaf.starlight.common.light.StarLightEngine
import ca.spottedleaf.starlight.common.light.StarLightInterface
import com.boy0000.modernlightapi.api.ResultCode
import com.boy0000.modernlightapi.api.engine.LightFlag
import com.boy0000.modernlightapi.bukkit.internal.BukkitPlatformImpl
import com.boy0000.modernlightapi.internal.engine.LightEngineType
import com.boy0000.modernlightapi.internal.utils.FlagUtils
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ThreadedLevelLightEngine
import net.minecraft.util.thread.ProcessorMailbox
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase
import net.minecraft.world.level.chunk.LightChunkGetter
import org.bukkit.World
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

open class StarlightNMSHandler : VanillaNMSHandler() {
    private val ALL_DIRECTIONS_BITSET = (1 shl 6) - 1
    private val FLAG_HAS_SIDED_TRANSPARENT_BLOCKS = Long.MIN_VALUE
    private val blockQueueMap: MutableMap<ChunkPos, MutableSet<LightPos>> = ConcurrentHashMap()
    private val skyQueueMap: MutableMap<ChunkPos, MutableSet<LightPos>> = ConcurrentHashMap()

    // StarLightInterface
    private var starInterface: Field? = null
    private var starInterface_coordinateOffset: Field? = null
    private var starInterface_getBlockLightEngine: Method? = null
    private var starInterface_getSkyLightEngine: Method? = null

    // StarLightEngine
    private var starEngine_setLightLevel: Method? = null
    private var starEngine_appendToIncreaseQueue: Method? = null
    private var starEngine_appendToDecreaseQueue: Method? = null
    private var starEngine_performLightIncrease: Method? = null
    private var starEngine_performLightDecrease: Method? = null
    private var starEngine_updateVisible: Method? = null
    private var starEngine_setupCaches: Method? = null
    private var starEngine_destroyCaches: Method? = null
    private fun scheduleChunkLight(
        starLightInterface: StarLightInterface, chunkCoordIntPair: ChunkPos,
        runnable: Runnable
    ) {
        starLightInterface.scheduleChunkLight(chunkCoordIntPair, runnable)
    }

    private fun addTaskToQueue(
        worldServer: ServerLevel, starLightInterface: StarLightInterface, sle: StarLightEngine,
        chunkCoordIntPair: ChunkPos, lightPoints: MutableSet<LightPos>
    ) {
        val type: Int = if (sle is BlockStarLightEngine) LightFlag.BLOCK_LIGHTING else LightFlag.SKY_LIGHTING
        scheduleChunkLight(starLightInterface, chunkCoordIntPair) {
            try {
                val chunkX = chunkCoordIntPair.x
                val chunkZ = chunkCoordIntPair.z
                if (!worldServer.getChunkSource().isChunkLoaded(chunkX, chunkZ)) {
                    return@scheduleChunkLight
                }

                // blocksChangedInChunk -- start
                // setup cache
                starEngine_setupCaches!!.invoke(
                    sle, worldServer.getChunkSource(), chunkX * 16 + 7, 128, chunkZ * 16 + 7,
                    true, true
                )
                try {
                    // propagateBlockChanges -- start
                    val it = lightPoints.iterator()
                    while (it.hasNext()) {
                        try {
                            val lightPos = it.next()
                            val blockPos = lightPos.blockPos
                            val lightLevel = lightPos.lightLevel
                            val currentLightLevel = getRawLightLevel(
                                worldServer.world, blockPos.x,
                                blockPos.y, blockPos.z, type
                            )
                            if (lightLevel <= currentLightLevel) {
                                // do nothing
                                continue
                            }
                            val encodeOffset = starInterface_coordinateOffset!!.getInt(sle)
                            val blockData: BlockStateBase = worldServer.getBlockState(blockPos)
                            starEngine_setLightLevel!!.invoke(
                                sle, blockPos.x, blockPos.y, blockPos.z,
                                lightLevel
                            )
                            if (lightLevel != 0) {
                                starEngine_appendToIncreaseQueue!!.invoke(
                                    sle,
                                    (blockPos.x + (blockPos.z shl 6) + (blockPos.y shl 6 + 6)
                                            + encodeOffset).toLong() and (1L shl 6 + 6 + 16) - 1 or (lightLevel.toLong() and 0xFL shl 6 + 6 + 16) or (ALL_DIRECTIONS_BITSET.toLong() shl 6 + 6 + 16 + 4) or if (blockData.isConditionallyFullOpaque) FLAG_HAS_SIDED_TRANSPARENT_BLOCKS else 0
                                )
                            }
                        } finally {
                            it.remove()
                        }
                    }
                    starEngine_performLightIncrease!!.invoke(sle, worldServer.getChunkSource())
                    // propagateBlockChanges -- end
                    starEngine_updateVisible!!.invoke(sle, worldServer.getChunkSource())
                } finally {
                    starEngine_destroyCaches!!.invoke(sle)
                }
                // blocksChangedInChunk -- end
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun executeSync(lightEngine: ThreadedLevelLightEngine?, task: Runnable) {
        if (isMainThread()) {
            task.run()
        } else {
            try {
                val future: CompletableFuture<Void?> = CompletableFuture<Void?>()
                val threadedMailbox = lightEngine_ThreadedMailbox!![lightEngine] as ProcessorMailbox<Runnable>
                threadedMailbox.tell(Runnable {
                    task.run()
                    future.complete(null)
                })
                future.join()
            } catch (e: IllegalAccessException) {
                throw toRuntimeException(e)
            }
        }
    }

    @Throws(Exception::class)
    override fun onInitialization(impl: BukkitPlatformImpl) {
        super.onInitialization(impl)
        try {
            starEngine_setLightLevel = StarLightEngine::class.java.getDeclaredMethod(
                "setLightLevel",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            starEngine_setLightLevel!!.isAccessible = true
            starEngine_appendToIncreaseQueue = StarLightEngine::class.java.getDeclaredMethod(
                "appendToIncreaseQueue",
                Long::class.javaPrimitiveType
            )
            starEngine_appendToIncreaseQueue!!.isAccessible = true
            starEngine_appendToDecreaseQueue = StarLightEngine::class.java.getDeclaredMethod(
                "appendToDecreaseQueue",
                Long::class.javaPrimitiveType
            )
            starEngine_appendToDecreaseQueue!!.isAccessible = true
            starEngine_performLightIncrease = StarLightEngine::class.java.getDeclaredMethod(
                "performLightIncrease",
                LightChunkGetter::class.java
            )
            starEngine_performLightIncrease!!.isAccessible = true
            starEngine_performLightDecrease = StarLightEngine::class.java.getDeclaredMethod(
                "performLightDecrease",
                LightChunkGetter::class.java
            )
            starEngine_performLightDecrease!!.isAccessible = true
            starEngine_updateVisible =
                StarLightEngine::class.java.getDeclaredMethod("updateVisible", LightChunkGetter::class.java)
            starEngine_updateVisible!!.isAccessible = true
            starEngine_setupCaches = StarLightEngine::class.java.getDeclaredMethod(
                "setupCaches",
                LightChunkGetter::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType
            )
            starEngine_setupCaches!!.isAccessible = true
            starEngine_destroyCaches = StarLightEngine::class.java.getDeclaredMethod("destroyCaches")
            starEngine_destroyCaches!!.isAccessible = true
            starInterface = ThreadedLevelLightEngine::class.java.getDeclaredField("theLightEngine")
            starInterface!!.isAccessible = true
            starInterface_getBlockLightEngine = StarLightInterface::class.java.getDeclaredMethod("getBlockLightEngine")
            starInterface_getBlockLightEngine!!.isAccessible = true
            starInterface_getSkyLightEngine = StarLightInterface::class.java.getDeclaredMethod("getSkyLightEngine")
            starInterface_getSkyLightEngine!!.isAccessible = true
            starInterface_coordinateOffset = StarLightEngine::class.java.getDeclaredField("coordinateOffset")
            starInterface_coordinateOffset!!.isAccessible = true
        } catch (e: Exception) {
            throw toRuntimeException(e)
        }
    }

    override fun getLightEngineType(): LightEngineType {
        return LightEngineType.STARLIGHT
    }

    override fun isLightingSupported(world: World?, lightFlags: Int): Boolean {
        val worldServer: ServerLevel? = (world as CraftWorld).handle
        val lightEngine = worldServer?.getChunkSource()?.lightEngine
        if (FlagUtils.isFlagSet(lightFlags, LightFlag.SKY_LIGHTING)) {
            return lightEngine?.getLayerListener(LightLayer.SKY) != null
        } else if (FlagUtils.isFlagSet(lightFlags, LightFlag.BLOCK_LIGHTING)) {
            return lightEngine?.getLayerListener(LightLayer.BLOCK) != null
        }
        return false
    }

    override fun setRawLightLevel(
        world: World,
        blockX: Int,
        blockY: Int,
        blockZ: Int,
        lightLevel: Int,
        flags: Int
    ): Int {
        val worldServer: ServerLevel = (world as CraftWorld).getHandle()
        val position = BlockPos(blockX, blockY, blockZ)
        val lightEngine = worldServer.getChunkSource().lightEngine
        val finalLightLevel = if (lightLevel < 0) 0 else Math.min(lightLevel, 15)
        val chunkCoordIntPair = ChunkPos(blockX shr 4, blockZ shr 4)
        if (!worldServer.getChunkSource().isChunkLoaded(blockX shr 4, blockZ shr 4)) {
            return ResultCode.CHUNK_NOT_LOADED
        }
        executeSync(lightEngine) {

            // block lighting
            if (FlagUtils.isFlagSet(flags, LightFlag.BLOCK_LIGHTING)) {
                if (isLightingSupported(world, LightFlag.BLOCK_LIGHTING)) {
                    val lele = lightEngine.getLayerListener(LightLayer.BLOCK)
                    if (finalLightLevel == 0) {
                        try {
                            val starLightInterface =
                                starInterface!![lightEngine] as StarLightInterface
                            starLightInterface.blockChange(position)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    } else if (lele.getDataLayerData(SectionPos.of(position)) != null) {
                        try {
                            if (blockQueueMap.containsKey(chunkCoordIntPair)) {
                                val lightPoints =
                                    blockQueueMap[chunkCoordIntPair]!!
                                lightPoints.add(LightPos(position, finalLightLevel))
                            } else {
                                val lightPoints: MutableSet<LightPos> =
                                    HashSet()
                                lightPoints.add(LightPos(position, finalLightLevel))
                                blockQueueMap[chunkCoordIntPair] = lightPoints
                            }
                        } catch (ignore: NullPointerException) {
                            // To prevent problems with the absence of the NibbleArray, even
                            // if leb.a(SectionPosition.a(position)) returns non-null value (corrupted data)
                        }
                    }
                }
            }

            // sky lighting
            if (FlagUtils.isFlagSet(flags, LightFlag.SKY_LIGHTING)) {
                if (isLightingSupported(world, LightFlag.SKY_LIGHTING)) {
                    val lele = lightEngine.getLayerListener(LightLayer.SKY)
                    if (finalLightLevel == 0) {
                        try {
                            val starLightInterface =
                                starInterface!![lightEngine] as StarLightInterface
                            starLightInterface.blockChange(position)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    } else if (lele.getDataLayerData(SectionPos.of(position)) != null) {
                        try {
                            if (skyQueueMap.containsKey(chunkCoordIntPair)) {
                                val lightPoints =
                                    skyQueueMap[chunkCoordIntPair]!!
                                lightPoints.add(LightPos(position, finalLightLevel))
                            } else {
                                val lightPoints: MutableSet<LightPos> =
                                    HashSet()
                                lightPoints.add(LightPos(position, finalLightLevel))
                                skyQueueMap[chunkCoordIntPair] = lightPoints
                            }
                        } catch (ignore: NullPointerException) {
                            // To prevent problems with the absence of the NibbleArray, even
                            // if les.a(SectionPosition.a(position)) returns non-null value (corrupted data)
                        }
                    }
                }
            }
        }
        var targetMap: Map<ChunkPos, MutableSet<LightPos>>? = null
        if (FlagUtils.isFlagSet(flags, LightFlag.SKY_LIGHTING)) {
            targetMap = skyQueueMap
        } else if (FlagUtils.isFlagSet(flags, LightFlag.BLOCK_LIGHTING)) {
            targetMap = blockQueueMap
        }
        return if (lightEngine.hasLightWork() || targetMap != null && targetMap.containsKey(chunkCoordIntPair)) {
            ResultCode.SUCCESS
        } else ResultCode.FAILED
    }

    override fun recalculateLighting(world: World, blockX: Int, blockY: Int, blockZ: Int, flags: Int): Int {
        val worldServer: ServerLevel = (world as CraftWorld).handle
        val lightEngine = worldServer.getChunkSource().lightEngine
        if (!worldServer.getChunkSource().isChunkLoaded(blockX shr 4, blockZ shr 4)) {
            return ResultCode.CHUNK_NOT_LOADED
        }

        // Do not recalculate if no changes!
        if (!lightEngine.hasLightWork() && blockQueueMap.isEmpty() && skyQueueMap.isEmpty()) {
            return ResultCode.RECALCULATE_NO_CHANGES
        }
        try {
            val starLightInterface = starInterface!![lightEngine] as StarLightInterface
            val blockIt: MutableIterator<*> = blockQueueMap.entries.iterator()
            while (blockIt.hasNext()) {
                val bsle = starInterface_getBlockLightEngine!!.invoke(
                    starLightInterface
                ) as BlockStarLightEngine
                val (chunkCoordIntPair, lightPoints) = blockIt.next() as Map.Entry<ChunkPos, MutableSet<LightPos>>
                addTaskToQueue(worldServer, starLightInterface, bsle, chunkCoordIntPair, lightPoints)
                blockIt.remove()
            }
            val skyIt: MutableIterator<*> = skyQueueMap.entries.iterator()
            while (skyIt.hasNext()) {
                val ssle = starInterface_getSkyLightEngine!!.invoke(
                    starLightInterface
                ) as SkyStarLightEngine
                val (chunkCoordIntPair, lightPoints) = skyIt.next() as Map.Entry<ChunkPos, MutableSet<LightPos>>
                addTaskToQueue(worldServer, starLightInterface, ssle, chunkCoordIntPair, lightPoints)
                skyIt.remove()
            }
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
        executeSync(lightEngine) {
            try {
                val starLightInterface = starInterface!![lightEngine] as StarLightInterface
                starLightInterface.propagateChanges()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        return ResultCode.SUCCESS
    }

    private class LightPos(var blockPos: BlockPos, var lightLevel: Int)
}
