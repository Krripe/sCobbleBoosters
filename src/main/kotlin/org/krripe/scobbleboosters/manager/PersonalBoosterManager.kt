package org.krripe.scobbleboosters.manager

import org.krripe.scobbleboosters.booster.BoosterRegistry
import org.krripe.scobbleboosters.booster.BoosterValidator
import org.krripe.scobbleboosters.config.BoosterConfigManager
import org.krripe.scobbleboosters.data.model.*
import org.krripe.scobbleboosters.data.persistence.BoosterRuntimeData
import java.util.Locale
import java.util.UUID

object PersonalBoosterManager {
    private val runtimes = linkedMapOf<UUID, MutableMap<String, BoosterRuntime>>()

    fun activate(playerUuid: UUID, instance: BoosterInstance, globalPaused: Boolean, now: Long = System.currentTimeMillis()): BoosterActivationResult {

        if (!BoosterValidator.validInstance(instance)) {
            return BoosterActivationResult.InvalidBooster
        }

        val type = normalize(instance.type)
        val definition = BoosterRegistry.get(type) ?: return BoosterActivationResult.UnknownBooster
        if (!definition.supportsPersonal) return BoosterActivationResult.UnsupportedScope

        val playerRuntimes = runtimes.getOrPut(playerUuid) { linkedMapOf() }
        val runtime = playerRuntimes.getOrPut(type) { BoosterRuntime() }

        advanceExpired(runtime, now)

        if (runtime.globalPaused != globalPaused) {
            runtime.globalPaused = globalPaused
            runtime.syncTimer(now)
        }

        val booster = if (instance.type == type) instance else instance.copy(type = type)
        val activated = runtime.add(booster, now)

        return if (activated) {
            BoosterActivationResult.Activated(booster)
        } else {
            BoosterActivationResult.Queued(
                booster,
                runtime.queuePosition(booster.id)
            )
        }
    }

    fun syncGlobalPause(typeId: String, paused: Boolean, now: Long = System.currentTimeMillis()) {
        val type = normalize(typeId)

        for (playerRuntimes in runtimes.values) {
            val runtime = playerRuntimes[type] ?: continue
            if (runtime.globalPaused == paused) continue

            runtime.globalPaused = paused
            runtime.syncTimer(now)
            advanceExpired(runtime, now)
        }

        cleanupEmpty()
    }

    fun clearActive(playerUuid: UUID, typeId: String, now: Long = System.currentTimeMillis()): BoosterClearResult {
        val type = normalize(typeId)
        val playerRuntimes = runtimes[playerUuid] ?: return BoosterClearResult(null, null)
        val runtime = playerRuntimes[type] ?: return BoosterClearResult(null, null)

        val removed = runtime.clearActive(now)
        val next = if (removed != null) runtime.startNext(now) else null

        cleanup(playerUuid, type, runtime)
        return BoosterClearResult(removed, next)
    }

    fun clearType(playerUuid: UUID, typeId: String, now: Long = System.currentTimeMillis()): Int {
        val type = normalize(typeId)
        val playerRuntimes = runtimes[playerUuid] ?: return 0
        val runtime = playerRuntimes.remove(type) ?: return 0

        val removed = (if (runtime.active != null) 1 else 0) + runtime.queueSize()
        runtime.clearAll(now)

        if (playerRuntimes.isEmpty()) runtimes.remove(playerUuid)
        return removed
    }

    fun clearPlayer(playerUuid: UUID, now: Long = System.currentTimeMillis()): Int {
        val playerRuntimes = runtimes.remove(playerUuid) ?: return 0
        var removed = 0

        for (runtime in playerRuntimes.values) {
            removed += (if (runtime.active != null) 1 else 0) + runtime.queueSize()
            runtime.clearAll(now)
        }

        return removed
    }

    fun clearActiveFromAll(typeId: String, now: Long = System.currentTimeMillis()): Int {
        val type = normalize(typeId)
        var removed = 0

        for ((playerUuid, playerRuntimes) in runtimes) {
            val runtime = playerRuntimes[type] ?: continue
            if (runtime.active == null) continue

            runtime.clearActive(now)
            runtime.startNext(now)
            removed++

            cleanup(playerUuid, type, runtime)
        }

        cleanupEmpty()
        return removed
    }

    fun clearTypeFromAll(typeId: String, now: Long = System.currentTimeMillis()): Int {
        val type = normalize(typeId)
        var removed = 0

        for (playerRuntimes in runtimes.values) {
            val runtime = playerRuntimes.remove(type) ?: continue
            removed += (if (runtime.active != null) 1 else 0) + runtime.queueSize()
            runtime.clearAll(now)
        }

        cleanupEmpty()
        return removed
    }

    fun clearEverything(now: Long = System.currentTimeMillis()): Int {
        var removed = 0

        for (playerRuntimes in runtimes.values) {
            for (runtime in playerRuntimes.values) {
                removed += (if (runtime.active != null) 1 else 0) + runtime.queueSize()
                runtime.clearAll(now)
            }
        }

        runtimes.clear()
        return removed
    }

