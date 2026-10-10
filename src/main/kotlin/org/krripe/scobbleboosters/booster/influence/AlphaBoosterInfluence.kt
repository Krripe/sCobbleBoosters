package org.krripe.scobbleboosters.booster.influence

import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnAction
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence
import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.scobbleboosters.booster.resolver.BoosterResolver
import org.krripe.scobbleboosters.config.BoosterConfigManager
import org.krripe.scobbleboosters.debug.DebugManager
import java.util.Locale
import java.util.UUID

class AlphaBoosterInfluence(
    private val playerUuid: UUID? = null,
    private val source: String
) : SpawningInfluence {

    override fun affectAction(action: SpawnAction<*>) {
        if (!BoosterConfigManager.isEnabled("alpha")) return

        val pokemonAction = action as? PokemonSpawnAction ?: return
        if (pokemonAction.props.isAlpha == true) return

        val uuid = playerUuid
            ?: (action.spawnablePosition.cause.entity as? ServerPlayerEntity)?.uuid

        val resolved = BoosterResolver.resolve("alpha", uuid) ?: return
        val multiplier = resolved.instance.multiplier.coerceAtLeast(1f)

        val baseChance = BoosterConfigManager
            .effectDouble("alpha", "baseChancePercent", 0.1)
            .coerceIn(0.0, 100.0)

        val maxChance = BoosterConfigManager
            .effectDouble("alpha", "maxChancePercent", 100.0)
            .coerceIn(0.0, 100.0)

        val chancePercent = (baseChance * multiplier)
            .coerceAtMost(maxChance)
            .coerceIn(0.0, 100.0)

        if (chancePercent <= 0.0) return

        val rolled = action.spawnablePosition.world.random.nextDouble() * 100.0

        if (DebugManager.isEnabled("alpha")) {
            DebugManager.log(
                "alpha",
                "source=$source | scope=${resolved.scope.name.lowercase()} | chance=${format(chancePercent)}% | roll=${format(rolled)} | multiplier=${format(multiplier)}x | result=${if (rolled < chancePercent) "alpha" else "normal"}"
            )
        }

        if (rolled >= chancePercent) return

        val properties = pokemonAction.props.copy()
        properties.isAlpha = true
        pokemonAction.updatePokemon(properties)
    }

    private fun format(value: Float): String {
        return format(value.toDouble())
    }

    private fun format(value: Double): String {
        return if (value % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f", value)
        } else {
            String.format(Locale.US, "%.3f", value)
        }
    }
}