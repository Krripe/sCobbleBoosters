package org.krripe.scobbleboosters.data.model

import java.util.UUID

data class GlobalBoosterExpiryTransition(
    val expired: BoosterInstance,
    val next: BoosterInstance?
)

data class PersonalBoosterExpiryTransition(
    val playerUuid: UUID,
    val expired: BoosterInstance,
    val next: BoosterInstance?
)