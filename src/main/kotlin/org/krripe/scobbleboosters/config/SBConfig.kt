package org.krripe.scobbleboosters.config

import com.google.gson.JsonObject
import org.krripe.scobbleboosters.utils.FileIO
import org.krripe.scobbleboosters.utils.Gsons
import java.io.File

object SBConfig {
    private val file = File("config/sCobbleBoosters/config.json")

    data class Config(
        val commands: Commands = Commands(),
        val shiny: Shiny = Shiny(),
        val webhook: Webhook = Webhook(),
        val webhooks: Webhooks = Webhooks(),
        val sounds: Sounds = Sounds()
    )

    data class Commands(
        val aliases: List<String> = listOf("sb")
    )

    data class Shiny(
        val enabled: Boolean = true,
        val minAllowedRate: Float = 1.0f,
        val showQueueOnBossbar: Boolean = true
    )

    data class Sounds(
        val boosterActivated: Sound = Sound()
    ) {
        data class Sound(
            val enabled: Boolean = true,
            val id: String = "minecraft:entity.player.levelup",
            val volume: Float = 1.0f,
            val pitch: Float = 1.0f
        )
    }

    data class Webhooks(
        val shiny: Webhook = Webhook()
    ) {
        fun forType(typeId: String, fallback: Webhook): Webhook {
            return when (typeId.lowercase()) {
                "shiny" -> shiny
                else -> fallback
            }
        }
    }

    data class Webhook(
        val enabled: Boolean = false,
        val url: String = "https://discord.com/api/webhooks/PUT_HERE",
        val mentionRoleId: String = "",
        val username: String? = null,
        val avatarUrl: String? = null,

        val embedTitle: String = "✨ Booster Activated!",
        val embedColor: Int = 3066993,
        val imageUrl: String = "",
        val thumbnailUrl: String = "",
        val footer: String = "sCobbleBoosters",

        val messageTemplate: String = "Booster activated: %.1fx for %dh %dm %ds"
    )

    @Volatile
    var config: Config = Config()
        private set

    fun load() {
        val gson = Gsons.pretty
        val defaults = gson.toJsonTree(Config()).asJsonObject

        if (!file.exists()) {
            FileIO.atomicWriteText(file, gson.toJson(defaults))
            config = gson.fromJson(defaults, Config::class.java)
            return
        }

        val existing = runCatching {
            gson.fromJson(FileIO.safeReadText(file) ?: "{}", JsonObject::class.java)
        }.getOrNull() ?: JsonObject()

        val merged = deepMerge(defaults, existing)
        FileIO.atomicWriteText(file, gson.toJson(merged))
        config = gson.fromJson(merged, Config::class.java)
    }

    fun reload() = load()

    private fun deepMerge(defaults: JsonObject, overrides: JsonObject): JsonObject {
        val out = JsonObject()
        for ((k, v) in defaults.entrySet()) out.add(k, v.deepCopy())
        for ((k, vOv) in overrides.entrySet()) {
            val existing = out.get(k)
            if (existing is JsonObject && vOv is JsonObject) out.add(k, deepMerge(existing, vOv))
            else out.add(k, vOv)
        }
        return out
    }
}
