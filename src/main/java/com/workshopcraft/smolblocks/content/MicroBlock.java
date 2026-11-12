package com.workshopcraft.smolblocks.content;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MicroBlock extends Block implements EntityBlock {
    public static final MapCodec<MicroBlock> CODEC = simpleCodec(MicroBlock::new);

    public MicroBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MicroBlockEntity(pos, state);
    }

    private static int mask(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MicroBlockEntity be ? be.mask() : 0;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int m = mask(level, pos);
        return m == 0 ? Shapes.block() : Cells.shape(m);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Cells.shape(mask(level, pos));
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof MicroBlockEntity be)) return super.getDestroyProgress(state, player, level, pos);
        float slowest = Float.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            BlockState s = be.cell(i);
            if (s != null) slowest = Math.min(slowest, s.getDestroyProgress(player, level, pos));
        }
        return slowest == Float.MAX_VALUE ? 1.0F : slowest;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (!(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof MicroBlockEntity be)) return List.of();
        return drops(be);
    }

    public static List<ItemStack> drops(MicroBlockEntity be) {
        Map<BlockState, Integer> masks = new LinkedHashMap<>();
        for (int i = 0; i < 8; i++) {
            BlockState s = be.cell(i);
            if (s != null) masks.merge(s, 1 << i, (a, b) -> a | b);
        }
        List<ItemStack> out = new ArrayList<>();
        masks.forEach((s, m) -> {
            Map<Shape, Integer> counts = new LinkedHashMap<>();
            for (Shape shape : Cells.decompose(m)) counts.merge(shape, 1, Integer::sum);
            counts.forEach((shape, n) -> out.add(PieceItem.create(s, shape, n)));
        });
        return out;
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        if (level.getBlockEntity(pos) instanceof MicroBlockEntity be && be.primary() != null) return PieceItem.create(be.primary(), Shape.EIGHTH, 1);
        return ItemStack.EMPTY;
    }

    //? if neoforge {
    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        if (level.getBlockEntity(pos) instanceof MicroBlockEntity be && be.primary() != null) return be.primary().getSoundType();
        return super.getSoundType(state, level, pos, entity);
    }
    //?}
}
