package org.krripe.scobbleboosters.manager

import net.minecraft.server.MinecraftServer
import org.krripe.scobbleboosters.gui.active.ActiveBoostersGui

object BoosterTimerManager {
    private const val UPDATE_INTERVAL_MS = 1000L

    private var lastUpdateMillis = 0L

    fun tick(server: MinecraftServer) {
        val now = System.currentTimeMillis()

        if (now - lastUpdateMillis >= UPDATE_INTERVAL_MS) {
            lastUpdateMillis = now

            BoosterCoordinator.tick(now)
            BoosterPresentationManager.refresh(
                server,
                now
            )
            ActiveBoostersGui.tick(
                server,
                now
            )
        }

        BoosterPersistenceManager.tick()
    }

    fun reset() {
        lastUpdateMillis = 0L
    }
}