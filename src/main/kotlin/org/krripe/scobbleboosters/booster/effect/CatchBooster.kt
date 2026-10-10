package org.krripe.scobbleboosters.booster.effect

import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents
import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.scobbleboosters.booster.resolver.BoosterResolver
import org.krripe.scobbleboosters.config.BoosterConfigManager
import org.krripe.scobbleboosters.debug.DebugManager
import java.util.Locale

object CatchBooster {
    private var registered = false

    fun register() {
        if (registered) return
        registered = true

        CobblemonEvents.POKEMON_CATCH_RATE.subscribe(Priority.HIGH) { event ->
            if (!BoosterConfigManager.isEnabled("catch")) return@subscribe

            val playerUuid = (event.thrower as? ServerPlayerEntity)?.uuid
            val resolved = BoosterResolver.resolve("catch", playerUuid) ?: return@subscribe

            val multiplier = resolved.instance.multiplier.coerceAtLeast(1f)
            if (multiplier <= 1f) return@subscribe

            val maxCatchRate = BoosterConfigManager
                .effectFloat("catch", "maxCatchRate", 255f)
                .coerceAtLeast(1f)

            val oldRate = event.catchRate
            val boostedRate = (oldRate * multiplier).coerceAtMost(maxCatchRate)
            val newRate = maxOf(oldRate, boostedRate)

            event.catchRate = newRate

            if (DebugManager.isEnabled("catch")) {
                DebugManager.log(
                    "catch",
                    "scope=${resolved.scope.name.lowercase()} | pokemon=${event.pokemonEntity.pokemon.species.resourceIdentifier} | rate=${format(oldRate)} -> ${format(newRate)} | multiplier=${format(multiplier)}x"
                )
            }
        }
    }

    private fun format(value: Float): String {
        return if (value % 1f == 0f) String.format(Locale.US, "%.0f", value)
        else String.format(Locale.US, "%.2f", value)
    }
}