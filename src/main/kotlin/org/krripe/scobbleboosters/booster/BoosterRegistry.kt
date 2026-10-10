package org.krripe.scobbleboosters.booster

import java.util.Locale

object BoosterRegistry {
    private val definitions = linkedMapOf(
        "shiny" to BoosterDefinition("shiny", "Shiny"),
        "alpha" to BoosterDefinition("alpha", "Alpha"),
        "catch" to BoosterDefinition("catch", "Catch"),
        "experience" to BoosterDefinition("experience", "Experience"),
        "bucket" to BoosterDefinition("bucket", "Bucket"),
        "bait" to BoosterDefinition("bait", "Bait"),
        "fishing" to BoosterDefinition("fishing", "Fishing"),
        "snack" to BoosterDefinition("snack", "Snack"),
        "iv" to BoosterDefinition("iv", "IV"),
        "type" to BoosterDefinition("type", "Type", "pokemon_type"),
        "hiddenability" to BoosterDefinition("hiddenability", "Hidden Ability")
    )

    fun get(id: String): BoosterDefinition? {
        return definitions[id.lowercase(Locale.ROOT)]
    }

    fun exists(id: String): Boolean {
        return get(id) != null
    }

    fun all(): List<BoosterDefinition> {
        return definitions.values.toList()
    }

    fun ids(): Set<String> {
        return definitions.keys
    }
}