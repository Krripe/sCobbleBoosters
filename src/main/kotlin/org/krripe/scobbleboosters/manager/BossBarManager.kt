package org.krripe.scobbleboosters.manager

import net.minecraft.entity.boss.BossBar
import net.minecraft.entity.boss.ServerBossBar
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.krripe.scobbleboosters.booster.BoosterRegistry
import org.krripe.scobbleboosters.config.BoosterConfigManager
import org.krripe.scobbleboosters.config.model.BossBarConfig
import org.krripe.scobbleboosters.config.model.BoosterScopeConfig
import org.krripe.scobbleboosters.data.model.BoosterInstance
import org.krripe.scobbleboosters.data.model.BoosterRuntime
import org.krripe.scobbleboosters.message.MessageManager
import org.krripe.scobbleboosters.utils.BoosterPlaceholders
import org.krripe.scobbleboosters.utils.CustomColorParser
import java.util.Locale
import java.util.UUID

object BossBarManager {
    private data class PersonalKey(val playerUuid: UUID, val type: String)

    private val globalBars = mutableMapOf<String, ServerBossBar>()
    private val personalBars = mutableMapOf<PersonalKey, ServerBossBar>()

    fun refresh(server: MinecraftServer, now: Long = System.currentTimeMillis()) {
        refreshGlobal(server, now)
        refreshPersonal(server, now)
    }

    fun onPlayerJoin(server: MinecraftServer, player: ServerPlayerEntity) {
        for ((type, bar) in globalBars) {
            if (!BoosterConfigManager.isEnabled(type)) continue
            val runtime = GlobalBoosterManager.getRuntime(type) ?: continue
            if (shouldShow(runtime, BoosterConfigManager.get(type)?.global?.bossBar, now = System.currentTimeMillis())) {
                bar.addPlayer(player)
            }
        }

        refreshPersonalPlayer(server, player.uuid, System.currentTimeMillis())
    }

    fun onPlayerDisconnect(player: ServerPlayerEntity) {
        globalBars.values.forEach { it.removePlayer(player) }

        val iterator = personalBars.entries.iterator()
        while (iterator.hasNext()) {
            val (key, bar) = iterator.next()
            if (key.playerUuid != player.uuid) continue

            bar.removePlayer(player)
            bar.isVisible = false
            iterator.remove()
        }
    }

    fun reset() {
        globalBars.values.forEach { it.isVisible = false }
        personalBars.values.forEach { it.isVisible = false }
        globalBars.clear()
        personalBars.clear()
    }

    private fun refreshGlobal(server: MinecraftServer, now: Long) {
        for (definition in BoosterRegistry.all()) {
            val type = definition.id

            if (!BoosterConfigManager.isEnabled(type)) {
                globalBars[type]?.isVisible = false
                continue
            }

            val runtime = GlobalBoosterManager.getRuntime(type)
            val scopeConfig = BoosterConfigManager.get(type)?.global

            if (runtime == null || scopeConfig == null || !shouldShow(runtime, scopeConfig.bossBar, now)) {
                globalBars[type]?.isVisible = false
                continue
            }

            val active = runtime.active ?: continue
            val bar = globalBars.getOrPut(type) {
                createBar(scopeConfig.bossBar)
            }

            for (player in server.playerManager.playerList) {
                bar.addPlayer(player)
            }

            updateBar(
                bar = bar,
                runtime = runtime,
                active = active,
                scopeConfig = scopeConfig,
                player = null,
                now = now
            )

            bar.isVisible = true
        }
    }

    private fun refreshPersonal(server: MinecraftServer, now: Long) {
        val validKeys = mutableSetOf<PersonalKey>()

        for (playerUuid in PersonalBoosterManager.playerUuids()) {
            for (type in PersonalBoosterManager.types(playerUuid)) {
                if (!BoosterConfigManager.isEnabled(type)) continue
                val key = PersonalKey(playerUuid, type)
                validKeys.add(key)

                val runtime = PersonalBoosterManager.getRuntime(playerUuid, type)
                val scopeConfig = BoosterConfigManager.get(type)?.personal
                val player = server.playerManager.getPlayer(playerUuid)

                if (
                    runtime == null ||
                    scopeConfig == null ||
                    player == null ||
                    !shouldShow(runtime, scopeConfig.bossBar, now)
                ) {
                    personalBars[key]?.isVisible = false
                    continue
                }

                val active = runtime.active ?: continue
                val bar = personalBars.getOrPut(key) {
                    createBar(scopeConfig.bossBar)
                }

                bar.addPlayer(player)

                updateBar(
                    bar = bar,
                    runtime = runtime,
                    active = active,
                    scopeConfig = scopeConfig,
                    player = player,
                    now = now
                )

                bar.isVisible = true
            }
        }

        val iterator = personalBars.entries.iterator()
        while (iterator.hasNext()) {
            val (key, bar) = iterator.next()

            if (key in validKeys) continue

            bar.isVisible = false
            iterator.remove()
        }
    }

