package net.lerariemann.infinity.block.entity;

import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;
import net.lerariemann.infinity.InfinityMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class NeitherPortalBlockEntity extends BlockEntity {
    private final PropertyDelegate propertyDelegate;
    private Identifier dimension;
    private long portalColor;
    private boolean isOpen;

    public NeitherPortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NEITHER_PORTAL.get(), pos, state);
        this.propertyDelegate = new PropertyDelegate() {
            public int get(int index) {
                if (index == 0) {
                    return (int)(NeitherPortalBlockEntity.this.portalColor);
                }
                return 0;
            }

            public void set(int index, int value) {
                if (index == 0) {
                    NeitherPortalBlockEntity.this.portalColor = value;
                }

            }
            public int size() {
                return 1;
            }
        };
    }

    public NeitherPortalBlockEntity(BlockPos pos, BlockState state, long i) {
        this(pos, state, i, InfinityMod.getId("generated_"+i));
    }

    public NeitherPortalBlockEntity(BlockPos pos, BlockState state, long i, Identifier id) {
        this(pos, state);
        this.dimension = id;
        this.portalColor = i;
        this.isOpen = false;
    }

    public Identifier getDimension() {
        return this.dimension;
    }

    public long getPortalColor() {
        return this.portalColor;
    }

    public boolean getOpen() {
        return this.isOpen;
    }

    public void setDimension(long c) {
        setDimension(c, InfinityMod.getId("generated_"+c));
    }

    public void setDimension(long c, Identifier i) {
        this.portalColor = c;
        this.dimension = i;
    }

    public void setOpen(boolean i) {
        this.isOpen = i;
    }
    public void writeNbt(NbtCompound tag) {
        super.writeNbt(tag);
        tag.putLong("Dimension", this.portalColor);
        tag.putString("DimensionName", this.dimension.toString());
        tag.putBoolean("Open", this.isOpen);
    }

    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);
        this.portalColor = tag.getLong("Dimension");
        if (tag.contains("DimensionName")) {
            this.dimension = Identifier.tryParse(tag.getString("DimensionName"));
        }
        else this.dimension = InfinityMod.getId("generated_" + this.portalColor);
        this.isOpen = tag.getBoolean("Open");
    }

    @Nullable
    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    @Override
    public Object getRenderData() {
        return propertyDelegate.get(0);
    }
}
