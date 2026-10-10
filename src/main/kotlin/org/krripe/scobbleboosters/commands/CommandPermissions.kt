package org.krripe.scobbleboosters.commands

import net.minecraft.server.command.ServerCommandSource
import org.krripe.scobbleboosters.utils.PermissionsSystem

object CommandPermissions {
    private val permissions = PermissionsSystem()

    fun admin(source: ServerCommandSource): Boolean {
        if (source.entity == null) return true
        if (source.hasPermissionLevel(2)) return true
        return permissions.hasPermission(source, "scobbleboosters.admin")
    }

    fun boosterBag(source: ServerCommandSource): Boolean {
        if (source.player == null) return false
        if (admin(source)) return true
        return permissions.hasPermission(source, "scobbleboosters.booster.bag")
    }
}