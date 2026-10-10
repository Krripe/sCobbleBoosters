package org.krripe.scobbleboosters.commands

import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import org.krripe.scobbleboosters.booster.BoosterRegistry
import org.krripe.scobbleboosters.data.model.BoosterActivationResult
import org.krripe.scobbleboosters.data.model.BoosterControlResult
import org.krripe.scobbleboosters.manager.BoosterCoordinator
import org.krripe.scobbleboosters.manager.GlobalBoosterManager
import org.krripe.scobbleboosters.message.MessageManager

object GlobalCommands {

    fun build(): LiteralArgumentBuilder<ServerCommandSource> {
        return CommandManager.literal("global")
            .then(
                CommandManager.literal("activate")
                    .then(
                        CommandManager.literal("type")
                            .then(
                                CommandManager.argument("pokemon_type", StringArgumentType.word())
                                    .suggests(CommandSuggestions.pokemonTypes)
                                    .then(activationTail("type", true))
                            )
                    )
                    .then(
                        CommandManager.argument("booster", StringArgumentType.word())
                            .suggests(CommandSuggestions.boosters)
                            .then(activationTail(null, false))
                    )
            )
            .then(
                CommandManager.literal("stop")
                    .then(
                        CommandManager.argument("booster", StringArgumentType.word())
                            .suggests(CommandSuggestions.boosters)
                            .executes(::stop)
                    )
            )
            .then(
                CommandManager.literal("start")
                    .then(
                        CommandManager.argument("booster", StringArgumentType.word())
                            .suggests(CommandSuggestions.boosters)
                            .executes(::start)
                    )
            )
            .then(
                CommandManager.literal("clear")
                    .then(
                        CommandManager.literal("all")
                            .executes(::clearEverything)
                            .then(
                                CommandManager.argument("booster", StringArgumentType.word())
                                    .suggests(CommandSuggestions.boosters)
                                    .executes(::clearType)
                            )
                    )
                    .then(
                        CommandManager.argument("booster", StringArgumentType.word())
                            .suggests(CommandSuggestions.boosters)
                            .executes(::clearActive)
                    )
            )
            .then(
                CommandManager.literal("info")
                    .then(
                        CommandManager.argument("booster", StringArgumentType.word())
                            .suggests(CommandSuggestions.boosters)
                            .executes(::info)
                    )
            )
    }

    private fun activationTail(forcedType: String?, hasData: Boolean) =
        CommandManager.argument("multiplier", FloatArgumentType.floatArg(1f))
            .then(
                CommandManager.argument("time", StringArgumentType.word())
                    .then(
                        CommandManager.argument("text", StringArgumentType.string())
                            .then(
                                CommandManager.argument("sound", StringArgumentType.greedyString())
                                    .suggests(CommandSuggestions.sounds)
                                    .executes { context ->
                                        activate(
                                            context,
                                            forcedType,
                                            if (hasData) StringArgumentType.getString(context, "pokemon_type") else null
                                        )
                                    }
                            )
                    )
            )

