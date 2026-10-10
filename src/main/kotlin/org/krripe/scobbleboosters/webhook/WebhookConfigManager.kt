package org.krripe.scobbleboosters.webhook

import net.fabricmc.loader.api.FabricLoader
import org.krripe.scobbleboosters.booster.BoosterRegistry
import org.krripe.scobbleboosters.config.JsonConfigIO
import org.krripe.scobbleboosters.webhook.model.*

object WebhookConfigManager {
    private val configs = linkedMapOf<String, BoosterWebhookConfig>()

    private val folder = FabricLoader.getInstance().configDir
        .resolve("scobbleboosters")
        .resolve("webhooks")

    fun loadAll() {
        configs.clear()

        for (definition in BoosterRegistry.all()) {
            val file = folder.resolve("${definition.id}.json")
            configs[definition.id] = JsonConfigIO.load(file, defaultConfig(definition.id), BoosterWebhookConfig::class.java)
        }
    }

    fun reloadAll() = loadAll()

    fun get(typeId: String): BoosterWebhookConfig? = configs[typeId.lowercase().trim()]

    private fun defaultConfig(typeId: String) = BoosterWebhookConfig(
        reasons = defaultReasons(),
        global = WebhookScopeConfig(
            enabled = false,
            url = "",
            username = "sCobbleBoosters",
            avatarUrl = "",
            allowMentions = false,
            events = globalEvents(typeId)
        ),
        personal = WebhookScopeConfig(
            enabled = false,
            url = "",
            username = "sCobbleBoosters",
            avatarUrl = "",
            allowMentions = false,
            events = personalEvents(typeId)
        )
    )

    private fun globalEvents(typeId: String) = WebhookEventsConfig(
        activated = event(
            title = "✨ Booster Activated!",
            color = "#57F287",
            fields = buildList {
                add(field("Booster", "%booster%"))
                add(field("Multiplier", "%multiplier%x"))
                add(field("Duration", "%duration%"))
                if (typeId == "type") add(field("Pokémon Type", "%pokemon_type%"))
                add(field("Activator", "%activator%"))
            }
        ),
        queued = event(
            title = "⏳ Booster Queued",
            color = "#FEE75C",
            fields = listOf(
                field("Booster", "%booster%"),
                field("Multiplier", "%multiplier%x"),
                field("Queue Position", "#%queue_position%"),
                field("Reason", "%reason%", false)
            )
        ),
        resumed = event(
            title = "▶️ Booster Resumed",
            color = "#5865F2",
            fields = listOf(
                field("Booster", "%booster%"),
                field("Multiplier", "%multiplier%x"),
                field("Remaining", "%remaining%"),
                field("Reason", "%reason%", false)
            )
        ),
        expired = event(
            title = "⌛ Booster Expired",
            color = "#ED4245",
            fields = buildList {
                add(field("Booster", "%booster%"))
                add(field("Multiplier", "%multiplier%x"))
                add(field("Activator", "%activator%"))
                if (typeId == "type") add(field("Pokémon Type", "%pokemon_type%"))
            }
        ),
        cleared = event(
            title = "🗑️ Booster Cleared",
            color = "#747F8D",
            fields = listOf(
                field("Booster", "%booster%"),
                field("Multiplier", "%multiplier%x"),
                field("Remaining", "%remaining%"),
                field("Reason", "%reason%", false)
            )
        )
    )

    private fun personalEvents(typeId: String) = WebhookEventsConfig(
        activated = event(
            title = "✨ Personal Booster Activated!",
            color = "#57F287",
            fields = buildList {
                add(field("Booster", "%booster%"))
                add(field("Multiplier", "%multiplier%x"))
                add(field("Duration", "%duration%"))
                add(field("Player", "%player%"))
                add(field("Activator", "%activator%"))
                if (typeId == "type") add(field("Pokémon Type", "%pokemon_type%"))
            }
        ),
        queued = event(
            title = "⏳ Personal Booster Queued",
            color = "#FEE75C",
            fields = listOf(
                field("Booster", "%booster%"),
                field("Multiplier", "%multiplier%x"),
                field("Queue Position", "#%queue_position%"),
                field("Player", "%player%"),
                field("Reason", "%reason%", false)
            )
        ),
        resumed = event(
            title = "▶️ Personal Booster Resumed",
            color = "#5865F2",
            fields = listOf(
                field("Booster", "%booster%"),
                field("Multiplier", "%multiplier%x"),
                field("Remaining", "%remaining%"),
                field("Player", "%player%"),
                field("Reason", "%reason%", false)
            )
        ),
        expired = event(
            title = "⌛ Personal Booster Expired",
            color = "#ED4245",
            fields = buildList {
                add(field("Booster", "%booster%"))
                add(field("Multiplier", "%multiplier%x"))
                add(field("Player", "%player%"))
                add(field("Activator", "%activator%"))
                if (typeId == "type") add(field("Pokémon Type", "%pokemon_type%"))
            }
        ),
        cleared = event(
            title = "🗑️ Personal Booster Cleared",
            color = "#747F8D",
            fields = listOf(
                field("Booster", "%booster%"),
                field("Multiplier", "%multiplier%x"),
                field("Remaining", "%remaining%"),
                field("Player", "%player%"),
                field("Reason", "%reason%", false)
            )
        )
    )

    private fun event(title: String, color: String, fields: List<WebhookFieldConfig>) = WebhookEventConfig(
        enabled = true,
        content = "",
        embed = WebhookEmbedConfig(
            enabled = true,
            title = title,
            description = "",
            color = color,
            footer = "sCobbleBoosters",
            footerIconUrl = "",
            imageUrl = "",
            thumbnailUrl = "",
            timestamp = true,
            fields = fields
        )
    )

    private fun field(name: String, value: String, inline: Boolean = true) = WebhookFieldConfig(name, value, inline)

    private fun defaultReasons() = linkedMapOf(
        "direct" to "Activated immediately",
        "preempted" to "Replaced by a higher-priority booster",
        "higher_priority" to "Higher-priority booster activated",
        "lower_or_equal_priority" to "Waiting behind a higher-priority booster",
        "manual_start" to "Started manually",
        "activated_while_global_active" to "Activated while a matching Global booster is active",
        "clear_active" to "Active booster cleared",
        "queue_after_clear" to "Started from queue after a booster was cleared",
        "clear_type" to "Booster type cleared",
        "clear_player" to "Player boosters cleared",
        "clear_active_from_all" to "Active booster cleared for all players",
        "clear_type_from_all" to "Booster type cleared for all players",
        "clear_all" to "All boosters cleared",
        "timer" to "Booster duration completed",
        "queue_after_expiration" to "Started from queue after the previous booster expired",
        "global_booster_ended" to "Matching Global booster ended"
    )
}