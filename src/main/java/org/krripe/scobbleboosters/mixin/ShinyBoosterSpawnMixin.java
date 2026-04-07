package org.krripe.scobbleboosters.mixin;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import org.krripe.scobbleboosters.boosters.BoosterService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

@Mixin(ServerWorld.class)
public class ShinyBoosterSpawnMixin {

    private static volatile boolean REFLECT_INIT = false;
    private static Method GET_SHINY = null;
    private static Method SET_SHINY = null;

    @Unique
    private static void initReflection(Pokemon pokemon) {
        if (REFLECT_INIT) return;
        REFLECT_INIT = true;

        Class<?> cls = pokemon.getClass();

        try { GET_SHINY = cls.getMethod("getShiny"); }
        catch (NoSuchMethodException ignored) {
            try { GET_SHINY = cls.getMethod("isShiny"); }
            catch (NoSuchMethodException ignored2) { GET_SHINY = null; }
        }

        try { SET_SHINY = cls.getMethod("setShiny", boolean.class); }
        catch (NoSuchMethodException ignored) { SET_SHINY = null; }
    }

    @Unique
    private static boolean isShiny(Pokemon pokemon) {
        if (GET_SHINY == null) return false;
        try {
            Object v = GET_SHINY.invoke(pokemon);
            return v instanceof Boolean && (Boolean) v;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Unique
    private static void setShiny(Pokemon pokemon) {
        if (SET_SHINY == null) return;
        try { SET_SHINY.invoke(pokemon, true); }
        catch (Throwable ignored) {}
    }

    @Inject(method = "spawnEntity", at = @At("TAIL"))
    private void scobbleboosters$applyShinyBooster(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!cir.getReturnValueZ()) return;
            if (!(entity instanceof PokemonEntity pokemonEntity)) return;

            Pokemon pokemon = pokemonEntity.getPokemon();
            if (!pokemon.isWild()) return;

            float mult = BoosterService.INSTANCE.getMultiplier("shiny");
            if (mult <= 1.0f) return;

            initReflection(pokemon);
            if (isShiny(pokemon)) return;

            double baseRate = Math.max(Cobblemon.INSTANCE.getConfig().getShinyRate(), 1.0);
            double denom = Math.max(baseRate / (double) mult, 1.0);
            double chance = 1.0 / denom;

            if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() < chance) {
                setShiny(pokemon);
            }
        } catch (Throwable ignored) {
        }
    }
}