package org.krripe.scobbleboosters.data.repository

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.krripe.scobbleboosters.SCobbleBoosters
import org.krripe.scobbleboosters.data.persistence.BoosterSaveData
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class JsonBoosterRepository(private val file: Path) : BoosterRepository {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    override fun load(): BoosterSaveData {
        if (!Files.exists(file)) return BoosterSaveData()

        return runCatching {
            Files.newBufferedReader(file).use { gson.fromJson(it, BoosterSaveData::class.java) ?: BoosterSaveData() }
        }.onFailure {
            SCobbleBoosters.LOGGER.error("Failed to load booster data", it)
        }.getOrDefault(BoosterSaveData())
    }

    override fun save(data: BoosterSaveData) {
        runCatching {
            Files.createDirectories(file.parent)

            val temp = file.resolveSibling("${file.fileName}.tmp")
            Files.newBufferedWriter(temp).use { gson.toJson(data, it) }

            runCatching {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            }.getOrElse {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING)
            }
        }.onFailure {
            SCobbleBoosters.LOGGER.error("Failed to save booster data", it)
        }
    }
}