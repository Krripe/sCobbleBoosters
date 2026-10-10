package org.krripe.scobbleboosters.booster.influence

import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence
import org.krripe.scobbleboosters.booster.effect.BucketBoostLogic
import org.krripe.scobbleboosters.booster.resolver.BoosterResolver
import org.krripe.scobbleboosters.config.BoosterConfigManager
import org.krripe.scobbleboosters.debug.DebugManager
import java.util.Locale
import java.util.UUID

class FishingBaitInfluence(
    private val playerUuid: UUID
) : SpawningInfluence {

    override fun affectBucketWeights(bucketWeights: MutableMap<String, Float>) {
        if (!BoosterConfigManager.isEnabled("bait") || bucketWeights.isEmpty()) return
        if (!BoosterConfigManager.effectBoolean("bait", "includeFishing", true)) return

        val resolved = BoosterResolver.resolve("bait", playerUuid) ?: return
        val multiplier = resolved.instance.multiplier.coerceAtLeast(1f)
        if (multiplier <= 1f) return

        val before = if (DebugManager.isEnabled("bait")) {
            BucketBoostLogic.formatWeights(bucketWeights)
        } else ""

        BucketBoostLogic.promoteWeights(
            weights = bucketWeights,
            multiplier = multiplier,
            maxSteps = BoosterConfigManager.effectInt("bait", "maxPromotionSteps", 1).coerceAtLeast(0),
            maxChancePercent = BoosterConfigManager.effectDouble("bait", "maxPromoteChancePercent", 95.0),
            includeBoss = false
        )

        if (DebugManager.isEnabled("bait")) {
            val after = BucketBoostLogic.formatWeights(bucketWeights)

            if (before != after) {
                DebugManager.logThrottled(
                    "bait",
                    "fishing-buckets-$playerUuid",
                    3000L,
                    "fishing rarity | scope=${resolved.scope.name.lowercase()} | multiplier=${format(multiplier)}x | before=$before | after=$after"
                )
            }
        }
    }

    private fun format(value: Float): String {
        return if (value % 1f == 0f) String.format(Locale.US, "%.0f", value)
        else String.format(Locale.US, "%.2f", value)
    }
}