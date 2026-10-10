package org.krripe.scobbleboosters.manager

import org.krripe.scobbleboosters.booster.BoosterRegistry
import org.krripe.scobbleboosters.config.BoosterConfigManager
import org.krripe.scobbleboosters.data.model.*
import org.krripe.scobbleboosters.webhook.WebhookManager
import org.krripe.scobbleboosters.webhook.model.WebhookEventContext
import org.krripe.scobbleboosters.webhook.model.WebhookEventType
import java.util.Locale
import java.util.UUID

object BoosterCoordinator {

    fun activateGlobal(instance: BoosterInstance): BoosterActivationResult {
        val now = System.currentTimeMillis()
        val before = GlobalBoosterManager.getActive(instance.type)
        val result = GlobalBoosterManager.activate(instance, now)

        when (result) {
            is BoosterActivationResult.Activated -> {
                if (before != null && before.id != result.booster.id) {
                    val position = GlobalBoosterManager
                        .getRuntime(instance.type)
                        ?.queuePosition(before.id)
                        ?: -1

                    if (position > 0) {
                        webhook(
                            WebhookEventType.QUEUED,
                            BoosterScope.GLOBAL,
                            before,
                            queuePosition = position,
                            reason = "preempted",
                            now = now
                        )
                    }
                }

                webhook(
                    WebhookEventType.ACTIVATED,
                    BoosterScope.GLOBAL,
                    result.booster,
                    reason = if (before == null) "direct" else "higher_priority",
                    now = now
                )
            }

            is BoosterActivationResult.Queued -> {
                webhook(
                    WebhookEventType.QUEUED,
                    BoosterScope.GLOBAL,
                    result.booster,
                    queuePosition = result.position,
                    reason = "lower_or_equal_priority",
                    now = now
                )
            }

            else -> Unit
        }

        syncPersonal(instance.type, now)
        return result
    }

    fun stopGlobal(typeId: String): BoosterControlResult {
        val now = System.currentTimeMillis()
        val result = GlobalBoosterManager.stop(typeId, now)
        syncPersonal(typeId, now)
        return result
    }

    fun startGlobal(typeId: String): BoosterControlResult {
        val now = System.currentTimeMillis()
        val result = GlobalBoosterManager.start(typeId, now)

        if (result == BoosterControlResult.SUCCESS) {
            GlobalBoosterManager.getActive(typeId)?.let {
                webhook(
                    WebhookEventType.RESUMED,
                    BoosterScope.GLOBAL,
                    it,
                    reason = "manual_start",
                    now = now
                )
            }
        }

        syncPersonal(typeId, now)
        return result
    }

    fun clearGlobalActive(typeId: String): BoosterClearResult {
        val now = System.currentTimeMillis()
        val result = GlobalBoosterManager.clearActive(typeId, now)

        result.removed?.let {
            webhook(
                WebhookEventType.CLEARED,
                BoosterScope.GLOBAL,
                it,
                reason = "clear_active",
                now = now
            )
        }

        result.next?.let {
            webhook(
                WebhookEventType.ACTIVATED,
                BoosterScope.GLOBAL,
                it,
                reason = "queue_after_clear",
                now = now
            )
        }

        syncPersonal(typeId, now)
        return result
    }

    fun clearGlobalType(typeId: String): Int {
        val now = System.currentTimeMillis()

        val before = buildList {
            GlobalBoosterManager.getActive(typeId)?.let {
                add(it.snapshot(now))
            }

            addAll(
                GlobalBoosterManager.getQueue(typeId)
                    .map { it.snapshot(now) }
            )
        }

        val removed = GlobalBoosterManager.clearType(typeId, now)

        if (removed > 0) {
            for (booster in before) {
                webhook(
                    WebhookEventType.CLEARED,
                    BoosterScope.GLOBAL,
                    booster,
                    reason = "clear_type",
                    now = now
                )
            }
        }

        syncPersonal(typeId, now)
        return removed
    }

    fun clearAllGlobal(): Int {
        val now = System.currentTimeMillis()
        val affectedTypes = GlobalBoosterManager.types()

        val before = affectedTypes.flatMap { type ->
            buildList {
                GlobalBoosterManager.getActive(type)?.let {
                    add(it.snapshot(now))
                }

                addAll(
                    GlobalBoosterManager.getQueue(type)
                        .map { it.snapshot(now) }
                )
            }
        }

        val removed = GlobalBoosterManager.clearEverything(now)

        if (removed > 0) {
            for (booster in before) {
                webhook(
                    WebhookEventType.CLEARED,
                    BoosterScope.GLOBAL,
                    booster,
                    reason = "clear_all",
                    now = now
                )
            }
        }

        for (type in affectedTypes) {
            syncPersonal(type, now)
        }

        return removed
    }

