package org.krripe.scobbleboosters.message

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.MutableText
import org.krripe.scobbleboosters.booster.BoosterRegistry
import org.krripe.scobbleboosters.config.JsonConfigIO
import org.krripe.scobbleboosters.config.model.MessageConfig
import org.krripe.scobbleboosters.data.model.BoosterScope
import org.krripe.scobbleboosters.data.model.BoosterState
import org.krripe.scobbleboosters.utils.CustomColorParser
import org.krripe.scobbleboosters.utils.PlaceholderTextApiBridge
import java.util.Locale

object MessageManager {
    private val file = FabricLoader.getInstance().configDir
        .resolve("scobbleboosters")
        .resolve("message.json")

    @Volatile
    private var config: MessageConfig = defaultConfig()

    fun load() {
        config = JsonConfigIO.load(
            file,
            defaultConfig(),
            MessageConfig::class.java
        )
    }

    fun reload() {
        load()
    }

    fun render(
        key: String,
        player: ServerPlayerEntity? = null,
        vararg placeholders: Pair<String, Any?>
    ): String {
        val template = config.messages[key]
            ?: "&cMissing message: &f$key"

        var result = template.replace(
            "%prefix%",
            config.prefix
        )

        for ((name, value) in placeholders) {
            result = result.replace(
                "%$name%",
                value?.toString() ?: ""
            )
        }

        return PlaceholderTextApiBridge.parse(
            result,
            player
        )
    }

    fun text(
        key: String,
        player: ServerPlayerEntity? = null,
        vararg placeholders: Pair<String, Any?>
    ): MutableText {
        return CustomColorParser.processAllFormats(
            render(
                key,
                player,
                *placeholders
            )
        )
    }

    fun send(
        source: ServerCommandSource,
        key: String,
        vararg placeholders: Pair<String, Any?>
    ) {
        source.sendFeedback(
            {
                text(
                    key,
                    source.player,
                    *placeholders
                )
            },
            false
        )
    }

    fun error(
        source: ServerCommandSource,
        key: String,
        vararg placeholders: Pair<String, Any?>
    ) {
        source.sendError(
            text(
                key,
                source.player,
                *placeholders
            )
        )
    }

    fun send(
        player: ServerPlayerEntity,
        key: String,
        vararg placeholders: Pair<String, Any?>
    ) {
        player.sendMessage(
            text(
                key,
                player,
                *placeholders
            ),
            false
        )
    }

    fun boosterName(typeId: String): String {
        val type = typeId
            .lowercase(Locale.ROOT)
            .trim()

        return config.messages["booster.$type"]
            ?: BoosterRegistry.get(type)?.displayName
            ?: type
    }

    fun stateName(state: BoosterState): String {
        val key = when (state) {
            BoosterState.ACTIVE -> "state.active"
            BoosterState.PAUSED_MANUAL -> "state.paused_manual"
            BoosterState.PAUSED_GLOBAL -> "state.paused_global"
            BoosterState.PAUSED_MANUAL_AND_GLOBAL -> "state.paused_manual_global"
            BoosterState.INACTIVE -> "state.inactive"
        }

        return render(key)
    }

    fun scopeName(scope: BoosterScope): String {
        return render(
            when (scope) {
                BoosterScope.GLOBAL -> "scope.global"
                BoosterScope.PERSONAL -> "scope.personal"
            }
        )
    }

