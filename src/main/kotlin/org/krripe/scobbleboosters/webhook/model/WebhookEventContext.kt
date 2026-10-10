package org.krripe.scobbleboosters.webhook.model

import org.krripe.scobbleboosters.data.model.BoosterInstance
import org.krripe.scobbleboosters.data.model.BoosterScope
import java.util.UUID

enum class WebhookEventType {
    ACTIVATED,
    QUEUED,
    RESUMED,
    EXPIRED,
    CLEARED
}

data class WebhookEventContext(
    val event: WebhookEventType,
    val scope: BoosterScope,
    val booster: BoosterInstance,
    val playerUuid: UUID? = null,
    val queuePosition: Int? = null,
    val reason: String = "",
    val now: Long = System.currentTimeMillis()
)