package org.krripe.scobbleboosters.utils

import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.scobbleboosters.message.MessageManager
import java.util.Locale

object BoosterPlaceholders {
    fun applyAndFormat(
        raw: String,
        multiplier: Float,
        h: Int,
        m: Int,
        s: Int,
        totalDurationMillis: Long,
        typeId: String,
        data: String? = null,
        playerName: String? = null,
        playerUuid: String? = null,
        activatorName: String? = null,
        activatorUuid: String? = null,
        placeholderPlayer: ServerPlayerEntity? = null
    ): String {
        val normalizedType = typeId.lowercase(Locale.ROOT)
        val boosterName = MessageManager.boosterName(normalizedType)
        val safeData = data?.trim().orEmpty()
        val pokemonType = formatPokemonType(safeData)
        val displayType = if (normalizedType == "type" && safeData.isNotBlank()) pokemonType else normalizedType

        val remaining = "${h}h ${m}m ${s}s"
        val totalSeconds = ((totalDurationMillis.coerceAtLeast(0L) + 999L) / 1000L)
        val totalH = (totalSeconds / 3600L).toInt()
        val totalM = ((totalSeconds / 60L) % 60L).toInt()
        val totalS = (totalSeconds % 60L).toInt()
        val totalDuration = "${totalH}h ${totalM}m ${totalS}s"
        val multText = fmt(multiplier)

        val replaced = raw
            .replace("%booster%", boosterName)
            .replace("%booster_id%", normalizedType)
            .replace("%booster_type%", normalizedType)
            .replace("%type%", displayType)
            .replace("%data%", safeData)
            .replace("%target%", safeData)
            .replace("%pokemon_type%", pokemonType)
            .replace("%mult%", multText)
            .replace("%multiplier%", multText)
            .replace("%duration%", remaining)
            .replace("%remaining%", remaining)
            .replace("%total_duration%", totalDuration)
            .replace("%h%", h.toString())
            .replace("%m%", m.toString())
            .replace("%s%", s.toString())
            .replace("%total_h%", totalH.toString())
            .replace("%total_m%", totalM.toString())
            .replace("%total_s%", totalS.toString())
            .replace("%player%", playerName.orEmpty())
            .replace("%player_uuid%", playerUuid.orEmpty())
            .replace("%player_active%", activatorName.orEmpty())
            .replace("%player_active_uuid%", activatorUuid.orEmpty())
            .replace("%activator%", activatorName.orEmpty())
            .replace("%activator_uuid%", activatorUuid.orEmpty())

        val formatted = legacyStringFormat(replaced, multiplier, h, m, s)
        return PlaceholderTextApiBridge.parse(formatted, placeholderPlayer)
    }

    private fun formatPokemonType(value: String): String {
        if (value.isBlank()) return ""

        return value.replace('_', ' ')
            .replace('-', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.lowercase(Locale.ROOT).replaceFirstChar(Char::uppercase) }
    }

    private fun legacyStringFormat(raw: String, mult: Float, h: Int, m: Int, s: Int): String {
        var dCount = 0
        var i = 0

        while (i < raw.length - 1) {
            if (raw[i] == '%' && raw[i + 1] == 'd') dCount++
            if (raw[i] == '%' && raw[i + 1] == '%') i++
            i++
        }

        val args: Array<Any> = when (dCount) {
            0 -> arrayOf(mult)
            1 -> arrayOf(mult, s)
            2 -> arrayOf(mult, m, s)
            else -> arrayOf(mult, h, m, s)
        }

        return runCatching { String.format(Locale.US, raw, *args) }.getOrElse { "Boost: ${fmt(mult)}x" }
    }

    private fun fmt(value: Float): String {
        return if (value % 1f == 0f) String.format(Locale.US, "%.0f", value)
        else String.format(Locale.US, "%.1f", value)
    }
}