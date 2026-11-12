package com.workshopcraft.smolblocks.client;

import com.workshopcraft.smolblocks.content.MicroBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Tint for one cell's tint layer, looked up from the block that cell was cut from. */
public record CellTint(int cell, int layer) implements BlockTintSource {
    public static List<BlockTintSource> all() {
        List<BlockTintSource> sources = new ArrayList<>();
        for (int i = 0; i < 8 * MicroQuads.TINTS_PER_CELL; i++) sources.add(new CellTint(i / MicroQuads.TINTS_PER_CELL, i % MicroQuads.TINTS_PER_CELL));
        return sources;
    }

    @Override
    public int color(BlockState state) {
        return -1;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        MicroBlockEntity.CellStates cells = ClientCells.get(level, pos);
        BlockState source = cells == null ? null : cells.states()[cell];
        if (source == null) return -1;
        BlockTintSource tint = Minecraft.getInstance().getBlockColors().getTintSource(source, layer);
        return tint == null ? -1 : tint.colorInWorld(source, level, pos);
    }
}
