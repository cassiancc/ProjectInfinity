package net.lerariemann.infinity.block.entity;

import com.mojang.datafixers.types.Type;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.lerariemann.infinity.InfinityMod;
import net.lerariemann.infinity.block.ModBlocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.function.Supplier;

import static net.lerariemann.infinity.PlatformMethods.unfreeze;

public class ModBlockEntities {

    public static Type<?> type(String id) {
        return Util.getChoiceType(TypeReferences.BLOCK_ENTITY, id);
    }
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(InfinityMod.MOD_ID, RegistryKeys.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockEntityType<NeitherPortalBlockEntity>> NEITHER_PORTAL = BLOCK_ENTITY_TYPES.register("neither_portal", () -> BlockEntityType.Builder.create(NeitherPortalBlockEntity::new, ModBlocks.NEITHER_PORTAL.get()).build(type("neither_portal")));
    public static final RegistrySupplier<BlockEntityType<TransfiniteAltarEntity>> ALTAR = BLOCK_ENTITY_TYPES.register("altar_block_entity", () -> BlockEntityType.Builder.create(TransfiniteAltarEntity::new, ModBlocks.ALTAR.get()).build(type("altar_block_entity")));
    public static final RegistrySupplier<BlockEntityType<CosmicAltarEntity>> ALTAR_COSMIC = BLOCK_ENTITY_TYPES.register("cosmic_block_entity", () -> BlockEntityType.Builder.create(CosmicAltarEntity::new, ModBlocks.ALTAR_COSMIC.get()).build(type("cosmic_block_entity")));


    public static void registerBlockEntities() {
        unfreeze(Registries.BLOCK_ENTITY_TYPE);
        BLOCK_ENTITY_TYPES.register();
    }
}