package com.workshopcraft.smolblocks;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class SmolBlocks {
    public static final String MOD_ID = "smolblocks";
    public static final TagKey<Block> CUTTABLE = TagKey.create(Registries.BLOCK, id("cuttable"));
    public static final TagKey<Block> UNCUTTABLE = TagKey.create(Registries.BLOCK, id("uncuttable"));
    public static final TagKey<Block> NEEDS_DIAMOND_SAW = TagKey.create(Registries.BLOCK, id("needs_diamond_saw"));

    private SmolBlocks() {}

    public static Identifier id(String path) {
        //? if >=1.21 {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
        //?} else {
        /*return new Identifier(MOD_ID, path);
        *///?}
    }
}
