package net.lerariemann.infinity.var;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.registry.registries.DeferredRegister;
import net.lerariemann.infinity.InfinityMod;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.Criterion;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

import static net.lerariemann.infinity.InfinityMod.MOD_ID;
import static net.lerariemann.infinity.PlatformMethods.freeze;
import static net.lerariemann.infinity.PlatformMethods.unfreeze;

public class ModCriteria {
    public static class DimensionOpenedCriterion extends AbstractCriterion<ScoredConditions> {
        @Override
        public Codec<ScoredConditions> getConditionsCodec() {
            return ScoredConditions.CODEC;
        }

        public void trigger(ServerPlayerEntity player) {
            this.trigger(player, (conditions) -> conditions.test(player.getStatHandler().getStat(ModStats.DIMS_OPENED_STAT)));
        }
    }

    public static class DimensionClosedCriterion extends AbstractCriterion<ScoredConditions> {
        @Override
        public Codec<ScoredConditions> getConditionsCodec() {
            return ScoredConditions.CODEC;
        }

        public void trigger(ServerPlayerEntity player) {
            this.trigger(player, (conditions) -> conditions.test(player.getStatHandler().getStat(ModStats.WORLDS_DESTROYED_STAT)));
        }
    }

    public static class WhoRemainsCriterion extends AbstractCriterion<EmptyConditions> {
        public void trigger(ServerPlayerEntity player) {
            this.trigger(player, (conditions) -> true);
        }

        @Override
        public Codec<EmptyConditions> getConditionsCodec() {
            return EmptyConditions.CODEC;
        }
    }

    public record EmptyConditions(Optional<LootContextPredicate> player) implements AbstractCriterion.Conditions {
        public static final Codec<EmptyConditions> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                                EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(EmptyConditions::player)
                        )
                        .apply(instance, EmptyConditions::new)
        );
    }

    public record ScoredConditions(Optional<LootContextPredicate> player, int score) implements AbstractCriterion.Conditions {
        public static final Codec<ScoredConditions> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                                EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(ScoredConditions::player),
                                Codec.INT.fieldOf("amount").forGetter(ScoredConditions::score)
                        )
                        .apply(instance, ScoredConditions::new)
        );

        public boolean test(int stat) {
            return stat >= this.score;
        }
    }

    public static DimensionOpenedCriterion DIMS_OPENED;
    public static DimensionClosedCriterion DIMS_CLOSED;
    public static WhoRemainsCriterion WHO_REMAINS;

    public static final DeferredRegister<Criterion<?>> CRITERIONS = DeferredRegister.create(MOD_ID, RegistryKeys.CRITERION);

    public static void registerCriteria() {
        CRITERIONS.register("dims_open", DimensionOpenedCriterion::new);
        CRITERIONS.register("dims_closed", DimensionClosedCriterion::new);
        CRITERIONS.register("who_remains", WhoRemainsCriterion::new);
    }
}
