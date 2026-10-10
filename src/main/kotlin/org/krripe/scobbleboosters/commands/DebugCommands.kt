package org.krripe.scobbleboosters.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import org.krripe.scobbleboosters.booster.BoosterRegistry
import org.krripe.scobbleboosters.debug.DebugManager
import org.krripe.scobbleboosters.message.MessageManager

object DebugCommands {

    fun build(): LiteralArgumentBuilder<ServerCommandSource> {
        return CommandManager.literal("debug")
            .then(
                CommandManager.literal("on")
                    .executes(::enableAll)
                    .then(
                        CommandManager.argument(
                            "booster",
                            StringArgumentType.word()
                        )
                            .suggests(
                                CommandSuggestions.boosters
                            )
                            .executes(::enableBooster)
                    )
            )
            .then(
                CommandManager.literal("off")
                    .executes(::disableAll)
                    .then(
                        CommandManager.argument(
                            "booster",
                            StringArgumentType.word()
                        )
                            .suggests(
                                CommandSuggestions.boosters
                            )
                            .executes(::disableBooster)
                    )
            )
    }

    private fun enableAll(
        context: CommandContext<ServerCommandSource>
    ): Int {
        DebugManager.enableAll()

        CommandUtils.success(
            context.source,
            "debug.enabled_all"
        )

        return 1
    }

    private fun disableAll(
        context: CommandContext<ServerCommandSource>
    ): Int {
        DebugManager.disableAll()

        CommandUtils.success(
            context.source,
            "debug.disabled_all"
        )

        return 1
    }

    private fun enableBooster(
        context: CommandContext<ServerCommandSource>
    ): Int {
        val type =
            StringArgumentType.getString(
                context,
                "booster"
            )

        if (!BoosterRegistry.exists(type)) {
            CommandUtils.error(
                context.source,
                "common.unknown_booster",
                "booster" to type
            )
            return 0
        }

        DebugManager.enable(type)

        CommandUtils.success(
            context.source,
            "debug.enabled_type",
            "booster" to MessageManager.boosterName(type)
        )

        return 1
    }

    private fun disableBooster(
        context: CommandContext<ServerCommandSource>
    ): Int {
        val type =
            StringArgumentType.getString(
                context,
                "booster"
            )

        if (!BoosterRegistry.exists(type)) {
            CommandUtils.error(
                context.source,
                "common.unknown_booster",
                "booster" to type
            )
            return 0
        }

        DebugManager.disable(type)

        CommandUtils.success(
            context.source,
            "debug.disabled_type",
            "booster" to MessageManager.boosterName(type)
        )

        return 1
    }
}