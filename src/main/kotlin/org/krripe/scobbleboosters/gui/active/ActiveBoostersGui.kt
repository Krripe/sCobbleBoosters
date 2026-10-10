package org.krripe.scobbleboosters.gui.active

import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.scobbleboosters.booster.BoosterRegistry
import org.krripe.scobbleboosters.config.BoosterConfigManager
import org.krripe.scobbleboosters.data.model.BoosterRuntime
import org.krripe.scobbleboosters.data.model.BoosterState
import org.krripe.scobbleboosters.gui.core.*
import org.krripe.scobbleboosters.gui.storage.BoosterBagGui
import org.krripe.scobbleboosters.manager.GlobalBoosterManager
import org.krripe.scobbleboosters.manager.PersonalBoosterManager
import org.krripe.scobbleboosters.message.MessageManager
import org.krripe.scobbleboosters.utils.DurationParser
import java.util.Locale
import java.util.UUID

object ActiveBoostersGui {
    private const val MENU_ID = "active_boosters"

    private enum class View {
        PERSONAL,
        GLOBAL
    }

    private data class Session(
        var view: View = View.PERSONAL,
        var renderedTypes: List<String> = emptyList()
    )

    private data class RuntimeEntry(
        val type: String,
        val runtime: BoosterRuntime
    )

    private val sessions = mutableMapOf<UUID, Session>()

    fun open(player: ServerPlayerEntity) {
        sessions[player.uuid] = Session(View.PERSONAL)
        openCurrent(player)
    }

    fun tick(server: MinecraftServer, now: Long = System.currentTimeMillis()) {
        val uuids = sessions.keys.toList()

        for (uuid in uuids) {
            val player = server.playerManager.getPlayer(uuid)

            if (player == null) {
                sessions.remove(uuid)
                continue
            }

            val session = sessions[uuid] ?: continue
            val size = GuiMenuUtils.normalizeSize(ActiveBoostersConfigManager.config.frame.size)
            val currentEntries = entries(player, session.view)
            val currentTypes = currentEntries.map { it.type }

            if (currentTypes != session.renderedTypes) {
                val updated = updateOpenGuiSlots(
                    player = player,
                    newItems = buildDynamicItems(player, currentEntries, session.view, now),
                    slots = dynamicSlots(),
                    expectedSize = size,
                    expectedMenuId = MENU_ID
                )

                if (updated) session.renderedTypes = currentTypes else sessions.remove(uuid)
                continue
            }

            val slots = boosterSlots()

            val activeItems = currentEntries.mapIndexedNotNull { index, entry ->
                if (entry.runtime.active == null) return@mapIndexedNotNull null

                val slot = slots.getOrNull(index) ?: return@mapIndexedNotNull null
                buildBoosterItem(player, entry, session.view, slot, now)
            }

            if (activeItems.isEmpty()) continue

            val updated = updateOpenGuiSlots(
                player = player,
                newItems = activeItems,
                slots = activeItems.map { it.slot },
                expectedSize = size,
                expectedMenuId = MENU_ID
            )

            if (!updated) sessions.remove(uuid)
        }
    }

    fun reset() {
        sessions.clear()
    }

