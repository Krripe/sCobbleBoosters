package org.krripe.scobbleboosters.gui.active

import net.fabricmc.loader.api.FabricLoader
import org.krripe.scobbleboosters.config.JsonConfigIO
import org.krripe.scobbleboosters.gui.core.*

data class ActiveBoostersConfig(
    var frame: GuiMenuFrameCfg = GuiMenuFrameCfg(
        title = "&8Active Boosters",
        size = 54,
        sounds = GuiSoundsCfg(),
        fillers = listOf(
            GuiFillerCfg(
                id = "background",
                item = GuiItemCfg(
                    item = "minecraft:gray_stained_glass_pane",
                    name = " "
                ),
                slots = listOf(
                    "0-8,9,17,18,26,27,35,36,44,45-53"
                )
            )
        ),
        buttons = listOf(
            GuiButtonCfg(
                id = "personal_view",
                slot = 45,
                item = GuiItemCfg(
                    item = "head|eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2ZhNTE0OGU4MjFjMWZiZTIzZDYzZDA4Nzc2YjA1NWIwNjU0MDI4ZTJlMzk2NWVjMGRjNDhiNGU2MmNlNmQwNSJ9fX0=",
                    name = "&bPersonal Boosters",
                    lore = listOf(
                        "&8• &7sʜᴏᴡ ʏᴏᴜʀ ᴀᴄᴛɪᴠᴇ ᴀɴᴅ ꞯᴜᴇᴜᴇᴅ ʙᴏᴏsᴛᴇʀs.",
                        "",
                        "&7ᴄᴜʀʀᴇɴᴛ ᴠɪᴇᴡ: &f%view%"
                    )
                )
            ),
            GuiButtonCfg(
                id = "booster_bag",
                slot = 51,
                item = GuiItemCfg(
                    item = "head|eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjRjMTY0YmFjMjE4NGE3NmExZWU5NjkxMzI0MmUzMzVmMWQ0MTFjYWZmNTEyMDVlYTM5YjIwNWU2ZjhmMDU4YSJ9fX0=",
                    name = "&dBooster Bag",
                    lore = listOf(
                        "&8• &7ᴏᴘᴇɴ ʏᴏᴜʀ sᴛᴏʀᴇᴅ ʙᴏᴏsᴛᴇʀs."
                    )
                )
            ),
            GuiButtonCfg(
                id = "close",
                slot = 49,
                item = GuiItemCfg(
                    item = "minecraft:barrier",
                    name = "&cᴄʟᴏsᴇ"
                )
            ),
            GuiButtonCfg(
                id = "global_view",
                slot = 53,
                item = GuiItemCfg(
                    item = "head|eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTVjMmZjOTIzMjhjNTQ1YTk4YjlmNTk4ZTM4Y2RlMjQ4NGFkMTNjOWY3ZmE3NWQ2ZmNkZjg3MjE0ZjJhMmFhMiJ9fX0=",
                    name = "&6Global Boosters",
                    lore = listOf(
                        "&8• &7sʜᴏᴡ sᴇʀᴠᴇʀ-ᴡɪᴅᴇ ᴀᴄᴛɪᴠᴇ ᴀɴᴅ ꞯᴜᴇᴜᴇᴅ ʙᴏᴏsᴛᴇʀs.",
                        "",
                        "&7ᴄᴜʀʀᴇɴᴛ ᴠɪᴇᴡ: &f%view%"
                    )
                )
            )
        )
    ),
    var boosterSlots: List<String> = listOf(
        "10-16",
        "19-25",
        "28-34",
        "37-43"
    ),
    var boosterItem: GuiItemCfg = GuiItemCfg(
        item = "minecraft:nether_star",
        name = "&d%booster% Booster",
        lore = listOf(
            "&7sᴄᴏᴘᴇ: &f%scope%",
            "&7sᴛᴀᴛᴇ: %state%",
            "",
            "&8• &7ᴍᴜʟᴛɪᴘʟɪᴇʀ: &f%multiplier%",
            "&8• &7ʀᴇᴍᴀɪɴɪɴɢ: &f%remaining%",
            "&8• &7ᴅᴜʀᴀᴛɪᴏɴ: &f%duration%",
            "&8• &7ᴛʏᴘᴇ: &f%data%",
            "&8• &7ᴀᴄᴛɪᴠᴀᴛᴏʀ: &f%activator%",
            "",
            "&7ꞯᴜᴇᴜᴇ sɪᴢᴇ: &f%queue_size%"
        )
    ),
    var queueHeader: String = "&8ꞯᴜᴇᴜᴇ:",
    var queueLine: String = "&7#%position% &f%multiplier%x &8- &7%remaining% %queue_data%",
    var queueMoreLine: String = "&8... &7+%amount% ᴍᴏʀᴇ",
    var maxQueueLines: Int = 6,
    var icons: Map<String, String> = mapOf(
        "shiny" to "cobblemon:shiny_stone",
        "alpha" to "cobblemon:heat_rock",
        "catch" to "cobblemon:poke_ball",
        "experience" to "cobblemon:rare_candy",
        "bucket" to "minecraft:bucket",
        "bait" to "cobblemon:poke_bait",
        "fishing" to "cobblemon:poke_rod",
        "snack" to "cobblemon:poke_snack",
        "iv" to "cobblemon:hp_up",
        "type" to "cobblemon:poke_ball",
        "hiddenability" to "cobblemon:blank_tm"
    ),
    var emptySlot: Int = 22,
    var emptyItem: GuiItemCfg = GuiItemCfg(
        item = "minecraft:gray_dye",
        name = "&cNo %view% boosters",
        lore = listOf(
            "&8• &7ᴛʜᴇʀᴇ ᴀʀᴇ ɴᴏ ᴀᴄᴛɪᴠᴇ ᴏʀ",
            "&7ꞯᴜᴇᴜᴇᴅ %view_lower% ʙᴏᴏsᴛᴇʀs."
        )
    )
)

object ActiveBoostersConfigManager {
    private val file = FabricLoader.getInstance().configDir
        .resolve("scobbleboosters")
        .resolve("gui")
        .resolve("active_boosters.json")

    @Volatile
    var config: ActiveBoostersConfig = ActiveBoostersConfig()
        private set

    fun load() {
        config = JsonConfigIO.load(
            file,
            ActiveBoostersConfig(),
            ActiveBoostersConfig::class.java
        )
    }

    fun reload() {
        load()
    }
}