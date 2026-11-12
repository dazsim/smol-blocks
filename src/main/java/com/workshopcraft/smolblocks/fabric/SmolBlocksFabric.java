package com.workshopcraft.smolblocks.fabric;

import com.workshopcraft.smolblocks.content.PrecisionPickItem;
import com.workshopcraft.smolblocks.content.SmolContent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class SmolBlocksFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        //? if >=1.21
        SmolContent.registerComponents((id, v) -> Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id, v));
        SmolContent.registerBlocks((id, v) -> Registry.register(BuiltInRegistries.BLOCK, id, v));
        SmolContent.registerItems((id, v) -> Registry.register(BuiltInRegistries.ITEM, id, v));
        SmolContent.registerBlockEntities((id, v) -> Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, v));
        SmolContent.registerRecipeSerializers((id, v) -> Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id, v));
        SmolContent.registerTabs((id, v) -> Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, v));
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, be) -> !PrecisionPickItem.breakPiece(level, pos, player));
    }
}
