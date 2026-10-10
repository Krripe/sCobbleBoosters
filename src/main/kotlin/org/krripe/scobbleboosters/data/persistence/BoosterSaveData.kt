package org.krripe.scobbleboosters.data.persistence

import org.krripe.scobbleboosters.data.model.BoosterInstance
import org.krripe.scobbleboosters.data.model.StoredBooster

data class BoosterSaveData(
    val version: Int = 1,
    val global: Map<String, BoosterRuntimeData> = emptyMap(),
    val personal: Map<String, Map<String, BoosterRuntimeData>> = emptyMap(),
    val storage: Map<String, List<StoredBooster>> = emptyMap()
)

data class BoosterRuntimeData(
    val active: BoosterInstance? = null,
    val queue: List<BoosterInstance> = emptyList(),
    val manualPaused: Boolean = false
)