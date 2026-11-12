package com.workshopcraft.smolblocks.client;

import com.workshopcraft.smolblocks.content.Cells;
import com.workshopcraft.smolblocks.content.Cuttable;
import com.workshopcraft.smolblocks.content.MicroBlock;
import com.workshopcraft.smolblocks.content.MicroBlockEntity;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class ClientCells {
    private ClientCells() {}

    /** The cells of a micro block space as the renderer sees them, from model data or the block entity. */
    public static MicroBlockEntity.@Nullable CellStates get(BlockAndTintGetter level, BlockPos pos) {
        //? if neoforge {
        MicroBlockEntity.CellStates cells = level.getModelData(pos).get(MicroBlockEntity.CELLS);
        //?} else {
        /*MicroBlockEntity.CellStates cells = level.getBlockEntityRenderData(pos) instanceof MicroBlockEntity.CellStates c ? c : null;
        *///?}
        if (cells == null && level.getBlockEntity(pos) instanceof MicroBlockEntity be) cells = be.snapshot();
        return cells;
    }

    /**
     * Outer faces covered by the next space over: the touching cell of a neighbouring micro block, or a
     * full block. Glass next to glass hides the shared face the way vanilla glass blocks do.
     */
    public static long hidden(BlockAndTintGetter level, BlockPos pos, MicroBlockEntity.CellStates cells) {
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
                    hidden |= MicroQuads.hiddenBit(i, d);
                }
            }
        }
        return hidden;
    }
}
