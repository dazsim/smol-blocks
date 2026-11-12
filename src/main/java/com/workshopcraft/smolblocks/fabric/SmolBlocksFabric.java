package com.workshopcraft.smolblocks.fabric;

import com.workshopcraft.smolblocks.content.SmolContent;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/** Common content only. Fabric rendering of micro blocks comes with the M3 ports. */
public class SmolBlocksFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SmolContent.registerComponents((id, v) -> Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id, v));
        SmolContent.registerBlocks((id, v) -> Registry.register(BuiltInRegistries.BLOCK, id, v));
        SmolContent.registerItems((id, v) -> Registry.register(BuiltInRegistries.ITEM, id, v));
        SmolContent.registerBlockEntities((id, v) -> Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, v));
        SmolContent.registerRecipeSerializers((id, v) -> Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id, v));
        SmolContent.registerTabs((id, v) -> Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, v));
    }
}
