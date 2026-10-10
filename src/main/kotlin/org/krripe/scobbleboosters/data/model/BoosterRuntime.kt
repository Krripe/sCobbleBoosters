package org.krripe.scobbleboosters.data.model

data class BoosterRuntime(
    var active: BoosterInstance? = null,
    val queue: MutableList<BoosterInstance> = mutableListOf(),
    var manualPaused: Boolean = false,
    var globalPaused: Boolean = false
) {
    fun state(): BoosterState {
        if (active == null) return BoosterState.INACTIVE

        return when {
            manualPaused && globalPaused -> BoosterState.PAUSED_MANUAL_AND_GLOBAL
            manualPaused -> BoosterState.PAUSED_MANUAL
            globalPaused -> BoosterState.PAUSED_GLOBAL
            else -> BoosterState.ACTIVE
        }
    }

    fun isPaused(): Boolean {
        return manualPaused || globalPaused
    }

    fun add(instance: BoosterInstance, now: Long = System.currentTimeMillis()): Boolean {
        val current = active

        if (current == null) {
            active = instance
            syncTimer(now)
            return true
        }

        if (instance.multiplier > current.multiplier) {
            current.pause(now)
            queue.add(current)
            sortQueue()

            active = instance
            syncTimer(now)
            return true
        }

        instance.pause(now)
        queue.add(instance)
        sortQueue()
        return false
    }

    fun syncTimer(now: Long = System.currentTimeMillis()) {
        val current = active ?: return

        if (isPaused()) current.pause(now)
        else current.start(now)
    }

    fun startNext(now: Long = System.currentTimeMillis()): BoosterInstance? {
        if (queue.isEmpty()) {
            active = null
            manualPaused = false
            return null
        }

        sortQueue()
        active = queue.removeAt(0)
        manualPaused = false
        syncTimer(now)
        return active
    }

    fun clearActive(now: Long = System.currentTimeMillis()): BoosterInstance? {
        val removed = active ?: return null
        removed.pause(now)
        active = null
        manualPaused = false
        return removed
    }

    fun clearQueue() {
        queue.clear()
    }

    fun clearAll(now: Long = System.currentTimeMillis()) {
        active?.pause(now)
        active = null
        queue.clear()
        manualPaused = false
        globalPaused = false
    }

    fun queueSize(): Int {
        return queue.size
    }

    fun queuePosition(boosterId: String): Int {
        val index = queue.indexOfFirst { it.id == boosterId }
        return if (index == -1) -1 else index + 1
    }

    fun sortQueue() {
        queue.sortWith(
            compareByDescending<BoosterInstance> { it.multiplier }
                .thenBy { it.createdAtMillis }
        )
    }

    fun normalize(now: Long = System.currentTimeMillis()) {
        active?.let {
            if (it.isExpired(now)) active = null
            else it.pause(now)
        }

        queue.removeIf { it.isExpired(now) }
        queue.forEach { it.pause(now) }
        sortQueue()

        val bestQueued = queue.firstOrNull()
        val current = active

        if (current == null) {
            if (bestQueued != null) {
                active = queue.removeAt(0)
            }
        } else if (bestQueued != null && bestQueued.multiplier > current.multiplier) {
            queue.removeAt(0)
            queue.add(current)
            active = bestQueued
            sortQueue()
        }

        syncTimer(now)
    }
}