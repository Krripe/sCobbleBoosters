package org.krripe.scobbleboosters.boosters

import net.minecraft.entity.boss.BossBar

object BoosterDefinitions {
    data class Def(
        val id: String,
        val bossBarColor: BossBar.Color,
        val bossBarStyle: BossBar.Style
    )

    val SHINY = Def("shiny", BossBar.Color.PURPLE, BossBar.Style.PROGRESS)
    val ALL: List<Def> = listOf(SHINY)

    fun byId(id: String): Def? = ALL.firstOrNull { it.id == id }
}
