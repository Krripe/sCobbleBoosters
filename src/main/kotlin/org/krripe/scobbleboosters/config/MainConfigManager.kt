package org.krripe.scobbleboosters.config

import net.fabricmc.loader.api.FabricLoader
import org.krripe.scobbleboosters.config.model.MainConfig

object MainConfigManager {
    private val file = FabricLoader.getInstance().configDir.resolve("scobbleboosters").resolve("config.json")

    @Volatile
    var config: MainConfig = MainConfig()
        private set

    fun load() {
        config = JsonConfigIO.load(file, MainConfig(), MainConfig::class.java)
    }

    fun reload() {
        load()
    }
}