package org.krripe.scobbleboosters.manager

import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.scobbleboosters.booster.BoosterRegistry
import org.krripe.scobbleboosters.config.BoosterConfigManager
import org.krripe.scobbleboosters.data.model.BoosterActivationResult
import org.krripe.scobbleboosters.data.model.BoosterInstance

sealed interface BoosterBagActivationResult {
    data class Activated(val booster: BoosterInstance) : BoosterBagActivationResult
    data class Queued(val booster: BoosterInstance, val position: Int) : BoosterBagActivationResult
    object NotFound : BoosterBagActivationResult
    object Disabled : BoosterBagActivationResult
    object Unsupported : BoosterBagActivationResult
}

object BoosterBagManager {

    fun activate(
        player: ServerPlayerEntity,
        storedBoosterId: String
    ): BoosterBagActivationResult {
        val stored = BoosterStorageManager.get(player.uuid)
            .firstOrNull { it.id == storedBoosterId }
            ?: return BoosterBagActivationResult.NotFound

        val definition = BoosterRegistry.get(stored.type)
            ?: return BoosterBagActivationResult.Unsupported

        if (!definition.supportsPersonal || !definition.supportsStorage) {
            return BoosterBagActivationResult.Unsupported
        }

        if (!BoosterConfigManager.isEnabled(stored.type)) {
            return BoosterBagActivationResult.Disabled
        }

        val taken = BoosterStorageManager.takeOne(
            player.uuid,
            storedBoosterId
        ) ?: return BoosterBagActivationResult.NotFound

        val instance = BoosterInstance(
            type = taken.type,
            multiplier = taken.multiplier,
            durationMillis = taken.durationMillis,
            text = taken.text,
            sound = taken.sound,
            data = taken.data,
            activatorName = player.name.string,
            activatorUuid = player.uuid
        )

        return when (val result = BoosterCoordinator.activatePersonal(player.uuid, instance)) {
            is BoosterActivationResult.Activated ->
                BoosterBagActivationResult.Activated(instance)

            is BoosterActivationResult.Queued ->
                BoosterBagActivationResult.Queued(
                    instance,
                    result.position
                )

            else -> {
                BoosterStorageManager.give(player.uuid, taken)
                BoosterBagActivationResult.Unsupported
            }
        }
    }
}