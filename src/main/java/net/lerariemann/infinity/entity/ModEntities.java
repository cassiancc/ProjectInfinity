package net.lerariemann.infinity.entity;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.lerariemann.infinity.InfinityMod;
import net.lerariemann.infinity.entity.custom.*;
import net.lerariemann.infinity.entity.client.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static <T extends Entity> EntityType<T> register(String id, FabricEntityTypeBuilder<T> type) {
        return Registry.register(Registries.ENTITY_TYPE, new Identifier(InfinityMod.MOD_ID, id), type.build());
    }
    public static final EntityType<DimensionalSlime> DIMENSIONAL_SLIME = register("dimensional_slime",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DimensionalSlime::new)
                    .dimensions(EntityDimensions.fixed(2.04f, 2.04f)).trackRangeChunks(10));
    public static final EntityType<DimensionalSkeleton> DIMENSIONAL_SKELETON = register("dimensional_skeleton",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DimensionalSkeleton::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.99f)).trackRangeChunks(8));
    public static final EntityType<DimensionalCreeper> DIMENSIONAL_CREEPER = register("dimensional_creeper",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DimensionalCreeper::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.7f)).trackRangeChunks(8));

    public static void registerEntities() {
        FabricDefaultAttributeRegistry.register(DIMENSIONAL_SLIME, DimensionalSlime.createAttributes());
        FabricDefaultAttributeRegistry.register(DIMENSIONAL_SKELETON, AbstractSkeletonEntity.createAbstractSkeletonAttributes());
        FabricDefaultAttributeRegistry.register(DIMENSIONAL_CREEPER, CreeperEntity.createCreeperAttributes());
    }

    public static void registerEntityRenderers() {
        EntityRendererRegistry.register(DIMENSIONAL_SLIME, DimensionalSlimeRenderer::new);
        EntityRendererRegistry.register(DIMENSIONAL_SKELETON, DimensionalSkeletonRenderer::new);
        EntityRendererRegistry.register(DIMENSIONAL_CREEPER, DimensionalCreeperRenderer::new);
    }
}
