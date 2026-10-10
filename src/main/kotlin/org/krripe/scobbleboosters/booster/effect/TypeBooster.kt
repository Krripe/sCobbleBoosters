package org.krripe.scobbleboosters.booster.effect

import com.cobblemon.mod.common.api.spawning.spawner.FishingSpawnerFactory
import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawnerFactory
import com.cobblemon.mod.common.api.spawning.spawner.PokeSnackSpawnerFactory
import org.krripe.scobbleboosters.booster.influence.TypeBoosterInfluence

object TypeBooster {
    private var registered = false

    fun register() {
        if (registered) return
        registered = true

        PlayerSpawnerFactory.influenceBuilders.add { player ->
            TypeBoosterInfluence(
                fixedPlayerUuid = player.uuid,
                source = "player"
            )
        }

        FishingSpawnerFactory.positionInfluenceBuilders.add { ctx ->
            listOf(
                TypeBoosterInfluence(
                    fixedPlayerUuid = ctx.player.uuid,
                    source = "fishing"
                )
            )
        }

        PokeSnackSpawnerFactory.influenceBuilders.add {
            TypeBoosterInfluence(
                source = "poke-snack"
            )
        }
    }
}