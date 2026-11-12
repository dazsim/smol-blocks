package com.workshopcraft.smolblocks.fabric.client;

import com.workshopcraft.smolblocks.SmolBlocks;
import com.workshopcraft.smolblocks.client.CellTint;
import com.workshopcraft.smolblocks.client.PieceItemModel;
import com.workshopcraft.smolblocks.content.SmolContent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.world.level.block.state.BlockState;

public class SmolBlocksFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ItemModels.ID_MAPPER.put(SmolBlocks.id("piece"), PieceItemModel.Unbaked.MAP_CODEC);
        BlockColorRegistry.register(CellTint.all(), SmolContent.MICRO);
        ModelLoadingPlugin.register(ctx -> {
            FabricMicroModel.clearCache();
            BlockStateModel.UnbakedRoot root = new FabricMicroModel.Unbaked().asRoot();
            ctx.registerBlockStateResolver(SmolContent.MICRO, rc -> {
                for (BlockState state : SmolContent.MICRO.getStateDefinition().getPossibleStates()) rc.setModel(state, root);
            });
        });
    }
}