    fun getRuntime(playerUuid: UUID, typeId: String): BoosterRuntime? {
        return runtimes[playerUuid]?.get(normalize(typeId))
    }

    fun getActive(playerUuid: UUID, typeId: String): BoosterInstance? {
        return getRuntime(playerUuid, typeId)?.active
    }

    fun getQueue(playerUuid: UUID, typeId: String): List<BoosterInstance> {
        return getRuntime(playerUuid, typeId)?.queue?.toList() ?: emptyList()
    }

    fun isEffectActive(
        playerUuid: UUID,
        typeId: String,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        val type = normalize(typeId)
        if (!BoosterConfigManager.isEnabled(type)) return false

        val runtime = getRuntime(playerUuid, type) ?: return false
        val active = runtime.active ?: return false

        return !runtime.isPaused() && !active.isExpired(now)
    }

    fun tick(
        now: Long = System.currentTimeMillis()
    ): List<PersonalBoosterExpiryTransition> {
        val transitions = mutableListOf<PersonalBoosterExpiryTransition>()
        val playerIterator = runtimes.entries.iterator()

        while (playerIterator.hasNext()) {
            val (playerUuid, playerRuntimes) = playerIterator.next()
            val typeIterator = playerRuntimes.entries.iterator()

            while (typeIterator.hasNext()) {
                val (_, runtime) = typeIterator.next()

                while (true) {
                    val active = runtime.active ?: break
                    if (!active.isExpired(now)) break

                    val expired = runtime.clearActive(now) ?: break
                    val next = runtime.startNext(now)

                    transitions += PersonalBoosterExpiryTransition(
                        playerUuid = playerUuid,
                        expired = expired,
                        next = next
                    )
                }

                if (runtime.active == null && runtime.queue.isEmpty()) {
                    typeIterator.remove()
                }
            }

            if (playerRuntimes.isEmpty()) {
                playerIterator.remove()
            }
        }

        return transitions
    }

    fun snapshot(now: Long = System.currentTimeMillis()): Map<String, Map<String, BoosterRuntimeData>> {
        return runtimes.mapKeys { it.key.toString() }.mapValues { (_, playerRuntimes) ->
            playerRuntimes.mapValues { (_, runtime) ->
                BoosterRuntimeData(
                    active = runtime.active?.snapshot(now),
                    queue = runtime.queue.map { it.snapshot(now) },
                    manualPaused = runtime.manualPaused
                )
            }
        }
    }

    fun restore(
        data: Map<String, Map<String, BoosterRuntimeData>>,
        now: Long = System.currentTimeMillis()
    ) {
        runtimes.clear()

        for ((uuidRaw, savedRuntimes) in data) {
            val uuid = runCatching { UUID.fromString(uuidRaw) }.getOrNull() ?: continue
            val playerRuntimes = linkedMapOf<String, BoosterRuntime>()

            for ((typeRaw, saved) in savedRuntimes) {
                val type = normalize(typeRaw)
                val definition = BoosterRegistry.get(type) ?: continue
                if (!definition.supportsPersonal) continue

                val active = saved.active
                    ?.snapshot(now)
                    ?.takeIf { it.type.equals(type, ignoreCase = true) }
                    ?.let { if (it.type == type) it else it.copy(type = type) }
                    ?.takeIf(BoosterValidator::validInstance)

                val queue = saved.queue
                    .map { it.snapshot(now) }
                    .filter { it.type.equals(type, ignoreCase = true) }
                    .map { if (it.type == type) it else it.copy(type = type) }
                    .filter(BoosterValidator::validInstance)
                    .toMutableList()

                val runtime = BoosterRuntime(
                    active = active,
                    queue = queue,
                    manualPaused = saved.manualPaused,
                    globalPaused = false
                )

                runtime.normalize(now)

                if (runtime.active != null || runtime.queue.isNotEmpty()) {
                    playerRuntimes[type] = runtime
                }
            }

            if (playerRuntimes.isNotEmpty()) {
                runtimes[uuid] = playerRuntimes
            }
        }
    }

    fun playerUuids(): Set<UUID> {
        return runtimes.keys.toSet()
    }

    fun types(playerUuid: UUID): Set<String> {
        return runtimes[playerUuid]?.keys?.toSet() ?: emptySet()
    }

    fun reset() {
        runtimes.clear()
    }

    private fun advanceExpired(runtime: BoosterRuntime, now: Long) {
        while (true) {
            val active = runtime.active ?: break
            if (!active.isExpired(now)) break

            runtime.clearActive(now)
            runtime.startNext(now)
        }
    }

    private fun cleanup(playerUuid: UUID, type: String, runtime: BoosterRuntime) {
        if (runtime.active != null || runtime.queue.isNotEmpty()) return

        val playerRuntimes = runtimes[playerUuid] ?: return
        playerRuntimes.remove(type)

        if (playerRuntimes.isEmpty()) runtimes.remove(playerUuid)
    }

    private fun cleanupEmpty() {
        runtimes.entries.removeIf { it.value.isEmpty() }
    }

    private fun normalize(typeId: String): String {
        return typeId.lowercase(Locale.ROOT).trim()
    }
}