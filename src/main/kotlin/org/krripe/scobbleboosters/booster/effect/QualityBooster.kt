package org.krripe.scobbleboosters.booster.effect

import com.cobblemon.mod.common.api.spawning.spawner.FishingSpawnerFactory
import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawnerFactory
import com.cobblemon.mod.common.api.spawning.spawner.PokeSnackSpawnerFactory
import org.krripe.scobbleboosters.booster.influence.QualityBoosterInfluence

object QualityBooster {
    private var registered = false

    fun register() {
        if (registered) return
        registered = true

        PlayerSpawnerFactory.influenceBuilders.add { player ->
            QualityBoosterInfluence(
                playerUuid = player.uuid,
                source = "player"
            )
        }

        FishingSpawnerFactory.positionInfluenceBuilders.add { ctx ->
            listOf(
                QualityBoosterInfluence(
                    playerUuid = ctx.player.uuid,
                    source = "fishing"
                )
            )
        }

        PokeSnackSpawnerFactory.influenceBuilders.add {
            QualityBoosterInfluence(
                source = "poke-snack"
            )
        }
    }
}