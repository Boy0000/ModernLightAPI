package com.boy0000.modernlightapi.v1_19_3

import com.boy0000.modernlightapi.api.ResultCode
import com.boy0000.modernlightapi.api.engine.LightFlag
import com.boy0000.modernlightapi.bukkit.internal.BukkitPlatformImpl
import com.boy0000.modernlightapi.craftbukkit.BaseNMSHandler
import com.boy0000.modernlightapi.internal.PlatformType
import com.boy0000.modernlightapi.internal.chunks.data.BitChunkData
import com.boy0000.modernlightapi.internal.chunks.data.IChunkData
import com.boy0000.modernlightapi.internal.engine.LightEngineType
import com.boy0000.modernlightapi.internal.engine.LightEngineVersion
import com.boy0000.modernlightapi.internal.utils.FlagUtils
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ThreadedLevelLightEngine
import net.minecraft.util.thread.ProcessorMailbox
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.lighting.*
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

open class VanillaNMSHandler : BaseNMSHandler() {
    var lightEngine_ThreadedMailbox: Field? = null
    private var threadedMailbox_State: Field? = null
    private var threadedMailbox_DoLoopStep: Method? = null
    private var lightEngineLayer_d: Field? = null
    private var lightEngineStorage_d: Method? = null
    private var lightEngineGraph_a: Method? = null
    private fun getDeltaLight(x: Int, dx: Int): Int {
        return (x xor (-dx shr 4 and 15)) + 1 and -(dx and 1)
    }

    private fun executeSync(lightEngine: ThreadedLevelLightEngine?, task: Runnable) {
        try {
            // ##### STEP 1: Pause light engine mailbox to process its tasks. #####
            val threadedMailbox = lightEngine_ThreadedMailbox!![lightEngine] as ProcessorMailbox<*>
            // State flags bit mask:
            // 0x0001 - Closing flag (ThreadedMailbox is closing if non zero).
            // 0x0002 - Busy flag (ThreadedMailbox performs a task from queue if non zero).
            val stateFlags = threadedMailbox_State!![threadedMailbox] as AtomicInteger
            var flags: Int // to hold values from stateFlags
            var timeToWait: Long = -1
            // Trying to set bit 1 in state bit mask when it is not set yet.
            // This will break the loop in other thread where light engine mailbox processes the taks.
            while (!stateFlags.compareAndSet(stateFlags.get() and 2.inv().also { flags = it }, flags or 2)) {
                if (flags and 1 != 0) {
                    // ThreadedMailbox is closing. The light engine mailbox may also stop processing tasks.
                    // The light engine mailbox can be close due to server shutdown or unloading (closing) the
                    // world.
                    // I am not sure is it unsafe to process our tasks while the world is closing is closing,
                    // but will try it (one can throw exception here if it crashes the server).
                    if (timeToWait == -1L) {
                        // Try to wait 3 seconds until light engine mailbox is busy.
                        timeToWait = System.currentTimeMillis() + 3 * 1000
                        //getPlatformImpl().debug("ThreadedMailbox is closing. Will wait...")
                    } else if (System.currentTimeMillis() >= timeToWait) {
                        throw RuntimeException("Failed to enter critical section while ThreadedMailbox is closing")
                    }
                    try {
                        Thread.sleep(50)
                    } catch (ignored: InterruptedException) {
                    }
                }
            }
            try {
                // ##### STEP 2: Safely running the task while the mailbox process is stopped. #####
                task.run()
            } finally {
                // STEP 3: ##### Continue light engine mailbox to process its tasks. #####
                // Firstly: Clearing busy flag to allow ThreadedMailbox to use it for running light engine
                // tasks.
                while (!stateFlags.compareAndSet(stateFlags.get().also { flags = it }, flags and 2.inv()));
                // Secondly: IMPORTANT! The main loop of ThreadedMailbox was broken. Not completed tasks may
                // still be
                // in the queue. Therefore, it is important to start the loop again to process tasks from
                // the queue.
                // Otherwise, the main server thread may be frozen due to tasks stuck in the queue.
                threadedMailbox_DoLoopStep!!.invoke(threadedMailbox)
            }
        } catch (e: InvocationTargetException) {
            throw toRuntimeException(e.cause)
        } catch (e: IllegalAccessException) {
            throw toRuntimeException(e)
        }
    }

