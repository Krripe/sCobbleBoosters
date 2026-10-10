package org.krripe.scobbleboosters.booster.effect

import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.spawning.spawner.PokeSnackSpawnerFactory
import org.krripe.scobbleboosters.booster.influence.PokeSnackBoosterInfluence
import org.krripe.scobbleboosters.booster.resolver.BoosterResolver
import org.krripe.scobbleboosters.config.BoosterConfigManager
import java.util.UUID

object SnackBooster {
    private var registered = false

    fun register() {
        if (registered) return
        registered = true

        PokeSnackSpawnerFactory.influenceBuilders.add { ctx ->
            PokeSnackBoosterInfluence(ctx)
        }

        CobblemonEvents.POKE_SNACK_SPAWN_POKEMON_POST.subscribe(Priority.HIGH) { event ->
            SnackSpawnTracker.markSuccess(event.pokeSnackBlockEntity)
        }
    }

    @JvmStatic
    fun multiplier(playerUuid: UUID?): Float {
        if (!BoosterConfigManager.isEnabled("snack")) return 1f
        return BoosterResolver.multiplier("snack", playerUuid)
    }

    @JvmStatic
    fun extraAttempts(): Int {
        return BoosterConfigManager.effectInt("snack", "extraAttempts", 3).coerceAtLeast(0)
    }
}