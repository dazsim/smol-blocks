package com.workshopcraft.smolblocks.content;

import com.workshopcraft.smolblocks.SmolBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public final class Cuttable {
    private Cuttable() {}

    public static boolean test(BlockState state) {
        if (state.isAir() || state.getBlock() instanceof MicroBlock) return false;
        if (state.is(SmolBlocks.CUTTABLE)) return true;
        if (state.is(SmolBlocks.UNCUTTABLE) || state.hasBlockEntity()) return false;
        return state.getRenderShape() == RenderShape.MODEL && solid(state)
                && state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    public static boolean solid(BlockState state) {
        //? if >=1.21.2 {
        return state.isSolidRender();
        //?} else {
        /*return state.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        *///?}
    }

    public static BlockState sourceOf(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem item) {
            BlockState state = item.getBlock().defaultBlockState();
            if (test(state)) return state;
        }
        return null;
    }
}
