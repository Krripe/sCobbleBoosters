package org.krripe.scobbleboosters.gui.storage

import net.fabricmc.loader.api.FabricLoader
import org.krripe.scobbleboosters.config.JsonConfigIO
import org.krripe.scobbleboosters.gui.core.*

data class BoosterBagConfig(
    var frame: GuiMenuFrameCfg = GuiMenuFrameCfg(
        title = "&6Booster Bag &7(&f%page%&7|&f%pages%&7)",
        size = 54,
        sounds = GuiSoundsCfg(),
        fillers = listOf(
            GuiFillerCfg(
                id = "background",
                item = GuiItemCfg(
                    item = "minecraft:gray_stained_glass_pane",
                    name = " "
                ),
                slots = listOf("0-8,9,17,18,26,27,35,36,44,45-53")
            )
        ),
        buttons = listOf(

            GuiButtonCfg(
                id = "active_boosters",
                slot = 51,
                item = GuiItemCfg(
                    item = "head|eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2VkZDIwYmU5MzUyMDk0OWU2Y2U3ODlkYzRmNDNlZmFlYjI4YzcxN2VlNmJmY2JiZTAyNzgwMTQyZjcxNiJ9fX0=",
                    name = "&bActive Boosters",
                    lore = listOf(
                        "&8• &7ᴠɪᴇᴡ ʏᴏᴜʀ ᴀᴄᴛɪᴠᴇ ᴀɴᴅ ꞯᴜᴇᴜᴇᴅ ʙᴏᴏsᴛᴇʀs."
                    )
                )
            ),

            GuiButtonCfg(
                id = "previous_page",
                slot = 45,
                item = GuiItemCfg(
                    item = "head|eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjZkYWI3MjcxZjRmZjA0ZDU0NDAyMTkwNjdhMTA5YjVjMGMxZDFlMDFlYzYwMmMwMDIwNDc2ZjdlYjYxMjE4MCJ9fX0=",
                    name = "&7ᴘʀᴇᴠɪᴏᴜs ᴘᴀɢᴇ",
                    lore = listOf("&7Page &f%page%&7|&f%pages%")
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
                id = "next_page",
                slot = 53,
                item = GuiItemCfg(
                    item = "head|eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGFhMTg3ZmVkZTg4ZGUwMDJjYmQ5MzA1NzVlYjdiYTQ4ZDNiMWEwNmQ5NjFiZGM1MzU4MDA3NTBhZjc2NDkyNiJ9fX0=",
                    name = "&7ɴᴇxᴛ ᴘᴀɢᴇ",
                    lore = listOf("&7Page &f%page%&7|&f%pages%")
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
            "&8• &7ᴍᴜʟᴛɪᴘʟɪᴇʀ: &f%multiplier%x",
            "&8• &7ᴅᴜʀᴀᴛɪᴏɴ: &f%duration%",
            "&8• &7ᴀᴍᴏᴜɴᴛ: &f%amount%",
            "&8• &7ᴛʏᴘᴇ: &f%data%",
            "",
            "&eClick to activate one."
        ),
        glow = true
    ),
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
        name = "&8• &cʏᴏᴜʀ ʙᴏᴏsᴛᴇʀ ʙᴀɢ ɪs ᴇᴍᴘᴛʏ",
        lore = listOf()
    )
)

object BoosterBagConfigManager {
    private val file = FabricLoader.getInstance().configDir
        .resolve("scobbleboosters")
        .resolve("gui")
        .resolve("bag.json")

    @Volatile
    var config: BoosterBagConfig = BoosterBagConfig()
        private set

    fun load() {
        config = JsonConfigIO.load(
            file,
            BoosterBagConfig(),
            BoosterBagConfig::class.java
        )
    }

    fun reload() {
        load()
    }
}