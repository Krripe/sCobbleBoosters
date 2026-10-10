package org.krripe.scobbleboosters.mixin;

import com.cobblemon.mod.common.api.spawning.detail.SpawnAction;
import com.cobblemon.mod.common.entity.fishing.PokeRodFishingBobberEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.krripe.scobbleboosters.booster.effect.FishingBooster;
import org.krripe.scobbleboosters.debug.DebugManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;
import java.util.UUID;

@Mixin(value = PokeRodFishingBobberEntity.class, remap = false)
public abstract class PokeRodFishingBobberMixin {

    @Shadow private int waitCountdown;
    @Shadow private int fishTravelCountdown;
    @Shadow private int hookCountdown;
    @Shadow private SpawnAction<?> plannedSpawnAction;

    @Unique
    private boolean scobbleboosters$extraPlanRunning;

    @Invoker("planSpawn")
    protected abstract void scobbleboosters$invokePlanSpawn(
            ServerPlayerEntity player,
            ItemStack rodItemStack,
            int lureLevel
    );

    @Unique
    private UUID scobbleboosters$getOwnerUuid() {
        Entity owner = ((ProjectileEntity) (Object) this).getOwner();
        return owner instanceof ServerPlayerEntity player ? player.getUuid() : null;
    }

    @Unique
    private float scobbleboosters$getMultiplier() {
        return FishingBooster.multiplier(scobbleboosters$getOwnerUuid());
    }

    @Inject(method = "tickFishingLogic", at = @At("TAIL"), remap = false)
    private void scobbleboosters$boostTimers(BlockPos pos, CallbackInfo ci) {
        float multiplier = scobbleboosters$getMultiplier();
        if (multiplier <= 1.0F) return;

        multiplier = Math.min(multiplier, FishingBooster.maxSpeedMultiplier());

        int minWait = FishingBooster.minWaitTicks();
        int minTravel = FishingBooster.minTravelTicks();
        int extraWait = Math.max(1, Math.round(multiplier - 1.0F));
        int extraHook = Math.max(1, Math.round((multiplier - 1.0F) / 2.0F));

        if (waitCountdown > minWait) {
            waitCountdown = Math.max(minWait, waitCountdown - extraWait);
        }

        if (fishTravelCountdown > minTravel) {
            fishTravelCountdown = Math.max(minTravel, fishTravelCountdown - extraWait);
        }

        if (hookCountdown > 1) {
            hookCountdown = Math.max(1, hookCountdown - extraHook);
        }
    }

    @Inject(method = "getPokemonSpawnChance", at = @At("RETURN"), cancellable = true, remap = false)
    private void scobbleboosters$boostPokemonChance(
            ItemStack stack,
            CallbackInfoReturnable<Integer> cir
    ) {
        float multiplier = scobbleboosters$getMultiplier();
        if (multiplier <= 1.0F) return;

        int original = cir.getReturnValueI();
        int cap = FishingBooster.maxPokemonChance();

        if (FishingBooster.guaranteePokemonBite()) {
            cap = Math.max(cap, 101);
        }

        int result = Math.max(
                original,
                Math.min(Math.round(original * multiplier), cap)
        );

        cir.setReturnValue(result);

        DebugManager.INSTANCE.logThrottled(
                "fishing",
                "pokemon-chance-" + scobbleboosters$getOwnerUuid(),
                2000L,
                "pokemon chance | " + original + " -> " + result +
                        " | multiplier=" + scobbleboosters$format(multiplier) + "x"
        );
    }

    @Inject(method = "planSpawn", at = @At("TAIL"), remap = false)
    private void scobbleboosters$retryPlanSpawn(
            ServerPlayerEntity player,
            ItemStack rodItemStack,
            int lureLevel,
            CallbackInfo ci
    ) {
        if (scobbleboosters$extraPlanRunning || plannedSpawnAction != null) return;

        float multiplier = scobbleboosters$getMultiplier();
        if (multiplier <= 1.0F) return;

        int attempts = FishingBooster.extraPlanAttempts();
        if (attempts <= 0) return;

        scobbleboosters$extraPlanRunning = true;
        int used = 0;

        try {
            while (used < attempts && plannedSpawnAction == null) {
                used++;
                scobbleboosters$invokePlanSpawn(player, rodItemStack, lureLevel);
            }

            DebugManager.INSTANCE.logThrottled(
                    "fishing",
                    "plan-retry-" + player.getUuid(),
                    2000L,
                    "spawn plan retry | attempts=" + used +
                            " | result=" + (plannedSpawnAction != null ? "found" : "missing") +
                            " | multiplier=" + scobbleboosters$format(multiplier) + "x"
            );
        } finally {
            scobbleboosters$extraPlanRunning = false;
        }
    }

    @Inject(method = "noBite", at = @At("HEAD"), cancellable = true, remap = false)
    private void scobbleboosters$suppressNoBite(
            ServerPlayerEntity player,
            CallbackInfo ci
    ) {
        if (!FishingBooster.suppressNoBiteMessage()) return;

        float multiplier = scobbleboosters$getMultiplier();
        if (multiplier <= 1.0F) return;

        DebugManager.INSTANCE.logThrottled(
                "fishing",
                "no-bite-" + player.getUuid(),
                2000L,
                "suppressed no-bite message | multiplier=" +
                        scobbleboosters$format(multiplier) + "x"
        );

        ci.cancel();
    }

    @Unique
    private String scobbleboosters$format(float value) {
        return value % 1.0F == 0.0F
                ? String.format(Locale.US, "%.0f", value)
                : String.format(Locale.US, "%.2f", value);
    }
}