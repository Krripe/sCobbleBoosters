package org.krripe.scobbleboosters.booster.resolver

import org.krripe.scobbleboosters.config.BoosterConfigManager
import org.krripe.scobbleboosters.data.model.BoosterInstance
import org.krripe.scobbleboosters.data.model.BoosterScope
import org.krripe.scobbleboosters.manager.GlobalBoosterManager
import org.krripe.scobbleboosters.manager.PersonalBoosterManager
import java.util.UUID

data class ResolvedBooster(
    val instance: BoosterInstance,
    val scope: BoosterScope
)

object BoosterResolver {

    fun resolve(
        typeId: String,
        playerUuid: UUID?,
        now: Long = System.currentTimeMillis()
    ): ResolvedBooster? {
        if (!BoosterConfigManager.isEnabled(typeId)) return null

        val global = GlobalBoosterManager.getActive(typeId)

        if (global != null && GlobalBoosterManager.isEffectActive(typeId, now)) {
            return ResolvedBooster(global, BoosterScope.GLOBAL)
        }

        if (playerUuid != null) {
            val personal = PersonalBoosterManager.getActive(playerUuid, typeId)

            if (personal != null && PersonalBoosterManager.isEffectActive(playerUuid, typeId, now)) {
                return ResolvedBooster(personal, BoosterScope.PERSONAL)
            }
        }

        return null
    }

    fun multiplier(
        typeId: String,
        playerUuid: UUID?,
        now: Long = System.currentTimeMillis()
    ): Float {
        return resolve(typeId, playerUuid, now)?.instance?.multiplier ?: 1f
    }
}