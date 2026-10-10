package org.krripe.scobbleboosters.commands

import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.argument.GameProfileArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import org.krripe.scobbleboosters.booster.BoosterRegistry
import org.krripe.scobbleboosters.booster.BoosterValidator
import org.krripe.scobbleboosters.data.model.BoosterActivationResult
import org.krripe.scobbleboosters.data.model.StoredBooster
import org.krripe.scobbleboosters.manager.BoosterCoordinator
import org.krripe.scobbleboosters.manager.BoosterStorageManager
import org.krripe.scobbleboosters.message.MessageManager

object PersonalCommands {

    fun build(): LiteralArgumentBuilder<ServerCommandSource> {
        return CommandManager.literal("personal")
            .then(CommandManager.literal("give").then(playerBoosterBranch(true)))
            .then(CommandManager.literal("activate").then(playerBoosterBranch(false)))
            .then(
                CommandManager.literal("clear")
                    .then(
                        CommandManager.literal("all")
                            .executes(::clearEverything)
                            .then(
                                CommandManager.argument("booster", StringArgumentType.word())
                                    .suggests(CommandSuggestions.boosters)
                                    .executes(::clearTypeFromAll)
                            )
                    )
                    .then(
                        CommandManager.argument("booster", StringArgumentType.word())
                            .suggests(CommandSuggestions.boosters)
                            .executes(::clearActiveFromAll)
                    )
                    .then(
                        CommandManager.literal("player")
                            .then(
                                CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                    .then(
                                        CommandManager.literal("all")
                                            .executes(::clearEverythingFromPlayer)
                                            .then(
                                                CommandManager.argument("booster", StringArgumentType.word())
                                                    .suggests(CommandSuggestions.boosters)
                                                    .executes(::clearTypeFromPlayer)
                                            )
                                    )
                                    .then(
                                        CommandManager.argument("booster", StringArgumentType.word())
                                            .suggests(CommandSuggestions.boosters)
                                            .executes(::clearActiveFromPlayer)
                                    )
                            )
                    )
            )
            .then(
                CommandManager.literal("remove")
                    .then(
                        CommandManager.literal("all")
                            .then(
                                CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                    .then(
                                        CommandManager.argument("booster", StringArgumentType.word())
                                            .suggests(CommandSuggestions.boosters)
                                            .executes(::removeAll)
                                    )
                            )
                    )
                    .then(
                        CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                            .then(
                                CommandManager.argument("booster", StringArgumentType.word())
                                    .suggests(CommandSuggestions.boosters)
                                    .executes(::removeOne)
                            )
                    )
            )
    }

    private fun playerBoosterBranch(
        give: Boolean
    ) =
        CommandManager.argument(
            "player",
            GameProfileArgumentType.gameProfile()
        )
            .then(
                CommandManager.literal("type")
                    .then(
                        CommandManager.argument(
                            "pokemon_type",
                            StringArgumentType.word()
                        )
                            .suggests(
                                CommandSuggestions.pokemonTypes
                            )
                            .then(
                                personalTail(
                                    "type",
                                    true,
                                    give
                                )
                            )
                    )
            )
            .then(
                CommandManager.argument(
                    "booster",
                    StringArgumentType.word()
                )
                    .suggests(
                        CommandSuggestions.boosters
                    )
                    .then(
                        personalTail(
                            null,
                            false,
                            give
                        )
                    )
            )

    private fun personalTail(forcedType: String?, hasData: Boolean, give: Boolean) =
        CommandManager.argument("multiplier", FloatArgumentType.floatArg(1f))
            .then(
                CommandManager.argument("time", StringArgumentType.word())
                    .then(
                        CommandManager.argument("text", StringArgumentType.string())
                            .then(
                                CommandManager.argument("sound", StringArgumentType.greedyString())
                                    .suggests(CommandSuggestions.sounds)
                                    .executes { context ->
                                        handlePersonal(
                                            context = context,
                                            forcedType = forcedType,
                                            data = if (hasData) StringArgumentType.getString(context, "pokemon_type") else null,
                                            give = give
                                        )
                                    }
                            )
                    )
            )

