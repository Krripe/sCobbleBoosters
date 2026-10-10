package org.krripe.scobbleboosters.commands

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource

object SBCommands {

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("sb")
                .requires(CommandPermissions::admin)
                .then(GlobalCommands.build())
                .then(PersonalCommands.build())
                .then(DebugCommands.build())
                .then(ReloadCommands.build())
        )
    }
}