    private fun refreshPersonalPlayer(server: MinecraftServer, playerUuid: UUID, now: Long) {
        val player = server.playerManager.getPlayer(playerUuid) ?: return

        for (type in PersonalBoosterManager.types(playerUuid)) {
            if (!BoosterConfigManager.isEnabled(type)) continue
            val runtime = PersonalBoosterManager.getRuntime(playerUuid, type) ?: continue
            val scopeConfig = BoosterConfigManager.get(type)?.personal ?: continue

            if (!shouldShow(runtime, scopeConfig.bossBar, now)) continue

            val active = runtime.active ?: continue
            val key = PersonalKey(playerUuid, type)
            val bar = personalBars.getOrPut(key) {
                createBar(scopeConfig.bossBar)
            }

            bar.addPlayer(player)
            updateBar(bar, runtime, active, scopeConfig, player, now)
            bar.isVisible = true
        }
    }

    private fun updateBar(
        bar: ServerBossBar,
        runtime: BoosterRuntime,
        active: BoosterInstance,
        scopeConfig: BoosterScopeConfig,
        player: ServerPlayerEntity?,
        now: Long
    ) {
        val raw = if (active.text.equals("default", true) || active.text.isBlank()) {
            scopeConfig.bossBar.text.ifBlank { scopeConfig.defaultText }
        } else {
            active.text
        }

        val remaining = active.remaining(now)
        val seconds = ((remaining + 999L) / 1000L).coerceAtLeast(0L)
        val h = (seconds / 3600L).toInt()
        val m = ((seconds / 60L) % 60L).toInt()
        val s = (seconds % 60L).toInt()

        val formatted = BoosterPlaceholders.applyAndFormat(
            raw = raw,
            multiplier = active.multiplier,
            h = h,
            m = m,
            s = s,
            totalDurationMillis = active.durationMillis,
            typeId = active.type,
            data = active.data,
            playerName = player?.name?.string,
            playerUuid = player?.uuid?.toString(),
            activatorName = active.activatorName,
            activatorUuid = active.activatorUuid?.toString(),
            placeholderPlayer = player
        )

        val text = CustomColorParser.processAllFormats(formatted, Style.EMPTY)

        if (scopeConfig.bossBar.showQueue && runtime.queue.isNotEmpty()) {
            val queue = runtime.queue.joinToString(" → ") { queueLabel(it) }
            text.append(MessageManager.text("bossbar.queue", player, "queue" to queue))
        }

        bar.setName(text)
        bar.setPercent(progress(active, now))
    }

    private fun shouldShow(runtime: BoosterRuntime, config: BossBarConfig?, now: Long): Boolean {
        if (config?.enabled != true) return false

        val active = runtime.active ?: return false
        if (runtime.isPaused()) return false
        if (active.isExpired(now)) return false

        return true
    }

    private fun createBar(config: BossBarConfig): ServerBossBar {
        val bar = ServerBossBar(
            Text.literal(""),
            parseColor(config.color),
            parseStyle(config.style)
        )

        bar.isVisible = false
        bar.setPercent(0f)
        return bar
    }

    private fun parseColor(value: String): BossBar.Color {
        return runCatching {
            BossBar.Color.valueOf(value.uppercase(Locale.ROOT))
        }.getOrDefault(BossBar.Color.PURPLE)
    }

    private fun parseStyle(value: String): BossBar.Style {
        return runCatching {
            BossBar.Style.valueOf(value.uppercase(Locale.ROOT))
        }.getOrDefault(BossBar.Style.PROGRESS)
    }

    private fun progress(active: BoosterInstance, now: Long): Float {
        if (active.durationMillis <= 0L) return 0f

        return (
                active.remaining(now).toDouble() /
                        active.durationMillis.toDouble()
                ).toFloat().coerceIn(0f, 1f)
    }

    private fun queueLabel(instance: BoosterInstance): String {
        val mult = if (instance.multiplier % 1f == 0f) {
            String.format(Locale.US, "%.0f", instance.multiplier)
        } else {
            String.format(Locale.US, "%.1f", instance.multiplier)
        }

        val data = instance.data?.takeIf { it.isNotBlank() }

        return if (data != null) {
            "${mult}x($data)"
        } else {
            "${mult}x"
        }
    }
}