package net.lerariemann.infinity.mixin;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.lerariemann.infinity.InfinityMod;
import net.lerariemann.infinity.access.Timebombable;
import net.lerariemann.infinity.access.ServerPlayerEntityAccess;
import net.lerariemann.infinity.options.PacketTransiever;
import net.lerariemann.infinity.util.WarpLogic;
import net.lerariemann.infinity.var.ModCommands;
import net.lerariemann.infinity.var.ModCriteria;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
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
    @Shadow @Final public MinecraftServer server;
    @Unique private long infinity$ticksUntilWarp;
    @Unique private Identifier infinity$idForWarp;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if (--this.infinity$ticksUntilWarp == 0L) {
            MinecraftServer s = this.getServerWorld().getServer();
            ServerWorld w = s.getWorld(RegistryKey.of(RegistryKeys.WORLD, this.infinity$idForWarp));
            if (w==null) return;
            double d = DimensionType.getCoordinateScaleFactor(this.getServerWorld().getDimension(), w.getDimension());
            Entity self = getCameraEntity();
            double y = MathHelper.clamp(self.getY(), w.getBottomY(), w.getTopY());
            BlockPos blockPos2 = WarpLogic.getPosForWarp(w.getWorldBorder().clamp(self.getX() * d, y, self.getZ() * d), w);
            this.teleport(w, blockPos2.getX(), blockPos2.getY(), blockPos2.getZ(), new HashSet<>(), self.getYaw(), self.getPitch());
        }
        int i = ((Timebombable)(getServerWorld())).projectInfinity$isTimebombed();
        if (i > 200) {
            if (i%4 == 0) {
                Registry<DamageType> r = getServerWorld().getServer().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE);
                RegistryEntry<DamageType> entry = r.getEntry(r.get(InfinityMod.getId("world_ceased")));
                damage(new DamageSource(entry), i > 400 ? 2.0f : 1.0f);
            }
            if (i > 3500) ModCriteria.WHO_REMAINS.trigger((ServerPlayerEntity)(Object)this);
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
        if (cir.getReturnValue()) ServerPlayNetworking.send(((ServerPlayerEntity)(Object)this), InfinityMod.SHADER_RELOAD, PacketTransiever.buildPacket(this.getServerWorld()));
    }

    @Inject(method= "teleport(Lnet/minecraft/server/world/ServerWorld;DDDFF)V", at = @At(value="INVOKE", target ="Lnet/minecraft/server/PlayerManager;sendCommandTree(Lnet/minecraft/server/network/ServerPlayerEntity;)V"))
    private void injected5(ServerWorld targetWorld, double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
        ServerPlayNetworking.send(((ServerPlayerEntity)(Object)this), InfinityMod.SHADER_RELOAD, PacketTransiever.buildPacket(targetWorld));
        ServerPlayNetworking.send(((ServerPlayerEntity)(Object)this), InfinityMod.STARS_RELOAD, PacketByteBufs.create());
    }


    @Override
    public void projectInfinity$setWarpTimer(long ticks, Identifier dim) {
        this.infinity$ticksUntilWarp = ticks;
        this.infinity$idForWarp = dim;
    }
}