    private fun lightEngineLayer_a(les: LayerLightEngine<*, *>, var0: BlockPos, var1: Int) {
        try {
            val ls = lightEngineLayer_d!![les] as LayerLightSectionStorage<*>
            lightEngineStorage_d!!.invoke(ls)
            lightEngineGraph_a!!.invoke(les, 9223372036854775807L, var0.asLong(), 15 - var1, true)
        } catch (e: InvocationTargetException) {
            throw toRuntimeException(e.cause)
        } catch (e: IllegalAccessException) {
            throw toRuntimeException(e)
        }
    }

    private fun createBitChunkData(worldName: String, chunkX: Int, chunkZ: Int): IChunkData {
        val world = Bukkit.getWorld(worldName)
        val worldServer: ServerLevel? = (world as CraftWorld?)?.handle
        val lightEngine = worldServer?.getChunkSource()?.lightEngine
        val bottom = lightEngine?.minLightSection ?: 1
        val top = lightEngine?.maxLightSection ?: 2
        return BitChunkData(worldName, chunkX, chunkZ, top, bottom)
    }

    @Throws(Exception::class)
    override fun onInitialization(impl: BukkitPlatformImpl) {
        super.onInitialization(impl)
        try {
            threadedMailbox_DoLoopStep = ProcessorMailbox::class.java.getDeclaredMethod("registerForExecution")
            threadedMailbox_DoLoopStep!!.isAccessible = true
            threadedMailbox_State = ProcessorMailbox::class.java.getDeclaredField("status")
            threadedMailbox_State!!.isAccessible = true
            //lightEngine_ThreadedMailbox = ThreadedLevelLightEngine::class.java.getDeclaredField("taskMailbox")
            //lightEngine_ThreadedMailbox!!.isAccessible = true
            lightEngineLayer_d = LayerLightEngine::class.java.getDeclaredField("storage")
            lightEngineLayer_d!!.isAccessible = true
            lightEngineStorage_d = LayerLightSectionStorage::class.java.getDeclaredMethod("runAllUpdates")
            lightEngineStorage_d!!.isAccessible = true
            lightEngineGraph_a = DynamicGraphMinFixedPoint::class.java.getDeclaredMethod(
                "checkEdge",
                Long::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType
            )
            lightEngineGraph_a!!.isAccessible = true
            impl.info("Handler initialization is done")
        } catch (e: Exception) {
            throw toRuntimeException(e)
        }
    }

    override fun onShutdown(impl: BukkitPlatformImpl) {}
    override fun getPlatformType(): PlatformType {
        return platformImpl?.platformType ?: PlatformType.CRAFTBUKKIT
    }

    override fun getLightEngineType(): LightEngineType {
        return LightEngineType.VANILLA
    }

    override fun onWorldLoad(event: WorldLoadEvent?) {}
    override fun onWorldUnload(event: WorldUnloadEvent?) {}

    override fun setRawLightLevel(
        world: World?,
        blockX: Int,
        blockY: Int,
        blockZ: Int,
        lightLevel: Int,
        lightFlags: Int
    ): Int {
        TODO("Not yet implemented")
    }

    override fun getRawLightLevel(world: World?, blockX: Int, blockY: Int, blockZ: Int, lightFlags: Int): Int {
        TODO("Not yet implemented")
    }

    override fun recalculateLighting(world: World?, blockX: Int, blockY: Int, blockZ: Int, lightFlags: Int): Int {
        TODO("Not yet implemented")
    }

    override fun createChunkData(worldName: String?, chunkX: Int, chunkZ: Int): IChunkData {
        TODO("Not yet implemented")
    }

