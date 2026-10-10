package org.krripe.scobbleboosters.config.model

data class MessageConfig(
    val configVersion: Int = 1,
    val prefix: String = "&8[<gradient:#FF8A00:#FFD166>Boosters</gradient>&8] &8» &r",
    val messages: Map<String, String> = emptyMap()
)