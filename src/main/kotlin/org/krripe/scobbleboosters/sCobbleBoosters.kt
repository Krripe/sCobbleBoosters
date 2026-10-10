package org.krripe.scobbleboosters

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import org.krripe.scobbleboosters.booster.effect.BoosterEffects
import org.krripe.scobbleboosters.commands.SBCommands
import org.krripe.scobbleboosters.config.BoosterConfigManager
import org.krripe.scobbleboosters.config.MainConfigManager
import org.krripe.scobbleboosters.debug.DebugManager
import org.krripe.scobbleboosters.manager.BoosterCoordinator
import org.krripe.scobbleboosters.manager.BoosterPersistenceManager
import org.krripe.scobbleboosters.manager.BoosterPresentationManager
import org.krripe.scobbleboosters.manager.BoosterStorageManager
import org.krripe.scobbleboosters.manager.BoosterTimerManager
import org.krripe.scobbleboosters.commands.BoosterCommands
import org.krripe.scobbleboosters.gui.active.ActiveBoostersConfigManager
import org.krripe.scobbleboosters.gui.active.ActiveBoostersGui
import org.krripe.scobbleboosters.gui.storage.BoosterBagConfigManager
import org.krripe.scobbleboosters.message.MessageManager
import org.krripe.scobbleboosters.webhook.WebhookManager
import org.krripe.scobbleboosters.webhook.WebhookConfigManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SCobbleBoosters : ModInitializer {

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger("sCobbleBoosters")
    }

    override fun onInitialize() {
        MainConfigManager.load()
        MessageManager.load()
        BoosterConfigManager.loadAll()
        BoosterBagConfigManager.load()
        ActiveBoostersConfigManager.load()
        WebhookConfigManager.loadAll()

        BoosterEffects.register()

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            SBCommands.register(dispatcher)
            BoosterCommands.register(dispatcher)
        }

        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            DebugManager.setServer(server)
            WebhookManager.initialize(server)
            BoosterPersistenceManager.load(server)
            BoosterPresentationManager.initialize(server)
        }

        ServerPlayConnectionEvents.JOIN.register { handler, _, server ->
            BoosterPresentationManager.onPlayerJoin(server, handler.player)
        }

        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            BoosterPresentationManager.onPlayerDisconnect(handler.player)
        }

        ServerTickEvents.END_SERVER_TICK.register { server ->
            BoosterTimerManager.tick(server)
        }

        ServerLifecycleEvents.SERVER_STOPPING.register {
            BoosterPersistenceManager.saveBlocking()
            ActiveBoostersGui.reset()
        }

        ServerLifecycleEvents.SERVER_STOPPED.register {
            DebugManager.reset()
            WebhookManager.reset()
            BoosterPresentationManager.reset()
            BoosterTimerManager.reset()
            BoosterPersistenceManager.reset()
            BoosterCoordinator.reset()
            BoosterStorageManager.reset()
        }

        LOGGER.info("sCobbleBoosters initialized")
    }
}