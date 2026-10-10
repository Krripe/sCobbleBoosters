package org.krripe.scobbleboosters.webhook

import net.minecraft.server.MinecraftServer
import org.krripe.scobbleboosters.message.MessageManager
import org.krripe.scobbleboosters.utils.DurationParser
import org.krripe.scobbleboosters.webhook.model.WebhookEventContext
import java.util.Locale

object WebhookPlaceholders {
    fun apply(
        input: String,
        context: WebhookEventContext,
        server: MinecraftServer?,
        reasonNames: Map<String, String> = emptyMap()
    ): String {
        if (input.isEmpty()) return input

        val booster = context.booster
        val player = context.playerUuid?.let {
            server?.playerManager?.getPlayer(it)?.name?.string ?: it.toString()
        } ?: "-"

        val replacements = mapOf(
            "%event%" to context.event.name.lowercase(Locale.ROOT),
            "%scope%" to context.scope.name.lowercase(Locale.ROOT),
            "%booster%" to MessageManager.boosterName(booster.type),
            "%booster_id%" to booster.type,
            "%instance_id%" to booster.id,
            "%multiplier%" to formatMultiplier(booster.multiplier),
            "%duration%" to DurationParser.formatMillis(booster.durationMillis),
            "%remaining%" to DurationParser.formatMillis(booster.remaining(context.now)),
            "%player%" to player,
            "%player_uuid%" to (context.playerUuid?.toString() ?: "-"),
            "%activator%" to (booster.activatorName ?: "Console"),
            "%activator_uuid%" to (booster.activatorUuid?.toString() ?: "-"),
            "%data%" to (booster.data ?: "-"),
            "%pokemon_type%" to formatPokemonType(booster.data),
            "%text%" to booster.text,
            "%sound%" to booster.sound,
            "%queue_position%" to (context.queuePosition?.toString() ?: "-"),
            "%reason%" to formatReason(context.reason, reasonNames),
            "%reason_id%" to context.reason.ifBlank { "-" }
        )

        var result = input
        for ((placeholder, value) in replacements) result = result.replace(placeholder, value)
        return result
    }

    private fun formatReason(reason: String, reasonNames: Map<String, String>): String {
        if (reason.isBlank()) return "-"
        reasonNames[reason]?.let { return it }

        return reason.replace('_', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
    }

    private fun formatPokemonType(value: String?): String {
        if (value.isNullOrBlank()) return "-"

        return value.trim()
            .replace('_', ' ')
            .replace('-', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase() } }
    }

    private fun formatMultiplier(value: Float): String {
        return if (value % 1f == 0f) String.format(Locale.US, "%.0f", value)
        else String.format(Locale.US, "%.2f", value)
    }
}