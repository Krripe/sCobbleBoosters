package org.krripe.scobbleboosters.booster.influence

import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.entity.Entity
import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.scobbleboosters.booster.resolver.BoosterResolver
import org.krripe.scobbleboosters.config.BoosterConfigManager
import org.krripe.scobbleboosters.debug.DebugManager
import java.util.Locale
import java.util.UUID

class TypeBoosterInfluence(
    private val fixedPlayerUuid: UUID? = null,
    private val source: String
) : SpawningInfluence {

    override fun affectWeight(
        detail: SpawnDetail,
        spawnablePosition: SpawnablePosition,
        weight: Float
    ): Float {
        if (weight <= 0f || !BoosterConfigManager.isEnabled("type")) return weight
        if (detail !is PokemonSpawnDetail) return weight

        val playerUuid = fixedPlayerUuid
            ?: (spawnablePosition.cause.entity as? ServerPlayerEntity)?.uuid

        val resolved = BoosterResolver.resolve("type", playerUuid) ?: return weight
        val target = resolved.instance.data
            ?.takeIf { it.isNotBlank() }
            ?.let(::normalize)
            ?: return weight

        if (detail.labels.none { normalize(it) == target }) return weight

        val maxMultiplier = BoosterConfigManager
            .effectFloat("type", "maxMultiplier", 100000f)
            .coerceAtLeast(1f)

        val multiplier = resolved.instance.multiplier
            .coerceIn(1f, maxMultiplier)

        return weight * multiplier
    }

    override fun affectSpawn(action: SpawnAction<*>, entity: Entity) {
        if (!DebugManager.isEnabled("type")) return
        if (!BoosterConfigManager.isEnabled("type")) return

        val pokemonEntity = entity as? PokemonEntity ?: return
        val playerUuid = fixedPlayerUuid
            ?: (action.spawnablePosition.cause.entity as? ServerPlayerEntity)?.uuid

        val resolved = BoosterResolver.resolve("type", playerUuid) ?: return
        val target = resolved.instance.data
            ?.takeIf { it.isNotBlank() }
            ?.let(::normalize)
            ?: return

        val types = pokemonEntity.pokemon.types.map {
            normalize(it.showdownId)
        }

        DebugManager.log(
            "type",
            "source=$source | scope=${resolved.scope.name.lowercase()} | target=$target | pokemon=${pokemonEntity.pokemon.species.resourceIdentifier} | types=${types.joinToString(",")} | matched=${target in types} | multiplier=${format(resolved.instance.multiplier)}x"
        )
    }

    private fun normalize(value: String): String {
        return value.substringAfterLast(":")
            .substringAfterLast(".")
            .replace("_", "")
            .replace("-", "")
            .replace(" ", "")
            .lowercase(Locale.ROOT)
            .trim()
    }

    private fun format(value: Float): String {
        return if (value % 1f == 0f) {
            String.format(Locale.US, "%.0f", value)
        } else {
            String.format(Locale.US, "%.2f", value)
        }
    }
}