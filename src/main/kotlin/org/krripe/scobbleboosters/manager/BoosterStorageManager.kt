package org.krripe.scobbleboosters.manager

import org.krripe.scobbleboosters.booster.BoosterValidator
import org.krripe.scobbleboosters.data.model.StoredBooster
import java.util.UUID

object BoosterStorageManager {
    private val storage = linkedMapOf<UUID, MutableList<StoredBooster>>()

    fun give(playerUuid: UUID, booster: StoredBooster): StoredBooster {
        require(booster.amount > 0) { "Booster amount must be greater than 0" }

        val boosters = storage.getOrPut(playerUuid) { mutableListOf() }
        val existing = boosters.firstOrNull { it.canStackWith(booster) }

        if (existing != null) {
            existing.amount += booster.amount
            return existing
        }

        boosters.add(booster)
        return booster
    }

    fun takeOne(playerUuid: UUID, boosterId: String): StoredBooster? {
        val boosters = storage[playerUuid] ?: return null
        val booster = boosters.firstOrNull { it.id == boosterId } ?: return null

        val result = booster.copy(amount = 1)

        if (booster.amount > 1) booster.amount--
        else boosters.remove(booster)

        if (boosters.isEmpty()) storage.remove(playerUuid)
        return result
    }

    fun removeOne(playerUuid: UUID, typeId: String): StoredBooster? {
        val boosters = storage[playerUuid] ?: return null
        val booster = boosters.firstOrNull { it.type.equals(typeId, ignoreCase = true) } ?: return null

        val result = booster.copy(amount = 1)

        if (booster.amount > 1) booster.amount--
        else boosters.remove(booster)

        if (boosters.isEmpty()) storage.remove(playerUuid)
        return result
    }

    fun removeAll(playerUuid: UUID, typeId: String): Int {
        val boosters = storage[playerUuid] ?: return 0
        var removed = 0

        val iterator = boosters.iterator()
        while (iterator.hasNext()) {
            val booster = iterator.next()
            if (!booster.type.equals(typeId, ignoreCase = true)) continue

            removed += booster.amount
            iterator.remove()
        }

        if (boosters.isEmpty()) storage.remove(playerUuid)
        return removed
    }

    fun clear(playerUuid: UUID): Int {
        val boosters = storage.remove(playerUuid) ?: return 0
        return boosters.sumOf { it.amount }
    }

    fun get(playerUuid: UUID): List<StoredBooster> {
        return storage[playerUuid]?.map { it.copy() } ?: emptyList()
    }

    fun count(playerUuid: UUID): Int {
        return storage[playerUuid]?.sumOf { it.amount } ?: 0
    }

    fun snapshot(): Map<String, List<StoredBooster>> {
        return storage.mapKeys { it.key.toString() }.mapValues { (_, boosters) -> boosters.map { it.copy() } }
    }

    fun restore(data: Map<String, List<StoredBooster>>) {
        storage.clear()

        for ((uuidRaw, boosters) in data) {
            val uuid = runCatching { UUID.fromString(uuidRaw) }.getOrNull() ?: continue

            for (booster in boosters) {
                if (!BoosterValidator.validStored(booster)) continue
                give(uuid, booster.copy())
            }
        }
    }

    fun reset() {
        storage.clear()
    }
}