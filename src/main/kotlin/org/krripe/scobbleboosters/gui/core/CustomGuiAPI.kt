package org.krripe.scobbleboosters.gui.core

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

data class GuiItem(
    val itemStack: ItemStack,
    val slot: Int,
    val id: String? = null
)

interface GuiInteraction {
    fun onItemClick(slot: Int, player: ServerPlayerEntity, rightClick: Boolean)
    fun onGuiClose(player: ServerPlayerEntity)
}

fun openGui(
    player: ServerPlayerEntity,
    title: Text,
    guiItems: List<GuiItem>,
    inventorySize: Int,
    menuId: String,
    interaction: GuiInteraction
) {
    require(inventorySize in 9..54 && inventorySize % 9 == 0)

    player.openHandledScreen(
        CustomGuiScreenHandlerFactory(
            title = title,
            guiItems = guiItems,
            inventorySize = inventorySize,
            menuId = menuId,
            interaction = interaction
        ).create(player)
    )
}

fun updateOpenGui(
    player: ServerPlayerEntity,
    newItems: List<GuiItem>,
    expectedSize: Int? = null,
    expectedMenuId: String? = null
): Boolean {
    val handler = player.currentScreenHandler

    if (handler !is CustomGuiScreenHandler) return false
    if (expectedSize != null && expectedSize != handler.size) return false
    if (expectedMenuId != null && expectedMenuId != handler.menuId) return false

    handler.updateGuiContents(newItems)
    return true
}

fun updateOpenGuiSlots(
    player: ServerPlayerEntity,
    newItems: List<GuiItem>,
    slots: Iterable<Int>,
    expectedSize: Int? = null,
    expectedMenuId: String? = null
): Boolean {
    val handler = player.currentScreenHandler

    if (handler !is CustomGuiScreenHandler) return false
    if (expectedSize != null && expectedSize != handler.size) return false
    if (expectedMenuId != null && expectedMenuId != handler.menuId) return false

    handler.updateGuiSlots(newItems, slots)
    return true
}

fun getGuiScreenHandlerType(size: Int): ScreenHandlerType<*> {
    return when (size) {
        9 -> ScreenHandlerType.GENERIC_9X1
        18 -> ScreenHandlerType.GENERIC_9X2
        27 -> ScreenHandlerType.GENERIC_9X3
        36 -> ScreenHandlerType.GENERIC_9X4
        45 -> ScreenHandlerType.GENERIC_9X5
        54 -> ScreenHandlerType.GENERIC_9X6
        else -> throw IllegalArgumentException("Invalid GUI size: $size")
    }
}

private class GuiLockedSlot(
    inventory: Inventory,
    index: Int,
    x: Int,
    y: Int
) : Slot(inventory, index, x, y) {
    override fun canInsert(stack: ItemStack) = false
    override fun canTakeItems(player: PlayerEntity) = false
}

class CustomGuiScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    initialGuiItems: List<GuiItem>,
    private val inventorySize: Int,
    val menuId: String,
    private val interaction: GuiInteraction
) : ScreenHandler(getGuiScreenHandlerType(inventorySize), syncId) {

    val size: Int
        get() = inventorySize

    private val guiInventory = object : Inventory {
        private val items = Array(inventorySize) { ItemStack.EMPTY }

        init {
            initialGuiItems.forEach {
                if (it.slot in 0 until inventorySize) {
                    items[it.slot] = it.itemStack
                }
            }
        }

        override fun clear() {
            items.fill(ItemStack.EMPTY)
        }

        override fun size() = items.size

        override fun isEmpty() = items.all { it.isEmpty }

        override fun getStack(slot: Int): ItemStack {
            return items[slot]
        }

        override fun removeStack(slot: Int, amount: Int): ItemStack {
            val stack = getStack(slot)

            return if (stack.count <= amount) {
                removeStack(slot)
            } else {
                stack.split(amount)
            }
        }

        override fun removeStack(slot: Int): ItemStack {
            val stack = items[slot]
            items[slot] = ItemStack.EMPTY
            return stack
        }

        override fun setStack(slot: Int, stack: ItemStack) {
            items[slot] = stack
        }

        override fun markDirty() {
            this@CustomGuiScreenHandler.sendContentUpdates()
        }

        override fun canPlayerUse(player: PlayerEntity?) = true
    }

    init {
        for (slotIndex in 0 until inventorySize) {
            addSlot(
                GuiLockedSlot(
                    guiInventory,
                    slotIndex,
                    8 + (slotIndex % 9) * 18,
                    18 * (slotIndex / 9)
                )
            )
        }

        for (row in 0..2) {
            for (column in 0..8) {
                addSlot(
                    Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        8 + column * 18,
                        84 + row * 18
                    )
                )
            }
        }

        for (column in 0..8) {
            addSlot(
                Slot(
                    playerInventory,
                    column,
                    8 + column * 18,
                    142
                )
            )
        }
    }

    override fun canUse(player: PlayerEntity?) = true

    override fun quickMove(
        player: PlayerEntity,
        slotIndex: Int
    ): ItemStack {
        val slot = slots.getOrNull(slotIndex) ?: return ItemStack.EMPTY
        if (!slot.hasStack()) return ItemStack.EMPTY
        if (slotIndex < inventorySize) return ItemStack.EMPTY

        val stack = slot.stack
        val original = stack.copy()

        val inventoryStart = inventorySize
        val hotbarStart = inventorySize + 27
        val inventoryEnd = inventorySize + 36

        when (slotIndex) {
            in inventoryStart until hotbarStart -> {
                if (!insertItem(stack, hotbarStart, inventoryEnd, false)) {
                    return ItemStack.EMPTY
                }
            }

            in hotbarStart until inventoryEnd -> {
                if (!insertItem(stack, inventoryStart, hotbarStart, false)) {
                    return ItemStack.EMPTY
                }
            }

            else -> return ItemStack.EMPTY
        }

        if (stack.isEmpty) {
            slot.stack = ItemStack.EMPTY
        } else {
            slot.markDirty()
        }

        return original
    }

    override fun onSlotClick(
        slotIndex: Int,
        button: Int,
        actionType: SlotActionType?,
        player: PlayerEntity?
    ) {
        if (
            slotIndex in 0 until inventorySize &&
            player is ServerPlayerEntity
        ) {
            interaction.onItemClick(
                slotIndex,
                player,
                button == 1
            )
            return
        }

        if (actionType == SlotActionType.PICKUP_ALL) return

        super.onSlotClick(
            slotIndex,
            button,
            actionType,
            player
        )
    }

    override fun onClosed(player: PlayerEntity?) {
        super.onClosed(player)

        if (player is ServerPlayerEntity) {
            interaction.onGuiClose(player)
        }
    }

    fun updateGuiContents(newItems: List<GuiItem>) {
        updateGuiSlots(newItems, 0 until inventorySize)
    }

    fun updateGuiSlots(newItems: List<GuiItem>, targetSlots: Iterable<Int>) {
        val itemsBySlot = newItems.associateBy { it.slot }

        targetSlots.distinct().forEach { slot ->
            if (slot !in 0 until inventorySize) return@forEach
            guiInventory.setStack(slot, itemsBySlot[slot]?.itemStack ?: ItemStack.EMPTY)
        }

        guiInventory.markDirty()
    }
}

private class CustomGuiScreenHandlerFactory(
    private val title: Text,
    private val guiItems: List<GuiItem>,
    private val inventorySize: Int,
    private val menuId: String,
    private val interaction: GuiInteraction
) {
    fun create(
        player: ServerPlayerEntity
    ): SimpleNamedScreenHandlerFactory {
        return SimpleNamedScreenHandlerFactory(
            { syncId, inventory, _ ->
                CustomGuiScreenHandler(
                    syncId = syncId,
                    playerInventory = inventory,
                    initialGuiItems = guiItems,
                    inventorySize = inventorySize,
                    menuId = menuId,
                    interaction = interaction
                )
            },
            title
        )
    }
}