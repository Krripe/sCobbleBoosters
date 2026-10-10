package org.krripe.scobbleboosters.booster.influence

import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnAction
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokemon.abilities.HiddenAbility
import com.cobblemon.mod.common.pokemon.stat.CobblemonStatProvider
import net.minecraft.entity.Entity
import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.scobbleboosters.booster.resolver.BoosterResolver
import org.krripe.scobbleboosters.config.BoosterConfigManager
import org.krripe.scobbleboosters.debug.DebugManager
import java.util.Locale
import java.util.UUID

class QualityBoosterInfluence(
    private val playerUuid: UUID? = null,
    private val source: String
) : SpawningInfluence {

    override fun affectSpawn(action: SpawnAction<*>, entity: Entity) {
        val pokemonEntity = entity as? PokemonEntity ?: return

        val uuid = playerUuid
            ?: (action.spawnablePosition.cause.entity as? ServerPlayerEntity)?.uuid

        applyIV(action, pokemonEntity, uuid)
        applyHiddenAbility(action, pokemonEntity, uuid)
    }

    private fun applyIV(
        action: SpawnAction<*>,
        entity: PokemonEntity,
        uuid: UUID?
    ) {
        if (!BoosterConfigManager.isEnabled("iv")) return

        val resolved = BoosterResolver.resolve("iv", uuid) ?: return

        val maxMultiplier = BoosterConfigManager
            .effectFloat("iv", "maxMultiplier", 10f)
            .coerceAtLeast(1f)

        val multiplier = resolved.instance.multiplier.coerceIn(1f, maxMultiplier)
        if (multiplier <= 1f) return

        val minIV = BoosterConfigManager
            .effectInt("iv", "minIV", 10)
            .coerceIn(0, 31)

        val chancePerMultiplier = BoosterConfigManager
            .effectDouble("iv", "perfectIvChancePerMultiplierPercent", 4.0)
            .coerceIn(0.0, 100.0) / 100.0

        val maxPerfectChance = BoosterConfigManager
            .effectDouble("iv", "maxPerfectIvChancePercent", 75.0)
            .coerceIn(0.0, 100.0) / 100.0

        val perfectChance = ((multiplier - 1f) * chancePerMultiplier)
            .coerceIn(0.0, maxPerfectChance)

        val pokemon = entity.pokemon
        val stats = CobblemonStatProvider.ofType(Stat.Type.PERMANENT)
        val random = action.spawnablePosition.world.random

        var changed = 0
        var perfect = 0

        for (stat in stats) {
            val current = pokemon.ivs[stat] ?: 0
            var target = current.coerceAtLeast(minIV)

            if (random.nextDouble() < perfectChance) {
                target = 31
            }

            if (target != current) {
                pokemon.setIV(stat, target)
                changed++
            }

            if (target == 31) perfect++
        }

        if (DebugManager.isEnabled("iv")) {
            DebugManager.log(
                "iv",
                "source=$source | scope=${resolved.scope.name.lowercase()} | pokemon=${pokemon.species.resourceIdentifier} | changed=$changed | perfect=$perfect | minIV=$minIV | perfectChance=${format(perfectChance * 100.0)}% | multiplier=${format(multiplier)}x"
            )
        }
    }

    private fun applyHiddenAbility(
        action: SpawnAction<*>,
        entity: PokemonEntity,
        uuid: UUID?
    ) {
        if (!BoosterConfigManager.isEnabled("hiddenability")) return

        val pokemonAction = action as? PokemonSpawnAction ?: return

        if (!pokemonAction.props.ability.isNullOrBlank()) {
            if (DebugManager.isEnabled("hiddenability")) {
                DebugManager.log(
                    "hiddenability",
                    "source=$source | pokemon=${entity.pokemon.species.resourceIdentifier} | explicit spawn ability | skipped"
                )
            }
            return
        }

        val resolved = BoosterResolver.resolve("hiddenability", uuid) ?: return

        val maxMultiplier = BoosterConfigManager
            .effectFloat("hiddenability", "maxMultiplier", 10f)
            .coerceAtLeast(1f)

        val multiplier = resolved.instance.multiplier.coerceIn(1f, maxMultiplier)
        if (multiplier <= 1f) return

        val chancePerMultiplier = BoosterConfigManager
            .effectDouble("hiddenability", "chancePerMultiplierPercent", 8.0)
            .coerceIn(0.0, 100.0) / 100.0

        val maxChance = BoosterConfigManager
            .effectDouble("hiddenability", "maxChancePercent", 75.0)
            .coerceIn(0.0, 100.0) / 100.0

        val chance = ((multiplier - 1f) * chancePerMultiplier)
            .coerceIn(0.0, maxChance)

        val pokemon = entity.pokemon
        val hiddenAbilities = pokemon.form.abilities
            .filterIsInstance<HiddenAbility>()

        if (hiddenAbilities.isEmpty()) {
            if (DebugManager.isEnabled("hiddenability")) {
                DebugManager.log(
                    "hiddenability",
                    "source=$source | pokemon=${pokemon.species.resourceIdentifier} | no hidden ability available"
                )
            }
            return
        }

        val alreadyHidden = hiddenAbilities.any {
            it.template == pokemon.ability.template
        }

        if (alreadyHidden) {
            if (DebugManager.isEnabled("hiddenability")) {
                DebugManager.log(
                    "hiddenability",
                    "source=$source | pokemon=${pokemon.species.resourceIdentifier} | already hidden=${pokemon.ability.name}"
                )
            }
            return
        }

        val random = action.spawnablePosition.world.random
        val rolled = random.nextDouble()

        if (rolled >= chance) {
            if (DebugManager.isEnabled("hiddenability")) {
                DebugManager.log(
                    "hiddenability",
                    "source=$source | scope=${resolved.scope.name.lowercase()} | pokemon=${pokemon.species.resourceIdentifier} | chance=${format(chance * 100.0)}% | result=normal | multiplier=${format(multiplier)}x"
                )
            }
            return
        }

        val selected = hiddenAbilities[random.nextInt(hiddenAbilities.size)]
        val before = pokemon.ability.name

        pokemon.updateAbility(
            selected.template.create(
                false,
                selected.priority
            )
        )

        if (DebugManager.isEnabled("hiddenability")) {
            DebugManager.log(
                "hiddenability",
                "source=$source | scope=${resolved.scope.name.lowercase()} | pokemon=${pokemon.species.resourceIdentifier} | ability=$before -> ${pokemon.ability.name} | chance=${format(chance * 100.0)}% | multiplier=${format(multiplier)}x"
            )
        }
    }

    private fun format(value: Float): String {
        return format(value.toDouble())
    }

    private fun format(value: Double): String {
        return if (value % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f", value)
        } else {
            String.format(Locale.US, "%.2f", value)
        }
    }
}