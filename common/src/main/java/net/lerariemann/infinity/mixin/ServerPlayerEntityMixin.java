package net.lerariemann.infinity.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import net.lerariemann.infinity.InfinityMod;
import net.lerariemann.infinity.access.Timebombable;
import net.lerariemann.infinity.access.ServerPlayerEntityAccess;
import net.lerariemann.infinity.options.InfinityOptions;
import net.lerariemann.infinity.util.InfinityMethods;
import net.lerariemann.infinity.util.PortalCreationLogic;
import net.lerariemann.infinity.util.WarpLogic;
import net.lerariemann.infinity.var.ModCriteria;
import net.lerariemann.infinity.var.ModPayloads;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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

    @Shadow public abstract @Nullable Entity teleportTo(TeleportTarget teleportTarget);

    @Unique private long infinity$ticksUntilWarp;
    @Unique private Identifier infinity$idForWarp;


    @Inject(method="findRespawnPosition", at = @At("HEAD"), cancellable = true)
    private static void injected(ServerWorld world, BlockPos pos, float angle, boolean forced, boolean alive, CallbackInfoReturnable<Optional<Vec3d>> cir) {
        if (((Timebombable)world).infinity$isTimebombed()) cir.setReturnValue(Optional.empty());
    }

    /* When the player is using the Infinity portal, this modifies the portal on the other side if needed. */
    @Inject(method = "teleportTo",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;setServerWorld(Lnet/minecraft/server/world/ServerWorld;)V")
    )
    private void injected2(TeleportTarget teleportTarget, CallbackInfoReturnable<Entity> cir, @Local(ordinal = 0) ServerWorld serverWorld, @Local RegistryKey<World> registryKey) {
        if (InfinityMod.provider.rule("returnPortalsEnabled") &&
                (registryKey.getValue().getNamespace().equals(InfinityMod.MOD_ID))) {
            BlockPos pos = BlockPos.ofFloored(teleportTarget.pos());
            ServerWorld destination = teleportTarget.world();
            for (BlockPos pos2: new BlockPos[] {pos, pos.add(1, 0, 0), pos.add(0, 0, 1),
                    pos.add(-1, 0, 0), pos.add(0, 0, -1)}) if (destination.getBlockState(pos2).isOf(Blocks.NETHER_PORTAL)) {
                Identifier dimensionName = registryKey.getValue();

                PortalCreationLogic.modifyPortalRecursive(destination, pos2, dimensionName, true);
                break;
            }
        }
    }

    @Inject(method = "teleportTo",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getPlayerManager()Lnet/minecraft/server/PlayerManager;"))
    private void injected3(TeleportTarget teleportTarget, CallbackInfoReturnable<Entity> cir) {
        InfinityMethods.sendS2CPayload(((ServerPlayerEntity)(Object)this), ModPayloads.setShaderFromWorld(teleportTarget.world()));
        InfinityMethods.sendS2CPayload(((ServerPlayerEntity)(Object)this), ModPayloads.StarsRePayLoad.INSTANCE);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        /* Handle infinity options */
        InfinityOptions opt = InfinityOptions.access(getWorld());
        if (!opt.effect.isEmpty()) {
            if (getWorld().getTime() % opt.effect.cooldown() == 0) {
                addStatusEffect(new StatusEffectInstance(opt.effect.id(), opt.effect.duration(), opt.effect.amplifier()));
            }
        }

        /* Handle the warp command */
        if (--this.infinity$ticksUntilWarp == 0L) {
            MinecraftServer s = this.getServerWorld().getServer();
            ServerWorld w = s.getWorld(RegistryKey.of(RegistryKeys.WORLD, this.infinity$idForWarp));
            if (w==null) return;
            double d = DimensionType.getCoordinateScaleFactor(this.getServerWorld().getDimension(), w.getDimension());
            Entity self = getCameraEntity();
            double y = MathHelper.clamp(self.getY(), w.getBottomY(), w.getTopY());
            BlockPos blockPos2 = WarpLogic.getPosForWarp(w.getWorldBorder().clamp(self.getX() * d, y, self.getZ() * d), w);
            BlockState state = w.getBlockState(blockPos2.down());
            if (state.isAir() || state.isOf(Blocks.LAVA)) w.setBlockState(blockPos2.down(), Blocks.OBSIDIAN.getDefaultState());
            this.teleport(w, blockPos2.getX() + 0.5, blockPos2.getY(), blockPos2.getZ() + 0.5, new HashSet<>(), self.getYaw(), self.getPitch());
        }

        /* Handle effects from dimension deletion */
        int i = ((Timebombable)(getServerWorld())).infinity$getTimebombProgress();
        if (i > 3540) {
            WarpLogic.respawnAlive((ServerPlayerEntity)(Object)this);
        }
        else if (i > 3500) {
            ModCriteria.WHO_REMAINS.get().trigger((ServerPlayerEntity)(Object)this);
        }
        else if (i > 200) {
            if (i%4 == 0) {
                Registry<DamageType> r = getServerWorld().getServer().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE);
                RegistryEntry<DamageType> entry = r.getEntry(r.get(InfinityMethods.getId("world_ceased")));
                damage(new DamageSource(entry), i > 400 ? 2.0f : 1.0f);
            }
        }
    }

    @Inject(method = "changeGameMode", at = @At("RETURN"))
    private void injected4(GameMode gameMode, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) InfinityMethods.sendS2CPayload(((ServerPlayerEntity)(Object)this), ModPayloads.setShaderFromWorld(this.getServerWorld()));
    }

    @Override
    public void infinity$setWarpTimer(long ticks, Identifier dim) {
        this.infinity$ticksUntilWarp = ticks;
        this.infinity$idForWarp = dim;
    }
}
