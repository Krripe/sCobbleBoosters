package org.krripe.scobbleboosters.debug

import net.minecraft.server.MinecraftServer
import org.krripe.scobbleboosters.SCobbleBoosters
import org.krripe.scobbleboosters.message.MessageManager
import org.krripe.scobbleboosters.utils.PermissionsSystem
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object DebugManager {
    private val permissions = PermissionsSystem()
    private val enabledTypes = mutableSetOf<String>()

    @Volatile
    private var server: MinecraftServer? = null

    @Volatile
    private var allEnabled = false

    fun setServer(server: MinecraftServer?) {
        this.server = server
    }

    fun enableAll() {
        allEnabled = true
        enabledTypes.clear()
    }

    fun disableAll() {
        allEnabled = false
        enabledTypes.clear()
    }

    fun enable(typeId: String) {
        enabledTypes.add(normalize(typeId))
    }

    fun disable(typeId: String) {
        enabledTypes.remove(normalize(typeId))
    }

    fun isEnabled(typeId: String): Boolean {
        return allEnabled || enabledTypes.contains(normalize(typeId))
    }

    fun status(): String {
        if (allEnabled) return "ALL"
        if (enabledTypes.isEmpty()) return "OFF"
        return enabledTypes.sorted().joinToString(", ")
    }

    fun log(
        typeId: String,
        message: String
    ) {
        if (!isEnabled(typeId)) return

        val type = normalize(typeId)

        SCobbleBoosters.LOGGER.info(
            "[Debug][$type] $message"
        )

        val srv = server ?: return

        for (player in srv.playerManager.playerList) {
            val source = player.commandSource

            if (
                player.hasPermissionLevel(2) ||
                permissions.hasPermission(
                    source,
                    "scobbleboosters.debug"
                )
            ) {
                player.sendMessage(
                    MessageManager.text(
                        "debug.output",
                        player,
                        "type" to type,
                        "message" to message
                    ),
                    false
                )
            }
        }
    }

    fun reset() {
        allEnabled = false
        enabledTypes.clear()
        server = null
        lastByKey.clear()
    }

    private fun normalize(value: String): String {
        return value.lowercase(Locale.ROOT).trim()
    }

    private val lastByKey = ConcurrentHashMap<String, Long>()

    fun logThrottled(typeId: String, key: String, cooldownMs: Long, message: String) {
        if (!isEnabled(typeId)) return

        val now = System.currentTimeMillis()
        val fullKey = "${normalize(typeId)}:$key"
        val last = lastByKey[fullKey] ?: 0L

        if (now - last < cooldownMs) return

        lastByKey[fullKey] = now
        log(typeId, message)
    }
}