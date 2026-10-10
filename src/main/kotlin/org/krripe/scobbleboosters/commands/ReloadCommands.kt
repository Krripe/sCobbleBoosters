package org.krripe.scobbleboosters.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import org.krripe.scobbleboosters.SCobbleBoosters
import org.krripe.scobbleboosters.config.BoosterConfigManager
import org.krripe.scobbleboosters.config.MainConfigManager
import org.krripe.scobbleboosters.gui.active.ActiveBoostersConfigManager
import org.krripe.scobbleboosters.gui.storage.BoosterBagConfigManager
import org.krripe.scobbleboosters.manager.BoosterCoordinator
import org.krripe.scobbleboosters.manager.BossBarManager
import org.krripe.scobbleboosters.message.MessageManager
import org.krripe.scobbleboosters.webhook.WebhookConfigManager

object ReloadCommands {

    fun build(): LiteralArgumentBuilder<ServerCommandSource> {
        return CommandManager.literal("reload")
            .executes(::reload)
    }

    private fun reload(
        context: CommandContext<ServerCommandSource>
    ): Int {
        val source =
            context.source

        val server =
            source.server

        val now =
            System.currentTimeMillis()

        return runCatching {
            MainConfigManager.reload()
            MessageManager.reload()
            BoosterConfigManager.reload()
            BoosterBagConfigManager.reload()
            ActiveBoostersConfigManager.reload()
            WebhookConfigManager.reloadAll()

            BoosterCoordinator.syncAllPersonalWithGlobal(
                now
            )

            BossBarManager.reset()
            BossBarManager.refresh(
                server,
                now
            )

            CommandUtils.success(
                source,
                "reload.success"
            )

            1
        }.getOrElse {
            SCobbleBoosters.LOGGER.error(
                "Failed to reload sCobbleBoosters configs",
                it
            )

            CommandUtils.error(
                source,
                "reload.failed"
            )

            0
        }
    }
}