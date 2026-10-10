package org.krripe.scobbleboosters.webhook

import org.krripe.scobbleboosters.SCobbleBoosters
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

object WebhookClient {
    private const val MAX_IN_FLIGHT = 64

    private val inFlight = AtomicInteger(0)

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    fun send(url: String, json: String) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return

        val uri = runCatching { URI.create(cleanUrl) }.getOrNull() ?: run {
            SCobbleBoosters.LOGGER.warn("Webhook URL is invalid")
            return
        }

        if (uri.scheme != "http" && uri.scheme != "https") {
            SCobbleBoosters.LOGGER.warn("Webhook URL must use HTTP or HTTPS")
            return
        }

        if (inFlight.incrementAndGet() > MAX_IN_FLIGHT) {
            inFlight.decrementAndGet()
            SCobbleBoosters.LOGGER.warn("Webhook request dropped because too many requests are already in flight")
            return
        }

        val request = runCatching {
            HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("User-Agent", "sCobbleBoosters")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build()
        }.getOrElse {
            inFlight.decrementAndGet()
            SCobbleBoosters.LOGGER.warn("Failed to build webhook request", it)
            return
        }

        client.sendAsync(
            request,
            HttpResponse.BodyHandlers.discarding()
        ).whenComplete { response, error ->
            inFlight.decrementAndGet()

            if (error != null) {
                SCobbleBoosters.LOGGER.warn("Webhook request failed: {}", error.message)
                return@whenComplete
            }

            if (response.statusCode() !in 200..299) {
                SCobbleBoosters.LOGGER.warn(
                    "Webhook returned HTTP status {}",
                    response.statusCode()
                )
            }
        }
    }
}