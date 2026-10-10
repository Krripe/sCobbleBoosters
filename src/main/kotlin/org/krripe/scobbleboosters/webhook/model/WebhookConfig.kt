package org.krripe.scobbleboosters.webhook.model

data class BoosterWebhookConfig(
    val reasons: Map<String, String> = emptyMap(),
    val global: WebhookScopeConfig = WebhookScopeConfig(),
    val personal: WebhookScopeConfig = WebhookScopeConfig()
)

data class WebhookScopeConfig(
    val enabled: Boolean = false,
    val url: String = "",
    val username: String = "sCobbleBoosters",
    val avatarUrl: String = "",
    val allowMentions: Boolean = false,
    val events: WebhookEventsConfig = WebhookEventsConfig()
)

data class WebhookEventsConfig(
    val activated: WebhookEventConfig = WebhookEventConfig(),
    val queued: WebhookEventConfig = WebhookEventConfig(),
    val resumed: WebhookEventConfig = WebhookEventConfig(),
    val expired: WebhookEventConfig = WebhookEventConfig(),
    val cleared: WebhookEventConfig = WebhookEventConfig()
)

data class WebhookEventConfig(
    val enabled: Boolean = true,
    val content: String = "",
    val embed: WebhookEmbedConfig = WebhookEmbedConfig()
)

data class WebhookEmbedConfig(
    val enabled: Boolean = true,
    val title: String = "",
    val description: String = "",
    val color: String = "#9B5DE5",
    val footer: String = "sCobbleBoosters",
    val footerIconUrl: String = "",
    val imageUrl: String = "",
    val thumbnailUrl: String = "",
    val timestamp: Boolean = true,
    val fields: List<WebhookFieldConfig> = emptyList()
)

data class WebhookFieldConfig(
    val name: String = "",
    val value: String = "",
    val inline: Boolean = true
)