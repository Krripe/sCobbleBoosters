package org.krripe.scobbleboosters.gui.storage

import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.scobbleboosters.booster.BoosterRegistry
import org.krripe.scobbleboosters.gui.active.ActiveBoostersGui
import org.krripe.scobbleboosters.gui.core.*
import org.krripe.scobbleboosters.manager.BoosterBagActivationResult
import org.krripe.scobbleboosters.manager.BoosterBagManager
import org.krripe.scobbleboosters.manager.BoosterStorageManager
import org.krripe.scobbleboosters.message.MessageManager
import org.krripe.scobbleboosters.utils.DurationParser
import java.util.Locale
import java.util.UUID

object BoosterBagGui {

    private const val MENU_ID = "booster_bag"

    private data class Session(
        var page: Int = 0
    )

    private val sessions = mutableMapOf<UUID, Session>()

    fun open(player: ServerPlayerEntity) {
        sessions.getOrPut(player.uuid) { Session() }

        val frame = BoosterBagConfigManager.config.frame
        val size = GuiMenuUtils.normalizeSize(frame.size)

        GuiMenuUtils.playOpen(player, frame.sounds)

        openGui(
            player = player,
            title = GuiMenuUtils.title(
                GuiPlaceholders.apply(
                    frame.title,
                    pagePlaceholders(player)
                )
            ),
            guiItems = buildItems(player),
            inventorySize = size,
            menuId = MENU_ID,
            interaction = object : GuiInteraction {
                override fun onItemClick(
                    slot: Int,
                    player: ServerPlayerEntity,
                    rightClick: Boolean
                ) {
                    handleClick(player, slot)
                }

                override fun onGuiClose(player: ServerPlayerEntity) {
                    GuiMenuUtils.playClose(
                        player,
                        BoosterBagConfigManager.config.frame.sounds
                    )

                    sessions.remove(player.uuid)
                }
            }
        )
    }

    private fun handleClick(
        player: ServerPlayerEntity,
        slot: Int
    ) {
        val cfg =
            BoosterBagConfigManager.config

        val frame =
            cfg.frame

        val size =
            GuiMenuUtils.normalizeSize(
                frame.size
            )

        val button =
            GuiMenuUtils.buttonBySlot(
                frame.buttons,
                size,
                slot
            )

        if (button != null) {
            handleButton(
                player,
                button
            )
            return
        }

        val stored =
            boosterAtSlot(
                player,
                slot
            ) ?: return

        when (
            val result =
                BoosterBagManager.activate(
                    player,
                    stored.id
                )
        ) {
            is BoosterBagActivationResult.Activated -> {
                GuiMenuUtils.playClick(
                    player,
                    frame.sounds
                )

                MessageManager.send(
                    player,
                    "bag.activated",
                    "booster" to displayName(
                        result.booster.type
                    ),
                    "multiplier" to formatMultiplier(
                        result.booster.multiplier
                    )
                )

                refresh(player)
            }

            is BoosterBagActivationResult.Queued -> {
                GuiMenuUtils.playClick(
                    player,
                    frame.sounds
                )

                MessageManager.send(
                    player,
                    "bag.queued",
                    "booster" to displayName(
                        result.booster.type
                    ),
                    "multiplier" to formatMultiplier(
                        result.booster.multiplier
                    ),
                    "position" to result.position
                )

                refresh(player)
            }

            BoosterBagActivationResult.Disabled -> {
                GuiMenuUtils.playDeny(
                    player,
                    frame.sounds
                )

                MessageManager.send(
                    player,
                    "bag.disabled"
                )
            }

            BoosterBagActivationResult.NotFound -> {
                GuiMenuUtils.playDeny(
                    player,
                    frame.sounds
                )

                refresh(player)
            }

            BoosterBagActivationResult.Unsupported -> {
                GuiMenuUtils.playDeny(
                    player,
                    frame.sounds
                )

                MessageManager.send(
                    player,
                    "bag.unsupported"
                )
            }
        }
    }

    private fun handleButton(
        player: ServerPlayerEntity,
        button: GuiButtonCfg
    ) {
        val session = sessions.getOrPut(
            player.uuid
        ) {
            Session()
        }

        val frame =
            BoosterBagConfigManager.config.frame

        when (button.id) {
            "previous_page" -> {
                if (session.page <= 0) {
                    GuiMenuUtils.playDeny(
                        player,
                        frame.sounds,
                        button.denySound
                    )
                    return
                }

                session.page--

                GuiMenuUtils.playClick(
                    player,
                    frame.sounds,
                    button.clickSound
                )

                refresh(player)
            }

            "next_page" -> {
                val maxPage = maxPage(player)

                if (session.page >= maxPage) {
                    GuiMenuUtils.playDeny(
                        player,
                        frame.sounds,
                        button.denySound
                    )
                    return
                }

                session.page++

                GuiMenuUtils.playClick(
                    player,
                    frame.sounds,
                    button.clickSound
                )

                refresh(player)
            }

            "active_boosters" -> {
                GuiMenuUtils.playClick(
                    player,
                    frame.sounds,
                    button.clickSound
                )

                ActiveBoostersGui.open(player)
            }

            "close" -> {
                GuiMenuUtils.playClick(
                    player,
                    frame.sounds,
                    button.clickSound
                )

                player.closeHandledScreen()
            }
        }
    }

