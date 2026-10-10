package org.krripe.scobbleboosters.commands

import com.mojang.brigadier.suggestion.SuggestionProvider
import net.minecraft.command.CommandSource
import net.minecraft.registry.Registries
import net.minecraft.server.command.ServerCommandSource
import org.krripe.scobbleboosters.booster.BoosterRegistry

object CommandSuggestions {
    val boosters = SuggestionProvider<ServerCommandSource> { _, builder ->
        CommandSource.suggestMatching(BoosterRegistry.ids(), builder)
    }

    val pokemonTypes = SuggestionProvider<ServerCommandSource> { _, builder ->
        CommandSource.suggestMatching(
            listOf(
                "normal", "fire", "water", "electric", "grass", "ice",
                "fighting", "poison", "ground", "flying", "psychic",
                "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy"
            ),
            builder
        )
    }

    val sounds = SuggestionProvider<ServerCommandSource> { _, builder ->
        val sounds = buildList {
            add("default")
            add("none")
            addAll(Registries.SOUND_EVENT.ids.map { it.toString() }.sorted())
        }

        CommandSource.suggestMatching(sounds, builder)
    }
}