    override fun isLightingSupported(world: World?, lightFlags: Int): Boolean {
        val worldServer: ServerLevel? = (world as CraftWorld).handle
        val lightEngine = worldServer?.getChunkSource()?.lightEngine
        if (FlagUtils.isFlagSet(lightFlags, LightFlag.SKY_LIGHTING)) {
            return lightEngine?.getLayerListener(LightLayer.SKY) is SkyLightEngine
        } else if (FlagUtils.isFlagSet(lightFlags, LightFlag.BLOCK_LIGHTING)) {
            return lightEngine?.getLayerListener(LightLayer.BLOCK) is BlockLightEngine
        }
        return false
    }

    override fun getLightEngineVersion(): LightEngineVersion {
        return LightEngineVersion.V2
    }

    override fun isMainThread(): Boolean {
        TODO("Not yet implemented")
    }

    open fun setRawLightLevel(world: World, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int, flags: Int): Int {
        val worldServer: ServerLevel = (world as CraftWorld).handle
        val position = BlockPos(blockX, blockY, blockZ)
        val lightEngine = worldServer.getChunkSource().lightEngine
        val finalLightLevel = if (lightLevel < 0) 0 else lightLevel.coerceAtMost(15)
        if (!worldServer.getChunkSource().isChunkLoaded(blockX shr 4, blockZ shr 4)) {
            return ResultCode.CHUNK_NOT_LOADED
        }
        if (FlagUtils.isFlagSet(flags, LightFlag.BLOCK_LIGHTING)) {
            if (!isLightingSupported(world, LightFlag.BLOCK_LIGHTING)) {
                return ResultCode.BLOCKLIGHT_DATA_NOT_AVAILABLE
            }
        }
        if (FlagUtils.isFlagSet(flags, LightFlag.SKY_LIGHTING)) {
            if (!isLightingSupported(world, LightFlag.SKY_LIGHTING)) {
                return ResultCode.SKYLIGHT_DATA_NOT_AVAILABLE
            }
        }
        executeSync(lightEngine) {

            // block lighting
            if (FlagUtils.isFlagSet(flags, LightFlag.BLOCK_LIGHTING)) {
                val leb = lightEngine.getLayerListener(LightLayer.BLOCK) as BlockLightEngine
                if (finalLightLevel == 0) {
                    leb.checkBlock(position)
                } else if (leb.getDataLayerData(SectionPos.of(position)) != null) {
                    try {
                        leb.onBlockEmissionIncrease(position, finalLightLevel)
                    } catch (ignore: NullPointerException) {
                        // To prevent problems with the absence of the NibbleArray, even
                        // if leb.a(SectionPosition.a(position)) returns non-null value (corrupted data)
                    }
                }
            }

            // sky lighting
            if (FlagUtils.isFlagSet(flags, LightFlag.SKY_LIGHTING)) {
                val les = lightEngine.getLayerListener(LightLayer.SKY) as SkyLightEngine
                if (finalLightLevel == 0) {
                    les.checkBlock(position)
                } else if (les.getDataLayerData(SectionPos.of(position)) != null) {
                    try {
                        lightEngineLayer_a(les, position, finalLightLevel)
                    } catch (ignore: NullPointerException) {
                        // To prevent problems with the absence of the NibbleArray, even
                        // if les.a(SectionPosition.a(position)) returns non-null value (corrupted data)
                    }
                }
            }
        }
        return if (lightEngine.hasLightWork()) {
            ResultCode.SUCCESS
        } else ResultCode.FAILED
    }

    fun getRawLightLevel(world: World, blockX: Int, blockY: Int, blockZ: Int, flags: Int): Int {
        var lightLevel = -1
        val worldServer: ServerLevel = (world as CraftWorld).getHandle()
        val position = BlockPos(blockX, blockY, blockZ)
        if (FlagUtils.isFlagSet(flags, LightFlag.BLOCK_LIGHTING) && FlagUtils.isFlagSet(
                flags,
                LightFlag.SKY_LIGHTING
            )
        ) {
            lightLevel = worldServer.getLightEmission(position)
        } else if (FlagUtils.isFlagSet(flags, LightFlag.BLOCK_LIGHTING)) {
            lightLevel = worldServer.getBrightness(LightLayer.BLOCK, position)
        } else if (FlagUtils.isFlagSet(flags, LightFlag.SKY_LIGHTING)) {
            lightLevel = worldServer.getBrightness(LightLayer.SKY, position)
        }
        return lightLevel
    }

