package org.krripe.scobbleboosters.booster.effect

import java.util.Locale

object BucketBoostLogic {
    private val normalOrder = listOf("common", "uncommon", "rare", "ultra-rare")
    private val bossOrder = listOf("common", "uncommon", "rare", "ultra-rare", "boss")

    fun promoteWeights(
        weights: MutableMap<String, Float>,
        multiplier: Float,
        maxSteps: Int,
        maxChancePercent: Double,
        includeBoss: Boolean
    ) {
        if (weights.isEmpty() || multiplier <= 1f || maxSteps <= 0) return

        val order = if (includeBoss) bossOrder else normalOrder
        val maxChance = (maxChancePercent / 100.0).coerceIn(0.0, 1.0)
        val chance = ((multiplier - 1f) / multiplier).toDouble().coerceIn(0.0, maxChance)
        if (chance <= 0.0) return

        val original = LinkedHashMap(weights)
        val keysByName = original.keys.associateBy(::normalize)
        val result = original.keys.associateWith { 0f }.toMutableMap()
        val chanceFloat = chance.toFloat()

        for ((bucket, weight) in original) {
            val index = order.indexOf(normalize(bucket))

            if (index == -1 || index >= order.lastIndex || weight <= 0f) {
                result[bucket] = (result[bucket] ?: 0f) + weight
                continue
            }

            val maxTarget = (index + maxSteps).coerceAtMost(order.lastIndex)
            val target = (maxTarget downTo index + 1).firstNotNullOfOrNull {
                keysByName[order[it]]
            }

            if (target == null) {
                result[bucket] = (result[bucket] ?: 0f) + weight
                continue
            }

            val promoted = weight * chanceFloat

            result[bucket] = (result[bucket] ?: 0f) + weight - promoted
            result[target] = (result[target] ?: 0f) + promoted
        }

        weights.clear()
        weights.putAll(result)
    }

    fun formatWeights(weights: Map<String, Float>): String {
        return weights.entries.joinToString(", ") {
            "${normalize(it.key)}=${format(it.value)}"
        }
    }

    private fun normalize(value: String): String {
        return value.substringAfterLast(":")
            .substringAfterLast(".")
            .replace("_", "-")
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