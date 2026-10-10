package org.krripe.scobbleboosters.commands

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import org.krripe.scobbleboosters.gui.storage.BoosterBagGui

object BoosterCommands {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("booster")
                .then(
                    CommandManager.literal("bag")
                        .requires(CommandPermissions::boosterBag)
                        .executes { context ->
                            val player = context.source.player ?: return@executes 0
                            BoosterBagGui.open(player)
                            1
                        }
                )
        )
    }
}