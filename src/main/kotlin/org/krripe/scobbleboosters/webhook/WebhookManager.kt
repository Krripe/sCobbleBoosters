package org.krripe.scobbleboosters.webhook

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.server.MinecraftServer
import org.krripe.scobbleboosters.data.model.BoosterScope
import org.krripe.scobbleboosters.utils.Gsons
import org.krripe.scobbleboosters.webhook.model.*
import java.time.Instant

object WebhookManager {
    @Volatile
    private var server: MinecraftServer? = null

    fun initialize(server: MinecraftServer) {
        this.server = server
    }

    fun dispatch(context: WebhookEventContext) {
        val config = WebhookConfigManager.get(context.booster.type) ?: return
        val scopeConfig = when (context.scope) {
            BoosterScope.GLOBAL -> config.global
            BoosterScope.PERSONAL -> config.personal
        }

        if (!scopeConfig.enabled || scopeConfig.url.isBlank()) return

        val eventConfig = when (context.event) {
            WebhookEventType.ACTIVATED -> scopeConfig.events.activated
            WebhookEventType.QUEUED -> scopeConfig.events.queued
            WebhookEventType.RESUMED -> scopeConfig.events.resumed
            WebhookEventType.EXPIRED -> scopeConfig.events.expired
            WebhookEventType.CLEARED -> scopeConfig.events.cleared
        }

        if (!eventConfig.enabled) return

        val payload = JsonObject()

        if (scopeConfig.username.isNotBlank()) {
            payload.addProperty("username", apply(scopeConfig.username, context, config.reasons).take(80))
        }

        if (scopeConfig.avatarUrl.isNotBlank()) {
            payload.addProperty("avatar_url", apply(scopeConfig.avatarUrl, context, config.reasons))
        }

        val content = apply(eventConfig.content, context, config.reasons).take(2000)
        if (content.isNotBlank()) payload.addProperty("content", content)

        if (eventConfig.embed.enabled) {
            val embed = buildEmbed(eventConfig.embed, context, config.reasons)

            if (embed.size() > 0) {
                val embeds = JsonArray()
                embeds.add(embed)
                payload.add("embeds", embeds)
            }
        }

        if (!scopeConfig.allowMentions) {
            val allowedMentions = JsonObject()
            allowedMentions.add("parse", JsonArray())
            payload.add("allowed_mentions", allowedMentions)
        }

        if (!payload.has("content") && !payload.has("embeds")) return
        WebhookClient.send(scopeConfig.url, Gsons.default.toJson(payload))
    }

    fun reset() {
        server = null
    }

    private fun buildEmbed(
        config: WebhookEmbedConfig,
        context: WebhookEventContext,
        reasonNames: Map<String, String>
    ): JsonObject {
        val embed = JsonObject()
        val title = apply(config.title, context, reasonNames).take(256)
        val description = apply(config.description, context, reasonNames).take(4096)
        val footer = apply(config.footer, context, reasonNames).take(2048)

        if (title.isNotBlank()) embed.addProperty("title", title)

        if (config.fields.isEmpty() && description.isNotBlank()) {
            embed.addProperty("description", description)
        }

        parseColor(config.color)?.let { embed.addProperty("color", it) }

        val fields = JsonArray()

        for (fieldConfig in config.fields.take(25)) {
            val name = apply(fieldConfig.name, context, reasonNames).take(256)
            val value = apply(fieldConfig.value, context, reasonNames).take(1024)
            if (name.isBlank() || value.isBlank()) continue

            val field = JsonObject()
            field.addProperty("name", name)
            field.addProperty("value", value)
            field.addProperty("inline", fieldConfig.inline)
            fields.add(field)
        }

        if (fields.size() > 0) embed.add("fields", fields)

        val thumbnailUrl = apply(config.thumbnailUrl, context, reasonNames)
        if (thumbnailUrl.isNotBlank()) {
            val thumbnail = JsonObject()
            thumbnail.addProperty("url", thumbnailUrl)
            embed.add("thumbnail", thumbnail)
        }

        val imageUrl = apply(config.imageUrl, context, reasonNames)
        if (imageUrl.isNotBlank()) {
            val image = JsonObject()
            image.addProperty("url", imageUrl)
            embed.add("image", image)
        }

        if (footer.isNotBlank()) {
            val footerObject = JsonObject()
            footerObject.addProperty("text", footer)

            val footerIconUrl = apply(config.footerIconUrl, context, reasonNames)
            if (footerIconUrl.isNotBlank()) footerObject.addProperty("icon_url", footerIconUrl)

            embed.add("footer", footerObject)
        }

        if (config.timestamp) embed.addProperty("timestamp", Instant.now().toString())
        return embed
    }

    private fun apply(
        value: String,
        context: WebhookEventContext,
        reasonNames: Map<String, String>
    ) = WebhookPlaceholders.apply(value, context, server, reasonNames)

    private fun parseColor(raw: String): Int? {
        val cleaned = raw.trim().removePrefix("#").removePrefix("0x").removePrefix("0X")
        return cleaned.toIntOrNull(16)?.coerceIn(0, 0xFFFFFF)
    }
}