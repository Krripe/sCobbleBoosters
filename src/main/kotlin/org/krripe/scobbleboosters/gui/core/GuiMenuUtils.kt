package org.krripe.scobbleboosters.gui.core

import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.scobbleboosters.utils.CustomColorParser
import org.krripe.scobbleboosters.utils.PermissionsSystem
import org.krripe.scobbleboosters.utils.SoundHelper

object GuiMenuUtils {
    private val permissions = PermissionsSystem()

    fun normalizeSize(size: Int): Int {
        return when {
            size <= 9 -> 9
            size <= 18 -> 18
            size <= 27 -> 27
            size <= 36 -> 36
            size <= 45 -> 45
            else -> 54
        }
    }

    fun title(raw: String) = CustomColorParser.processAllFormats(raw)

    fun hasPermission(player: ServerPlayerEntity, permission: String?): Boolean {
        if (permission.isNullOrBlank()) return true
        return permissions.hasPermission(player.commandSource, permission)
    }

    fun playOpen(player: ServerPlayerEntity, sounds: GuiSoundsCfg) {
        if (sounds.enabled) SoundHelper.play(player, sounds.open)
    }

    fun playClose(player: ServerPlayerEntity, sounds: GuiSoundsCfg) {
        if (sounds.enabled) SoundHelper.play(player, sounds.close)
    }

    fun playClick(player: ServerPlayerEntity, sounds: GuiSoundsCfg, override: String? = null) {
        if (sounds.enabled) SoundHelper.play(player, override ?: sounds.click)
    }

    fun playDeny(player: ServerPlayerEntity, sounds: GuiSoundsCfg, override: String? = null) {
        if (sounds.enabled) SoundHelper.play(player, override ?: sounds.deny)
    }

    fun fillerItems(
        player: ServerPlayerEntity,
        fillers: List<GuiFillerCfg>,
        size: Int,
        placeholders: Map<String, String> = emptyMap()
    ): List<GuiItem> {
        val items = mutableListOf<GuiItem>()

        fillers.forEach { filler ->
            GuiSlotParser.parse(filler.slots, size).forEach { slot ->
                items += GuiItem(
                    GuiItemFactory.fromCfg(player, filler.item, placeholders),
                    slot,
                    filler.id
                )
            }
        }

        return items
    }

    fun buttonItems(
        player: ServerPlayerEntity,
        button: GuiButtonCfg,
        size: Int,
        placeholders: Map<String, String> = emptyMap(),
        enabled: Boolean = true
    ): List<GuiItem> {
        val item = if (enabled) button.item else button.disabledItem ?: button.item

        return GuiSlotParser.buttonSlots(button, size).map { slot ->
            GuiItem(
                GuiItemFactory.fromCfg(player, item, placeholders),
                slot,
                button.id
            )
        }
    }

    fun buttonBySlot(
        buttons: List<GuiButtonCfg>,
        size: Int,
        slot: Int
    ): GuiButtonCfg? {
        return buttons.firstOrNull {
            GuiSlotParser.buttonSlots(it, size).contains(slot)
        }
    }
}