    private fun activate(
        context: CommandContext<ServerCommandSource>,
        forcedType: String?,
        data: String?
    ): Int {
        val source = context.source
        val rawType =
            forcedType
                ?: StringArgumentType.getString(
                    context,
                    "booster"
                )

        val type =
            CommandUtils.validateBooster(
                source,
                rawType
            ) ?: return 0

        val definition =
            BoosterRegistry.get(type)
                ?: return 0

        val displayName =
            MessageManager.boosterName(type)

        if (!definition.supportsGlobal) {
            CommandUtils.error(
                source,
                "common.unsupported_global",
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

        val booster =
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
                BoosterCoordinator.activateGlobal(
                    booster
                )
        ) {
            is BoosterActivationResult.Activated -> {
                CommandUtils.success(
                    source,
                    "global.activated",
                    "booster" to displayName,
                    "multiplier" to CommandUtils.formatMultiplier(multiplier),
                    "duration" to CommandUtils.formatDuration(duration)
                )
                1
            }

            is BoosterActivationResult.Queued -> {
                CommandUtils.success(
                    source,
                    "global.queued",
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
                    "common.unsupported_global",
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

    private fun stop(
        context: CommandContext<ServerCommandSource>
    ): Int {
        val source = context.source
        val type =
            StringArgumentType.getString(
                context,
                "booster"
            )

        val displayName =
            MessageManager.boosterName(type)

        return when (
            BoosterCoordinator.stopGlobal(type)
        ) {
            BoosterControlResult.SUCCESS -> {
                CommandUtils.success(
                    source,
                    "global.paused",
                    "booster" to displayName
                )
                1
            }

            BoosterControlResult.ALREADY_PAUSED -> {
                CommandUtils.error(
                    source,
                    "global.already_paused",
                    "booster" to displayName
                )
                0
            }

            BoosterControlResult.NOT_FOUND -> {
                CommandUtils.error(
                    source,
                    "global.not_found",
                    "booster" to displayName
                )
                0
            }

            BoosterControlResult.UNKNOWN_BOOSTER -> {
                CommandUtils.error(
                    source,
                    "common.unknown_booster",
                    "booster" to type
                )
                0
            }

            BoosterControlResult.UNSUPPORTED_SCOPE -> {
                CommandUtils.error(
                    source,
                    "common.unsupported_global",
                    "booster" to displayName
                )
                0
            }

            BoosterControlResult.ALREADY_RUNNING -> 0
        }
    }

    private fun start(
        context: CommandContext<ServerCommandSource>
    ): Int {
        val source = context.source
        val type =
            StringArgumentType.getString(
                context,
                "booster"
            )

        val displayName =
            MessageManager.boosterName(type)

        return when (
            BoosterCoordinator.startGlobal(type)
        ) {
            BoosterControlResult.SUCCESS -> {
                CommandUtils.success(
                    source,
                    "global.resumed",
                    "booster" to displayName
                )
                1
            }

            BoosterControlResult.ALREADY_RUNNING -> {
                CommandUtils.error(
                    source,
                    "global.already_running",
                    "booster" to displayName
                )
                0
            }

            BoosterControlResult.NOT_FOUND -> {
                CommandUtils.error(
                    source,
                    "global.not_found",
                    "booster" to displayName
                )
                0
            }

            BoosterControlResult.UNKNOWN_BOOSTER -> {
                CommandUtils.error(
                    source,
                    "common.unknown_booster",
                    "booster" to type
                )
                0
            }

            BoosterControlResult.UNSUPPORTED_SCOPE -> {
                CommandUtils.error(
                    source,
                    "common.unsupported_global",
                    "booster" to displayName
                )
                0
            }

            BoosterControlResult.ALREADY_PAUSED -> 0
        }
    }

    private fun clearActive(
        context: CommandContext<ServerCommandSource>
    ): Int {
        val source = context.source
        val type =
            StringArgumentType.getString(
                context,
                "booster"
            )

        val displayName =
            MessageManager.boosterName(type)

        val result =
            BoosterCoordinator.clearGlobalActive(
                type
            )

        if (result.removed == null) {
            CommandUtils.error(
                source,
                "global.not_found",
                "booster" to displayName
            )
            return 0
        }

        if (result.next != null) {
            CommandUtils.success(
                source,
                "global.clear_active_next",
                "booster" to displayName,
                "multiplier" to CommandUtils.formatMultiplier(
                    result.next.multiplier
                )
            )
        } else {
            CommandUtils.success(
                source,
                "global.clear_active",
                "booster" to displayName
            )
        }

        return 1
    }

    private fun clearType(
        context: CommandContext<ServerCommandSource>
    ): Int {
        val source = context.source
        val type =
            StringArgumentType.getString(
                context,
                "booster"
            )

        val removed =
            BoosterCoordinator.clearGlobalType(
                type
            )

        CommandUtils.success(
            source,
            "global.clear_type",
            "amount" to removed,
            "booster" to MessageManager.boosterName(type)
        )

        return 1
    }

    private fun clearEverything(
        context: CommandContext<ServerCommandSource>
    ): Int {
        val removed =
            BoosterCoordinator.clearAllGlobal()

        CommandUtils.success(
            context.source,
            "global.clear_all",
            "amount" to removed
        )

        return 1
    }

    private fun info(
        context: CommandContext<ServerCommandSource>
    ): Int {
        val source = context.source
        val type =
            StringArgumentType.getString(
                context,
                "booster"
            )

        val displayName =
            MessageManager.boosterName(type)

        val runtime =
            GlobalBoosterManager.getRuntime(type)

        if (runtime?.active == null) {
            CommandUtils.error(
                source,
                "global.info_empty",
                "booster" to displayName
            )
            return 0
        }

        val now =
            System.currentTimeMillis()

        val active =
            runtime.active!!

        CommandUtils.success(
            source,
            "global.info_header",
            "booster" to displayName,
            "state" to MessageManager.stateName(
                runtime.state()
            ),
            "multiplier" to CommandUtils.formatMultiplier(
                active.multiplier
            ),
            "remaining" to CommandUtils.formatDuration(
                active.remaining(now)
            ),
            "queue" to runtime.queueSize()
        )

        runtime.queue.forEachIndexed {
                index,
                queued ->

            val dataSuffix =
                queued.data
                    ?.takeIf { it.isNotBlank() }
                    ?.let { " &8| &b$it" }
                    ?: ""

            CommandUtils.success(
                source,
                "global.info_queue",
                "position" to index + 1,
                "multiplier" to CommandUtils.formatMultiplier(
                    queued.multiplier
                ),
                "remaining" to CommandUtils.formatDuration(
                    queued.remaining(now)
                ),
                "data_suffix" to dataSuffix
            )
        }

        return 1
    }
}