    fun activatePersonal(
        playerUuid: UUID,
        instance: BoosterInstance
    ): BoosterActivationResult {
        val now = System.currentTimeMillis()

        val before = PersonalBoosterManager.getActive(
            playerUuid,
            instance.type
        )

        val globalPaused =
            BoosterConfigManager.isEnabled(instance.type) &&
                    GlobalBoosterManager.isEffectActive(instance.type, now)

        val result = PersonalBoosterManager.activate(
            playerUuid = playerUuid,
            instance = instance,
            globalPaused = globalPaused,
            now = now
        )

        when (result) {
            is BoosterActivationResult.Activated -> {
                if (before != null && before.id != result.booster.id) {
                    val position = PersonalBoosterManager
                        .getRuntime(playerUuid, instance.type)
                        ?.queuePosition(before.id)
                        ?: -1

                    if (position > 0) {
                        webhook(
                            WebhookEventType.QUEUED,
                            BoosterScope.PERSONAL,
                            before,
                            playerUuid = playerUuid,
                            queuePosition = position,
                            reason = "preempted",
                            now = now
                        )
                    }
                }

                webhook(
                    WebhookEventType.ACTIVATED,
                    BoosterScope.PERSONAL,
                    result.booster,
                    playerUuid = playerUuid,
                    reason = if (globalPaused) "activated_while_global_active" else "direct",
                    now = now
                )
            }

            is BoosterActivationResult.Queued -> {
                webhook(
                    WebhookEventType.QUEUED,
                    BoosterScope.PERSONAL,
                    result.booster,
                    playerUuid = playerUuid,
                    queuePosition = result.position,
                    reason = "lower_or_equal_priority",
                    now = now
                )
            }

            else -> Unit
        }

        return result
    }

    fun clearPersonalActive(
        playerUuid: UUID,
        typeId: String
    ): BoosterClearResult {
        val now = System.currentTimeMillis()

        val result = PersonalBoosterManager.clearActive(
            playerUuid,
            typeId,
            now
        )

        result.removed?.let {
            webhook(
                WebhookEventType.CLEARED,
                BoosterScope.PERSONAL,
                it,
                playerUuid = playerUuid,
                reason = "clear_active",
                now = now
            )
        }

        result.next?.let {
            webhook(
                WebhookEventType.ACTIVATED,
                BoosterScope.PERSONAL,
                it,
                playerUuid = playerUuid,
                reason = "queue_after_clear",
                now = now
            )
        }

        return result
    }

    fun clearPersonalType(
        playerUuid: UUID,
        typeId: String
    ): Int {
        val now = System.currentTimeMillis()
        val before = personalInstances(playerUuid, typeId, now)

        val removed = PersonalBoosterManager.clearType(
            playerUuid,
            typeId,
            now
        )

        if (removed > 0) {
            for (booster in before) {
                webhook(
                    WebhookEventType.CLEARED,
                    BoosterScope.PERSONAL,
                    booster,
                    playerUuid = playerUuid,
                    reason = "clear_type",
                    now = now
                )
            }
        }

        return removed
    }

    fun clearPersonalPlayer(playerUuid: UUID): Int {
        val now = System.currentTimeMillis()

        val before = PersonalBoosterManager.types(playerUuid)
            .flatMap { type ->
                personalInstances(playerUuid, type, now)
            }

        val removed = PersonalBoosterManager.clearPlayer(
            playerUuid,
            now
        )

        if (removed > 0) {
            for (booster in before) {
                webhook(
                    WebhookEventType.CLEARED,
                    BoosterScope.PERSONAL,
                    booster,
                    playerUuid = playerUuid,
                    reason = "clear_player",
                    now = now
                )
            }
        }

        return removed
    }

    fun clearPersonalActiveFromAll(typeId: String): Int {
        val now = System.currentTimeMillis()

        val before = PersonalBoosterManager.playerUuids()
            .mapNotNull { playerUuid ->
                PersonalBoosterManager
                    .getActive(playerUuid, typeId)
                    ?.snapshot(now)
                    ?.let { playerUuid to it }
            }

        val removed = PersonalBoosterManager.clearActiveFromAll(
            typeId,
            now
        )

        for ((playerUuid, booster) in before) {
            webhook(
                WebhookEventType.CLEARED,
                BoosterScope.PERSONAL,
                booster,
                playerUuid = playerUuid,
                reason = "clear_active_from_all",
                now = now
            )

            val next = PersonalBoosterManager.getActive(
                playerUuid,
                typeId
            )

            if (next != null && next.id != booster.id) {
                webhook(
                    WebhookEventType.ACTIVATED,
                    BoosterScope.PERSONAL,
                    next,
                    playerUuid = playerUuid,
                    reason = "queue_after_clear",
                    now = now
                )
            }
        }

        return removed
    }

    fun clearPersonalTypeFromAll(typeId: String): Int {
        val now = System.currentTimeMillis()

        val before = PersonalBoosterManager.playerUuids()
            .flatMap { playerUuid ->
                personalInstances(
                    playerUuid,
                    typeId,
                    now
                ).map { playerUuid to it }
            }

        val removed = PersonalBoosterManager.clearTypeFromAll(
            typeId,
            now
        )

        if (removed > 0) {
            for ((playerUuid, booster) in before) {
                webhook(
                    WebhookEventType.CLEARED,
                    BoosterScope.PERSONAL,
                    booster,
                    playerUuid = playerUuid,
                    reason = "clear_type_from_all",
                    now = now
                )
            }
        }

        return removed
    }

