package org.krripe.scobbleboosters

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import org.krripe.scobbleboosters.boosters.BoosterService
import org.krripe.scobbleboosters.commands.SBCommands
import org.krripe.scobbleboosters.config.SBConfig
import org.krripe.scobbleboosters.lang.LangManager
import org.krripe.scobbleboosters.utils.SoundHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SCobbleBoosters : ModInitializer {

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger("sCobbleBoosters")
    }

    override fun onInitialize() {
        SBConfig.load()
        LangManager.load()

        BoosterService.init()
        SBCommands.register()

        ServerTickEvents.START_SERVER_TICK.register { server ->
            BoosterService.tick(server)
        }

        ServerLifecycleEvents.SERVER_STOPPING.register {
            LOGGER.info("Saving boosters before server stop")
            BoosterService.saveNow()
        }

        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            BoosterService.addPlayer(handler.player)
        }

        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            BoosterService.removePlayer(handler.player)
            SoundHelper.resetPlayer(handler.player.uuid)
        }
    }
}