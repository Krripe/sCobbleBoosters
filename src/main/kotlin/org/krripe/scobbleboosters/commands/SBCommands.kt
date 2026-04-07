package org.krripe.scobbleboosters.commands

import com.cobblemon.mod.common.Cobblemon
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import org.krripe.scobbleboosters.boosters.BoosterService
import org.krripe.scobbleboosters.config.SBConfig
import org.krripe.scobbleboosters.lang.LangManager
import org.krripe.scobbleboosters.utils.DurationParser
import org.krripe.scobbleboosters.utils.PermissionsSystem

object SBCommands {

    private val perms = PermissionsSystem()

    private fun hasPerm(src: ServerCommandSource, node: String, opLevelFallback: Int = 2): Boolean {
        val isPlayer = src.entity is net.minecraft.server.network.ServerPlayerEntity
        return if (isPlayer) {
            perms.hasPermission(src, node) || src.hasPermissionLevel(opLevelFallback)
        } else {
            src.hasPermissionLevel(opLevelFallback)
        }
    }

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            val aliases = SBConfig.config.commands.aliases.ifEmpty { listOf("sb") }

            for (alias in aliases) {
                dispatcher.register(
                    literal(alias)
                        .then(literal("reload")
                            .requires { hasPerm(it, "scobbleboosters.admin") }
                            .executes {
                                SBConfig.reload()
                                LangManager.reload()
                                it.source.sendMessage(LangManager.msg("reloaded"))
                                1
                            }
                        )
                        .then(literal("shiny")
                            .then(literal("values")
                                .requires { hasPerm(it, "scobbleboosters.shiny.info") }
                                .executes { ctx ->
                                    sendShinyValues(ctx.source)
                                    1
                                }
                            )
                            .then(literal("info")
                                .requires { hasPerm(it, "scobbleboosters.shiny.info") }
                                .executes { ctx ->
                                    val data = BoosterService.info("shiny")
                                        ?: throw SimpleCommandExceptionType(
                                            LangManager.msg("unknownType", "type" to "shiny")
                                        ).create()

                                    val typeName = LangManager.str("typeNameShiny", "Shiny")
                                    ctx.source.sendMessage(LangManager.msg("infoHeader", "type" to typeName))

                                    val active = data.active
                                    if (active == null) {
                                        ctx.source.sendMessage(LangManager.msg("infoNone"))
                                    } else {
                                        val (h, m, s) = ticksToHms(active.remainingTicks)
                                        ctx.source.sendMessage(
                                            LangManager.msg(
                                                "infoActive",
                                                "mult" to "%.1f".format(active.multiplier),
                                                "h" to h, "m" to m, "s" to s
                                            )
                                        )
                                    }

                                    if (data.queue.isNotEmpty()) {
                                        ctx.source.sendMessage(LangManager.msg("infoQueueHeader"))
                                        var pos = 1
                                        for (b in data.queue) {
                                            val (h, m, s) = ticksToHms(b.remainingTicks)
                                            ctx.source.sendMessage(
                                                LangManager.msg(
                                                    "infoQueueEntry",
                                                    "pos" to pos++,
                                                    "mult" to "%.1f".format(b.multiplier),
                                                    "h" to h, "m" to m, "s" to s
                                                )
                                            )
                                        }
                                    }
                                    1
                                }
                            )
                            .then(literal("reset")
                                .requires { hasPerm(it, "scobbleboosters.shiny.reset") }
                                .then(argument("multiplier", FloatArgumentType.floatArg(1.0f))
                                    .executes { ctx ->
                                        val mult = FloatArgumentType.getFloat(ctx, "multiplier")
                                        when (val r = BoosterService.remove("shiny", mult)) {
                                            is BoosterService.RemoveResult.RemovedActiveNext ->
                                                ctx.source.server.playerManager.broadcast(
                                                    LangManager.msg("boostRemovedNext", "mult" to "%.1f".format(r.mult)),
                                                    false
                                                )

                                            is BoosterService.RemoveResult.RemovedActiveNone ->
                                                ctx.source.server.playerManager.broadcast(
                                                    LangManager.msg("boostRemovedNone", "mult" to "%.1f".format(r.mult)),
                                                    false
                                                )

                                            is BoosterService.RemoveResult.RemovedQueued ->
                                                ctx.source.server.playerManager.broadcast(
                                                    LangManager.msg("boostRemovedQueued", "mult" to "%.1f".format(r.mult)),
                                                    false
                                                )

                                            is BoosterService.RemoveResult.NotFound ->
                                                ctx.source.sendMessage(
                                                    LangManager.msg("boostNotFound", "mult" to "%.1f".format(r.mult))
                                                )

                                            is BoosterService.RemoveResult.UnknownType ->
                                                ctx.source.sendMessage(
                                                    LangManager.msg("unknownType", "type" to r.typeId)
                                                )
                                        }
                                        1
                                    }
                                )
                            )
                            .then(argument("multiplier", FloatArgumentType.floatArg(1.0f))
                                .requires { hasPerm(it, "scobbleboosters.shiny.activate") }
                                .then(argument("duration", StringArgumentType.word())
                                    .executes { ctx ->
                                        val mult = FloatArgumentType.getFloat(ctx, "multiplier")
                                        val durStr = StringArgumentType.getString(ctx, "duration")
                                        val seconds = DurationParser.parseSeconds(durStr)
                                            ?: throw SimpleCommandExceptionType(
                                                LangManager.msg("invalidDuration", "input" to durStr)
                                            ).create()

                                        activateShiny(ctx.source, mult, seconds, null)
                                        1
                                    }
                                    .then(argument("message", StringArgumentType.greedyString())
                                        .executes { ctx ->
                                            val mult = FloatArgumentType.getFloat(ctx, "multiplier")
                                            val durStr = StringArgumentType.getString(ctx, "duration")
                                            val msg = StringArgumentType.getString(ctx, "message")
                                            val seconds = DurationParser.parseSeconds(durStr)
                                                ?: throw SimpleCommandExceptionType(
                                                    LangManager.msg("invalidDuration", "input" to durStr)
                                                ).create()

                                            activateShiny(ctx.source, mult, seconds, msg)
                                            1
                                        }
                                    )
                                )
                            )
                        )
                )
            }
        }
    }

    private fun sendShinyValues(src: ServerCommandSource) {
        val typeName = LangManager.str("typeNameShiny", "Shiny")
        val baseDenom = Cobblemon.config.shinyRate.toDouble().coerceAtLeast(1.0)
        val minDenom = SBConfig.config.shiny.minAllowedRate.toDouble().coerceAtLeast(1.0)
        val activeMultRaw = BoosterService.getMultiplier("shiny").toDouble().coerceAtLeast(1.0)
        val maxMult = (baseDenom / minDenom).coerceAtLeast(1.0)
        val activeMultApplied = activeMultRaw.coerceAtMost(maxMult)
        val effectiveDenom = (baseDenom / activeMultApplied).coerceAtLeast(minDenom)

        val effectiveChancePct = (100.0 / effectiveDenom)

        val pctStr = when {
            effectiveChancePct >= 1.0 -> "%.2f".format(effectiveChancePct)
            effectiveChancePct >= 0.01 -> "%.4f".format(effectiveChancePct)
            else -> "%.6f".format(effectiveChancePct)
        }

        src.sendMessage(LangManager.msg("valuesHeader", "type" to typeName))
        src.sendMessage(LangManager.msg("valuesCobblemonShinyRate", "denom" to "%.2f".format(baseDenom)))
        src.sendMessage(LangManager.msg("valuesActiveMult", "mult" to "%.2f".format(activeMultRaw)))
        src.sendMessage(
            LangManager.msg(
                "valuesEffective",
                "denom" to "%.2f".format(effectiveDenom),
                "pct" to pctStr
            )
        )

        src.sendMessage(
            LangManager.msg(
                "valuesLimits",
                "minDenom" to "%.2f".format(minDenom),
                "maxMult" to "%.2f".format(maxMult)
            )
        )
    }

    private fun activateShiny(src: ServerCommandSource, mult: Float, seconds: Long, msgOverride: String?) {
        val ticks = (seconds * 20).toInt()

        val baseRate = Cobblemon.config.shinyRate.toDouble().coerceAtLeast(1.0)
        val minDenom = SBConfig.config.shiny.minAllowedRate.toDouble().coerceAtLeast(1.0)

        val maxMult = (baseRate / minDenom).toFloat().coerceAtLeast(1.0f)
        val effectiveMult = mult.coerceAtMost(maxMult)

        val msg = msgOverride ?: LangManager.str("bossbarShiny", "&6Shiny Boost Active! &b%.1fx &8(&e%dh %dm %ds&8)")

        val activeNow = BoosterService.activate(src.server, "shiny", effectiveMult, ticks, msg, announce = true)

        if (!activeNow) {
            val typeName = LangManager.str("typeNameShiny", "Shiny")
            val (h, m, s) = secondsToHms(seconds)
            src.sendMessage(
                LangManager.msg(
                    "boostQueued",
                    "type" to typeName,
                    "mult" to "%.1f".format(effectiveMult),
                    "h" to h, "m" to m, "s" to s
                )
            )
        }
    }

    private fun ticksToHms(ticks: Int): Triple<Int, Int, Int> {
        val secTot = (ticks / 20).coerceAtLeast(0)
        val h = secTot / 3600
        val m = (secTot / 60) % 60
        val s = secTot % 60
        return Triple(h, m, s)
    }

    private fun secondsToHms(seconds: Long): Triple<Int, Int, Int> {
        val secTot = seconds.coerceAtLeast(0)
        val h = (secTot / 3600).toInt()
        val m = ((secTot / 60) % 60).toInt()
        val s = (secTot % 60).toInt()
        return Triple(h, m, s)
    }
}