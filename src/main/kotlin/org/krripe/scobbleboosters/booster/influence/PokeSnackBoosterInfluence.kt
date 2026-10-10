package org.krripe.scobbleboosters.booster.influence

import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition
import com.cobblemon.mod.common.api.spawning.spawner.PokeSnackSpawnerFactory
import com.cobblemon.mod.common.block.entity.PokeSnackBlockEntity
import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.scobbleboosters.booster.effect.BucketBoostLogic
import org.krripe.scobbleboosters.booster.resolver.BoosterResolver
import org.krripe.scobbleboosters.config.BoosterConfigManager
import org.krripe.scobbleboosters.debug.DebugManager
import java.util.Locale
import java.util.UUID

class PokeSnackBoosterInfluence(
    private val ctx: PokeSnackSpawnerFactory.Context
) : SpawningInfluence {

    private var playerUuid: UUID? = null

    override fun affectSpawnablePosition(spawnablePosition: SpawnablePosition) {
        playerUuid = (spawnablePosition.cause.entity as? ServerPlayerEntity)?.uuid
    }

    override fun affectBucketWeights(bucketWeights: MutableMap<String, Float>) {
        if (bucketWeights.isEmpty()) return

        val effect = resolveEffect() ?: return
        val before = if (DebugManager.isEnabled(effect.debugType)) {
            BucketBoostLogic.formatWeights(bucketWeights)
        } else ""

        BucketBoostLogic.promoteWeights(
            weights = bucketWeights,
            multiplier = effect.multiplier,
            maxSteps = effect.maxSteps,
            maxChancePercent = effect.maxChance,
            includeBoss = false
        )

        if (DebugManager.isEnabled(effect.debugType)) {
            val after = BucketBoostLogic.formatWeights(bucketWeights)

            if (before != after) {
                DebugManager.logThrottled(
                    effect.debugType,
                    "poke-snack-buckets-${ctx.pos}",
                    3000L,
                    "poke snack rarity | source=${effect.source} | multiplier=${format(effect.multiplier)}x | before=$before | after=$after"
                )
            }
        }
    }

    private fun resolveEffect(): PromotionEffect? {
        val lure = isLure()

        val baitResolved = if (
            BoosterConfigManager.isEnabled("bait") &&
            if (lure) {
                BoosterConfigManager.effectBoolean("bait", "includeSnack", true)
            } else {
                BoosterConfigManager.effectBoolean("bait", "includeCake", true)
            }
        ) {
            BoosterResolver.resolve("bait", playerUuid)
        } else null

        val snackResolved = if (
            lure &&
            BoosterConfigManager.isEnabled("snack")
        ) {
            BoosterResolver.resolve("snack", playerUuid)
        } else null

        val baitMultiplier = baitResolved?.instance?.multiplier ?: 1f
        val snackMultiplier = snackResolved?.instance?.multiplier ?: 1f

        if (baitMultiplier <= 1f && snackMultiplier <= 1f) return null

        return if (lure && snackMultiplier >= baitMultiplier) {
            PromotionEffect(
                multiplier = snackMultiplier,
                maxSteps = BoosterConfigManager.effectInt("snack", "maxPromotionSteps", 1).coerceAtLeast(0),
                maxChance = BoosterConfigManager.effectDouble("snack", "maxPromoteChancePercent", 95.0),
                source = "snack",
                debugType = "snack"
            )
        } else {
            PromotionEffect(
                multiplier = baitMultiplier,
                maxSteps = BoosterConfigManager.effectInt("bait", "maxPromotionSteps", 1).coerceAtLeast(0),
                maxChance = BoosterConfigManager.effectDouble("bait", "maxPromoteChancePercent", 95.0),
                source = "bait",
                debugType = "bait"
            )
        }
    }

    private fun isLure(): Boolean {
        return (ctx.world.getBlockEntity(ctx.pos) as? PokeSnackBlockEntity)?.isLure() == true
    }

    private fun format(value: Float): String {
        return if (value % 1f == 0f) String.format(Locale.US, "%.0f", value)
        else String.format(Locale.US, "%.2f", value)
    }

    private data class PromotionEffect(
        val multiplier: Float,
        val maxSteps: Int,
        val maxChance: Double,
        val source: String,
        val debugType: String
    )
}