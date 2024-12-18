package net.lerariemann.infinity.mixin;

import net.lerariemann.infinity.block.custom.InfinityPortalBlock;
import net.lerariemann.infinity.util.InfinityMethods;
import net.lerariemann.infinity.util.PortalCreationLogic;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockMixin {
	@Inject(at = @At("HEAD"), method = "onEntityCollision(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)V")
	private void injected(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo info) {
		if (world instanceof ServerWorld w && entity instanceof ItemEntity e) {
			PortalCreationLogic.tryCreatePortalFromItem(w, pos, e);
		}
	}

	@Inject(method = "createTeleportTarget", at = @At(value = "HEAD"), cancellable = true)
	private void injected(ServerWorld world, Entity entity, BlockPos pos, CallbackInfoReturnable<TeleportTarget> cir) {
		if (InfinityMethods.isInfinity(world)) {
			PortalCreationLogic.modifyPortalRecursive(world, pos, World.OVERWORLD.getValue(), true);
			cir.setReturnValue(InfinityPortalBlock.findNewTeleportTarget(world, pos, world.getServer().getOverworld(), entity));
		}
	}

	@Redirect(method="getStateForNeighborUpdate(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/Direction;Lnet/minecraft/block/BlockState;Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z"))
	private boolean injected(BlockState neighborState, Block block) {
		return (neighborState.getBlock() instanceof NetherPortalBlock);
	}
}
