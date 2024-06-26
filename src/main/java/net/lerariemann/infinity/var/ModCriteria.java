package net.lerariemann.infinity.var;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.lerariemann.infinity.InfinityMod;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.AdvancementEntityPredicateSerializer;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ModCriteria {
    public static class DimensionOpenedCriterion extends AbstractCriterion<ScoredConditions> {
        static final Identifier ID = InfinityMod.getId("dims_open");

        public DimensionOpenedCriterion() {
            super();
        }

        public Identifier getId() {
            return ID;
        }

        @Override
        public ScoredConditions conditionsFromJson(JsonObject jsonObject, LootContextPredicate lootContextPredicate, AdvancementEntityPredicateDeserializer advancementEntityPredicateDeserializer) {
            int score = jsonObject.getAsJsonPrimitive("amount").getAsInt();
            return new ScoredConditions(lootContextPredicate, score, getId());
        }

        public void trigger(ServerPlayerEntity player) {
            this.trigger(player, (conditions) -> conditions.test(player.getStatHandler().getStat(ModStats.DIMS_OPENED_STAT)));
        }
    }

    public static class DimensionClosedCriterion extends AbstractCriterion<ScoredConditions> {
        static final Identifier ID = InfinityMod.getId("dims_closed");

        public DimensionClosedCriterion() {
            super();
        }

        public Identifier getId() {
            return ID;
        }

        @Override
        public ScoredConditions conditionsFromJson(JsonObject jsonObject, LootContextPredicate lootContextPredicate, AdvancementEntityPredicateDeserializer advancementEntityPredicateDeserializer) {
            int score = jsonObject.getAsJsonPrimitive("amount").getAsInt();
            return new ScoredConditions(lootContextPredicate, score, getId());
        }

        public void trigger(ServerPlayerEntity player) {
            this.trigger(player, (conditions) -> conditions.test(player.getStatHandler().getStat(ModStats.WORLDS_DESTROYED_STAT)));
        }
    }


    public static class WhoRemainsCriterion extends AbstractCriterion<EmptyConditions> {
        static final Identifier ID = InfinityMod.getId("who_remains");

        public WhoRemainsCriterion() {
            super();
        }

        public Identifier getId() {
            return ID;
        }

        @Override
        public EmptyConditions conditionsFromJson(JsonObject jsonObject, LootContextPredicate lootContextPredicate, AdvancementEntityPredicateDeserializer advancementEntityPredicateDeserializer) {
            return new EmptyConditions(lootContextPredicate, ID);
        }

        public void trigger(ServerPlayerEntity player) {
            this.trigger(player, (conditions) -> true);
        }
    }

    public static class EmptyConditions extends AbstractCriterionConditions {

        public EmptyConditions(LootContextPredicate player, Identifier ID) {
            super(ID, player);
        }

        public static EmptyConditions create(Identifier ID) {
            return new EmptyConditions(LootContextPredicate.EMPTY, ID);
        }

        public JsonObject toJson(AdvancementEntityPredicateSerializer predicateSerializer) {
            return super.toJson(predicateSerializer);
        }
    }

    public static class ScoredConditions extends AbstractCriterionConditions {
        private final int score;

        public ScoredConditions(LootContextPredicate player, int score, Identifier ID) {
            super(ID, player);
            this.score = score;
        }

        public boolean test(int stat) {
            return stat >= this.score;
        }

        public static ScoredConditions create(int i, Identifier ID) {
            return new ScoredConditions(LootContextPredicate.EMPTY, i, ID);
        }

        public JsonObject toJson(AdvancementEntityPredicateSerializer predicateSerializer) {
            JsonObject jsonObject = super.toJson(predicateSerializer);
            jsonObject.add("amount", new JsonPrimitive(this.score));
            return jsonObject;
        }
    }

    public static DimensionOpenedCriterion DIMS_OPENED;
    public static DimensionClosedCriterion DIMS_CLOSED;
    public static WhoRemainsCriterion HE_WHO_REMAINS;

    public static void registerCriteria() {
        DIMS_OPENED = Criteria.register(new DimensionOpenedCriterion());
        DIMS_CLOSED = Criteria.register(new DimensionClosedCriterion());
        HE_WHO_REMAINS = Criteria.register(new WhoRemainsCriterion());
    }
}
