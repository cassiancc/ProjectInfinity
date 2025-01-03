package net.lerariemann.infinity.fluids.fabric;

import dev.architectury.core.fluid.ArchitecturyFlowingFluid;
import dev.architectury.core.fluid.ArchitecturyFluidAttributes;
import dev.architectury.core.fluid.SimpleArchitecturyFluidAttributes;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.lerariemann.infinity.registry.core.ModBlocks;
import net.lerariemann.infinity.iridescence.Iridescence;
import net.lerariemann.infinity.registry.core.ModItems;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;

import static net.lerariemann.infinity.InfinityMod.MOD_ID;

public class ModFluidsFabric {
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(MOD_ID, RegistryKeys.FLUID);
    public static final SimpleArchitecturyFluidAttributes iridescence_attributes =
            SimpleArchitecturyFluidAttributes.ofSupplier(() -> ModFluidsFabric.IRIDESCENCE_FLOWING, () -> ModFluidsFabric.IRIDESCENCE_STILL)
                    .blockSupplier(() -> ModBlocks.IRIDESCENCE)
                    .bucketItemSupplier(() -> ModItems.IRIDESCENCE_BUCKET)
                    .sourceTexture(Iridescence.TEXTURE)
                    .flowingTexture(Iridescence.FLOWING_TEXTURE)
                    .overlayTexture(Iridescence.OVERLAY_TEXTURE);
    public static final RegistrySupplier<ArchitecturyFlowingFluid> IRIDESCENCE_FLOWING =
            FLUIDS.register("flowing_iridescence", () -> new IridescenceFlowing(iridescence_attributes));
    public static final RegistrySupplier<ArchitecturyFlowingFluid> IRIDESCENCE_STILL =
            FLUIDS.register("iridescence", () -> new IridescenceStill(iridescence_attributes));

    public static void registerModFluids() {
        FLUIDS.register();
    }

    public static class IridescenceStill extends ArchitecturyFlowingFluid.Source {
        public IridescenceStill(ArchitecturyFluidAttributes attributes) {
            super(attributes);
        }

        @Override
        protected boolean isInfinite(World world) {
            return Iridescence.isInfinite(world);
        }
    }

    public static class IridescenceFlowing extends ArchitecturyFlowingFluid.Flowing {
        public IridescenceFlowing(ArchitecturyFluidAttributes attributes) {
            super(attributes);
        }

        @Override
        protected boolean isInfinite(World world) {
            return Iridescence.isInfinite(world);
        }
    }
}
