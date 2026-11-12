package com.workshopcraft.smolblocks.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
//? if >=1.21 && <26.3
import com.mojang.serialization.MapCodec;
//? if >=26.1 {
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ScheduledTickAccess;
//?} else {
/*import net.minecraft.world.level.LevelAccessor;
*///?}
//? if neoforge || forge
import net.minecraft.world.level.Explosion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The block state carries which cells are opaque and the brightest light any cell gives off, so light
 * occlusion, neighbour face culling and emission work from vanilla's state caches. Everything else
 * comes from the block entity.
 */
public class MicroBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {
    //? if >=1.21 && <26.3
    public static final MapCodec<MicroBlock> CODEC = simpleCodec(MicroBlock::new);
    public static final IntegerProperty OPAQUE = IntegerProperty.create("opaque", 0, 255);
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public MicroBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OPAQUE, 0).setValue(LIGHT, 0).setValue(WATERLOGGED, false));
    }

    //? if >=1.21 && <26.3 {
    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
    //?}

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPAQUE, LIGHT, WATERLOGGED);
    }

    /** The state matching a set of cells. A space with every cell filled has no room for water. */
    public BlockState stateFor(@Nullable BlockState[] cells, boolean waterlogged) {
        int opaque = 0, light = 0, filled = 0;
        for (int i = 0; i < 8; i++) {
            BlockState s = cells[i];
            if (s == null) continue;
            filled |= 1 << i;
            if (Cuttable.solid(s)) opaque |= 1 << i;
            light = Math.max(light, s.getLightEmission());
        }
        return defaultBlockState().setValue(OPAQUE, opaque).setValue(LIGHT, light).setValue(WATERLOGGED, waterlogged && filled != 255);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MicroBlockEntity(pos, state);
    }

    private static int mask(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MicroBlockEntity be ? be.mask() : 0;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int m = mask(level, pos);
        return m == 0 ? Shapes.block() : Cells.shape(m);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Cells.shape(mask(level, pos));
    }

    //? if >=1.21.2 {
    @Override
    public VoxelShape getOcclusionShape(BlockState state) {
        return Cells.shape(state.getValue(OPAQUE));
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state) {
        return state.getValue(OPAQUE) != 255 && !state.getValue(WATERLOGGED);
    }
    //?} else {
    /*@Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Cells.shape(state.getValue(OPAQUE));
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(OPAQUE) != 255 && !state.getValue(WATERLOGGED);
    }
    *///?}

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    //? if >=1.21.2 {
    @Override
    public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                  BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }
    //?} else {
    /*@Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, direction, neighbourState, level, pos, neighbourPos);
    }
    *///?}

    // Water flow between spaces uses the collision shape, so a face only holds water back when its 4 cells are filled
    //? if >=1.21.2 {
    @Override
    public boolean canPlaceLiquid(@Nullable LivingEntity user, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
    //?} else if >=1.21 {
    /*@Override
    public boolean canPlaceLiquid(@Nullable Player user, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
    *///?} else {
    /*@Override
    public boolean canPlaceLiquid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
    *///?}
        return fluid == Fluids.WATER && mask(level, pos) != 255;
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof MicroBlockEntity be)) return super.getDestroyProgress(state, player, level, pos);
        if (PrecisionPickItem.held(player)) {
            int cell = PrecisionPickItem.aimedCell(player, pos);
            if (cell >= 0 && be.cell(cell) != null) return be.cell(cell).getDestroyProgress(player, level, pos);
        }
        float slowest = Float.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            BlockState s = be.cell(i);
            if (s != null) slowest = Math.min(slowest, s.getDestroyProgress(player, level, pos));
        }
        return slowest == Float.MAX_VALUE ? 1.0F : slowest;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (!(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof MicroBlockEntity be)) return List.of();
        Player player = params.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof Player p ? p : null;
        Map<BlockState, Boolean> drops = new HashMap<>();
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : drops(be)) {
            BlockState source = PieceItem.source(stack);
            if (source == null) continue;
            if (drops.computeIfAbsent(source, s -> wouldDrop(s, player, params))) out.add(stack);
        }
        return out;
    }

    /**
     * Pieces drop when the source block would drop anything if broken the same way, so the right tool,
     * Silk Touch and explosion survival all follow the source block's own rules and loot table.
     */
    private static boolean wouldDrop(BlockState source, @Nullable Player player, LootParams.Builder params) {
        if (player != null && !player.hasCorrectToolForDrops(source)) return false;
        return !source.getDrops(params).isEmpty();
    }

    public static boolean wouldDrop(BlockState source, Player player, ServerLevel level, BlockPos pos, ItemStack tool) {
        return player.hasCorrectToolForDrops(source) && !Block.getDrops(source, level, pos, null, player, tool).isEmpty();
    }

    public static List<ItemStack> drops(MicroBlockEntity be) {
        List<ItemStack> out = new ArrayList<>();
        sourceMasks(be).forEach((s, m) -> {
            Map<Shape, Integer> counts = new LinkedHashMap<>();
            for (Cells.Piece piece : Cells.decompose(m)) counts.merge(piece.shape(), 1, Integer::sum);
            counts.forEach((shape, n) -> out.add(PieceItem.create(s, shape, n)));
        });
        return out;
    }

    private static Map<BlockState, Integer> sourceMasks(MicroBlockEntity be) {
        Map<BlockState, Integer> masks = new LinkedHashMap<>();
        for (int i = 0; i < 8; i++) {
            BlockState s = be.cell(i);
            if (s != null) masks.merge(s, 1 << i, (a, b) -> a | b);
        }
        return masks;
    }

    /** The piece that covers a cell, as it would drop. */
    public static ItemStack pieceAt(MicroBlockEntity be, int cell) {
        BlockState s = be.cell(cell);
        if (s == null) return ItemStack.EMPTY;
        for (Cells.Piece piece : Cells.decompose(sourceMasks(be).get(s))) {
            if ((piece.mask() & 1 << cell) != 0) return PieceItem.create(s, piece.shape(), 1);
        }
        return ItemStack.EMPTY;
    }

    private static int firstCell(MicroBlockEntity be) {
        for (int i = 0; i < 8; i++) if (be.cell(i) != null) return i;
        return -1;
    }

    private static ItemStack firstPiece(BlockGetter level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof MicroBlockEntity be) || firstCell(be) < 0) return ItemStack.EMPTY;
        return pieceAt(be, firstCell(be));
    }

    /** The piece under a hit on this space, or the first piece when the hit misses every cell. */
    private static ItemStack pieceHit(BlockGetter level, BlockPos pos, @Nullable HitResult hit) {
        int cell = PrecisionPickItem.cellAt(pos, hit);
        if (cell >= 0 && level.getBlockEntity(pos) instanceof MicroBlockEntity be && be.cell(cell) != null) return pieceAt(be, cell);
        return firstPiece(level, pos);
    }

    //? if >=1.21.4 {
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return firstPiece(level, pos);
    }
    //?} else if >=1.21 {
    /*@Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return firstPiece(level, pos);
    }
    *///?} else {
    /*@Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return firstPiece(level, pos);
    }
    *///?}

    //? if neoforge && >=1.21.4 {
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        return pieceHit(level, pos, player.pick(player.blockInteractionRange(), 1.0F, false));
    }
    //?} else if neoforge {
    /*@Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return pieceHit(level, pos, target);
    }
    *///?} else if forge {
    /*@Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        return pieceHit(level, pos, target);
    }
    *///?}

    // The client would remove the whole space while the server breaks one piece, so it leaves the space alone
    //? if neoforge && >=26.1 {
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, ItemStack tool, boolean willHarvest, FluidState fluid) {
        if (level.isClientSide() && PrecisionPickItem.breakPiece(level, pos, player)) return false;
        return super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
    }
    //?} else if neoforge || forge {
    /*@Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (level.isClientSide() && PrecisionPickItem.breakPiece(level, pos, player)) return false;
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }
    *///?}

    //? if neoforge || forge {
    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        if (!(level.getBlockEntity(pos) instanceof MicroBlockEntity be)) return super.getExplosionResistance(state, level, pos, explosion);
        float best = 0.0F;
        for (int i = 0; i < 8; i++) {
            BlockState s = be.cell(i);
            if (s != null) best = Math.max(best, s.getExplosionResistance(level, pos, explosion));
        }
        return best;
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        if (level.getBlockEntity(pos) instanceof MicroBlockEntity be && be.primary() != null) return be.primary().getSoundType();
        return super.getSoundType(state, level, pos, entity);
    }
    //?}
}
