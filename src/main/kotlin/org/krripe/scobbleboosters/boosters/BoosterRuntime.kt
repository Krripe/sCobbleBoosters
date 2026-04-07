package org.krripe.scobbleboosters.boosters

import net.minecraft.entity.boss.ServerBossBar
import java.util.LinkedList

class BoosterRuntime(
    val def: BoosterDefinitions.Def,
    val bossBar: ServerBossBar
) {
    var active: BoosterInstance? = null
    val queue: LinkedList<BoosterInstance> = LinkedList()
    var initialDuration: Int = 0
    var lastShownSeconds: Int = Int.MIN_VALUE
    var lastQueueSig: Int = 0
}
