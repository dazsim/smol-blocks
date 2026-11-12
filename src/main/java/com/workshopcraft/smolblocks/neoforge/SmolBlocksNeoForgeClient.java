package com.workshopcraft.smolblocks.neoforge;

import com.workshopcraft.smolblocks.SmolBlocks;
import com.workshopcraft.smolblocks.client.MicroQuads;
import com.workshopcraft.smolblocks.client.PieceItemModel;
import com.workshopcraft.smolblocks.content.MicroBlockEntity;
import com.workshopcraft.smolblocks.content.SmolContent;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;

import java.util.ArrayList;
import java.util.List;

@Mod(value = SmolBlocks.MOD_ID, dist = Dist.CLIENT)
public class SmolBlocksNeoForgeClient {
    public SmolBlocksNeoForgeClient(IEventBus modBus) {
        modBus.addListener((RegisterBlockStateModels e) -> e.registerModel(SmolBlocks.id("micro"), MicroBlockModel.Unbaked.CODEC));
        modBus.addListener((ModelEvent.BakingCompleted e) -> MicroBlockModel.clearCache());
        modBus.addListener((RegisterItemModelsEvent e) -> e.register(SmolBlocks.id("piece"), PieceItemModel.Unbaked.MAP_CODEC));
        modBus.addListener((RegisterColorHandlersEvent.BlockTintSources e) -> {
            List<BlockTintSource> sources = new ArrayList<>();
            for (int i = 0; i < 8 * MicroQuads.TINTS_PER_CELL; i++) sources.add(new CellTint(e.getBlockColors(), i / MicroQuads.TINTS_PER_CELL, i % MicroQuads.TINTS_PER_CELL));
            e.register(sources, SmolContent.MICRO);
        });
    }

    /** Tint for one cell's tint layer, looked up from the block that cell was cut from. */
    private record CellTint(BlockColors colors, int cell, int layer) implements BlockTintSource {
        @Override
        public int color(BlockState state) {
            return -1;
        }

        @Override
        public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            MicroBlockEntity.CellStates cells = level.getModelData(pos).get(MicroBlockEntity.CELLS);
            BlockState source = cells == null ? null : cells.states()[cell];
            if (source == null) return -1;
            BlockTintSource tint = colors.getTintSource(source, layer);
            return tint == null ? -1 : tint.colorInWorld(source, level, pos);
        }
    }
}
