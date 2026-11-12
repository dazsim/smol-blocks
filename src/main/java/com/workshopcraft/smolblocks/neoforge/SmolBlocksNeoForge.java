package com.workshopcraft.smolblocks.neoforge;

import com.workshopcraft.smolblocks.SmolBlocks;
import com.workshopcraft.smolblocks.content.SmolContent;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(SmolBlocks.MOD_ID)
public class SmolBlocksNeoForge {
    public SmolBlocksNeoForge(IEventBus modBus) {
        modBus.addListener((RegisterEvent e) -> {
            //? if >=1.21
            e.register(Registries.DATA_COMPONENT_TYPE, h -> SmolContent.registerComponents(h::register));
            e.register(Registries.BLOCK, h -> SmolContent.registerBlocks(h::register));
            e.register(Registries.ITEM, h -> SmolContent.registerItems(h::register));
            e.register(Registries.BLOCK_ENTITY_TYPE, h -> SmolContent.registerBlockEntities(h::register));
            e.register(Registries.RECIPE_SERIALIZER, h -> SmolContent.registerRecipeSerializers(h::register));
            e.register(Registries.CREATIVE_MODE_TAB, h -> SmolContent.registerTabs(h::register));
        });
    }
}
