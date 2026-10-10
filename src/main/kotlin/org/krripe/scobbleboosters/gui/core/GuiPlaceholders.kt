package org.krripe.scobbleboosters.gui.core

object GuiPlaceholders {

    fun apply(input: String, placeholders: Map<String, String>): String {
        var output = input

        placeholders.forEach { (key, value) ->
            output = output.replace("%$key%", value)
        }

        return output
    }

    fun applyList(input: List<String>, placeholders: Map<String, String>): List<String> {
        return input.map { apply(it, placeholders) }
    }
}