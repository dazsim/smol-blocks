package com.workshopcraft.smolblocks.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
//? if <1.21 {
/*import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
*///?}

public class PieceItem extends Item {
    public PieceItem(Properties properties) {
        super(properties);
    }

    //? if >=1.21 {
    public static ItemStack create(BlockState source, Shape shape, int count) {
        ItemStack stack = new ItemStack(SmolContent.PIECE, count);
        stack.set(SmolContent.SOURCE, source);
        stack.set(SmolContent.SHAPE, shape);
        return stack;
    }

    public static @Nullable BlockState source(ItemStack stack) {
        return stack.get(SmolContent.SOURCE);
    }

    public static @Nullable Shape shape(ItemStack stack) {
        return stack.get(SmolContent.SHAPE);
    }
    //?} else {
    /*private static final String SOURCE = "smolblocks:source", SHAPE = "smolblocks:shape";

    public static ItemStack create(BlockState source, Shape shape, int count) {
        ItemStack stack = new ItemStack(SmolContent.PIECE, count);
        CompoundTag tag = stack.getOrCreateTag();
        tag.put(SOURCE, NbtUtils.writeBlockState(source));
        tag.putString(SHAPE, shape.getSerializedName());
        return stack;
    }

    public static @Nullable BlockState source(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(SOURCE)) return null;
        BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound(SOURCE));
        return state.isAir() ? null : state;
    }

    public static @Nullable Shape shape(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? null : Shape.byName(tag.getString(SHAPE));
    }
    *///?}

    @Override
    public Component getName(ItemStack stack) {
        BlockState source = source(stack);
        Shape shape = shape(stack);
        if (source == null || shape == null) return super.getName(stack);
        return Component.translatable("item.smolblocks.piece." + shape.getSerializedName(), source.getBlock().getName());
    }

    /** Where a piece would go: the space and the cells it fills there. */
    public record Placement(BlockPos pos, int mask) {}

    public static Placement placement(Shape shape, Vec3 hit, Direction face, Direction facing) {
        Vec3 p = hit.add(face.getStepX() * 0.001, face.getStepY() * 0.001, face.getStepZ() * 0.001);
        BlockPos pos = BlockPos.containing(p);
        int cx = p.x - pos.getX() >= 0.5 ? 1 : 0;
        int cy = p.y - pos.getY() >= 0.5 ? 1 : 0;
        int cz = p.z - pos.getZ() >= 0.5 ? 1 : 0;
        boolean sx = false, sy = false, sz = false;
        switch (shape) {
            case SLAB -> { sx = true; sz = true; }
            case VERTICAL_SLAB -> {
                Direction.Axis thin = face.getAxis().isHorizontal() ? face.getAxis() : facing.getAxis();
                sy = true;
                if (thin == Direction.Axis.X) sz = true; else sx = true;
            }
            case VERTICAL_QUARTER -> sy = true;
            case QUARTER -> {
                if (facing.getAxis() == Direction.Axis.X) sz = true; else sx = true;
            }
            case EIGHTH -> {}
        }
        return new Placement(pos, Cells.mask(sx, sy, sz, cx, cy, cz));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        ItemStack stack = ctx.getItemInHand();
        BlockState source = source(stack);
        Shape shape = shape(stack);
        if (source == null || shape == null) return InteractionResult.PASS;
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        Placement pl = placement(shape, ctx.getClickLocation(), ctx.getClickedFace(), ctx.getHorizontalDirection());
        BlockPos clicked = ctx.getClickedPos();
        BlockState clickedState = level.getBlockState(clicked);
        if (!(clickedState.getBlock() instanceof MicroBlock) && clickedState.canBeReplaced()) {
            // Clicking grass, snow or similar puts the piece in that space, resting on the floor like a placed block
            Vec3 hit = ctx.getClickLocation();
            Vec3 floor = new Vec3(Mth.clamp(hit.x, clicked.getX() + 0.01, clicked.getX() + 0.99), clicked.getY(),
                    Mth.clamp(hit.z, clicked.getZ() + 0.01, clicked.getZ() + 0.99));
            pl = placement(shape, floor, Direction.UP, ctx.getHorizontalDirection());
        }
        BlockPos pos = pl.pos();
        if (player != null && !player.mayUseItemAt(pos, ctx.getClickedFace(), stack)) return InteractionResult.FAIL;
        if (!level.isInWorldBounds(pos)) return InteractionResult.FAIL;
        BlockState here = level.getBlockState(pos);
        MicroBlockEntity be = null;
        if (here.getBlock() instanceof MicroBlock) {
            if (!(level.getBlockEntity(pos) instanceof MicroBlockEntity m) || (m.mask() & pl.mask()) != 0) return InteractionResult.FAIL;
            be = m;
        } else if (!here.canBeReplaced()) {
            return InteractionResult.FAIL;
        }
        if (!level.isUnobstructed(null, Cells.shape(pl.mask()).move(pos.getX(), pos.getY(), pos.getZ()))) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (be == null) {
            BlockState[] cells = new BlockState[8];
            for (int i = 0; i < 8; i++) if ((pl.mask() & 1 << i) != 0) cells[i] = source;
            boolean water = here.getFluidState().is(Fluids.WATER);
            BlockState state = ((MicroBlock) SmolContent.MICRO).stateFor(cells, water);
            if (!level.setBlock(pos, state, 3)) return InteractionResult.FAIL;
            if (water) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            if (!(level.getBlockEntity(pos) instanceof MicroBlockEntity m)) return InteractionResult.FAIL;
            be = m;
        }
        be.fill(pl.mask(), source);
        SoundType sound = source.getSoundType();
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, source));
        //? if >=1.21 {
        stack.consume(1, player);
        //?} else {
        /*if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
        *///?}
        return InteractionResult.SUCCESS;
    }
}