    fun clearAllPersonal(): Int {
        val now = System.currentTimeMillis()

        val before = PersonalBoosterManager.playerUuids()
            .flatMap { playerUuid ->
                PersonalBoosterManager.types(playerUuid)
                    .flatMap { type ->
                        personalInstances(
                            playerUuid,
                            type,
                            now
                        ).map { playerUuid to it }
                    }
            }

        val removed = PersonalBoosterManager.clearEverything(now)

        if (removed > 0) {
            for ((playerUuid, booster) in before) {
                webhook(
                    WebhookEventType.CLEARED,
                    BoosterScope.PERSONAL,
                    booster,
                    playerUuid = playerUuid,
                    reason = "clear_all",
                    now = now
                )
            }
        }

        return removed
    }

    fun tick(now: Long = System.currentTimeMillis()) {
        val globalTransitions = GlobalBoosterManager.tick(now)
        val affectedGlobalTypes = mutableSetOf<String>()

        for (transition in globalTransitions) {
            affectedGlobalTypes += transition.expired.type

            webhook(
                WebhookEventType.EXPIRED,
                BoosterScope.GLOBAL,
                transition.expired,
                reason = "timer",
                now = now
            )

            transition.next?.let {
                webhook(
                    WebhookEventType.ACTIVATED,
                    BoosterScope.GLOBAL,
                    it,
                    reason = "queue_after_expiration",
                    now = now
                )
            }
        }

        for (type in affectedGlobalTypes) {
            syncPersonal(type, now)
        }

        val personalTransitions = PersonalBoosterManager.tick(now)

        for (transition in personalTransitions) {
            webhook(
                WebhookEventType.EXPIRED,
                BoosterScope.PERSONAL,
                transition.expired,
                playerUuid = transition.playerUuid,
                reason = "timer",
                now = now
            )

            transition.next?.let {
                webhook(
                    WebhookEventType.ACTIVATED,
                    BoosterScope.PERSONAL,
                    it,
                    playerUuid = transition.playerUuid,
                    reason = "queue_after_expiration",
                    now = now
                )
            }
        }
    }

    fun syncAllPersonalWithGlobal(
        now: Long = System.currentTimeMillis()
    ) {
        for (type in BoosterRegistry.ids()) {
            syncPersonal(type, now)
        }
    }

    fun reset() {
        GlobalBoosterManager.reset()
        PersonalBoosterManager.reset()
    }

    private fun syncPersonal(
        typeId: String,
        now: Long
    ) {
        val type = typeId
            .lowercase(Locale.ROOT)
            .trim()

        val globalActive =
            BoosterConfigManager.isEnabled(type) &&
                    GlobalBoosterManager.isEffectActive(type, now)

        val previouslyPaused = if (!globalActive) {
            PersonalBoosterManager.playerUuids()
                .mapNotNull { playerUuid ->
                    val runtime = PersonalBoosterManager
                        .getRuntime(playerUuid, type)
                        ?: return@mapNotNull null

                    if (!runtime.globalPaused || runtime.active == null) {
                        return@mapNotNull null
                    }

                    playerUuid
                }
        } else {
            emptyList()
        }

        PersonalBoosterManager.syncGlobalPause(
            typeId = type,
            paused = globalActive,
            now = now
        )

        if (!globalActive) {
            for (playerUuid in previouslyPaused) {
                val active = PersonalBoosterManager
                    .getActive(playerUuid, type)
                    ?: continue

                if (
                    PersonalBoosterManager.isEffectActive(
                        playerUuid,
                        type,
                        now
                    )
                ) {
                    webhook(
                        WebhookEventType.RESUMED,
                        BoosterScope.PERSONAL,
                        active,
                        playerUuid = playerUuid,
                        reason = "global_booster_ended",
                        now = now
                    )
                }
            }
        }
    }

    private fun personalInstances(
        playerUuid: UUID,
        typeId: String,
        now: Long
    ): List<BoosterInstance> {
        return buildList {
            PersonalBoosterManager
                .getActive(playerUuid, typeId)
                ?.let {
                    add(it.snapshot(now))
                }

            addAll(
                PersonalBoosterManager
                    .getQueue(playerUuid, typeId)
                    .map { it.snapshot(now) }
            )
        }
    }

    private fun webhook(
        event: WebhookEventType,
        scope: BoosterScope,
        booster: BoosterInstance,
        playerUuid: UUID? = null,
        queuePosition: Int? = null,
        reason: String = "",
        now: Long
    ) {
        WebhookManager.dispatch(
            WebhookEventContext(
                event = event,
                scope = scope,
                booster = booster,
                playerUuid = playerUuid,
                queuePosition = queuePosition,
                reason = reason,
                now = now
            )
        )
    }
}