package com.workshopcraft.smolblocks.neoforge.client;

import com.workshopcraft.smolblocks.SmolBlocks;
import com.workshopcraft.smolblocks.client.CellTint;
import com.workshopcraft.smolblocks.client.PieceItemModel;
import com.workshopcraft.smolblocks.content.SmolContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;

@Mod(value = SmolBlocks.MOD_ID, dist = Dist.CLIENT)
public class SmolBlocksNeoForgeClient {
    public SmolBlocksNeoForgeClient(IEventBus modBus) {
        modBus.addListener((RegisterBlockStateModels e) -> e.registerModel(SmolBlocks.id("micro"), MicroBlockModel.Unbaked.CODEC));
        modBus.addListener((ModelEvent.BakingCompleted e) -> MicroBlockModel.clearCache());
        modBus.addListener((RegisterItemModelsEvent e) -> e.register(SmolBlocks.id("piece"), PieceItemModel.Unbaked.MAP_CODEC));
        modBus.addListener((RegisterColorHandlersEvent.BlockTintSources e) -> e.register(CellTint.all(), SmolContent.MICRO));
    }
}