    private fun defaultConfig(): MessageConfig {
        return MessageConfig(
            prefix = "&8[<gradient:#FF8A00:#FFD166>Boosters</gradient>&8] &8» &r",
            messages = linkedMapOf(
                "common.unknown_booster" to "%prefix%&cUnknown booster: &f%booster%&c.",
                "common.booster_disabled" to "%prefix%&cBooster &f%booster% &cis currently disabled.",
                "common.booster_not_storable" to "%prefix%&cBooster &f%booster% &ccannot be stored.",
                "common.exactly_one_player" to "%prefix%&cYou must specify exactly one player.",
                "common.player_uuid_failed" to "%prefix%&cCould not resolve player UUID.",
                "common.requires_data" to "%prefix%&cBooster &f%booster% &crequires &f<%argument%>&c.",
                "common.invalid_duration" to "%prefix%&cInvalid duration &f%input%&c. Example: &f30m, 1h, 1h30m&c.",
                "common.invalid_booster" to "%prefix%&cInvalid booster multiplier, duration or data.",
                "common.unsupported_global" to "%prefix%&cBooster &f%booster% &cdoes not support global activation.",
                "common.unsupported_personal" to "%prefix%&cBooster &f%booster% &cdoes not support personal activation.",

                "global.activated" to "%prefix%&aGlobal &f%booster% &a%multiplier%x activated for &f%duration%&a.",
                "global.queued" to "%prefix%&eGlobal &f%booster% &e%multiplier%x added to queue at position &f%position%&e.",
                "global.paused" to "%prefix%&eGlobal &f%booster% &ebooster paused.",
                "global.already_paused" to "%prefix%&cGlobal &f%booster% &cis already paused.",
                "global.not_found" to "%prefix%&cNo active Global &f%booster% &cbooster.",
                "global.resumed" to "%prefix%&aGlobal &f%booster% &abooster resumed.",
                "global.already_running" to "%prefix%&cGlobal &f%booster% &cis already running.",
                "global.clear_active_next" to "%prefix%&aCleared active Global &f%booster%&a. Next &f%multiplier%x &abooster activated.",
                "global.clear_active" to "%prefix%&aCleared active Global &f%booster% &abooster.",
                "global.clear_type" to "%prefix%&aRemoved &f%amount% &aGlobal &f%booster% &abooster(s), including queue.",
                "global.clear_all" to "%prefix%&aRemoved &f%amount% &aGlobal booster(s).",
                "global.info_empty" to "%prefix%&cNo Global &f%booster% &cbooster is active or queued.",
                "global.info_header" to "%prefix%&6Global &f%booster% &8| &7State: %state% &8| &7Active: &f%multiplier%x &8| &7Remaining: &f%remaining% &8| &7Queue: &f%queue%",
                "global.info_queue" to "&8  #%position% &f%multiplier%x &8| &7%remaining%%data_suffix%",

                "personal.given" to "%prefix%&aGave &f%player% %booster% %multiplier%x &afor &f%duration%&a. Stored amount: &f%amount%&a.",
                "personal.activated" to "%prefix%&aPersonal &f%booster% %multiplier%x &aactivated for &f%player%&a.",
                "personal.queued" to "%prefix%&ePersonal &f%booster% %multiplier%x &equeued for &f%player% &eat position &f%position%&e.",
                "personal.clear_active_all" to "%prefix%&aCleared active Personal &f%booster% &abooster from &f%amount% &aplayer(s).",
                "personal.clear_type_all" to "%prefix%&aRemoved &f%amount% &aPersonal &f%booster% &abooster(s), including queues.",
                "personal.clear_all" to "%prefix%&aRemoved &f%amount% &aPersonal booster(s).",
                "personal.clear_active_player" to "%prefix%&aCleared active Personal &f%booster% &abooster from &f%player%&a.",
                "personal.clear_active_player_next" to "%prefix%&aCleared active Personal &f%booster% &abooster from &f%player%&a. Next &f%multiplier%x &abooster activated.",
                "personal.clear_active_player_none" to "%prefix%&c%player% has no active Personal &f%booster% &cbooster.",
                "personal.clear_type_player" to "%prefix%&aRemoved &f%amount% &aPersonal &f%booster% &abooster(s), including queue, from &f%player%&a.",
                "personal.clear_type_player_none" to "%prefix%&c%player% has no Personal &f%booster% &cboosters active or queued.",
                "personal.clear_player" to "%prefix%&aRemoved &f%amount% &aPersonal booster(s), including queues, from &f%player%&a.",
                "personal.clear_player_none" to "%prefix%&c%player% has no Personal boosters active or queued.",
                "personal.storage_not_found" to "%prefix%&c%player% has no stored &f%booster% &cbooster.",
                "personal.storage_removed_one" to "%prefix%&aRemoved one stored &f%booster% &abooster from &f%player%&a.",
                "personal.storage_removed_all" to "%prefix%&aRemoved &f%amount% &astored &f%booster% &abooster(s) from &f%player%&a.",

                "debug.enabled_all" to "%prefix%&aDebug enabled for all boosters.",
                "debug.disabled_all" to "%prefix%&eDebug disabled.",
                "debug.enabled_type" to "%prefix%&aDebug enabled for &f%booster%&a.",
                "debug.disabled_type" to "%prefix%&eDebug disabled for &f%booster%&e.",
                "debug.output" to "&8[&6sCobbleBoosters Debug&8][&e%type%&8] &7%message%",

                "reload.success" to "%prefix%&aConfigs reloaded successfully.",
                "reload.failed" to "%prefix%&cFailed to reload configs. Check console.",

                "bag.activated" to "%prefix%&a%booster% &f%multiplier%x &aactivated.",
                "bag.queued" to "%prefix%&e%booster% &f%multiplier%x &equeued at position &f%position%&e.",
                "bag.disabled" to "%prefix%&cThis booster is currently disabled.",
                "bag.unsupported" to "%prefix%&cThis booster cannot be activated from storage.",

                "state.active" to "&aActive",
                "state.paused_manual" to "&ePaused manually",
                "state.paused_global" to "&6Paused by Global",
                "state.paused_manual_global" to "&6Paused by Global &7+ &eManual",
                "state.inactive" to "&7Inactive",
                "state.disabled" to "&cDisabled by config",

                "scope.global" to "Global",
                "scope.personal" to "Personal",

                "bossbar.queue" to " &8| &6Queue: &e%queue%",

                "booster.shiny" to "Shiny",
                "booster.alpha" to "Alpha",
                "booster.catch" to "Catch",
                "booster.experience" to "Experience",
                "booster.bucket" to "Bucket",
                "booster.bait" to "Bait",
                "booster.fishing" to "Fishing",
                "booster.snack" to "Snack",
                "booster.iv" to "IV",
                "booster.type" to "Type",
                "booster.hiddenability" to "Hidden Ability"
            )
        )
    }
}