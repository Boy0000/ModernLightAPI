package com.boy0000.modernlightapi.craftbukkit

import com.boy0000.modernlightapi.bukkit.internal.BukkitPlatformImpl
import com.boy0000.modernlightapi.internal.PlatformType
import com.boy0000.modernlightapi.internal.chunks.data.IChunkData
import com.boy0000.modernlightapi.internal.engine.LightEngineType
import com.boy0000.modernlightapi.internal.engine.LightEngineVersion
import org.bukkit.World
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent

interface IHandler {
    /**
     * N/A
     */
    abstract fun onInitialization(impl: BukkitPlatformImpl)

    /**
     * N/A
     */
    fun onShutdown(impl: BukkitPlatformImpl)

    /**
     * Platform that is being used
     *
     * @return One of the proposed options from [PlatformType]
     */
    fun getPlatformType(): PlatformType

    /**
     * N/A
     */
    fun getLightEngineType(): LightEngineType

    /**
     * Used lighting engine version.
     *
     * @return One of the proposed options from [LightEngineVersion]
     */
    fun getLightEngineVersion(): LightEngineVersion

    /**
     * N/A
     */
    fun isMainThread(): Boolean

    /**
     * N/A
     */
    fun onWorldLoad(event: WorldLoadEvent?)

    /**
     * N/A
     */
    fun onWorldUnload(event: WorldUnloadEvent?)

    /**
     * N/A
     */
    fun isLightingSupported(world: World?, lightFlags: Int): Boolean

    /**
     * Sets "directly" the level of light in given coordinates without additional processing.
     */
    fun setRawLightLevel(world: World?, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int, lightFlags: Int): Int

    /**
     * Gets "directly" the level of light from given coordinates without additional processing.
     */
    fun getRawLightLevel(world: World?, blockX: Int, blockY: Int, blockZ: Int, lightFlags: Int): Int

    /**
     * Performs re-illumination of the light in the given coordinates.
     */
    fun recalculateLighting(world: World?, blockX: Int, blockY: Int, blockZ: Int, lightFlags: Int): Int

    /**
     * N/A
     */
    fun createChunkData(worldName: String?, chunkX: Int, chunkZ: Int): IChunkData

    /**
     * Collects modified сhunks with sections around a given coordinate in the radius of the light
     * level. The light level is taken from the arguments.
     *
     * @return List changed chunk sections around the given coordinate.
     */
    fun collectChunkSections(
        world: World?, blockX: Int, blockY: Int, blockZ: Int, lightLevel: Int,
        lightFlags: Int
    ): List<IChunkData?>?

    /**
     * N/A
     */
    fun isValidChunkSection(world: World?, sectionY: Int): Boolean

    /**
     * N/A
     */
    fun sendChunk(data: IChunkData?): Int

    /**
     * Can be used for specific commands
     */
    fun sendCmd(cmdId: Int, vararg args: Any?): Int
}
