package org.krripe.scobbleboosters.manager

import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.scobbleboosters.booster.BoosterRegistry
import org.krripe.scobbleboosters.utils.SoundHelper
import java.util.UUID

object BoosterPresentationManager {
    private data class PersonalKey(val playerUuid: UUID, val type: String)

    private val announcedGlobal = mutableMapOf<String, String>()
    private val announcedPersonal = mutableMapOf<PersonalKey, String>()

    fun initialize(server: MinecraftServer, now: Long = System.currentTimeMillis()) {
        announcedGlobal.clear()
        announcedPersonal.clear()

        for (type in BoosterRegistry.ids()) {
            val active = GlobalBoosterManager.getActive(type) ?: continue

            if (GlobalBoosterManager.isEffectActive(type, now)) {
                announcedGlobal[type] = active.id
            }
        }

        for (playerUuid in PersonalBoosterManager.playerUuids()) {
            for (type in PersonalBoosterManager.types(playerUuid)) {
                val active = PersonalBoosterManager.getActive(playerUuid, type) ?: continue

                if (PersonalBoosterManager.isEffectActive(playerUuid, type, now)) {
                    announcedPersonal[PersonalKey(playerUuid, type)] = active.id
                }
            }
        }

        BossBarManager.refresh(server, now)
    }

    fun refresh(server: MinecraftServer, now: Long = System.currentTimeMillis()) {
        refreshGlobalSounds(server, now)
        refreshPersonalSounds(server, now)
        BossBarManager.refresh(server, now)
    }

    fun onPlayerJoin(server: MinecraftServer, player: ServerPlayerEntity) {
        BossBarManager.onPlayerJoin(server, player)
    }

    fun onPlayerDisconnect(player: ServerPlayerEntity) {
        BossBarManager.onPlayerDisconnect(player)
        SoundHelper.resetPlayer(player.uuid)
    }

    fun reset() {
        announcedGlobal.clear()
        announcedPersonal.clear()
        BossBarManager.reset()
        SoundHelper.reset()
    }

    private fun refreshGlobalSounds(server: MinecraftServer, now: Long) {
        for (type in BoosterRegistry.ids()) {
            val active = GlobalBoosterManager.getActive(type)

            if (active == null) {
                announcedGlobal.remove(type)
                continue
            }

            if (!GlobalBoosterManager.isEffectActive(type, now)) continue
            if (announcedGlobal[type] == active.id) continue

            BoosterFeedbackManager.playGlobalActivation(server, active)
            announcedGlobal[type] = active.id
        }
    }

    private fun refreshPersonalSounds(server: MinecraftServer, now: Long) {
        val validKeys = mutableSetOf<PersonalKey>()

        for (playerUuid in PersonalBoosterManager.playerUuids()) {
            for (type in PersonalBoosterManager.types(playerUuid)) {
                val key = PersonalKey(playerUuid, type)
                validKeys.add(key)

                val active = PersonalBoosterManager.getActive(playerUuid, type)

                if (active == null) {
                    announcedPersonal.remove(key)
                    continue
                }

                if (!PersonalBoosterManager.isEffectActive(playerUuid, type, now)) continue
                if (announcedPersonal[key] == active.id) continue

                BoosterFeedbackManager.playPersonalActivation(server, playerUuid, active)
                announcedPersonal[key] = active.id
            }
        }

        announcedPersonal.keys.removeIf { it !in validKeys }
    }
}