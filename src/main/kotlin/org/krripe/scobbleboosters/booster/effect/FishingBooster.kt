package org.krripe.scobbleboosters.booster.effect

import org.krripe.scobbleboosters.booster.resolver.BoosterResolver
import org.krripe.scobbleboosters.config.BoosterConfigManager
import java.util.UUID

object FishingBooster {

    @JvmStatic
    fun multiplier(playerUuid: UUID?): Float {
        if (!BoosterConfigManager.isEnabled("fishing")) return 1f
        return BoosterResolver.multiplier("fishing", playerUuid)
    }

    @JvmStatic
    fun maxSpeedMultiplier(): Float {
        return BoosterConfigManager.effectFloat("fishing", "maxSpeedMultiplier", 5f).coerceAtLeast(1f)
    }

    @JvmStatic
    fun minWaitTicks(): Int {
        return BoosterConfigManager.effectInt("fishing", "minWaitTicks", 20).coerceAtLeast(1)
    }

    @JvmStatic
    fun minTravelTicks(): Int {
        return BoosterConfigManager.effectInt("fishing", "minTravelTicks", 5).coerceAtLeast(1)
    }

    @JvmStatic
    fun maxPokemonChance(): Int {
        return BoosterConfigManager.effectInt("fishing", "maxPokemonChance", 101).coerceAtLeast(1)
    }

    @JvmStatic
    fun guaranteePokemonBite(): Boolean {
        return BoosterConfigManager.effectBoolean("fishing", "guaranteePokemonBite", true)
    }

    @JvmStatic
    fun extraPlanAttempts(): Int {
        return BoosterConfigManager.effectInt("fishing", "extraPlanAttempts", 5).coerceAtLeast(0)
    }

    @JvmStatic
    fun suppressNoBiteMessage(): Boolean {
        return BoosterConfigManager.effectBoolean("fishing", "suppressNoBiteMessage", true)
    }
}