package org.krripe.scobbleboosters.utils

import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.server.command.ServerCommandSource

class PermissionsSystem {
    fun hasPermission(player: ServerCommandSource, permission: String): Boolean {
        return try {
            Permissions.getPermissionValue(player, permission).orElse(false)
        } catch (_: NoClassDefFoundError) {
            false
        }
    }
}