    private fun handlePersonal(
        context: CommandContext<ServerCommandSource>,
        forcedType: String?,
        data: String?,
        give: Boolean
    ): Int {
        val source =
            context.source

        val profile =
            CommandUtils.getSingleProfile(
                context,
                "player"
            ) ?: return 0

        val playerUuid =
            profile.id
                ?: run {
                    CommandUtils.error(
                        source,
                        "common.player_uuid_failed"
                    )
                    return 0
                }

        val rawType =
            forcedType
                ?: StringArgumentType.getString(
                    context,
                    "booster"
                )

        val type =
            CommandUtils.validateBooster(
                source,
                rawType,
                storage = give
            ) ?: return 0

        val definition =
            BoosterRegistry.get(type)
                ?: return 0

        val displayName =
            MessageManager.boosterName(type)

        if (!definition.supportsPersonal) {
            CommandUtils.error(
                source,
                "common.unsupported_personal",
                "booster" to displayName
            )
            return 0
        }

        if (
            definition.dataArgument != null &&
            data.isNullOrBlank()
        ) {
            CommandUtils.error(
                source,
                "common.requires_data",
                "booster" to displayName,
                "argument" to definition.dataArgument
            )
            return 0
        }

        val multiplier =
            FloatArgumentType.getFloat(
                context,
                "multiplier"
            )

        val durationRaw =
            StringArgumentType.getString(
                context,
                "time"
            )

        val duration =
            CommandUtils.parseDurationMillis(
                durationRaw
            )

        if (duration == null) {
            CommandUtils.error(
                source,
                "common.invalid_duration",
                "input" to durationRaw
            )
            return 0
        }

        val text =
            StringArgumentType.getString(
                context,
                "text"
            )

        val sound =
            StringArgumentType.getString(
                context,
                "sound"
            )

        if (
            !BoosterValidator.validMultiplier(multiplier) ||
            !BoosterValidator.validDuration(duration) ||
            !BoosterValidator.validData(type, data)
        ) {
            CommandUtils.error(
                source,
                "common.invalid_booster"
            )
            return 0
        }

        if (give) {
            val stored =
                StoredBooster(
                    type = type,
                    multiplier = multiplier,
                    durationMillis = duration,
                    text = text,
                    sound = sound,
                    data = data
                        ?.lowercase()
                        ?.trim()
                )

            val result =
                BoosterStorageManager.give(
                    playerUuid,
                    stored
                )

            CommandUtils.success(
                source,
                "personal.given",
                "player" to profile.name,
                "booster" to displayName,
                "multiplier" to CommandUtils.formatMultiplier(multiplier),
                "duration" to CommandUtils.formatDuration(duration),
                "amount" to result.amount
            )

            return 1
        }

        val instance =
            CommandUtils.createInstance(
                source = source,
                type = type,
                multiplier = multiplier,
                durationMillis = duration,
                text = text,
                sound = sound,
                data = data
            )

        return when (
            val result =
                BoosterCoordinator.activatePersonal(
                    playerUuid,
                    instance
                )
        ) {
            is BoosterActivationResult.Activated -> {
                CommandUtils.success(
                    source,
                    "personal.activated",
                    "player" to profile.name,
                    "booster" to displayName,
                    "multiplier" to CommandUtils.formatMultiplier(multiplier)
                )
                1
            }

            is BoosterActivationResult.Queued -> {
                CommandUtils.success(
                    source,
                    "personal.queued",
                    "player" to profile.name,
                    "booster" to displayName,
                    "multiplier" to CommandUtils.formatMultiplier(multiplier),
                    "position" to result.position
                )
                1
            }

            BoosterActivationResult.UnknownBooster -> {
                CommandUtils.error(
                    source,
                    "common.unknown_booster",
                    "booster" to type
                )
                0
            }

            BoosterActivationResult.UnsupportedScope -> {
                CommandUtils.error(
                    source,
                    "common.unsupported_personal",
                    "booster" to displayName
                )
                0
            }

            BoosterActivationResult.InvalidBooster -> {
                CommandUtils.error(
                    source,
                    "common.invalid_booster"
                )
                0
            }
        }
    }

    private fun clearActiveFromAll(
        context: CommandContext<ServerCommandSource>
    ): Int {
        val type =
            StringArgumentType.getString(
                context,
                "booster"
            )

        val removed =
            BoosterCoordinator.clearPersonalActiveFromAll(
                type
            )

        CommandUtils.success(
            context.source,
            "personal.clear_active_all",
            "booster" to MessageManager.boosterName(type),
            "amount" to removed
        )

        return 1
    }

    private fun clearTypeFromAll(
        context: CommandContext<ServerCommandSource>
    ): Int {
        val type =
            StringArgumentType.getString(
                context,
                "booster"
            )

        val removed =
            BoosterCoordinator.clearPersonalTypeFromAll(
                type
            )

        CommandUtils.success(
            context.source,
            "personal.clear_type_all",
            "booster" to MessageManager.boosterName(type),
            "amount" to removed
        )

        return 1
    }

