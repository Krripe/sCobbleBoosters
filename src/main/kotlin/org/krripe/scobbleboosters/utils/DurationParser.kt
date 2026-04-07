package org.krripe.scobbleboosters.utils

object DurationParser {
    private val part = Regex("(\\d+)([smhd])", RegexOption.IGNORE_CASE)

    fun parseSeconds(input: String): Long? {
        val s = input.trim()
        if (s.isEmpty()) return null

        var total = 0L
        var matchedAny = false

        for (m in part.findAll(s)) {
            matchedAny = true
            val amount = m.groupValues[1].toLongOrNull() ?: return null
            val unit = m.groupValues[2].lowercase()

            total += when (unit) {
                "s" -> amount
                "m" -> amount * 60
                "h" -> amount * 3600
                "d" -> amount * 86400
                else -> return null
            }
        }

        if (!matchedAny) return null

        val rebuilt = part.findAll(s).joinToString("") { it.value }
        if (!s.equals(rebuilt, ignoreCase = true)) return null

        return total.coerceAtLeast(1L)
    }
}
