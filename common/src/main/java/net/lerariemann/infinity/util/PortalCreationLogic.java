package net.lerariemann.infinity.util;

import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import dev.architectury.platform.Platform;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.lerariemann.infinity.InfinityMod;
import net.lerariemann.infinity.PlatformMethods;
import net.lerariemann.infinity.access.MinecraftServerAccess;
import net.lerariemann.infinity.block.ModBlocks;
import net.lerariemann.infinity.block.custom.InfinityPortalBlock;
import net.lerariemann.infinity.block.entity.InfinityPortalBlockEntity;
import net.lerariemann.infinity.dimensions.RandomDimension;
import net.lerariemann.infinity.item.function.ModItemFunctions;
import net.lerariemann.infinity.item.ModItems;
import net.lerariemann.infinity.loading.DimensionGrabber;
import net.lerariemann.infinity.options.PortalColorApplier;
import net.lerariemann.infinity.var.ModCriteria;
import net.lerariemann.infinity.var.ModPayloads;
import net.lerariemann.infinity.var.ModStats;
import net.minecraft.block.BlockState;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static net.lerariemann.infinity.compat.ComputerCraftCompat.checkPrintedPage;
import static net.lerariemann.infinity.compat.ComputerCraftCompat.isPrintedPage;

public interface PortalCreationLogic {
    static void tryCreatePortalFromItem(BlockState state, World world, BlockPos pos, ItemEntity entity) {
        ItemStack itemStack = entity.getStack();
        if (itemStack.getItem() == ModItems.TRANSFINITE_KEY.get()) {
            Identifier key_dest;
            if (itemStack.getNbt() != null) {
                key_dest = Identifier.tryParse(itemStack.getNbt().getString("key_destination"));
            }
            else {
                key_dest = Identifier.of("minecraft", "random");
            }
            if (key_dest != null) {
                MinecraftServer server = world.getServer();
                if (server != null) {
                    if (world instanceof ServerWorld serverWorld) {
                        boolean bl = modifyOnInitialCollision(key_dest, serverWorld, pos);
                        if (bl) entity.remove(Entity.RemovalReason.CHANGED_DIMENSION);
                    }
                }
            }
        }
        else if (itemStack.getItem() == Items.WRITTEN_BOOK || itemStack.getItem() == Items.WRITABLE_BOOK) {
            NbtCompound compound = itemStack.getNbt();
            String content;
            if (compound != null) {
                content = parseComponents(compound, itemStack.getItem());
            }
            else content = "";
            MinecraftServer server = world.getServer();
            if (server != null) {
                Identifier id = WarpLogic.getIdentifier(content, server);
                if (world instanceof ServerWorld serverWorld) {
                    boolean bl = modifyOnInitialCollision(id, serverWorld, pos);
                    if (bl) entity.remove(Entity.RemovalReason.CHANGED_DIMENSION);
                }
            }
        }
        else if (Platform.isModLoaded("computercraft")) {
            if (isPrintedPage(itemStack.getItem())) {
                NbtCompound compound = itemStack.getNbt();
                String content;
                if (compound != null) {
                    content = checkPrintedPage(compound);
                }
                else content = "";
                MinecraftServer server = world.getServer();
                if (server != null) {
                    Identifier id = WarpLogic.getIdentifier(content, server);
                    if (world instanceof ServerWorld serverWorld) {
                        boolean bl = modifyOnInitialCollision(id, serverWorld, pos);
                        if (bl) entity.remove(Entity.RemovalReason.CHANGED_DIMENSION);
                    }
                }
            }
        }

    }

    /* Extracts the string used to generate the dimension ID from component content. */
    static String parseComponents(NbtCompound compound, Item item) {
        NbtList pages = compound.getList("pages", NbtElement.STRING_TYPE);
        if (pages.isEmpty()) {
            return "";
        }
        else if (item == Items.WRITTEN_BOOK) {
            String pagesString = pages.get(0).asString();
            return pagesString.substring(pagesString.indexOf(':')+2, pagesString.length()-2);
        }
        else if (item == Items.WRITABLE_BOOK) {
            return pages.get(0).asString();
        }
        else return "";
    }

