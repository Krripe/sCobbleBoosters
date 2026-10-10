package org.krripe.scobbleboosters.booster.effect

import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents
import org.krripe.scobbleboosters.booster.resolver.BoosterResolver
import org.krripe.scobbleboosters.config.BoosterConfigManager
import org.krripe.scobbleboosters.debug.DebugManager
import java.util.Locale
import kotlin.math.roundToInt

object ExperienceBooster {
    private var registered = false

    fun register() {
        if (registered) return
        registered = true

        CobblemonEvents.EXPERIENCE_GAINED_EVENT_PRE.subscribe(Priority.HIGH) { event ->
            if (!BoosterConfigManager.isEnabled("experience")) return@subscribe

            val playerUuid = event.pokemon.getOwnerUUID()
            val resolved = BoosterResolver.resolve("experience", playerUuid) ?: return@subscribe

            val maxMultiplier = BoosterConfigManager
                .effectFloat("experience", "maxMultiplier", 100f)
                .coerceAtLeast(1f)

            val multiplier = resolved.instance.multiplier.coerceIn(1f, maxMultiplier)
            if (multiplier <= 1f || event.experience <= 0) return@subscribe

            val oldExperience = event.experience
            val boosted = (oldExperience.toDouble() * multiplier)
                .coerceAtMost(Int.MAX_VALUE.toDouble())
                .roundToInt()

            val newExperience = boosted.coerceAtLeast(oldExperience)
            event.experience = newExperience

            if (DebugManager.isEnabled("experience")) {
                DebugManager.log(
                    "experience",
                    "scope=${resolved.scope.name.lowercase()} | pokemon=${event.pokemon.species.resourceIdentifier} | xp=$oldExperience -> $newExperience | multiplier=${format(multiplier)}x"
                )
            }
        }
    }

    private fun format(value: Float): String {
        return if (value % 1f == 0f) String.format(Locale.US, "%.0f", value)
        else String.format(Locale.US, "%.2f", value)
    }
}