    private fun refresh(player: ServerPlayerEntity) {
        normalizePage(player)

        val size = GuiMenuUtils.normalizeSize(BoosterBagConfigManager.config.frame.size)

        updateOpenGuiSlots(
            player = player,
            newItems = buildItems(player),
            slots = refreshSlots(),
            expectedSize = size,
            expectedMenuId = MENU_ID
        )
    }

    private fun refreshSlots(): List<Int> {
        val cfg = BoosterBagConfigManager.config
        val size = GuiMenuUtils.normalizeSize(cfg.frame.size)

        val navigationSlots = cfg.frame.buttons
            .filter { it.id == "previous_page" || it.id == "next_page" }
            .flatMap { GuiSlotParser.buttonSlots(it, size) }

        return (boosterSlots() + navigationSlots + cfg.emptySlot).distinct()
    }

    private fun buildItems(player: ServerPlayerEntity): List<GuiItem> {
        normalizePage(player)

        val cfg = BoosterBagConfigManager.config
        val frame = cfg.frame
        val size = GuiMenuUtils.normalizeSize(frame.size)
        val placeholders = pagePlaceholders(player)
        val items = mutableListOf<GuiItem>()

        items += GuiMenuUtils.fillerItems(
            player,
            frame.fillers,
            size,
            placeholders
        )

        frame.buttons.forEach { button ->
            val visible = when (button.id) {
                "previous_page" -> currentPage(player) > 0
                "next_page" -> currentPage(player) < maxPage(player)
                else -> true
            }

            if (!visible) return@forEach

            items += GuiMenuUtils.buttonItems(
                player,
                button,
                size,
                placeholders
            )
        }

        val boosters = pageBoosters(player)
        val slots = boosterSlots()

        if (boosters.isEmpty() && BoosterStorageManager.get(player.uuid).isEmpty()) {
            items += GuiItem(
                GuiItemFactory.fromCfg(
                    player,
                    cfg.emptyItem,
                    placeholders
                ),
                cfg.emptySlot,
                "empty"
            )

            return items
        }

        boosters.forEachIndexed { index, booster ->
            val slot = slots.getOrNull(index) ?: return@forEachIndexed

            val itemCfg = cfg.boosterItem.copy(
                item = cfg.icons[booster.type]
                    ?: cfg.boosterItem.item,
                amount = booster.amount.coerceIn(1, 64)
            )

            items += GuiItem(
                GuiItemFactory.fromCfg(
                    player,
                    itemCfg,
                    boosterPlaceholders(booster)
                ),
                slot,
                "booster:${booster.id}"
            )
        }

        return items
    }

    private fun boosterAtSlot(
        player: ServerPlayerEntity,
        slot: Int
    ) = boosterSlots()
        .indexOf(slot)
        .takeIf { it >= 0 }
        ?.let { pageBoosters(player).getOrNull(it) }

    private fun pageBoosters(player: ServerPlayerEntity) =
        sortedBoosters(player)
            .drop(currentPage(player) * boosterSlots().size)
            .take(boosterSlots().size)

    private fun sortedBoosters(player: ServerPlayerEntity) =
        BoosterStorageManager.get(player.uuid)
            .sortedWith(
                compareBy<org.krripe.scobbleboosters.data.model.StoredBooster> {
                    it.type
                }
                    .thenByDescending { it.multiplier }
                    .thenByDescending { it.durationMillis }
            )

    private fun boosterSlots(): List<Int> {
        val cfg = BoosterBagConfigManager.config
        val size = GuiMenuUtils.normalizeSize(cfg.frame.size)

        return GuiSlotParser.parse(
            cfg.boosterSlots,
            size
        )
    }

    private fun normalizePage(player: ServerPlayerEntity) {
        val session = sessions.getOrPut(player.uuid) { Session() }

        session.page = session.page.coerceIn(
            0,
            maxPage(player)
        )
    }

    private fun currentPage(player: ServerPlayerEntity): Int {
        return sessions.getOrPut(player.uuid) {
            Session()
        }.page
    }

    private fun maxPage(player: ServerPlayerEntity): Int {
        val slots = boosterSlots()

        if (slots.isEmpty()) return 0

        val count = BoosterStorageManager.get(player.uuid).size

        return if (count <= 0) {
            0
        } else {
            (count - 1) / slots.size
        }
    }

    private fun pagePlaceholders(
        player: ServerPlayerEntity
    ): Map<String, String> {
        val max = maxPage(player)

        return mapOf(
            "page" to (currentPage(player) + 1).toString(),
            "pages" to (max + 1).toString(),
            "total" to BoosterStorageManager.count(player.uuid).toString()
        )
    }

    private fun boosterPlaceholders(
        booster: org.krripe.scobbleboosters.data.model.StoredBooster
    ): Map<String, String> {
        return mapOf(
            "booster" to displayName(booster.type),
            "booster_id" to booster.type,
            "multiplier" to formatMultiplier(booster.multiplier),
            "duration" to DurationParser.formatMillis(booster.durationMillis),
            "amount" to booster.amount.toString(),
            "data" to (booster.data ?: "-")
        )
    }

    private fun displayName(
        type: String
    ): String {
        return MessageManager.boosterName(type)
    }

    private fun formatMultiplier(value: Float): String {
        return if (value % 1f == 0f) {
            String.format(Locale.US, "%.0f", value)
        } else {
            String.format(Locale.US, "%.2f", value)
        }
    }
}