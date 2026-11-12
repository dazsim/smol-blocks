package com.workshopcraft.smolblocks.legacy.forge;

import com.workshopcraft.smolblocks.SmolBlocks;
import com.workshopcraft.smolblocks.content.PrecisionPickItem;
import com.workshopcraft.smolblocks.content.SmolContent;
import com.workshopcraft.smolblocks.legacy.client.LegacyCells;
import com.workshopcraft.smolblocks.legacy.client.PieceBakedModel;
import com.workshopcraft.smolblocks.legacy.forgelike.MicroBakedModel;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.RegisterEvent;

@Mod(SmolBlocks.MOD_ID)
public class SmolBlocksForge {
    public SmolBlocksForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener((RegisterEvent e) -> {
            e.register(Registries.BLOCK, h -> SmolContent.registerBlocks(h::register));
            e.register(Registries.ITEM, h -> SmolContent.registerItems(h::register));
            e.register(Registries.BLOCK_ENTITY_TYPE, h -> SmolContent.registerBlockEntities(h::register));
            e.register(Registries.RECIPE_SERIALIZER, h -> SmolContent.registerRecipeSerializers(h::register));
            e.register(Registries.CREATIVE_MODE_TAB, h -> SmolContent.registerTabs(h::register));
        });
        // Last, so protection mods get to cancel the break first
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, BlockEvent.BreakEvent.class, e -> {
            if (e.getLevel() instanceof Level level && PrecisionPickItem.breakPiece(level, e.getPos(), e.getPlayer())) e.setCanceled(true);
        });
        if (FMLEnvironment.dist == Dist.CLIENT) Client.init(modBus);
    }

    private static final class Client {
        static void init(IEventBus modBus) {
            modBus.addListener((ModelEvent.ModifyBakingResult e) -> {
                MicroBakedModel micro = new MicroBakedModel();
                for (BlockState s : SmolContent.MICRO.getStateDefinition().getPossibleStates()) {
                    e.getModels().put(BlockModelShaper.stateToModelLocation(s), micro);
                }
                ModelResourceLocation piece = new ModelResourceLocation(SmolBlocks.id("piece"), "inventory");
                BakedModel base = e.getModels().get(piece);
                if (base != null) e.getModels().put(piece, new PieceBakedModel(base));
            });
            modBus.addListener((RegisterColorHandlersEvent.Block e) ->
                    e.register((state, level, pos, tint) -> LegacyCells.tint(level, pos, tint), SmolContent.MICRO));
            modBus.addListener((RegisterColorHandlersEvent.Item e) -> e.register(LegacyCells::itemTint, SmolContent.PIECE));
        }
    }
}
