package org.krripe.scobbleboosters.booster.influence

import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition
import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.scobbleboosters.booster.effect.BucketBoostLogic
import org.krripe.scobbleboosters.booster.resolver.BoosterResolver
import org.krripe.scobbleboosters.config.BoosterConfigManager
import org.krripe.scobbleboosters.debug.DebugManager
import java.util.Locale
import java.util.UUID

class BucketBoosterInfluence(
    private val fixedPlayerUuid: UUID? = null,
    private val source: String
) : SpawningInfluence {

    private var contextPlayerUuid: UUID? = null

    override fun affectSpawnablePosition(spawnablePosition: SpawnablePosition) {
        if (fixedPlayerUuid != null) return

        contextPlayerUuid =
            (spawnablePosition.cause.entity as? ServerPlayerEntity)?.uuid
    }

    override fun affectBucketWeights(bucketWeights: MutableMap<String, Float>) {
        if (!BoosterConfigManager.isEnabled("bucket") || bucketWeights.isEmpty()) return

        val playerUuid = fixedPlayerUuid ?: contextPlayerUuid
        val resolved = BoosterResolver.resolve("bucket", playerUuid) ?: return
        val multiplier = resolved.instance.multiplier.coerceAtLeast(1f)

        if (multiplier <= 1f) return

        val maxSteps = BoosterConfigManager
            .effectInt("bucket", "maxPromotionSteps", 1)
            .coerceAtLeast(0)

        val maxChance = BoosterConfigManager
            .effectDouble("bucket", "maxPromoteChancePercent", 95.0)
            .coerceIn(0.0, 100.0)

        val includeBoss = BoosterConfigManager
            .effectBoolean("bucket", "includeBoss", false)

        val debug = DebugManager.isEnabled("bucket")
        val before = if (debug) BucketBoostLogic.formatWeights(bucketWeights) else ""

        BucketBoostLogic.promoteWeights(
            weights = bucketWeights,
            multiplier = multiplier,
            maxSteps = maxSteps,
            maxChancePercent = maxChance,
            includeBoss = includeBoss
        )

        if (debug) {
            val after = BucketBoostLogic.formatWeights(bucketWeights)

            if (before != after) {
                DebugManager.log(
                    "bucket",
                    "source=$source | scope=${resolved.scope.name.lowercase()} | multiplier=${format(multiplier)}x | before=$before | after=$after"
                )
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