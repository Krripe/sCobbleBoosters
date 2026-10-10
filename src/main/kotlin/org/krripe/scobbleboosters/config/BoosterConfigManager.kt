package org.krripe.scobbleboosters.config

import net.fabricmc.loader.api.FabricLoader
import org.krripe.scobbleboosters.booster.BoosterRegistry
import org.krripe.scobbleboosters.config.model.BoosterConfig
import org.krripe.scobbleboosters.config.model.BoosterScopeConfig
import org.krripe.scobbleboosters.config.model.SoundConfig
import org.krripe.scobbleboosters.config.model.BossBarConfig
import com.google.gson.JsonObject
import java.util.Locale

object BoosterConfigManager {
    private val folder = FabricLoader.getInstance().configDir.resolve("scobbleboosters").resolve("boosters")

    @Volatile
    private var configs: Map<String, BoosterConfig> = emptyMap()

    fun loadAll() {
        val loaded = linkedMapOf<String, BoosterConfig>()

        for (definition in BoosterRegistry.all()) {
            val file = folder.resolve("${definition.id}.json")
            loaded[definition.id] = JsonConfigIO.load(
                file,
                defaultConfig(definition.id),
                BoosterConfig::class.java
            )
        }

        configs = loaded.toMap()
    }

    fun reload() {
        loadAll()
    }

    fun get(typeId: String): BoosterConfig? {
        return configs[typeId.lowercase(Locale.ROOT).trim()]
    }

    fun isEnabled(typeId: String): Boolean {
        return get(typeId)?.enabled == true
    }

    fun all(): Map<String, BoosterConfig> {
        return configs
    }

    fun effectFloat(typeId: String, key: String, default: Float): Float {
        val value = get(typeId)?.effect?.get(key) ?: return default
        return runCatching { value.asFloat }.getOrDefault(default)
    }

    fun effectDouble(typeId: String, key: String, default: Double): Double {
        val value = get(typeId)?.effect?.get(key) ?: return default
        return runCatching { value.asDouble }.getOrDefault(default)
    }

    fun effectInt(typeId: String, key: String, default: Int): Int {
        val value = get(typeId)?.effect?.get(key) ?: return default
        return runCatching { value.asInt }.getOrDefault(default)
    }

    fun effectBoolean(typeId: String, key: String, default: Boolean): Boolean {
        val value = get(typeId)?.effect?.get(key) ?: return default
        return runCatching { value.asBoolean }.getOrDefault(default)
    }

    private fun defaultConfig(typeId: String): BoosterConfig {
        return BoosterConfig(
            global = BoosterScopeConfig(
                defaultText = "&6%booster% Booster! &b%multiplier%x &8(&e%duration%&8)",
                bossBar = BossBarConfig(text = bossBarText(typeId, false)),
                activationSound = SoundConfig(
                    enabled = true,
                    id = "minecraft:block.beacon.activate",
                    volume = 1.0f,
                    pitch = 1.0f
                )
            ),
            personal = BoosterScopeConfig(
                defaultText = "&6Personal &e%booster% &6Booster! &b%multiplier%x &8(&e%duration%&8)",
                bossBar = BossBarConfig(text = bossBarText(typeId, true)),
                activationSound = SoundConfig(
                    enabled = true,
                    id = "minecraft:entity.experience_orb.pickup",
                    volume = 1.0f,
                    pitch = 1.0f
                )
            ),
            effect = defaultEffect(typeId)
        )
    }

    private fun bossBarText(typeId: String, personal: Boolean): String {
        val prefix = if (personal) "&dPersonal " else ""
        val color = when (typeId) {
            "shiny" -> "&6"
            "alpha" -> "&c"
            "catch" -> "&a"
            "experience" -> "&9"
            "bucket" -> "&e"
            "bait" -> "&c"
            "fishing" -> "&b"
            "snack" -> "&a"
            "iv" -> "&d"
            "type" -> "&f"
            "hiddenability" -> "&2"
            else -> "&6"
        }

        val data = if (typeId == "type") " &8[&e%pokemon_type%&8]" else ""
        return "$prefix$color%booster% Booster$data &8• &b%multiplier%x &8• &e%remaining%"
    }

    private fun defaultEffect(typeId: String): JsonObject {
        return JsonObject().apply {
            when (typeId) {
                "shiny" -> {
                    addProperty("minAllowedRate", 1.0)
                }

                "alpha" -> {
                    addProperty("baseChancePercent", 0.1)
                    addProperty("maxChancePercent", 100.0)
                }

                "catch" -> {
                    addProperty("maxCatchRate", 255.0)
                }

                "experience" -> {
                    addProperty("maxMultiplier", 100.0)
                }

                "iv" -> {
                    addProperty("maxMultiplier", 10.0)
                    addProperty("minIV", 10)
                    addProperty("perfectIvChancePerMultiplierPercent", 4.0)
                    addProperty("maxPerfectIvChancePercent", 75.0)
                }

                "hiddenability" -> {
                    addProperty("maxMultiplier", 10.0)
                    addProperty("chancePerMultiplierPercent", 8.0)
                    addProperty("maxChancePercent", 75.0)
                }

                "bucket" -> {
                    addProperty("maxPromotionSteps", 1)
                    addProperty("maxPromoteChancePercent", 95.0)
                    addProperty("includeBoss", false)
                }

                "type" -> {
                    addProperty("maxMultiplier", 100000.0)
                }

                "fishing" -> {
                    addProperty("maxSpeedMultiplier", 5.0)
                    addProperty("minWaitTicks", 20)
                    addProperty("minTravelTicks", 5)
                    addProperty("maxPokemonChance", 101)
                    addProperty("guaranteePokemonBite", true)
                    addProperty("extraPlanAttempts", 5)
                    addProperty("suppressNoBiteMessage", true)
                }

                "bait" -> {
                    addProperty("maxPromotionSteps", 1)
                    addProperty("maxPromoteChancePercent", 95.0)
                    addProperty("maxSaveChancePercent", 90.0)
                    addProperty("includeFishing", true)
                    addProperty("includeCake", true)
                    addProperty("includeSnack", true)
                }

                "snack" -> {
                    addProperty("maxPromotionSteps", 1)
                    addProperty("maxPromoteChancePercent", 95.0)
                    addProperty("extraAttempts", 3)
                }
            }
        }
    }
}