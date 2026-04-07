package org.krripe.scobbleboosters.lang

import com.google.gson.JsonObject
import net.minecraft.text.MutableText
import org.krripe.scobbleboosters.utils.CustomColorParser
import org.krripe.scobbleboosters.utils.FileIO
import org.krripe.scobbleboosters.utils.Gsons
import java.io.File

object LangManager {

    private val file = File("config/sCobbleBoosters/messages.json")
    private const val PRUNE_UNUSED_KEYS = true

    fun str(key: String, fallback: String): String = raw(key) ?: fallback

    private fun defaultJson(): JsonObject = JsonObject().apply {
        addProperty("prefix", "&8[<gradient:8A2BE2:FF00FF>sCobbleBoosters</gradient>&8] ")

        addProperty("reloaded", "%prefix% &aReloaded configs.")
        addProperty("invalidDuration", "%prefix% &cInvalid duration: &f%input% &7(Examples: 30m, 2h30m, 10s)")
        addProperty("multiplierTooHigh", "%prefix% &cMultiplier too high! Max allowed: &e%max%x")
        addProperty("unknownType", "%prefix% &cUnknown booster type: &f%type%")
        addProperty("broadcastBoostActivated", "%prefix% %msg%")
        addProperty("boostQueued", "%prefix% &eQueued &b%type%&e booster &b%mult%x &7(%h%h %m%m %s%s).")
        addProperty("boostRemovedNext", "%prefix% &eRemoved &b%mult%x&7. Next booster activated!")
        addProperty("boostRemovedNone", "%prefix% &eRemoved &b%mult%x&7. No more boosters.")
        addProperty("boostRemovedQueued", "%prefix% &eRemoved queued &b%mult%x&7.")
        addProperty("boostNotFound", "%prefix% &cNo booster with &f%mult%x &cwas found.")
        addProperty("infoHeader", "%prefix% &6%type% boosters:")
        addProperty("infoNone", "&7- &cNo active booster.")
        addProperty("infoActive", "&7- &aActive: &b%mult%x &7(%h%h %m%m %s%s left)")
        addProperty("infoQueueHeader", "&7- &eQueue:")
        addProperty("infoQueueEntry", "&8  %pos%. &b%mult%x &7(%h%h %m%m %s%s)")
        addProperty("typeNameShiny", "Shiny")
        addProperty("bossbarShiny", "&6Shiny Boost Active! &b%.1fx &8(&e%dh %dm %ds&8)")
        addProperty("valuesHeader", "%prefix% &6Cobblemon values for &e%type%&6:")
        addProperty("valuesCobblemonShinyRate", "&7- Cobblemon shinyRate denom: &f%denom%")
        addProperty("valuesActiveMult", "&7- Active booster multiplier: &b%mult%x")
        addProperty("valuesEffective", "&7- Effective denom: &f%denom% &8(&a%pct%%&8)")
        addProperty("valuesLimits", "&7- Limits: min denom &f%minDenom%&7, max mult &e%maxMult%x")
        addProperty("valuesChange", "&7- Modified: &f1/%base% &7-> &f1/%eff% &8(&a%pct%%&8)")
    }

    @Volatile
    private var json: JsonObject = defaultJson()

    fun load() {
        val defaults = defaultJson()
        val gson = Gsons.pretty

        if (!file.exists()) {
            FileIO.atomicWriteText(file, gson.toJson(defaults))
            json = defaults
            return
        }

        val existing = runCatching {
            gson.fromJson(FileIO.safeReadText(file) ?: "{}", JsonObject::class.java)
        }.getOrNull() ?: JsonObject()

        for ((k, v) in defaults.entrySet()) {
            if (!existing.has(k) || existing.get(k).isJsonNull) existing.add(k, v)
        }

        if (PRUNE_UNUSED_KEYS) {
            val toRemove = existing.entrySet().map { it.key }.filter { !defaults.has(it) }
            toRemove.forEach { existing.remove(it) }
        }

        FileIO.atomicWriteText(file, gson.toJson(existing))
        json = existing
    }

    fun reload() = load()

    fun raw(key: String): String? =
        if (json.has(key) && json.get(key).isJsonPrimitive) json.get(key).asString else null

    fun msg(key: String, vararg args: Pair<String, Any?>): MutableText {
        val prefix = raw("prefix") ?: ""
        var s = raw(key) ?: "%prefix%&cMissing lang key: &f$key"

        s = s.replace("%prefix%", prefix)
        for ((k, v) in args) s = s.replace("%$k%", v?.toString() ?: "")

        return CustomColorParser.processAllFormats(s)
    }
}