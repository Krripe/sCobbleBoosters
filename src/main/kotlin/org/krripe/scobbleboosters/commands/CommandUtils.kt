package org.krripe.scobbleboosters.commands

import com.mojang.authlib.GameProfile
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.argument.GameProfileArgumentType
import net.minecraft.server.command.ServerCommandSource
import org.krripe.scobbleboosters.booster.BoosterRegistry
import org.krripe.scobbleboosters.config.BoosterConfigManager
import org.krripe.scobbleboosters.data.model.BoosterInstance
import org.krripe.scobbleboosters.message.MessageManager
import org.krripe.scobbleboosters.utils.DurationParser
import java.util.Locale

object CommandUtils {

    fun parseDurationMillis(raw: String): Long? {
        val seconds = DurationParser.parseSeconds(raw) ?: return null
        return runCatching {
            Math.multiplyExact(
                seconds,
                1000L
            )
        }.getOrNull()
    }

    fun validateBooster(
        source: ServerCommandSource,
        typeRaw: String,
        storage: Boolean = false
    ): String? {
        val type = typeRaw
            .lowercase(Locale.ROOT)
            .trim()

        val definition = BoosterRegistry.get(type)

        if (definition == null) {
            error(
                source,
                "common.unknown_booster",
                "booster" to typeRaw
            )
            return null
        }

        if (!BoosterConfigManager.isEnabled(type)) {
            error(
                source,
                "common.booster_disabled",
                "booster" to MessageManager.boosterName(type)
            )
            return null
        }

        if (
            storage &&
            !definition.supportsStorage
        ) {
            error(
                source,
                "common.booster_not_storable",
                "booster" to MessageManager.boosterName(type)
            )
            return null
        }

        return type
    }

    fun createInstance(
        source: ServerCommandSource,
        type: String,
        multiplier: Float,
        durationMillis: Long,
        text: String,
        sound: String,
        data: String? = null
    ): BoosterInstance {
        return BoosterInstance(
            type = type,
            multiplier = multiplier,
            durationMillis = durationMillis,
            text = text,
            sound = sound,
            data = data
                ?.lowercase(Locale.ROOT)
                ?.trim(),
            activatorName = source.name,
            activatorUuid = source.player?.uuid
        )
    }

    fun getSingleProfile(
        context: CommandContext<ServerCommandSource>,
        argument: String
    ): GameProfile? {
        val profiles =
            GameProfileArgumentType.getProfileArgument(
                context,
                argument
            )

        if (profiles.size != 1) {
            error(
                context.source,
                "common.exactly_one_player"
            )
            return null
        }

        return profiles.first()
    }

    fun success(
        source: ServerCommandSource,
        key: String,
        vararg placeholders: Pair<String, Any?>
    ) {
        MessageManager.send(
            source,
            key,
            *placeholders
        )
    }

    fun error(
        source: ServerCommandSource,
        key: String,
        vararg placeholders: Pair<String, Any?>
    ) {
        MessageManager.error(
            source,
            key,
            *placeholders
        )
    }

    fun formatMultiplier(multiplier: Float): String {
        return if (multiplier % 1f == 0f) {
            String.format(
                Locale.US,
                "%.0f",
                multiplier
            )
        } else {
            String.format(
                Locale.US,
                "%.2f",
                multiplier
            )
        }
    }

    fun formatDuration(milliseconds: Long): String {
        return DurationParser.formatMillis(
            milliseconds
        )
    }
}