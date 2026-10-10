package org.krripe.scobbleboosters.utils

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

object PlaceholderTextApiBridge {

    fun parse(input: String, player: ServerPlayerEntity?): String {
        if (player == null || input.isBlank()) return input

        return runCatching {
            val contextClass = Class.forName("eu.pb4.placeholders.api.PlaceholderContext")
            val placeholdersClass = Class.forName("eu.pb4.placeholders.api.Placeholders")

            val contextMethod = contextClass.methods.firstOrNull { method ->
                method.name == "of" &&
                        method.parameterCount == 1 &&
                        method.parameterTypes[0].isAssignableFrom(player.javaClass)
            } ?: return input

            val context = contextMethod.invoke(null, player)

            val parseMethod = placeholdersClass.methods.firstOrNull { method ->
                method.name == "parseText" && method.parameterCount == 2
            } ?: return input

            val parsed = parseMethod.invoke(null, Text.literal(input), context)

            (parsed as? Text)?.string ?: input
        }.getOrElse {
            input
        }
    }
}