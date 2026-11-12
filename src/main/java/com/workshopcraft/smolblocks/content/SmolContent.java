package com.workshopcraft.smolblocks.content;

import com.workshopcraft.smolblocks.SmolBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
//? if >=1.21 {
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
//?}
//? if >=1.21.2 {
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
//?}
//? if fabric && >=26.1
/*import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;*/
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

import java.util.List;
//? if neoforge && >=26.1
import java.util.Set;
import java.util.function.BiConsumer;

public final class SmolContent {
    //? if >=1.21 {
    public static final DataComponentType<BlockState> SOURCE = DataComponentType.<BlockState>builder()
            .persistent(BlockState.CODEC).networkSynchronized(ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY)).build();
    public static final DataComponentType<Shape> SHAPE = DataComponentType.<Shape>builder()
            .persistent(Shape.CODEC).networkSynchronized(Shape.STREAM_CODEC).build();
    //?}

    public static Block MICRO;
    public static BlockEntityType<MicroBlockEntity> MICRO_ENTITY;
    public static Item IRON_SAW;
    public static Item DIAMOND_SAW;
    public static Item PIECE;
    public static CreativeModeTab TAB;

    private static final List<Block> SAMPLES = List.of(Blocks.STONE, Blocks.COBBLESTONE, Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS,
            Blocks.BRICKS, Blocks.STONE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.QUARTZ_BLOCK,
            //? if >=26.2 {
            /*Blocks.WOOL.white(),
            *///?} else {
            Blocks.WHITE_WOOL,
            //?}
            Blocks.GRASS_BLOCK,
            Blocks.OAK_LOG, Blocks.DIAMOND_BLOCK);

    private SmolContent() {}

    //? if >=1.21 {
    public static void registerComponents(BiConsumer<Identifier, DataComponentType<?>> r) {
        r.accept(SmolBlocks.id("source"), SOURCE);
        r.accept(SmolBlocks.id("shape"), SHAPE);
    }
    //?}

    public static void registerBlocks(BiConsumer<Identifier, Block> r) {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(1.5F, 6.0F).sound(SoundType.STONE)
                .lightLevel(s -> s.getValue(MicroBlock.LIGHT)).dynamicShape()
                //? if >=26.3 {
                /*.pushReaction(PushReaction.IMMOVEABLE)
                *///?} else {
                .pushReaction(PushReaction.BLOCK)
                //?}
                //? if >=1.21.2 {
                .setId(ResourceKey.create(Registries.BLOCK, SmolBlocks.id("micro")))
                //?}
                ;
        MICRO = new MicroBlock(props);
        r.accept(SmolBlocks.id("micro"), MICRO);
    }

    public static void registerItems(BiConsumer<Identifier, Item> r) {
        IRON_SAW = new SawItem(SawItem.Tier.IRON, props("iron_saw").durability(250));
        DIAMOND_SAW = new SawItem(SawItem.Tier.DIAMOND, props("diamond_saw").durability(1561));
        PIECE = new PieceItem(props("piece"));
        r.accept(SmolBlocks.id("iron_saw"), IRON_SAW);
        r.accept(SmolBlocks.id("diamond_saw"), DIAMOND_SAW);
        r.accept(SmolBlocks.id("piece"), PIECE);
    }

    public static void registerBlockEntities(BiConsumer<Identifier, BlockEntityType<?>> r) {
        //? if fabric && >=26.1 {
        /*MICRO_ENTITY = FabricBlockEntityTypeBuilder.create(MicroBlockEntity::new, MICRO).build();
        *///?} else if >=26.1 {
        MICRO_ENTITY = new BlockEntityType<>(MicroBlockEntity::new, Set.of(MICRO));
        //?} else {
        /*MICRO_ENTITY = BlockEntityType.Builder.of(MicroBlockEntity::new, MICRO).build(null);
        *///?}
        r.accept(SmolBlocks.id("micro"), MICRO_ENTITY);
    }

    public static void registerRecipeSerializers(BiConsumer<Identifier, RecipeSerializer<?>> r) {
        r.accept(SmolBlocks.id("cutting"), CutRecipe.SERIALIZER);
    }

    public static void registerTabs(BiConsumer<Identifier, CreativeModeTab> r) {
        TAB = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemGroup.smolblocks"))
                .icon(() -> PieceItem.create(Blocks.OAK_PLANKS.defaultBlockState(), Shape.VERTICAL_SLAB, 1))
                .displayItems((params, out) -> {
                    out.accept(IRON_SAW);
                    out.accept(DIAMOND_SAW);
                    for (Block b : SAMPLES) {
                        BlockState s = b.defaultBlockState();
                        if (!Cuttable.test(s)) continue;
                        for (Shape shape : Shape.values()) out.accept(PieceItem.create(s, shape, 1));
                    }
                })
                .build();
        r.accept(SmolBlocks.id("main"), TAB);
    }

    private static Item.Properties props(String path) {
        //? if >=1.21.2 {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, SmolBlocks.id(path)));
        //?} else {
        /*return new Item.Properties();
        *///?}
    }

    public static ItemStack saw(boolean diamond) {
        return new ItemStack(diamond ? DIAMOND_SAW : IRON_SAW);
    }
}