    private fun clearEverything(
        context: CommandContext<ServerCommandSource>
    ): Int {
        val removed =
            BoosterCoordinator.clearAllPersonal()

        CommandUtils.success(
            context.source,
            "personal.clear_all",
            "amount" to removed
        )

        return 1
    }

    private fun clearActiveFromPlayer(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val profile = CommandUtils.getSingleProfile(context, "player") ?: return 0
        val uuid = profile.id ?: run {
            CommandUtils.error(source, "common.player_uuid_failed")
            return 0
        }

        val type = StringArgumentType.getString(context, "booster")

        if (!BoosterRegistry.exists(type)) {
            CommandUtils.error(source, "common.unknown_booster", "booster" to type)
            return 0
        }

        val result = BoosterCoordinator.clearPersonalActive(uuid, type)
        val displayName = MessageManager.boosterName(type)

        if (result.removed == null) {
            CommandUtils.error(
                source,
                "personal.clear_active_player_none",
                "player" to profile.name,
                "booster" to displayName
            )
            return 0
        }

        if (result.next != null) {
            CommandUtils.success(
                source,
                "personal.clear_active_player_next",
                "player" to profile.name,
                "booster" to displayName,
                "multiplier" to CommandUtils.formatMultiplier(result.next.multiplier)
            )
        } else {
            CommandUtils.success(
                source,
                "personal.clear_active_player",
                "player" to profile.name,
                "booster" to displayName
            )
        }

        return 1
    }

    private fun clearTypeFromPlayer(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val profile = CommandUtils.getSingleProfile(context, "player") ?: return 0
        val uuid = profile.id ?: run {
            CommandUtils.error(source, "common.player_uuid_failed")
            return 0
        }

        val type = StringArgumentType.getString(context, "booster")

        if (!BoosterRegistry.exists(type)) {
            CommandUtils.error(source, "common.unknown_booster", "booster" to type)
            return 0
        }

        val removed = BoosterCoordinator.clearPersonalType(uuid, type)
        val displayName = MessageManager.boosterName(type)

        if (removed <= 0) {
            CommandUtils.error(
                source,
                "personal.clear_type_player_none",
                "player" to profile.name,
                "booster" to displayName
            )
            return 0
        }

        CommandUtils.success(
            source,
            "personal.clear_type_player",
            "player" to profile.name,
            "booster" to displayName,
            "amount" to removed
        )

        return 1
    }

    private fun clearEverythingFromPlayer(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val profile = CommandUtils.getSingleProfile(context, "player") ?: return 0
        val uuid = profile.id ?: run {
            CommandUtils.error(source, "common.player_uuid_failed")
            return 0
        }

        val removed = BoosterCoordinator.clearPersonalPlayer(uuid)

        if (removed <= 0) {
            CommandUtils.error(
                source,
                "personal.clear_player_none",
                "player" to profile.name
            )
            return 0
        }

        CommandUtils.success(
            source,
            "personal.clear_player",
            "player" to profile.name,
            "amount" to removed
        )

        return 1
    }

    private fun removeOne(
        context: CommandContext<ServerCommandSource>
    ): Int {
        val source =
            context.source

        val profile =
            CommandUtils.getSingleProfile(
                context,
                "player"
            ) ?: return 0

        val uuid =
            profile.id
                ?: return 0

        val type =
            StringArgumentType.getString(
                context,
                "booster"
            )

        val removed =
            BoosterStorageManager.removeOne(
                uuid,
                type
            )

        val displayName =
            MessageManager.boosterName(type)

        if (removed == null) {
            CommandUtils.error(
                source,
                "personal.storage_not_found",
                "player" to profile.name,
                "booster" to displayName
            )
            return 0
        }

        CommandUtils.success(
            source,
            "personal.storage_removed_one",
            "player" to profile.name,
            "booster" to displayName
        )

        return 1
    }

    private fun removeAll(
        context: CommandContext<ServerCommandSource>
    ): Int {
        val source =
            context.source

        val profile =
            CommandUtils.getSingleProfile(
                context,
                "player"
            ) ?: return 0

        val uuid =
            profile.id
                ?: return 0

        val type =
            StringArgumentType.getString(
                context,
                "booster"
            )

        val removed =
            BoosterStorageManager.removeAll(
                uuid,
                type
            )

        CommandUtils.success(
            source,
            "personal.storage_removed_all",
            "player" to profile.name,
            "booster" to MessageManager.boosterName(type),
            "amount" to removed
        )

        return 1
    }
}