package org.krripe.scobbleboosters.data.model

import java.util.UUID

data class BoosterInstance(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val multiplier: Float,
    val durationMillis: Long,
    var remainingMillis: Long = durationMillis,
    var startedAtMillis: Long? = null,
    val text: String = "default",
    val sound: String = "default",
    val data: String? = null,
    val activatorName: String? = null,
    val activatorUuid: UUID? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun start(now: Long = System.currentTimeMillis()): Boolean {
        if (remainingMillis <= 0L || startedAtMillis != null) return false
        startedAtMillis = now
        return true
    }

    fun pause(now: Long = System.currentTimeMillis()): Boolean {
        val started = startedAtMillis ?: return false
        remainingMillis = (remainingMillis - (now - started).coerceAtLeast(0L)).coerceAtLeast(0L)
        startedAtMillis = null
        return true
    }

    fun remaining(now: Long = System.currentTimeMillis()): Long {
        val started = startedAtMillis ?: return remainingMillis.coerceAtLeast(0L)
        return (remainingMillis - (now - started).coerceAtLeast(0L)).coerceAtLeast(0L)
    }

    fun isRunning(): Boolean {
        return startedAtMillis != null
    }

    fun isExpired(now: Long = System.currentTimeMillis()): Boolean {
        return remaining(now) <= 0L
    }

    fun snapshot(now: Long = System.currentTimeMillis()): BoosterInstance {
        return copy(remainingMillis = remaining(now), startedAtMillis = null)
    }
}