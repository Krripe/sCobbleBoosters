package org.krripe.scobbleboosters.mixin;

import com.cobblemon.mod.common.block.entity.PokeSnackBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.krripe.scobbleboosters.booster.effect.SnackBooster;
import org.krripe.scobbleboosters.booster.effect.SnackSpawnTracker;
import org.krripe.scobbleboosters.debug.DebugManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PokeSnackBlockEntity.class, remap = false)
public abstract class PokeSnackBlockEntityMixin {

    @Shadow
    public abstract boolean isLure();

    @Shadow
    public abstract void attemptSpawn(PlayerEntity player);

    @Unique
    private boolean scobbleboosters$extraAttemptRunning;

    @Unique
    private long scobbleboosters$startSuccessCount;

    @Unique
    private PokeSnackBlockEntity scobbleboosters$self() {
        return (PokeSnackBlockEntity) (Object) this;
    }

    @Inject(method = "attemptSpawn", at = @At("HEAD"), remap = false)
    private void scobbleboosters$startAttempt(PlayerEntity player, CallbackInfo ci) {
        if (scobbleboosters$extraAttemptRunning) return;

        scobbleboosters$startSuccessCount =
                SnackSpawnTracker.count(scobbleboosters$self());
    }

    @Inject(method = "attemptSpawn", at = @At("TAIL"), remap = false)
    private void scobbleboosters$retryFailedSpawn(PlayerEntity player, CallbackInfo ci) {
        if (scobbleboosters$extraAttemptRunning) return;
        if (!isLure()) return;

        PokeSnackBlockEntity self = scobbleboosters$self();

        if (SnackSpawnTracker.count(self) > scobbleboosters$startSuccessCount) {
            return;
        }

        float multiplier = SnackBooster.multiplier(player.getUuid());
        if (multiplier <= 1.0F) return;

        int maxAttempts = SnackBooster.extraAttempts();
        if (maxAttempts <= 0) return;

        scobbleboosters$extraAttemptRunning = true;

        int used = 0;
        boolean success = false;

        try {
            while (used < maxAttempts) {
                long before = SnackSpawnTracker.count(self);

                used++;
                attemptSpawn(player);

                if (SnackSpawnTracker.count(self) > before) {
                    success = true;
                    break;
                }
            }

            DebugManager.INSTANCE.logThrottled(
                    "snack",
                    "extra-attempts-" + player.getUuid(),
                    3000L,
                    "spawn retry | attempts=" + used +
                            " | result=" + (success ? "found" : "missing") +
                            " | multiplier=" + multiplier + "x"
            );
        } finally {
            scobbleboosters$extraAttemptRunning = false;
        }
    }
}