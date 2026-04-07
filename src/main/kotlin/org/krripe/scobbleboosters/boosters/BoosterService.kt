package org.krripe.scobbleboosters.boosters

import net.minecraft.server.MinecraftServer
import net.minecraft.entity.boss.ServerBossBar
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.krripe.scobbleboosters.config.SBConfig
import org.krripe.scobbleboosters.lang.LangManager
import org.krripe.scobbleboosters.utils.CustomColorParser
import org.krripe.scobbleboosters.utils.SoundHelper
import org.krripe.scobbleboosters.webhook.DiscordWebhook
import java.util.Comparator

object BoosterService {

    private const val AUTOSAVE_INTERVAL_TICKS = 20 * 60 * 5
    private var autosaveCounter = 0

    private val runtimes: MutableMap<String, BoosterRuntime> = mutableMapOf()

    sealed class RemoveResult {
        data class RemovedActiveNext(val mult: Float) : RemoveResult()
        data class RemovedActiveNone(val mult: Float) : RemoveResult()
        data class RemovedQueued(val mult: Float) : RemoveResult()
        data class NotFound(val mult: Float) : RemoveResult()
        data class UnknownType(val typeId: String) : RemoveResult()
    }

    data class InfoData(
        val typeId: String,
        val active: BoosterInstance?,
        val queue: List<BoosterInstance>
    )

    fun init() {
        runtimes.clear()
        for (def in BoosterDefinitions.ALL) {
            val bar = ServerBossBar(Text.literal(""), def.bossBarColor, def.bossBarStyle)
            bar.isVisible = false
            runtimes[def.id] = BoosterRuntime(def, bar)
        }

        BoosterStorage.load(runtimes)

        runtimes.values.forEach { rt ->
            if (rt.active != null && !rt.active!!.isExpired()) {
                rt.bossBar.isVisible = true
                rt.lastShownSeconds = Int.MIN_VALUE
                rt.lastQueueSig = 0
                updateBossbar(rt)
            }
        }
    }

    fun saveNow() = BoosterStorage.save(runtimes)

    fun tick(server: MinecraftServer) {
        val anyActive = runtimes.values.any { it.active != null }
        if (anyActive) {
            autosaveCounter++
            if (autosaveCounter >= AUTOSAVE_INTERVAL_TICKS) {
                saveNow()
                autosaveCounter = 0
            }
        }

        for (rt in runtimes.values) {
            val a = rt.active ?: continue
            a.tick()

            if (a.isExpired()) {
                if (rt.queue.isNotEmpty()) {
                    activateNext(server, rt)
                } else {
                    rt.active = null
                    rt.initialDuration = 0
                    rt.bossBar.isVisible = false
                    rt.lastShownSeconds = Int.MIN_VALUE
                    rt.lastQueueSig = 0
                    updateBossbar(rt)
                    saveNow()
                }
            } else {
                updateBossbar(rt)
            }
        }
    }

    fun addPlayer(player: ServerPlayerEntity) {
        for (rt in runtimes.values) rt.bossBar.addPlayer(player)
    }

    fun removePlayer(player: ServerPlayerEntity) {
        for (rt in runtimes.values) rt.bossBar.removePlayer(player)
    }

    fun getMultiplier(typeId: String): Float {
        val rt = runtimes[typeId] ?: return 1.0f
        return rt.active?.multiplier ?: 1.0f
    }

    fun info(typeId: String): InfoData? {
        val rt = runtimes[typeId] ?: return null
        return InfoData(typeId, rt.active, rt.queue.toList())
    }

    fun activate(
        server: MinecraftServer,
        typeId: String,
        multiplier: Float,
        durationTicks: Int,
        message: String,
        announce: Boolean = true
    ): Boolean {
        val rt = runtimes[typeId] ?: return false

        val becomesActiveNow: Boolean

        if (rt.active != null) {
            val current = rt.active!!
            if (multiplier > current.multiplier) {
                rt.queue.add(current)
                rt.queue.sortWith(Comparator.comparing(BoosterInstance::multiplier).reversed())
                rt.active = BoosterInstance(typeId, multiplier, durationTicks, message)
                rt.initialDuration = durationTicks
                becomesActiveNow = true
            } else {
                rt.queue.add(BoosterInstance(typeId, multiplier, durationTicks, message))
                rt.queue.sortWith(Comparator.comparing(BoosterInstance::multiplier).reversed())
                becomesActiveNow = false
            }
        } else {
            rt.active = BoosterInstance(typeId, multiplier, durationTicks, message)
            rt.initialDuration = durationTicks
            rt.bossBar.isVisible = true
            becomesActiveNow = true
        }

        rt.lastShownSeconds = Int.MIN_VALUE
        rt.lastQueueSig = 0

        updateBossbar(rt)
        saveNow()

        if (announce && becomesActiveNow) {
            announceActivation(server, rt)
        }

        return becomesActiveNow
    }