    open fun recalculateLighting(world: World, blockX: Int, blockY: Int, blockZ: Int, flags: Int): Int {
        val worldServer: ServerLevel = (world as CraftWorld).getHandle()
        val lightEngine = worldServer.getChunkSource().lightEngine
        if (!worldServer.getChunkSource().isChunkLoaded(blockX shr 4, blockZ shr 4)) {
            return ResultCode.CHUNK_NOT_LOADED
        }

        // Do not recalculate if no changes!
        if (!lightEngine.hasLightWork()) {
            return ResultCode.RECALCULATE_NO_CHANGES
        }
        executeSync(lightEngine) {
            if (FlagUtils.isFlagSet(flags, LightFlag.BLOCK_LIGHTING) && FlagUtils.isFlagSet(
                    flags,
                    LightFlag.SKY_LIGHTING
                )
            ) {
                if (isLightingSupported(world, LightFlag.SKY_LIGHTING) && isLightingSupported(
                        world,
                        LightFlag.BLOCK_LIGHTING
                    )
                ) {
                    val leb = lightEngine.getLayerListener(LightLayer.BLOCK) as BlockLightEngine
                    val les = lightEngine.getLayerListener(LightLayer.SKY) as SkyLightEngine

                    // nms
                    val maxUpdateCount = Int.MAX_VALUE
                    val integer4 = maxUpdateCount / 2
                    val integer5 = leb.runUpdates(integer4, true, true)
                    val integer6 = maxUpdateCount - integer4 + integer5
                    val integer7 = les.runUpdates(integer6, true, true)
                    if (integer5 == 0 && integer7 > 0) {
                        leb.runUpdates(integer7, true, true)
                    }
                } else {
                    // block lighting
                    if (FlagUtils.isFlagSet(flags, LightFlag.BLOCK_LIGHTING)) {
                        if (isLightingSupported(world, LightFlag.BLOCK_LIGHTING)) {
                            val leb =
                                lightEngine.getLayerListener(LightLayer.BLOCK) as BlockLightEngine
                            leb.runUpdates(Int.MAX_VALUE, true, true)
                        }
                    }

                    // sky lighting
                    if (FlagUtils.isFlagSet(flags, LightFlag.SKY_LIGHTING)) {
                        if (isLightingSupported(world, LightFlag.SKY_LIGHTING)) {
                            val les = lightEngine.getLayerListener(LightLayer.SKY) as SkyLightEngine
                            les.runUpdates(Int.MAX_VALUE, true, true)
                        }
                    }
                }
            } else {
                // block lighting
                if (FlagUtils.isFlagSet(flags, LightFlag.BLOCK_LIGHTING)) {
                    if (isLightingSupported(world, LightFlag.BLOCK_LIGHTING)) {
                        val leb = lightEngine.getLayerListener(LightLayer.BLOCK) as BlockLightEngine
                        leb.runUpdates(Int.MAX_VALUE, true, true)
                    }
                }

                // sky lighting
                if (FlagUtils.isFlagSet(flags, LightFlag.SKY_LIGHTING)) {
                    if (isLightingSupported(world, LightFlag.SKY_LIGHTING)) {
                        val les = lightEngine.getLayerListener(LightLayer.SKY) as SkyLightEngine
                        les.runUpdates(Int.MAX_VALUE, true, true)
                    }
                }
            }
        }
        return ResultCode.SUCCESS
    }

    fun createChunkData(worldName: String, chunkX: Int, chunkZ: Int): IChunkData {
        return createBitChunkData(worldName, chunkX, chunkZ)
    }

    private fun searchChunkDataFromList(list: List<IChunkData>, world: World, chunkX: Int, chunkZ: Int): IChunkData {
        for (data in list) {
            if (data.worldName.equals(world.name) && data.chunkX == chunkX && data.chunkZ == chunkZ) {
                return data
            }
        }
        return createChunkData(world.name, chunkX, chunkZ)
    }

