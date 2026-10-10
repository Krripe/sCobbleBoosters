package org.krripe.scobbleboosters.booster.effect

import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.spawning.spawner.FishingSpawnerFactory
import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.scobbleboosters.booster.influence.FishingBaitInfluence
import org.krripe.scobbleboosters.booster.resolver.BoosterResolver
import org.krripe.scobbleboosters.config.BoosterConfigManager
import org.krripe.scobbleboosters.debug.DebugManager
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

object BaitBooster {
    private var registered = false
    private val fishingPlayerContext = ThreadLocal<UUID?>()

    fun register() {
        if (registered) return
        registered = true

        FishingSpawnerFactory.positionInfluenceBuilders.add { ctx ->
            listOf(FishingBaitInfluence(ctx.player.uuid))
        }

        CobblemonEvents.BOBBER_SPAWN_POKEMON_POST.subscribe(Priority.HIGH) { event ->
            val player = event.spawnAction.spawnablePosition.cause.entity as? ServerPlayerEntity
            fishingPlayerContext.set(player?.uuid)
        }

        CobblemonEvents.BAIT_CONSUMED.subscribe(Priority.HIGH) { event ->
            val playerUuid = fishingPlayerContext.get()

            try {
                if (!BoosterConfigManager.isEnabled("bait")) return@subscribe
                if (!BoosterConfigManager.effectBoolean("bait", "includeFishing", true)) return@subscribe

                val resolved = BoosterResolver.resolve("bait", playerUuid) ?: return@subscribe
                val multiplier = resolved.instance.multiplier.coerceAtLeast(1f)
                if (multiplier <= 1f) return@subscribe

                val maxSaveChance = (
                        BoosterConfigManager.effectDouble(
                            "bait",
                            "maxSaveChancePercent",
                            90.0
                        ) / 100.0
                        ).coerceIn(0.0, 1.0)

                val saveChance = ((multiplier - 1f) / multiplier)
                    .toDouble()
                    .coerceIn(0.0, maxSaveChance)

                val saved = ThreadLocalRandom.current().nextDouble() < saveChance

                if (saved) event.cancel()

                DebugManager.logThrottled(
                    "bait",
                    "consume-${playerUuid ?: "global"}",
                    2000L,
                    "bait consume | scope=${resolved.scope.name.lowercase()} | saveChance=${format(saveChance * 100.0)}% | result=${if (saved) "saved" else "consumed"} | multiplier=${format(multiplier)}x"
                )
            } finally {
                fishingPlayerContext.remove()
            }
        }
    }

    private fun format(value: Float): String {
        return format(value.toDouble())
    }

    private fun format(value: Double): String {
        return if (value % 1.0 == 0.0) String.format(Locale.US, "%.0f", value)
        else String.format(Locale.US, "%.2f", value)
    }
}