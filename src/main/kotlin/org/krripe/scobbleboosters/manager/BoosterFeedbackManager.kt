package org.krripe.scobbleboosters.manager

import net.minecraft.server.MinecraftServer
import org.krripe.scobbleboosters.config.BoosterConfigManager
import org.krripe.scobbleboosters.config.model.SoundConfig
import org.krripe.scobbleboosters.data.model.BoosterInstance
import org.krripe.scobbleboosters.utils.SoundHelper
import java.util.UUID

object BoosterFeedbackManager {

    fun playGlobalActivation(server: MinecraftServer, booster: BoosterInstance) {
        val config = BoosterConfigManager.get(booster.type)?.global?.activationSound ?: return
        val soundId = resolveSound(booster.sound, config) ?: return

        for (player in server.playerManager.playerList) {
            SoundHelper.play(player, soundId, config.volume, config.pitch)
        }
    }

    fun playPersonalActivation(server: MinecraftServer, playerUuid: UUID, booster: BoosterInstance) {
        val player = server.playerManager.getPlayer(playerUuid) ?: return
        val config = BoosterConfigManager.get(booster.type)?.personal?.activationSound ?: return
        val soundId = resolveSound(booster.sound, config) ?: return

        SoundHelper.play(player, soundId, config.volume, config.pitch)
    }

    private fun resolveSound(value: String, config: SoundConfig): String? {
        if (!config.enabled) return null

        return when {
            value.equals("none", ignoreCase = true) -> null
            value.equals("default", ignoreCase = true) -> config.id.takeIf { it.isNotBlank() }
            value.isBlank() -> config.id.takeIf { it.isNotBlank() }
            else -> value
        }
    }
}