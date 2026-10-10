package org.krripe.scobbleboosters.booster.effect

import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents
import org.krripe.scobbleboosters.booster.resolver.BoosterResolver
import org.krripe.scobbleboosters.config.BoosterConfigManager
import org.krripe.scobbleboosters.debug.DebugManager
import java.util.Locale

object ShinyBooster {
    private var registered = false

    fun register() {
        if (registered) return
        registered = true

        CobblemonEvents.SHINY_CHANCE_CALCULATION.subscribe(Priority.LOWEST) { event ->
            if (!BoosterConfigManager.isEnabled("shiny")) return@subscribe

            event.addModificationFunction { currentRate, player, _ ->
                val resolved = BoosterResolver.resolve("shiny", player?.uuid)
                    ?: return@addModificationFunction currentRate

                val multiplier = resolved.instance.multiplier.coerceAtLeast(1f)
                val minRate = BoosterConfigManager
                    .effectFloat("shiny", "minAllowedRate", 1f)
                    .coerceAtLeast(1f)

                val rate = currentRate.coerceAtLeast(1f)
                val newRate = (rate / multiplier).coerceAtLeast(minRate)

                if (DebugManager.isEnabled("shiny")) {
                    DebugManager.log(
                        "shiny",
                        "scope=${resolved.scope.name.lowercase()} | rate=1/${format(rate)} -> 1/${format(newRate)} | multiplier=${format(multiplier)}x"
                    )
                }

                newRate
            }
        }
    }

    private fun format(value: Float): String {
        return if (value % 1f == 0f) {
            String.format(Locale.US, "%.0f", value)
        } else {
            String.format(Locale.US, "%.2f", value)
        }
    }
}