package org.krripe.scobbleboosters.webhook

import org.krripe.scobbleboosters.SCobbleBoosters.Companion.LOGGER
import org.krripe.scobbleboosters.config.SBConfig
import org.krripe.scobbleboosters.utils.Gsons
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Instant

object DiscordWebhook {

    data class AllowedMentions(val parse: List<String> = listOf("roles"))
    data class EmbedImage(val url: String)
    data class EmbedFooter(val text: String)
    data class EmbedField(val name: String, val value: String, val inline: Boolean = true)

    data class Embed(
        val title: String,
        val color: Int,
        val fields: List<EmbedField>,
        val timestamp: String,
        val footer: EmbedFooter,
        val image: EmbedImage? = null,
        val thumbnail: EmbedImage? = null
    )

    data class Payload(
        val content: String? = null,
        val username: String? = null,
        val avatar_url: String? = null,
        val allowed_mentions: AllowedMentions = AllowedMentions(),
        val embeds: List<Embed> = emptyList()
    )

    fun sendBoosterActivated(typeId: String, multiplier: Float, durationTicks: Int) {
        val fallback = SBConfig.config.webhook
        val cfg = SBConfig.config.webhooks.forType(typeId, fallback)

        if (!cfg.enabled) return
        if (cfg.url.isBlank()) return

        try {
            val secTot = durationTicks / 20
            val h = secTot / 3600
            val m = (secTot / 60) % 60
            val s = secTot % 60

            val mention = cfg.mentionRoleId.takeIf { it.isNotBlank() }?.let { "<@&$it>" }

            val msg = runCatching {
                String.format(cfg.messageTemplate, multiplier, h, m, s)
            }.getOrElse { "Booster activated: ${"%.1f".format(multiplier)}x" }

            val fields = listOf(
                EmbedField("Type", typeId, true),
                EmbedField("Multiplier", "${"%.1f".format(multiplier)}x", true),
                EmbedField("Duration", "${h}h ${m}m ${s}s", true),
                EmbedField("Message", msg, false)
            )

            val embed = Embed(
                title = cfg.embedTitle,
                color = cfg.embedColor,
                fields = fields,
                timestamp = Instant.now().toString(),
                footer = EmbedFooter(cfg.footer),
                image = cfg.imageUrl.takeIf { it.isNotBlank() }?.let { EmbedImage(it) },
                thumbnail = cfg.thumbnailUrl.takeIf { it.isNotBlank() }?.let { EmbedImage(it) }
            )

            val payload = Payload(
                content = mention,
                username = cfg.username,
                avatar_url = cfg.avatarUrl,
                embeds = listOf(embed)
            )

            val json = Gsons.default.toJson(payload)
            val url = URI.create(cfg.url).toURL()
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            conn.outputStream.use { os: OutputStream ->
                os.write(json.toByteArray(StandardCharsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.readAllBytes()?.toString(StandardCharsets.UTF_8) ?: "No error body"
                LOGGER.error("Webhook failed: {} | {}", code, err)
            }
        } catch (e: Exception) {
            LOGGER.error("Error while sending webhook", e)
        }
    }
}