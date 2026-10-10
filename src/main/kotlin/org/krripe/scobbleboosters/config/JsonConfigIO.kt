package org.krripe.scobbleboosters.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.krripe.scobbleboosters.SCobbleBoosters
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object JsonConfigIO {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun <T : Any> load(file: Path, defaults: T, clazz: Class<T>): T {
        return runCatching {
            Files.createDirectories(file.parent)

            val defaultJson = gson.toJsonTree(defaults).asJsonObject
            val existing = if (Files.exists(file)) {
                runCatching {
                    Files.newBufferedReader(file).use { gson.fromJson(it, JsonObject::class.java) }
                }.getOrNull() ?: JsonObject()
            } else JsonObject()

            val merged = merge(defaultJson, existing)
            write(file, merged)

            gson.fromJson(merged, clazz)
        }.onFailure {
            SCobbleBoosters.LOGGER.error("Failed to load config {}", file.fileName, it)
        }.getOrDefault(defaults)
    }

    fun write(file: Path, json: JsonObject) {
        Files.createDirectories(file.parent)

        val temp = file.resolveSibling("${file.fileName}.tmp")
        Files.newBufferedWriter(temp).use { gson.toJson(json, it) }

        runCatching {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }.getOrElse {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun merge(defaults: JsonObject, existing: JsonObject): JsonObject {
        val result = defaults.deepCopy()

        for ((key, value) in existing.entrySet()) {
            if (value.isJsonObject && result.has(key) && result.get(key).isJsonObject) {
                result.add(key, merge(result.getAsJsonObject(key), value.asJsonObject))
            } else {
                result.add(key, value.deepCopy())
            }
        }

        return result
    }
}