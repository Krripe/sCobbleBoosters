package org.krripe.scobbleboosters.booster.effect

import com.cobblemon.mod.common.block.entity.PokeSnackBlockEntity
import java.util.WeakHashMap

object SnackSpawnTracker {
    private val successCounts = WeakHashMap<PokeSnackBlockEntity, Long>()

    @JvmStatic
    fun markSuccess(blockEntity: PokeSnackBlockEntity) {
        successCounts[blockEntity] = (successCounts[blockEntity] ?: 0L) + 1L
    }

    @JvmStatic
    fun count(blockEntity: PokeSnackBlockEntity): Long {
        return successCounts[blockEntity] ?: 0L
    }
}