    fun remove(typeId: String, multiplier: Float): RemoveResult {
        val rt = runtimes[typeId] ?: return RemoveResult.UnknownType(typeId)
        val eps = 0.01f

        val a = rt.active
        if (a != null && kotlin.math.abs(a.multiplier - multiplier) < eps) {
            if (rt.queue.isNotEmpty()) {
                rt.active = rt.queue.poll()
                rt.initialDuration = rt.active!!.remainingTicks
                rt.bossBar.isVisible = true
                rt.lastShownSeconds = Int.MIN_VALUE
                rt.lastQueueSig = 0

                updateBossbar(rt)
                saveNow()
                return RemoveResult.RemovedActiveNext(multiplier)
            } else {
                rt.active = null
                rt.initialDuration = 0
                rt.bossBar.isVisible = false
                rt.lastShownSeconds = Int.MIN_VALUE
                rt.lastQueueSig = 0

                updateBossbar(rt)
                saveNow()
                return RemoveResult.RemovedActiveNone(multiplier)
            }
        }

        val it = rt.queue.iterator()
        while (it.hasNext()) {
            val b = it.next()
            if (kotlin.math.abs(b.multiplier - multiplier) < eps) {
                it.remove()

                rt.lastShownSeconds = Int.MIN_VALUE
                rt.lastQueueSig = 0

                updateBossbar(rt)
                saveNow()
                return RemoveResult.RemovedQueued(multiplier)
            }
        }

        return RemoveResult.NotFound(multiplier)
    }

    private fun activateNext(server: MinecraftServer, rt: BoosterRuntime) {
        rt.queue.sortWith(Comparator.comparing(BoosterInstance::multiplier).reversed())
        rt.active = rt.queue.poll()
        rt.initialDuration = rt.active?.remainingTicks ?: 0
        rt.bossBar.isVisible = rt.active != null
        rt.lastShownSeconds = Int.MIN_VALUE
        rt.lastQueueSig = 0

        updateBossbar(rt)
        saveNow()

        if (rt.active != null) {
            announceActivation(server, rt)
        }
    }

    private fun announceActivation(server: MinecraftServer, rt: BoosterRuntime) {
        val a = rt.active ?: return

        val (h, m, s) = hms(a.remainingTicks)
        val formattedBossText = formatMessageSmart(a.message, a.multiplier, h, m, s)
        val broadcast = LangManager.msg("broadcastBoostActivated", "msg" to formattedBossText)
        server.playerManager.broadcast(broadcast, false)

        val snd = SBConfig.config.sounds.boosterActivated
        if (snd.enabled && snd.id.isNotBlank()) {
            for (p in server.playerManager.playerList) {
                SoundHelper.play(p, snd.id, snd.volume, snd.pitch)
            }
        }

        DiscordWebhook.sendBoosterActivated(rt.def.id, a.multiplier, rt.initialDuration)
    }

    private fun updateBossbar(rt: BoosterRuntime) {
        val a = rt.active
        if (a == null) {
            rt.bossBar.setName(Text.literal(""))
            rt.bossBar.setPercent(0f)
            rt.lastShownSeconds = Int.MIN_VALUE
            rt.lastQueueSig = 0
            return
        }

        val secondsLeft = (a.remainingTicks / 20).coerceAtLeast(0)

        val showQueue = (rt.def.id == "shiny" && SBConfig.config.shiny.showQueueOnBossbar)
        val queueSig = if (showQueue && rt.queue.isNotEmpty()) {
            var sig = 1
            for (b in rt.queue) {
                sig = 31 * sig + java.lang.Float.floatToIntBits(b.multiplier)
            }
            sig = 31 * sig + rt.queue.size
            sig
        } else 0

        if (secondsLeft != rt.lastShownSeconds || queueSig != rt.lastQueueSig) {
            val (h, m, s) = hmsSeconds(secondsLeft)

            val formatted = formatMessageSmart(a.message, a.multiplier, h, m, s)
            val colored = CustomColorParser.processAllFormats(formatted, Style.EMPTY)

            val queueText = if (showQueue && rt.queue.isNotEmpty()) {
                val q = rt.queue.joinToString(" \u2192 ") { "${"%.1f".format(it.multiplier)}x" }
                Text.literal(" ").append(Text.literal("| Queue: $q").formatted(Formatting.GOLD))
            } else Text.literal("")

            rt.bossBar.setName(Text.literal("").append(colored).append(queueText))

            rt.lastShownSeconds = secondsLeft
            rt.lastQueueSig = queueSig
        }

        val denom = rt.initialDuration.coerceAtLeast(1)
        rt.bossBar.setPercent(a.remainingTicks / denom.toFloat())
    }

    private fun hms(ticks: Int): Triple<Int, Int, Int> {
        val secTot = (ticks / 20).coerceAtLeast(0)
        val h = secTot / 3600
        val m = (secTot / 60) % 60
        val s = secTot % 60
        return Triple(h, m, s)
    }

    private fun hmsSeconds(secTot: Int): Triple<Int, Int, Int> {
        val s0 = secTot.coerceAtLeast(0)
        val h = s0 / 3600
        val m = (s0 / 60) % 60
        val s = s0 % 60
        return Triple(h, m, s)
    }

    private fun formatMessageSmart(raw: String, mult: Float, h: Int, m: Int, s: Int): String {
        var dCount = 0
        var i = 0
        while (i < raw.length - 1) {
            if (raw[i] == '%' && raw[i + 1] == 'd') dCount++
            if (raw[i] == '%' && raw[i + 1] == '%') i++
            i++
        }

        val args: Array<Any> = when (dCount) {
            0 -> arrayOf(mult)
            1 -> arrayOf(mult, s)
            2 -> arrayOf(mult, m, s)
            else -> arrayOf(mult, h, m, s)
        }

        return runCatching { String.format(raw, *args) }
            .getOrElse { "Boost: ${"%.1f".format(mult)}x" }
    }
}