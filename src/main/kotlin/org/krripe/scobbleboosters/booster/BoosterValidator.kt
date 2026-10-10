package org.krripe.scobbleboosters.booster

import org.krripe.scobbleboosters.data.model.BoosterInstance
import org.krripe.scobbleboosters.data.model.StoredBooster

object BoosterValidator {

    fun validMultiplier(multiplier: Float): Boolean {
        return multiplier.isFinite() && multiplier >= 1f
    }

    fun validDuration(durationMillis: Long): Boolean {
        return durationMillis > 0L
    }

    fun validData(typeId: String, data: String?): Boolean {
        val definition = BoosterRegistry.get(typeId) ?: return false
        return definition.dataArgument == null || !data.isNullOrBlank()
    }

    fun validInstance(instance: BoosterInstance): Boolean {
        return BoosterRegistry.exists(instance.type) &&
                validMultiplier(instance.multiplier) &&
                validDuration(instance.durationMillis) &&
                instance.remainingMillis > 0L &&
                validData(instance.type, instance.data)
    }

    fun validStored(booster: StoredBooster): Boolean {
        return BoosterRegistry.exists(booster.type) &&
                validMultiplier(booster.multiplier) &&
                validDuration(booster.durationMillis) &&
                booster.amount > 0 &&
                validData(booster.type, booster.data)
    }
}