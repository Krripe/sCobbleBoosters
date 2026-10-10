package org.krripe.scobbleboosters.data.model

sealed interface BoosterActivationResult {
    data class Activated(val booster: BoosterInstance) : BoosterActivationResult
    data class Queued(val booster: BoosterInstance, val position: Int) : BoosterActivationResult
    object UnknownBooster : BoosterActivationResult
    object UnsupportedScope : BoosterActivationResult
    object InvalidBooster : BoosterActivationResult
}

enum class BoosterControlResult {
    SUCCESS,
    UNKNOWN_BOOSTER,
    UNSUPPORTED_SCOPE,
    NOT_FOUND,
    ALREADY_PAUSED,
    ALREADY_RUNNING
}

data class BoosterClearResult(
    val removed: BoosterInstance?,
    val next: BoosterInstance?
)