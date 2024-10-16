package net.lerariemann.infinity.mixin;

import com.google.common.collect.ImmutableList;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.lerariemann.infinity.InfinityMod;
import net.lerariemann.infinity.PlatformMethods;
import net.lerariemann.infinity.dimensions.RandomProvider;
import net.lerariemann.infinity.access.MinecraftServerAccess;
import net.minecraft.network.QueryableServer;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.world.ChunkErrorHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.random.RandomSequencesState;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.border.WorldBorderListener;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.spawner.SpecialSpawner;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin extends ReentrantThreadExecutor<ServerTask> implements QueryableServer, ChunkErrorHandler, CommandOutput, AutoCloseable, MinecraftServerAccess {
    @Final @Shadow
    private Map<RegistryKey<World>, ServerWorld> worlds;
    @Final @Shadow
    private Executor workerExecutor;
    @Final @Shadow
    protected LevelStorage.Session session;
    @Final @Shadow
    protected SaveProperties saveProperties;
    @Final @Shadow
    private WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory;

    public MinecraftServerMixin(String string) {
        super(string);
    }

    @Shadow
    public ServerWorld getWorld(RegistryKey<World> key) {
        return null;
    }
    @Shadow
    protected ServerTask createTask(Runnable runnable) {
        return null;
    }
    @Shadow
    public Path getSavePath(WorldSavePath worldSavePath) {
        return null;
    }

    @Shadow public abstract DynamicRegistryManager.Immutable getRegistryManager();

    @Unique
    public Map<RegistryKey<World>, ServerWorld> worldsToAdd;
    @Unique
    public RandomProvider dimensionProvider;
    @Unique
    public boolean needsInvocation;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void injected(CallbackInfo info) {
        worldsToAdd = new HashMap<>();
        needsInvocation = !Files.exists(InfinityMod.invocationLock);
        LogManager.getLogger().info("Invocation {}", needsInvocation ? "needed..." : "not needed");
        projectInfinity$setDimensionProvider();
    }
    @Override
    public boolean projectInfinity$needsInvocation() {return needsInvocation;}
    @Override
    public void projectInfinity$onInvocation() {
        needsInvocation = false;
        try {
            Path p = InfinityMod.invocationLock;
            if (!Files.exists(p)) {
                Files.createDirectories(p.getParent());
                Files.writeString(p, "Delete this file to regenerate modular configs automatically\n" +
                        "(e.g. you may want to do this when adding new mods to the instance)");
            }
            LogManager.getLogger().info("Invocation complete");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public RandomProvider projectInfinity$getDimensionProvider() {
        return dimensionProvider;
    }

    @Override
    public void projectInfinity$setDimensionProvider() {
        RandomProvider p = new RandomProvider("config/" + InfinityMod.MOD_ID + "/",
                getSavePath(WorldSavePath.DATAPACKS).toString() + "/" + InfinityMod.MOD_ID);
        p.kickGhostsOut(getRegistryManager());
        dimensionProvider = p;
    }

    @Override
    public void projectInfinity$addWorld(RegistryKey<World> key, DimensionOptions options) {
        ServerWorldProperties serverWorldProperties = saveProperties.getMainWorldProperties();
        ServerWorld world = new ServerWorld(((MinecraftServer) (Object) this), workerExecutor, session, serverWorldProperties,
                key, options, worldGenerationProgressListenerFactory.create(11), saveProperties.isDebugWorld(),
                BiomeAccess.hashSeed(saveProperties.getGeneratorOptions().getSeed()), ImmutableList.of(), false, getWorld(World.OVERWORLD).getRandomSequences());
        getWorld(World.OVERWORLD).getWorldBorder().addListener(new WorldBorderListener.WorldBorderSyncer(world.getWorldBorder()));
        worldsToAdd.put(key, world);
        send(createTask(() -> {
            worlds.put(key, world);
            worldsToAdd.clear();
        }));
        PlatformMethods.onWorldLoad(this, world);
    }


    @Override
    public boolean projectInfinity$hasToAdd(RegistryKey<World> key) {
        return (worldsToAdd.containsKey(key));
    }

    @Redirect(method="createWorlds", at=@At(value="NEW", target="(Lnet/minecraft/server/MinecraftServer;Ljava/util/concurrent/Executor;Lnet/minecraft/world/level/storage/LevelStorage$Session;Lnet/minecraft/world/level/ServerWorldProperties;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/world/dimension/DimensionOptions;Lnet/minecraft/server/WorldGenerationProgressListener;ZJLjava/util/List;ZLnet/minecraft/util/math/random/RandomSequencesState;)Lnet/minecraft/server/world/ServerWorld;"))
    public ServerWorld create(MinecraftServer server, Executor workerExecutor, LevelStorage.Session session, ServerWorldProperties properties, RegistryKey<World> worldKey, DimensionOptions dimensionOptions, WorldGenerationProgressListener worldGenerationProgressListener, boolean debugWorld, long seed, List<SpecialSpawner> spawners, boolean shouldTickTime, RandomSequencesState randomSequencesState) {
        ServerWorldProperties prop = (worldKey.getValue().toString().contains("infinity")) ? saveProperties.getMainWorldProperties() : properties;
        return new ServerWorld(server, workerExecutor, session, prop, worldKey, dimensionOptions, worldGenerationProgressListener, debugWorld, seed, spawners, shouldTickTime, randomSequencesState);
    }
}
