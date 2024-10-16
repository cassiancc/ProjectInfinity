package net.lerariemann.infinity.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import net.lerariemann.infinity.InfinityMod;
import net.lerariemann.infinity.PlatformMethods;
import net.lerariemann.infinity.access.Timebombable;
import net.lerariemann.infinity.block.custom.NeitherPortalBlock;
import net.lerariemann.infinity.access.MinecraftServerAccess;
import net.lerariemann.infinity.access.ServerPlayerEntityAccess;
import net.lerariemann.infinity.var.ModCommands;
import net.lerariemann.infinity.var.ModCriteria;
import net.lerariemann.infinity.var.ModPayloads;
import net.lerariemann.infinity.var.ModStats;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements ServerPlayerEntityAccess {
    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Shadow public abstract ServerWorld getServerWorld();

    @Shadow public abstract boolean teleport(ServerWorld world, double destX, double destY, double destZ, Set<PositionFlag> flags, float yaw, float pitch);

    @Shadow public abstract Entity getCameraEntity();

    @Shadow public abstract boolean damage(DamageSource source, float amount);
    @Shadow public boolean notInAnyWorld;
    @Shadow public ServerPlayNetworkHandler networkHandler;

    @Shadow public abstract @Nullable Entity teleportTo(TeleportTarget teleportTarget);

    @Unique private long ticksUntilWarp;
    @Unique private long idForWarp;


    @Inject(method="findRespawnPosition", at = @At("HEAD"), cancellable = true)
    private static void injected(ServerWorld world, BlockPos pos, float angle, boolean forced, boolean alive, CallbackInfoReturnable<Optional<Vec3d>> cir) {
        if (((Timebombable)world).projectInfinity$isTimebobmed() > 0) cir.setReturnValue(Optional.empty());
    }

    @Inject(method = "teleportTo",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;setServerWorld(Lnet/minecraft/server/world/ServerWorld;)V")
    )
    private void injected2(TeleportTarget teleportTarget, CallbackInfoReturnable<Entity> cir, @Local(ordinal = 0) ServerWorld serverWorld, @Local RegistryKey<World> registryKey) {
        if (((MinecraftServerAccess)(serverWorld.getServer())).projectInfinity$getDimensionProvider().rule("returnPortalsEnabled") &&
                (registryKey.getValue().getNamespace().equals(InfinityMod.MOD_ID))) {
            BlockPos pos = BlockPos.ofFloored(teleportTarget.pos());
            ServerWorld destination = teleportTarget.world();
            boolean bl = false;
            for (BlockPos pos2: new BlockPos[] {pos, pos.add(1, 0, 0), pos.add(0, 0, 1),
                    pos.add(-1, 0, 0), pos.add(0, 0, -1)}) if (destination.getBlockState(pos2).isOf(Blocks.NETHER_PORTAL)) {
                bl = true;
                String keystr = registryKey.getValue().getPath();
                String is = keystr.substring(keystr.lastIndexOf("_") + 1);
                long i;
                try {
                    i = Long.parseLong(is);
                } catch (Exception e) {
                    i = ModCommands.getDimensionSeed(is, serverWorld.getServer());
                }
                NeitherPortalBlock.modifyPortal(destination, pos2, destination.getBlockState(pos), i, true);
                break;
            }
            if (bl) {
                this.increaseStat(ModStats.PORTALS_OPENED_STAT, 1);
            }
        }
    }

    @Inject(method = "teleportTo",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getPlayerManager()Lnet/minecraft/server/PlayerManager;"))
    private void injected3(TeleportTarget teleportTarget, CallbackInfoReturnable<Entity> cir) {
        PlatformMethods.sendServerPlayerEntity(((ServerPlayerEntity)(Object)this), ModPayloads.setShaderFromWorld(teleportTarget.world()));
        PlatformMethods.sendServerPlayerEntity(((ServerPlayerEntity)(Object)this), ModPayloads.StarsRePayLoad.INSTANCE);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if (--this.ticksUntilWarp == 0L) {
            MinecraftServer s = this.getServerWorld().getServer();
            ServerWorld w = s.getWorld(ModCommands.getKey(this.idForWarp, s));
            if (w==null) return;
            double d = DimensionType.getCoordinateScaleFactor(this.getServerWorld().getDimension(), w.getDimension());
            Entity self = getCameraEntity();
            BlockPos blockPos2 = w.getWorldBorder().clamp(self.getX() * d, self.getY(), self.getZ() * d);
            this.teleport(w, blockPos2.getX(), blockPos2.getY(), blockPos2.getZ(), new HashSet<>(), self.getYaw(), self.getPitch());
        }
        int i = ((Timebombable)(getServerWorld())).projectInfinity$isTimebobmed();
        if (i > 200) {
            if (i%4 == 0) {
                Registry<DamageType> r = getServerWorld().getServer().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE);
                RegistryEntry<DamageType> entry = r.getEntry(r.get(InfinityMod.getId("world_ceased")));
                damage(new DamageSource(entry), i > 400 ? 2.0f : 1.0f);
            }
            if (i > 3500) {
                ModCriteria.WHO_REMAINS.get().trigger((ServerPlayerEntity)(Object)this);
            }
            if (i > 3540) {
                this.detach();
                this.getServerWorld().removePlayer((ServerPlayerEntity)(Object)this, Entity.RemovalReason.CHANGED_DIMENSION);
                if (!this.notInAnyWorld) {
                    this.notInAnyWorld = true;
                    this.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.GAME_WON, 0));
                }
            }
        }
    }

    @Inject(method = "changeGameMode", at = @At("RETURN"))
    private void injected4(GameMode gameMode, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) PlatformMethods.sendServerPlayerEntity(((ServerPlayerEntity)(Object)this), ModPayloads.setShaderFromWorld(this.getServerWorld()));
    }

    @Override
    public void projectInfinity$setWarpTimer(long ticks, long dim) {
        this.ticksUntilWarp = ticks;
        this.idForWarp = dim;
    }
}
