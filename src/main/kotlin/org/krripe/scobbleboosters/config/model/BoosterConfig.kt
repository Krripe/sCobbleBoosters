package org.krripe.scobbleboosters.config.model

import com.google.gson.JsonObject

data class BoosterConfig(
    val enabled: Boolean = true,
    val global: BoosterScopeConfig = BoosterScopeConfig(),
    val personal: BoosterScopeConfig = BoosterScopeConfig(),
    val effect: JsonObject = JsonObject()
)

data class BoosterScopeConfig(
    val defaultText: String = "&6%booster% Booster! &b%multiplier%x &8(&e%duration%&8)",
    val bossBar: BossBarConfig = BossBarConfig(),
    val activationSound: SoundConfig = SoundConfig()
)

data class BossBarConfig(
    val enabled: Boolean = true,
    val text: String = "&6%booster% Booster &8• &b%multiplier%x &8• &e%remaining%",
    val color: String = "PURPLE",
    val style: String = "PROGRESS",
    val showQueue: Boolean = true
)

data class SoundConfig(
    val enabled: Boolean = true,
    val id: String = "minecraft:block.beacon.activate",
    val volume: Float = 1.0f,
    val pitch: Float = 1.0f
)