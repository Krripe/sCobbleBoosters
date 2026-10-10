package org.krripe.scobbleboosters.utils

import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.util.Identifier
import java.util.UUID

object SoundHelper {
    private const val DEFAULT_GAP_MS = 180L
    private val lastByPlayerAndId = mutableMapOf<UUID, MutableMap<String, Long>>()

    @JvmOverloads
    fun play(
        player: ServerPlayerEntity,
        id: String?,
        volume: Float = 1f,
        pitch: Float = 1f,
        minGapMs: Long = DEFAULT_GAP_MS
    ) {
        if (id.isNullOrBlank()) return

        val key = runCatching { Identifier.of(id.trim()) }.getOrNull() ?: return
        val event = Registries.SOUND_EVENT.get(key) ?: return

        val now = System.currentTimeMillis()
        val sounds = lastByPlayerAndId.getOrPut(player.uuid) { mutableMapOf() }
        val last = sounds[id] ?: 0L

        if (now - last < minGapMs) return

        sounds[id] = now
        player.playSoundToPlayer(event, SoundCategory.PLAYERS, volume.coerceAtLeast(0f), pitch.coerceAtLeast(0f))
    }

    fun resetPlayer(uuid: UUID) {
        lastByPlayerAndId.remove(uuid)
    }

    fun reset() {
        lastByPlayerAndId.clear()
    }
}