    private fun openCurrent(player: ServerPlayerEntity) {
        val frame = ActiveBoostersConfigManager.config.frame
        val size = GuiMenuUtils.normalizeSize(frame.size)

        GuiMenuUtils.playOpen(
            player,
            frame.sounds
        )

        openGui(
            player = player,
            title = GuiMenuUtils.title(frame.title),
            guiItems = buildItems(
                player,
                System.currentTimeMillis()
            ),
            inventorySize = size,
            menuId = MENU_ID,
            interaction = object : GuiInteraction {
                override fun onItemClick(
                    slot: Int,
                    player: ServerPlayerEntity,
                    rightClick: Boolean
                ) {
                    handleClick(
                        player,
                        slot
                    )
                }

                override fun onGuiClose(
                    player: ServerPlayerEntity
                ) {
                    GuiMenuUtils.playClose(
                        player,
                        ActiveBoostersConfigManager.config.frame.sounds
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
        val cfg = ActiveBoostersConfigManager.config
        val frame = cfg.frame
        val size = GuiMenuUtils.normalizeSize(frame.size)

        val button = GuiMenuUtils.buttonBySlot(
            frame.buttons,
            size,
            slot
        ) ?: return

        val session = sessions[player.uuid] ?: return

        when (button.id) {
            "personal_view" -> {
                session.view = View.PERSONAL

                GuiMenuUtils.playClick(
                    player,
                    frame.sounds,
                    button.clickSound
                )

                refresh(player)
            }

            "global_view" -> {
                session.view = View.GLOBAL

                GuiMenuUtils.playClick(
                    player,
                    frame.sounds,
                    button.clickSound
                )

                refresh(player)
            }

            "booster_bag" -> {
                GuiMenuUtils.playClick(
                    player,
                    frame.sounds,
                    button.clickSound
                )

                BoosterBagGui.open(player)
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
        val size = GuiMenuUtils.normalizeSize(ActiveBoostersConfigManager.config.frame.size)
        val updated = updateOpenGuiSlots(
            player = player,
            newItems = buildItems(player, System.currentTimeMillis()),
            slots = refreshSlots(),
            expectedSize = size,
            expectedMenuId = MENU_ID
        )

        if (!updated) sessions.remove(player.uuid)
    }

    private fun buildItems(player: ServerPlayerEntity, now: Long): List<GuiItem> {
        val cfg = ActiveBoostersConfigManager.config
        val frame = cfg.frame
        val size = GuiMenuUtils.normalizeSize(frame.size)
        val session = sessions.getOrPut(player.uuid) { Session() }
        val entries = entries(player, session.view)
        val placeholders = viewPlaceholders(player, session.view, entries.size)
        val items = mutableListOf<GuiItem>()

        session.renderedTypes = entries.map { it.type }

        items += GuiMenuUtils.fillerItems(player, frame.fillers, size, placeholders)

        frame.buttons.forEach { button ->
            val selected =
                (button.id == "personal_view" && session.view == View.PERSONAL) ||
                        (button.id == "global_view" && session.view == View.GLOBAL)

            val renderedButton = if (selected) button.copy(item = button.item.copy(glow = true)) else button
            items += GuiMenuUtils.buttonItems(player, renderedButton, size, placeholders)
        }

        items += buildDynamicItems(player, entries, session.view, now)
        return items
    }

    private fun buildDynamicItems(
        player: ServerPlayerEntity,
        entries: List<RuntimeEntry>,
        view: View,
        now: Long
    ): List<GuiItem> {
        val cfg = ActiveBoostersConfigManager.config

        if (entries.isEmpty()) {
            return listOf(
                GuiItem(
                    GuiItemFactory.fromCfg(player, cfg.emptyItem, viewPlaceholders(player, view, 0)),
                    cfg.emptySlot,
                    "empty"
                )
            )
        }

        val slots = boosterSlots()

        return entries.mapIndexedNotNull { index, entry ->
            val slot = slots.getOrNull(index) ?: return@mapIndexedNotNull null
            buildBoosterItem(player, entry, view, slot, now)
        }
    }

    private fun buildBoosterItem(
        player: ServerPlayerEntity,
        entry: RuntimeEntry,
        view: View,
        slot: Int,
        now: Long
    ): GuiItem {
        val cfg = ActiveBoostersConfigManager.config
        val lore = cfg.boosterItem.lore.toMutableList()

        if (entry.runtime.queue.isNotEmpty()) {
            lore += ""
            lore += cfg.queueHeader

            val visibleQueue = entry.runtime.queue.take(cfg.maxQueueLines.coerceAtLeast(0))

            visibleQueue.forEachIndexed { queueIndex, booster ->
                val queueData = booster.data?.takeIf { it.isNotBlank() }?.let { " &8(&b$it&8)" } ?: ""

                lore += GuiPlaceholders.apply(
                    cfg.queueLine,
                    mapOf(
                        "position" to (queueIndex + 1).toString(),
                        "multiplier" to formatMultiplier(booster.multiplier),
                        "remaining" to DurationParser.formatMillis(booster.remaining(now)),
                        "queue_data" to queueData
                    )
                )
            }

            val hidden = entry.runtime.queue.size - visibleQueue.size

            if (hidden > 0) {
                lore += GuiPlaceholders.apply(
                    cfg.queueMoreLine,
                    mapOf("amount" to hidden.toString())
                )
            }
        }

        val itemCfg = cfg.boosterItem.copy(
            item = cfg.icons[entry.type] ?: cfg.boosterItem.item,
            lore = lore,
            glow = BoosterConfigManager.isEnabled(entry.type) && entry.runtime.state() == BoosterState.ACTIVE
        )

        return GuiItem(
            GuiItemFactory.fromCfg(
                player,
                itemCfg,
                boosterPlaceholders(entry.type, entry.runtime, view, now)
            ),
            slot,
            "booster:${entry.type}"
        )
    }

    private fun dynamicSlots(): List<Int> {
        val cfg = ActiveBoostersConfigManager.config
        return (boosterSlots() + cfg.emptySlot).distinct()
    }

    private fun refreshSlots(): List<Int> {
        val cfg = ActiveBoostersConfigManager.config
        val size = GuiMenuUtils.normalizeSize(cfg.frame.size)

        val viewSlots = cfg.frame.buttons
            .filter { it.id == "personal_view" || it.id == "global_view" }
            .flatMap { GuiSlotParser.buttonSlots(it, size) }

        return (dynamicSlots() + viewSlots).distinct()
    }

    private fun entries(
        player: ServerPlayerEntity,
        view: View
    ): List<RuntimeEntry> {
        return BoosterRegistry.all()
            .mapNotNull { definition ->
                val runtime = when (view) {
                    View.PERSONAL ->
                        PersonalBoosterManager.getRuntime(
                            player.uuid,
                            definition.id
                        )

                    View.GLOBAL ->
                        GlobalBoosterManager.getRuntime(
                            definition.id
                        )
                } ?: return@mapNotNull null

                if (
                    runtime.active == null &&
                    runtime.queue.isEmpty()
                ) {
                    return@mapNotNull null
                }

                RuntimeEntry(
                    type = definition.id,
                    runtime = runtime
                )
            }
    }

    private fun boosterSlots(): List<Int> {
        val cfg = ActiveBoostersConfigManager.config
        val size = GuiMenuUtils.normalizeSize(
            cfg.frame.size
        )

        return GuiSlotParser.parse(
            cfg.boosterSlots,
            size
        )
    }

    private fun boosterPlaceholders(
        type: String,
        runtime: BoosterRuntime,
        scope: View,
        now: Long
    ): Map<String, String> {
        val active = runtime.active

        return mapOf(
            "booster" to MessageManager.boosterName(type),
            "booster_id" to type,
            "scope" to scopeDisplay(scope),
            "state" to stateDisplay(
                type,
                runtime
            ),
            "multiplier" to (
                    active?.let {
                        "${formatMultiplier(it.multiplier)}x"
                    } ?: "-"
                    ),
            "remaining" to (
                    active?.let {
                        DurationParser.formatMillis(
                            it.remaining(now)
                        )
                    } ?: "-"
                    ),
            "duration" to (
                    active?.let {
                        DurationParser.formatMillis(
                            it.durationMillis
                        )
                    } ?: "-"
                    ),
            "queue_size" to runtime.queueSize().toString(),
            "data" to (
                    active?.data
                        ?.takeIf { it.isNotBlank() }
                        ?: "-"
                    ),
            "activator" to (
                    active?.activatorName
                        ?.takeIf { it.isNotBlank() }
                        ?: "-"
                    )
        )
    }

    private fun viewPlaceholders(
        player: ServerPlayerEntity,
        view: View,
        total: Int
    ): Map<String, String> {
        val display = scopeDisplay(view)

        return mapOf(
            "view" to display,
            "view_lower" to display.lowercase(Locale.ROOT),
            "player" to player.name.string,
            "total" to total.toString()
        )
    }

    private fun stateDisplay(
        type: String,
        runtime: BoosterRuntime
    ): String {
        if (!BoosterConfigManager.isEnabled(type)) {
            return MessageManager.render(
                "state.disabled"
            )
        }

        return MessageManager.stateName(
            runtime.state()
        )
    }

    private fun scopeDisplay(
        view: View
    ): String {
        return MessageManager.render(
            when (view) {
                View.PERSONAL -> "scope.personal"
                View.GLOBAL -> "scope.global"
            }
        )
    }

    private fun formatMultiplier(
        value: Float
    ): String {
        return if (value % 1f == 0f) {
            String.format(
                Locale.US,
                "%.0f",
                value
            )
        } else {
            String.format(
                Locale.US,
                "%.2f",
                value
            )
        }
    }
}