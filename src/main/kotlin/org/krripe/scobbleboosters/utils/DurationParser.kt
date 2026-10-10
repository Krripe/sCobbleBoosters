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

    fun formatMillis(milliseconds: Long): String {
        var seconds = (milliseconds / 1000L).coerceAtLeast(0L)

        val days = seconds / 86400L
        seconds %= 86400L

        val hours = seconds / 3600L
        seconds %= 3600L

        val minutes = seconds / 60L
        seconds %= 60L

        return buildList {
            if (days > 0L) add("${days}d")
            if (hours > 0L) add("${hours}h")
            if (minutes > 0L) add("${minutes}m")
            if (seconds > 0L || isEmpty()) add("${seconds}s")
        }.joinToString(" ")
    }
}
