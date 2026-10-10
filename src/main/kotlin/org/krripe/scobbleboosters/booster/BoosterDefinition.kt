package org.krripe.scobbleboosters.booster

data class BoosterDefinition(
    val id: String,
    val displayName: String,
    val dataArgument: String? = null,
    val supportsGlobal: Boolean = true,
    val supportsPersonal: Boolean = true,
    val supportsStorage: Boolean = true
)