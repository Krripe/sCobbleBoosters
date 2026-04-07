package org.krripe.scobbleboosters.boosters

import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtSizeTracker
import org.krripe.scobbleboosters.SCobbleBoosters.Companion.LOGGER
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object BoosterStorage {
    private val saveFile = File("config/sCobbleBoosters/data/boosters.nbt")

    fun save(runtimes: Map<String, BoosterRuntime>) {
        try {
            val root = NbtCompound()
            val types = NbtList()

            for ((typeId, rt) in runtimes) {
                val t = NbtCompound()
                t.putString("id", typeId)
                t.putInt("initialDuration", rt.initialDuration)

                rt.active?.let { a ->
                    val an = NbtCompound()
                    a.writeToNbt(an)
                    t.put("active", an)
                }

                val q = NbtList()
                rt.queue.forEach { b ->
                    val bn = NbtCompound()
                    b.writeToNbt(bn)
                    q.add(bn)
                }
                t.put("queue", q)
                types.add(t)
            }

            root.put("types", types)
            saveFile.parentFile?.mkdirs()
            FileOutputStream(saveFile).use { fos -> NbtIo.writeCompressed(root, fos) }
        } catch (e: Exception) {
            LOGGER.error("Failed to save boosters", e)
        }
    }

    fun load(into: Map<String, BoosterRuntime>) {
        if (!saveFile.exists()) return
        try {
            FileInputStream(saveFile).use { fis ->
                val root = NbtIo.readCompressed(fis, NbtSizeTracker.ofUnlimitedBytes()) ?: return
                val list = root.getList("types", 10)

                for (i in 0 until list.size) {
                    val t = list.getCompound(i)
                    val id = t.getString("id")
                    val rt = into[id] ?: continue

                    rt.initialDuration = t.getInt("initialDuration")
                    rt.active = null
                    rt.queue.clear()

                    if (t.contains("active")) {
                        val a = BoosterInstance.fromNbt(t.getCompound("active"))
                        if (!a.isExpired()) rt.active = a
                    }

                    val q = t.getList("queue", 10)
                    for (qi in 0 until q.size) {
                        val b = BoosterInstance.fromNbt(q.getCompound(qi))
                        if (!b.isExpired()) rt.queue.add(b)
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to load boosters", e)
        }
    }
}