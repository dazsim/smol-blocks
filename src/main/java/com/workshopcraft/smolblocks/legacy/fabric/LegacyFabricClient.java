package com.workshopcraft.smolblocks.legacy.fabric;

import com.workshopcraft.smolblocks.SmolBlocks;
import com.workshopcraft.smolblocks.content.SmolContent;
import com.workshopcraft.smolblocks.legacy.client.LegacyCells;
import com.workshopcraft.smolblocks.legacy.client.PieceBakedModel;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.resources.model.ModelResourceLocation;

public class LegacyFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ColorProviderRegistry.BLOCK.register((state, level, pos, tint) -> LegacyCells.tint(level, pos, tint), SmolContent.MICRO);
        ColorProviderRegistry.ITEM.register(LegacyCells::itemTint, SmolContent.PIECE);
        ModelLoadingPlugin.register(plugin -> plugin.modifyModelAfterBake().register((model, context) -> {
            //? if >=1.21 {
            ModelResourceLocation id = context.topLevelId();
            if (model == null || id == null || !id.id().getNamespace().equals(SmolBlocks.MOD_ID)) return model;
            String path = id.id().getPath(), variant = id.variant();
            //?} else {
            /*if (model == null || !(context.id() instanceof ModelResourceLocation id) || !id.getNamespace().equals(SmolBlocks.MOD_ID)) return model;
            String path = id.getPath(), variant = id.getVariant();
            *///?}
            if (path.equals("micro") && !variant.equals("inventory")) return new FabricMicroBakedModel();
            if (path.equals("piece") && variant.equals("inventory")) return new PieceBakedModel(model);
            return model;
        }));
    }
}
