package org.krripe.scobbleboosters.data.model

import java.util.UUID

data class StoredBooster(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val multiplier: Float,
    val durationMillis: Long,
    val text: String = "default",
    val sound: String = "default",
    val data: String? = null,
    var amount: Int = 1,
    val receivedAtMillis: Long = System.currentTimeMillis()
) {
    fun canStackWith(other: StoredBooster): Boolean {
        return type == other.type &&
                multiplier == other.multiplier &&
                durationMillis == other.durationMillis &&
                text == other.text &&
                sound == other.sound &&
                data == other.data
    }
}