package com.workshopcraft.smolblocks.legacy.neoforge;

import com.workshopcraft.smolblocks.SmolBlocks;
import com.workshopcraft.smolblocks.content.SmolContent;
import com.workshopcraft.smolblocks.legacy.client.LegacyCells;
import com.workshopcraft.smolblocks.legacy.client.PieceBakedModel;
import com.workshopcraft.smolblocks.legacy.forgelike.MicroBakedModel;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

import java.util.Map;

@Mod(value = SmolBlocks.MOD_ID, dist = Dist.CLIENT)
public class LegacyNeoClient {
    public LegacyNeoClient(IEventBus modBus) {
        modBus.addListener((ModelEvent.ModifyBakingResult e) -> bake(e.getModels()));
        modBus.addListener((RegisterColorHandlersEvent.Block e) ->
                e.register((state, level, pos, tint) -> LegacyCells.tint(level, pos, tint), SmolContent.MICRO));
        modBus.addListener((RegisterColorHandlersEvent.Item e) -> e.register(LegacyCells::itemTint, SmolContent.PIECE));
    }

    private static void bake(Map<ModelResourceLocation, BakedModel> models) {
        MicroBakedModel micro = new MicroBakedModel();
        for (BlockState s : SmolContent.MICRO.getStateDefinition().getPossibleStates()) {
            models.put(BlockModelShaper.stateToModelLocation(s), micro);
        }
        ModelResourceLocation piece = ModelResourceLocation.inventory(SmolBlocks.id("piece"));
        BakedModel base = models.get(piece);
        if (base != null) models.put(piece, new PieceBakedModel(base));
    }
}
