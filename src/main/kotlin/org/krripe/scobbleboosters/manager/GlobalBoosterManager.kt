package org.krripe.scobbleboosters.manager

import org.krripe.scobbleboosters.booster.BoosterRegistry
import org.krripe.scobbleboosters.data.model.*
import org.krripe.scobbleboosters.data.persistence.BoosterRuntimeData
import org.krripe.scobbleboosters.booster.BoosterValidator
import org.krripe.scobbleboosters.config.BoosterConfigManager
import java.util.Locale

object GlobalBoosterManager {
    private val runtimes = linkedMapOf<String, BoosterRuntime>()

    fun activate(instance: BoosterInstance, now: Long = System.currentTimeMillis()): BoosterActivationResult {

        if (!BoosterValidator.validInstance(instance)) {
            return BoosterActivationResult.InvalidBooster
        }

        val type = normalize(instance.type)
        val definition = BoosterRegistry.get(type) ?: return BoosterActivationResult.UnknownBooster
        if (!definition.supportsGlobal) return BoosterActivationResult.UnsupportedScope

        val runtime = runtimes.getOrPut(type) { BoosterRuntime() }
        advanceExpired(runtime, now)

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

    fun stop(typeId: String, now: Long = System.currentTimeMillis()): BoosterControlResult {
        val type = normalize(typeId)
        val definition = BoosterRegistry.get(type) ?: return BoosterControlResult.UNKNOWN_BOOSTER
        if (!definition.supportsGlobal) return BoosterControlResult.UNSUPPORTED_SCOPE

        val runtime = runtimes[type] ?: return BoosterControlResult.NOT_FOUND
        if (runtime.active == null) return BoosterControlResult.NOT_FOUND
        if (runtime.manualPaused) return BoosterControlResult.ALREADY_PAUSED

        runtime.manualPaused = true
        runtime.syncTimer(now)
        return BoosterControlResult.SUCCESS
    }

    fun start(typeId: String, now: Long = System.currentTimeMillis()): BoosterControlResult {
        val type = normalize(typeId)
        val definition = BoosterRegistry.get(type) ?: return BoosterControlResult.UNKNOWN_BOOSTER
        if (!definition.supportsGlobal) return BoosterControlResult.UNSUPPORTED_SCOPE

        val runtime = runtimes[type] ?: return BoosterControlResult.NOT_FOUND
        if (runtime.active == null) return BoosterControlResult.NOT_FOUND
        if (!runtime.manualPaused) return BoosterControlResult.ALREADY_RUNNING

        runtime.manualPaused = false
        runtime.syncTimer(now)
        return BoosterControlResult.SUCCESS
    }

    fun clearActive(typeId: String, now: Long = System.currentTimeMillis()): BoosterClearResult {
        val type = normalize(typeId)
        val runtime = runtimes[type] ?: return BoosterClearResult(null, null)

        val removed = runtime.clearActive(now)
        val next = if (removed != null) runtime.startNext(now) else null

        cleanup(type, runtime)
        return BoosterClearResult(removed, next)
    }

    fun clearType(typeId: String, now: Long = System.currentTimeMillis()): Int {
        val type = normalize(typeId)
        val runtime = runtimes.remove(type) ?: return 0
        val removed = (if (runtime.active != null) 1 else 0) + runtime.queueSize()

        runtime.clearAll(now)
        return removed
    }

    fun clearEverything(now: Long = System.currentTimeMillis()): Int {
        var removed = 0

        for (runtime in runtimes.values) {
            removed += (if (runtime.active != null) 1 else 0) + runtime.queueSize()
            runtime.clearAll(now)
        }

        runtimes.clear()
        return removed
    }

    fun isEffectActive(
        typeId: String,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        val type = normalize(typeId)
        if (!BoosterConfigManager.isEnabled(type)) return false

        val runtime = runtimes[type] ?: return false
        val active = runtime.active ?: return false

        return !runtime.manualPaused && !active.isExpired(now)
    }

    fun getRuntime(typeId: String): BoosterRuntime? {
        return runtimes[normalize(typeId)]
    }

    fun getActive(typeId: String): BoosterInstance? {
        return runtimes[normalize(typeId)]?.active
    }

    fun getQueue(typeId: String): List<BoosterInstance> {
        return runtimes[normalize(typeId)]?.queue?.toList() ?: emptyList()
    }

    fun types(): Set<String> {
        return runtimes.keys.toSet()
    }

    fun tick(
        now: Long = System.currentTimeMillis()
    ): List<GlobalBoosterExpiryTransition> {
        val transitions = mutableListOf<GlobalBoosterExpiryTransition>()
        val iterator = runtimes.entries.iterator()

        while (iterator.hasNext()) {
            val (_, runtime) = iterator.next()

            while (true) {
                val active = runtime.active ?: break
                if (!active.isExpired(now)) break

                val expired = runtime.clearActive(now) ?: break
                val next = runtime.startNext(now)

                transitions += GlobalBoosterExpiryTransition(
                    expired = expired,
                    next = next
                )
            }

            if (runtime.active == null && runtime.queue.isEmpty()) {
                iterator.remove()
            }
        }

        return transitions
    }

    fun snapshot(now: Long = System.currentTimeMillis()): Map<String, BoosterRuntimeData> {
        return runtimes.mapValues { (_, runtime) ->
            BoosterRuntimeData(
                active = runtime.active?.snapshot(now),
                queue = runtime.queue.map { it.snapshot(now) },
                manualPaused = runtime.manualPaused
            )
        }
    }

    fun restore(
        data: Map<String, BoosterRuntimeData>,
        now: Long = System.currentTimeMillis()
    ) {
        runtimes.clear()

        for ((typeRaw, saved) in data) {
            val type = normalize(typeRaw)
            val definition = BoosterRegistry.get(type) ?: continue
            if (!definition.supportsGlobal) continue

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
                manualPaused = saved.manualPaused
            )

            runtime.normalize(now)

            if (runtime.active != null || runtime.queue.isNotEmpty()) {
                runtimes[type] = runtime
            }
        }
    }

    fun reset() {
        runtimes.clear()
    }

    private fun advanceExpired(runtime: BoosterRuntime, now: Long): Boolean {
        var changed = false

        while (true) {
            val active = runtime.active ?: break
            if (!active.isExpired(now)) break

            runtime.clearActive(now)
            runtime.startNext(now)
            changed = true
        }

        return changed
    }

    private fun cleanup(type: String, runtime: BoosterRuntime) {
        if (runtime.active == null && runtime.queue.isEmpty()) runtimes.remove(type)
    }

    private fun normalize(typeId: String): String {
        return typeId.lowercase(Locale.ROOT).trim()
    }
}