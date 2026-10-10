package org.krripe.scobbleboosters.booster.effect

import com.cobblemon.mod.common.api.spawning.spawner.FishingSpawnerFactory
import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawnerFactory
import com.cobblemon.mod.common.api.spawning.spawner.PokeSnackSpawnerFactory
import org.krripe.scobbleboosters.booster.influence.AlphaBoosterInfluence

object AlphaBooster {
    private var registered = false

    fun register() {
        if (registered) return
        registered = true

        PlayerSpawnerFactory.influenceBuilders.add { player ->
            AlphaBoosterInfluence(
                playerUuid = player.uuid,
                source = "player"
            )
        }

        FishingSpawnerFactory.positionInfluenceBuilders.add { ctx ->
            listOf(
                AlphaBoosterInfluence(
                    playerUuid = ctx.player.uuid,
                    source = "fishing"
                )
            )
        }

        PokeSnackSpawnerFactory.influenceBuilders.add {
            AlphaBoosterInfluence(
                source = "poke-snack"
            )
        }
    }
}