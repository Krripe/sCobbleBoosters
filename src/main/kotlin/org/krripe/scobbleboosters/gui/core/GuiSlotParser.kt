package org.krripe.scobbleboosters.gui.core

object GuiSlotParser {

    fun parse(expressions: List<String>, size: Int): List<Int> {
        val slots = linkedSetOf<Int>()

        expressions
            .flatMap { it.split(",") }
            .forEach { raw ->
                val expression = raw.trim()
                if (expression.isBlank()) return@forEach

                if ("-" in expression) {
                    val start = expression.substringBefore("-").trim().toIntOrNull()
                    val end = expression.substringAfter("-").trim().toIntOrNull()

                    if (start != null && end != null) {
                        for (slot in minOf(start, end)..maxOf(start, end)) {
                            if (slot in 0 until size) slots.add(slot)
                        }
                    }

                    return@forEach
                }

                expression.toIntOrNull()
                    ?.takeIf { it in 0 until size }
                    ?.let(slots::add)
            }

        return slots.toList()
    }

    fun buttonSlots(button: GuiButtonCfg, size: Int): List<Int> {
        val configured = parse(button.slots, size)

        if (configured.isNotEmpty()) return configured

        return if (button.slot in 0 until size) {
            listOf(button.slot)
        } else {
            emptyList()
        }
    }
}