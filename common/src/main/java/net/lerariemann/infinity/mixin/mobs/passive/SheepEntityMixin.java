package net.lerariemann.infinity.mixin.mobs.passive;

import net.lerariemann.infinity.access.SpawnableInterface;
import net.lerariemann.infinity.mixin.mobs.LivingEntityMixin;
import net.lerariemann.infinity.util.InfinityMethods;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.DyeColor;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SheepEntity.class)
public abstract class SheepEntityMixin extends LivingEntityMixin implements Shearable {
    protected SheepEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void injected_sheep(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue() && !this.isSheared() && source.getAttacker() != null &&
                source.getAttacker() instanceof PlayerEntity && getWorld().getRegistryKey().getValue().toString().contains("infinity:classic")) {
            this.sheared(SoundCategory.AMBIENT);
        }
    }

    @Shadow public abstract void setColor(DyeColor color);

    @Shadow public abstract void sheared(SoundCategory shearedSoundCategory);

    @Shadow public abstract boolean isSheared();

    @Inject(method = "initialize", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/SheepEntity;setColor(Lnet/minecraft/util/DyeColor;)V", shift = At.Shift.AFTER))
    private void injected(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, EntityData entityData, CallbackInfoReturnable<EntityData> cir) {
        if (world.getBiome(getBlockPos()).matchesId(InfinityMethods.getId("classic"))) {
            setColor(DyeColor.WHITE);
        }
        else if (SpawnableInterface.isBiomeInfinity(world, getBlockPos())) {
            setColor(DyeColor.byId(world.getRandom().nextInt(16)));
        }
    }
}
