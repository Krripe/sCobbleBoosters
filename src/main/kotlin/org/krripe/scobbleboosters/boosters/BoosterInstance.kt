package org.krripe.scobbleboosters.boosters

import net.minecraft.nbt.NbtCompound

data class BoosterInstance(
    val typeId: String,
    val multiplier: Float,
    var remainingTicks: Int,
    val message: String
) {
    fun tick() {
        if (remainingTicks > 0) remainingTicks--
    }

    fun isExpired(): Boolean = remainingTicks <= 0

    fun writeToNbt(nbt: NbtCompound) {
        nbt.putString("typeId", typeId)
        nbt.putFloat("multiplier", multiplier)
        nbt.putInt("remainingTicks", remainingTicks)
        nbt.putString("message", message)
    }

    companion object {
        fun fromNbt(nbt: NbtCompound): BoosterInstance {
            return BoosterInstance(
                typeId = nbt.getString("typeId"),
                multiplier = nbt.getFloat("multiplier"),
                remainingTicks = nbt.getInt("remainingTicks"),
                message = nbt.getString("message")
            )
        }
    }
}