    override fun collectChunkSections(
        world: World?, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int,
        lightFlags: Int
    ): List<IChunkData> {
        val worldServer: ServerLevel? = (world as CraftWorld?)?.handle
        val list: MutableList<IChunkData> = ArrayList<IChunkData>()
        val finalLightLevel = if (lightLevel < 0) 0 else lightLevel.coerceAtMost(15)
        if (world == null) {
            return list
        }
        for (dX in -1..1) {
            val lightLevelX = finalLightLevel - getDeltaLight(blockX and 15, dX)
            if (lightLevelX > 0) {
                for (dZ in -1..1) {
                    val lightLevelZ = lightLevelX - getDeltaLight(blockZ and 15, dZ)
                    if (lightLevelZ > 0) {
                        val chunkX = (blockX shr 4) + dX
                        val chunkZ = (blockZ shr 4) + dZ
                        if (worldServer?.getChunkSource()?.isChunkLoaded(chunkX, chunkZ) != true) {
                            continue
                        }
                        for (dY in -1..1) {
                            if (lightLevelZ > getDeltaLight(blockY and 15, dY)) {
                                val sectionY = (blockY shr 4) + dY
                                if (isValidChunkSection(world, sectionY)) {
                                    val data: IChunkData = searchChunkDataFromList(list, world, chunkX, chunkZ)
                                    if (!list.contains(data)) {
                                        list.add(data)
                                    }
                                    data.markSectionForUpdate(lightFlags, sectionY)
                                }
                            }
                        }
                    }
                }
            }
        }
        return list
    }

    override fun isValidChunkSection(world: World?, sectionY: Int): Boolean {
        TODO("Not yet implemented")
    }

    fun isValidChunkSection(world: World, sectionY: Int): Boolean {
        val worldServer: ServerLevel = (world as CraftWorld).getHandle()
        val lightEngine = worldServer.getChunkSource().lightEngine
        return sectionY >= lightEngine.minLightSection && sectionY <= lightEngine.maxLightSection
    }

    override fun sendChunk(data: IChunkData?): Int {
        val world: World? = Bukkit.getWorld(data?.worldName.toString())
        if (data is BitChunkData) {
            val icd: BitChunkData = data
            return sendChunk(
                world, icd.chunkX, icd.chunkZ, icd.skyLightUpdateBits,
                icd.blockLightUpdateBits
            )
        }
        return ResultCode.NOT_IMPLEMENTED
    }

    private fun sendChunk(
        world: World?,
        chunkX: Int,
        chunkZ: Int,
        sectionMaskSky: BitSet?,
        sectionMaskBlock: BitSet?
    ): Int {
        if (world == null) {
            return ResultCode.WORLD_NOT_AVAILABLE
        }
        val worldServer: ServerLevel = (world as CraftWorld).handle
        if (!worldServer.getChunkSource().isChunkLoaded(chunkX, chunkZ)) {
            return ResultCode.CHUNK_NOT_LOADED
        }
        val chunk = worldServer.getChunk(chunkX, chunkZ)
        val chunkCoordIntPair = chunk.pos
        val stream = worldServer.getChunkSource().chunkMap.getPlayers(
            chunkCoordIntPair,
            false
        ).stream()
        val packet = ClientboundLightUpdatePacket(
            chunk.pos,
            chunk.getLevel().lightEngine, sectionMaskSky, sectionMaskBlock, true
        )
        stream.forEach { e: ServerPlayer -> e.connection.send(packet) }
        return ResultCode.SUCCESS
    }

    override fun sendCmd(cmdId: Int, vararg args: Any?): Int {
        return 0
    }

    companion object {
        @JvmStatic
        protected fun toRuntimeException(e: Throwable?): RuntimeException {
            if (e is RuntimeException) {
                return e
            }
            val cls: Class<out Throwable> = e!!.javaClass
            return RuntimeException(
                String.format(
                    "(%s) %s",
                    if (RuntimeException::class.java.getPackage() == cls.getPackage()) cls.simpleName else cls.name,
                    e.message
                ), e
            )
        }
    }
}
