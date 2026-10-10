package org.krripe.scobbleboosters.config.model

data class MainConfig(
    val configVersion: Int = 1,
    val autosaveSeconds: Int = 300
)