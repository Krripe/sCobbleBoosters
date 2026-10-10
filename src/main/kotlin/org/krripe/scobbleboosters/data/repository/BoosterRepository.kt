package org.krripe.scobbleboosters.data.repository

import org.krripe.scobbleboosters.data.persistence.BoosterSaveData

interface BoosterRepository {
    fun load(): BoosterSaveData
    fun save(data: BoosterSaveData)
}