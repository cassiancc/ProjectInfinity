package net.lerariemann.infinity.fabric;

import dev.architectury.registry.registries.RegistrySupplier;
import me.basiqueevangelist.dynreg.util.RegistryUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.lerariemann.infinity.fluids.ModFluidsFabric;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
/**
 * See {@link net.lerariemann.infinity.PlatformMethods} for usages.
 */
@SuppressWarnings("unused")
public class PlatformMethodsImpl {

    public static PacketByteBuf createPacketByteBufs() {
        return PacketByteBufs.create();
    }

    public static void onWorldLoad(Object mixin, ServerWorld world) {
        ServerWorldEvents.LOAD.invoker().onWorldLoad((MinecraftServer) mixin, world);
    }

    public static void unfreeze(Registry<?> registry) {
        RegistryUtils.unfreeze(registry);

    }

    public static void freeze(Registry<?> registry) {
        registry.freeze();

    }

    public static void addAfter(RegistrySupplier<Item> blockItem, RegistryKey<ItemGroup> group, Item item) {
        ItemGroupEvents.modifyEntriesEvent(group).register(content -> content.addAfter(item, blockItem.get()));
    }


    public static RegistrySupplier<? extends FlowableFluid> getIridescenceStill() {
        return ModFluidsFabric.IRIDESCENCE_STILL;
    }

    public static RegistrySupplier<? extends FlowableFluid> getIridescenceFlowing() {
        return ModFluidsFabric.IRIDESCENCE_FLOWING;
    }

}
