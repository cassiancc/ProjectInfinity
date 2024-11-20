package net.lerariemann.infinity.var;

import dev.architectury.platform.Platform;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.lerariemann.infinity.InfinityMod;
import net.lerariemann.infinity.access.InfinityOptionsAccess;
import net.lerariemann.infinity.access.WorldRendererAccess;
import net.lerariemann.infinity.loading.DimensionGrabber;
import net.lerariemann.infinity.options.InfinityOptions;
import net.lerariemann.infinity.options.ShaderLoader;
import net.lerariemann.infinity.util.CommonIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.Objects;

public class ModPayloads {

    public static MinecraftClient client(Object context) {
        ClientPlayNetworking.Context clientContext = (ClientPlayNetworking.Context) context;
        return clientContext.client();
    }

    public record WorldAddPayload(Identifier world_id, NbtCompound world_data) implements CustomPayload {
        public static final CustomPayload.Id<WorldAddPayload> ID = new CustomPayload.Id<>(InfinityMod.getId("add_world"));
        public static final PacketCodec<RegistryByteBuf, WorldAddPayload> CODEC = PacketCodec.tuple(
                Identifier.PACKET_CODEC, WorldAddPayload::world_id,
                PacketCodecs.NBT_COMPOUND, WorldAddPayload::world_data,
                WorldAddPayload::new);
        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
    public static void addWorld(WorldAddPayload payload, Object context) {
        client(context).execute(() ->
                (new DimensionGrabber(Objects.requireNonNull(client(context).getNetworkHandler()).getRegistryManager()))
                        .grab_dim_for_client(payload.world_id, payload.world_data));
    }

    public record BiomeAddPayload(Identifier biome_id, NbtCompound biome_data) implements CustomPayload {
        public static final CustomPayload.Id<BiomeAddPayload> ID = new CustomPayload.Id<>(InfinityMod.getId("add_biome"));
        public static final PacketCodec<RegistryByteBuf, BiomeAddPayload> CODEC = PacketCodec.tuple(
                Identifier.PACKET_CODEC, BiomeAddPayload::biome_id,
                PacketCodecs.NBT_COMPOUND, BiomeAddPayload::biome_data,
                BiomeAddPayload::new);
        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
    public static void addBiome(BiomeAddPayload payload, Object context) {
        client(context).execute(() ->
                (new DimensionGrabber(Objects.requireNonNull(client(context).getNetworkHandler()).getRegistryManager()))
                        .grab_biome_for_client(payload.biome_id, payload.biome_data));
    }

    public record ShaderRePayload(NbtCompound shader_data) implements CustomPayload {
        public static final CustomPayload.Id<ShaderRePayload> ID = new CustomPayload.Id<>(InfinityMod.getId("reload_shader"));
        public static final PacketCodec<RegistryByteBuf, ShaderRePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.NBT_COMPOUND, ShaderRePayload::shader_data,
                ShaderRePayload::new);
        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
    public static void receiveShader(ShaderRePayload payload, Object context) {
        InfinityOptions options = new InfinityOptions(payload.shader_data);
        MinecraftClient client = client(context);
        ((InfinityOptionsAccess)client).infinity$setOptions(options);
        NbtCompound shader = options.getShader();
        boolean bl = shader.isEmpty();
        if (bl) client.execute(() -> ShaderLoader.reloadShaders(client, false));
        else {
            client.execute(() -> {
                CommonIO.write(shader, ShaderLoader.shaderDir(client), ShaderLoader.FILENAME);
                ShaderLoader.reloadShaders(client, true);
                if (!resourcesReloaded) {
                    client.reloadResources();
                    resourcesReloaded = true;
                }
            });
        }
    }
    
    public static boolean resourcesReloaded = Path.of(Platform.getGameFolder() + "/resourcepacks/infinity/assets/infinity/shaders").toFile().exists();

    public record StarsRePayLoad() implements CustomPayload {
        public static final StarsRePayLoad INSTANCE = new StarsRePayLoad();
        public static final CustomPayload.Id<StarsRePayLoad> ID = new CustomPayload.Id<>(InfinityMod.getId("reload_stars"));
        public static final PacketCodec<RegistryByteBuf, StarsRePayLoad> CODEC = PacketCodec.unit(INSTANCE);
        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
    public static void receiveStars(StarsRePayLoad payload, Object context) {
        ((WorldRendererAccess)(client(context).worldRenderer)).projectInfinity$setNeedsStars(true);
    }


    public record RespawnAlivePayload() implements CustomPayload {
        public static final RespawnAlivePayload INSTANCE = new RespawnAlivePayload();
        public static final CustomPayload.Id<RespawnAlivePayload> ID = new CustomPayload.Id<>(InfinityMod.getId("respawn_alive"));
        public static final PacketCodec<RegistryByteBuf, RespawnAlivePayload> CODEC = PacketCodec.unit(INSTANCE);
        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
    public static void respawnAliveClient(RespawnAlivePayload payload, Object context) {
        client(context).player.networkHandler.sendPacket(new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.PERFORM_RESPAWN));
    }

    public static ShaderRePayload setShaderFromWorld(ServerWorld destination) {
        return new ShaderRePayload(((InfinityOptionsAccess)(destination)).infinity$getOptions().data());
    }

    public static void registerPayloadsServer() {
        PayloadTypeRegistry.playS2C().register(WorldAddPayload.ID, WorldAddPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BiomeAddPayload.ID, BiomeAddPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ShaderRePayload.ID, ShaderRePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StarsRePayLoad.ID, StarsRePayLoad.CODEC);
        PayloadTypeRegistry.playS2C().register(RespawnAlivePayload.ID, RespawnAlivePayload.CODEC);

    }

    public static void registerPayloadsClient() {
        ClientPlayNetworking.registerGlobalReceiver(ModPayloads.WorldAddPayload.ID, ModPayloads::addWorld);
        ClientPlayNetworking.registerGlobalReceiver(ModPayloads.BiomeAddPayload.ID, ModPayloads::addBiome);
        ClientPlayNetworking.registerGlobalReceiver(ModPayloads.ShaderRePayload.ID, ModPayloads::receiveShader);
        ClientPlayNetworking.registerGlobalReceiver(ModPayloads.StarsRePayLoad.ID, ModPayloads::receiveStars);
        ClientPlayNetworking.registerGlobalReceiver(ModPayloads.RespawnAlivePayload.ID, ModPayloads::respawnAliveClient);
    }
}