    /* Sets the portal color and destination and calls to open the portal immediately if the portal key is blank.
     * Statistics for opening the portal are attributed to the nearest player. */
    static boolean modifyOnInitialCollision(Identifier dimName, ServerWorld world, BlockPos pos) {
        MinecraftServer server = world.getServer();
        if (dimName.toString().equals("minecraft:random")) {
            dimName = WarpLogic.getRandomId(world.getRandom());
        }
        PlayerEntity nearestPlayer = world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 5, false);

        if (((MinecraftServerAccess)server).infinity$needsInvocation()) {
            WarpLogic.onInvocationNeedDetected(nearestPlayer);
            return false;
        }

        /* Set color and destination. Open status = the world that is being accessed exists already. */
        boolean dimensionExistsAlready = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, dimName)) != null;
        modifyPortalRecursive(world, pos, dimName, dimensionExistsAlready);

        if (dimensionExistsAlready) {
            if (nearestPlayer != null) nearestPlayer.increaseStat(ModStats.PORTALS_OPENED_STAT, 1);
            runAfterEffects(world, pos, false);
        }

        /* If the portal key is blank, open the portal immediately. */
        else if (RandomProvider.getProvider(server).portalKey.isBlank()) {
            openWithStatIncrease(nearestPlayer, server, world, pos);
        }
        return true;
    }

    /* Calls to open the portal and attributes the relevant statistics to a player provided. */
    static void openWithStatIncrease(PlayerEntity player, MinecraftServer s, ServerWorld world, BlockPos pos) {
        if (((MinecraftServerAccess)s).infinity$needsInvocation()) {
            WarpLogic.onInvocationNeedDetected(player);
            return;
        }
        boolean isDimensionNew = open(s, world, pos);
        if (player != null) {
            if (isDimensionNew) {
                player.increaseStat(ModStats.DIMS_OPENED_STAT, 1);
                ModCriteria.DIMS_OPENED.trigger((ServerPlayerEntity)player);
            }
            player.increaseStat(ModStats.PORTALS_OPENED_STAT, 1);
        }
    }

    /* Opens the portal by trying to make it usable, including a call to generate a dimension if needed. */
    static boolean open(MinecraftServer s, ServerWorld world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        boolean bl = false;
        if (blockEntity instanceof InfinityPortalBlockEntity npbe) {
            /* Call dimension creation. */
            Identifier i = npbe.getDimension();
            if (i.getNamespace().equals(InfinityMod.MOD_ID)) {
                bl = addInfinityDimension(s, i);
            }

            /* Set the portal's open status making it usable. */
            modifyPortalRecursive(world, pos, new PortalModifierUnion()
                    .addModifier(be -> be.setOpen(true))
                    .addModifier(BlockEntity::markDirty));
            runAfterEffects(world, pos, bl);
        }
        return bl;
    }

    /* Recursively creates a queue of neighbouring portal blocks and for each of them executes an action. */
    static void modifyPortalRecursive(ServerWorld world, BlockPos pos, BiConsumer<World, BlockPos> modifier) {
        Set<BlockPos> set = Sets.newHashSet();
        Queue<BlockPos> queue = Queues.newArrayDeque();
        queue.add(pos);
        BlockPos blockPos;
        Direction.Axis axis = world.getBlockState(pos).get(NetherPortalBlock.AXIS);
        while ((blockPos = queue.poll()) != null) {
            set.add(blockPos);
            BlockState blockState = world.getBlockState(blockPos);
            if (blockState.getBlock() instanceof NetherPortalBlock || blockState.getBlock() instanceof InfinityPortalBlock) {
                modifier.accept(world, blockPos);
                Set<Direction> toCheck = (axis == Direction.Axis.Z) ?
                        Set.of(Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH) :
                        Set.of(Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST);
                BlockPos blockPos2;
                for (Direction dir : toCheck) {
                    blockPos2 = blockPos.offset(dir);
                    if (!set.contains(blockPos2))
                        queue.add(blockPos2);
                }
            }
        }
    }

    /* Updates this and all neighbouring portal blocks with a new dimension and open status. */
    static void modifyPortalRecursive(ServerWorld world, BlockPos pos, Identifier id, boolean open) {
        PortalColorApplier applier = WarpLogic.getPortalColorApplier(id, world.getServer());
        BlockState originalState = world.getBlockState(pos);
        BlockState state = (originalState.isOf(ModBlocks.NEITHER_PORTAL.get())) ?
                originalState.with(InfinityPortalBlock.BOOP, !originalState.get(InfinityPortalBlock.BOOP)) :
                ModBlocks.NEITHER_PORTAL.get().getDefaultState()
                        .with(NetherPortalBlock.AXIS, originalState.get(NetherPortalBlock.AXIS));
        modifyPortalRecursive(world, pos, new PortalModifierUnion()
                .addSetupper(p -> world.setBlockState(p, state))
                .addModifier(nbpe -> nbpe.setDimension(id))
                .addModifier(npbe -> npbe.setColor(applier.apply(npbe.getPos())))
                .addModifier(npbe -> npbe.setOpen(open))
                .addModifier(BlockEntity::markDirty));
    }

    /* Calls to create the dimension based on its ID. Returns true if the dimension being opened is indeed brand new. */
    static boolean addInfinityDimension(MinecraftServer server, Identifier id) {
        /* checks if the dimension requested is valid and does not already exist */
        if (!id.getNamespace().equals(InfinityMod.MOD_ID)) return false;
        RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, id);
        if ((server.getWorld(key) != null) || ((MinecraftServerAccess)(server)).infinity$hasToAdd(key)) return false;

        /* creates the dimension datapack */
        RandomDimension d = new RandomDimension(id, server);

        if (!RandomProvider.getProvider(server).rule("runtimeGenerationEnabled")) return false;
        ((MinecraftServerAccess)(server)).infinity$addWorld(
                key, (new DimensionGrabber(server.getRegistryManager())).grab_all(d)); // create the dimension
        server.getPlayerManager().getPlayerList().forEach(
                a -> sendNewWorld(a, id, d)); //and send everyone its data for clientside updating
        return true;
    }

    /* Create and send S2C packets necessary for the client to process a freshly added dimension. */
    static void sendNewWorld(ServerPlayerEntity player, Identifier id, RandomDimension d) {
        ServerPlayNetworking.send(player, ModPayloads.WORLD_ADD, buildPacket(id, d));
    }

    /* Create and send S2C packets necessary for the client to process a freshly added dimension. */
    static PacketByteBuf buildPacket(Identifier id, RandomDimension d) {
        PacketByteBuf buf = PlatformMethods.createPacketByteBufs();
        buf.writeIdentifier(id);
        buf.writeNbt(d.type != null ? d.type.data : new NbtCompound());
        buf.writeInt(d.random_biomes.size());
        d.random_biomes.forEach(b -> {
            buf.writeIdentifier(InfinityMod.getId(b.name));
            buf.writeNbt(b.data);
        });
        return buf;
    }

    /* Jingle signaling the portal is now usable. */
    static void runAfterEffects(ServerWorld world, BlockPos pos, boolean dimensionIsNew) {
        if (dimensionIsNew) world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1f, 1f);
        world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1f, 1f);
    }

    record PortalModifier(Consumer<InfinityPortalBlockEntity> modifier) implements BiConsumer<World, BlockPos> {
        @Override
        public void accept(World world, BlockPos pos) {
            if (world.getBlockEntity(pos) instanceof InfinityPortalBlockEntity npbe) modifier.accept(npbe);
        }
    }

    record PortalModifierUnion(List<Consumer<BlockPos>> setuppers, List<Consumer<InfinityPortalBlockEntity>> modifiers)
            implements BiConsumer<World, BlockPos> {
        PortalModifierUnion() {
            this(new ArrayList<>(), new ArrayList<>());
        }
        PortalModifierUnion addSetupper(Consumer<BlockPos> setupper) {
            setuppers.add(setupper);
            return this;
        }
        PortalModifierUnion addModifier(Consumer<InfinityPortalBlockEntity> modifier) {
            modifiers.add(modifier);
            return this;
        }

        @Override
        public void accept(World world, BlockPos pos) {
            setuppers.forEach(setupper -> setupper.accept(pos));
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof InfinityPortalBlockEntity npbe)
                modifiers.forEach(modifier -> modifier.accept(npbe));
        }
    }
}
