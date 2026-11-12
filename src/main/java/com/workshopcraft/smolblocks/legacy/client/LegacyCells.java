package com.workshopcraft.smolblocks.legacy.client;

import com.workshopcraft.smolblocks.content.Cells;
import com.workshopcraft.smolblocks.content.Cuttable;
import com.workshopcraft.smolblocks.content.MicroBlock;
import com.workshopcraft.smolblocks.content.MicroBlockEntity;
import com.workshopcraft.smolblocks.content.PieceItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class LegacyCells {
    private LegacyCells() {}

    public static MicroBlockEntity.@Nullable CellStates get(BlockGetter level, BlockPos pos) {
        //? if fabric {
        /*if (level.getBlockEntityRenderData(pos) instanceof MicroBlockEntity.CellStates c) return c;
        *///?}
        return level.getBlockEntity(pos) instanceof MicroBlockEntity be ? be.snapshot() : null;
    }

    /**
     * Outer faces covered by the next space over: the touching cell of a neighbouring micro block, or a
     * full block. Glass next to glass hides the shared face the way vanilla glass blocks do.
     */
    public static long hidden(BlockGetter level, BlockPos pos, MicroBlockEntity.CellStates cells) {
        long hidden = 0L;
        BlockState[] own = cells.states();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (Direction d : Direction.values()) {
            at.setWithOffset(pos, d);
            BlockState next = level.getBlockState(at);
            if (next.isAir()) continue;
            BlockState[] other = null;
            if (next.getBlock() instanceof MicroBlock) {
                MicroBlockEntity.CellStates nc = get(level, at);
                if (nc == null) continue;
                other = nc.states();
            }
            for (int i = 0; i < 8; i++) {
                BlockState self = own[i];
                if (self == null || Cells.neighbour(i, d) >= 0) continue;
                BlockState touching = other == null ? next : other[i ^ 1 << d.getAxis().ordinal()];
                if (touching != null && (Cuttable.solid(touching) || self.skipRendering(touching, d))) {
                    hidden |= LegacyQuads.hiddenBit(i, d);
                }
            }
        }
        return hidden;
    }

    /** Block colour for the micro block: each cell's tint indexes map back to the block that cell was cut from. */
    public static int tint(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tint) {
        if (level == null || pos == null || tint < 0) return -1;
        MicroBlockEntity.CellStates cells = get(level, pos);
        int cell = tint / LegacyQuads.TINTS_PER_CELL;
        BlockState source = cells == null || cell >= 8 ? null : cells.states()[cell];
        if (source == null) return -1;
        return Minecraft.getInstance().getBlockColors().getColor(source, level, pos, tint % LegacyQuads.TINTS_PER_CELL);
    }

    /** Item colour for pieces, from the block they were cut from. */
    public static int itemTint(ItemStack stack, int tint) {
        BlockState source = PieceItem.source(stack);
        if (source == null || tint < 0) return -1;
        return 0xFF000000 | Minecraft.getInstance().getBlockColors().getColor(source, null, null, tint);
    }
}
