package com.workshopcraft.smolblocks.neoforge.jei;

import com.workshopcraft.smolblocks.SmolBlocks;
import com.workshopcraft.smolblocks.content.PieceItem;
import com.workshopcraft.smolblocks.content.Shape;
import com.workshopcraft.smolblocks.content.SmolContent;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Only loaded by JEI. Shows example cuts, since the real cutting recipe works for any eligible block. */
@JeiPlugin
public class SmolBlocksJei implements IModPlugin {
    private static final List<Block> EXAMPLES = List.of(Blocks.STONE, Blocks.OAK_PLANKS);
    private static final Shape[] FROM = {null, Shape.SLAB, Shape.VERTICAL_SLAB, Shape.QUARTER, Shape.VERTICAL_QUARTER};

    @Override
    public Identifier getPluginUid() {
        return SmolBlocks.id("jei");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration r) {
        r.registerFromDataComponentTypes(SmolContent.PIECE, SmolContent.SOURCE, SmolContent.SHAPE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration r) {
        List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();
        Ingredient saws = Ingredient.of(SmolContent.IRON_SAW, SmolContent.DIAMOND_SAW);
        for (Block block : EXAMPLES) {
            BlockState state = block.defaultBlockState();
            for (Shape from : FROM) {
                for (boolean horizontal : new boolean[]{true, false}) {
                    Shape to = Shape.cut(from, horizontal);
                    if (to == null) continue;
                    Ingredient input = from == null ? Ingredient.of(block) : DataComponentIngredient.of(false, PieceItem.create(state, from, 1));
                    recipes.add(example(block, from, horizontal, saws, input, PieceItem.create(state, to, 2)));
                }
            }
        }
        r.addRecipes(RecipeTypes.CRAFTING, recipes);
        r.addItemStackInfo(List.of(new ItemStack(SmolContent.IRON_SAW), new ItemStack(SmolContent.DIAMOND_SAW)),
                Component.translatable("jei.smolblocks.saw"));
        r.addItemStackInfo(List.of(new ItemStack(SmolContent.IRON_PRECISION_PICKAXE), new ItemStack(SmolContent.DIAMOND_PRECISION_PICKAXE)),
                Component.translatable("jei.smolblocks.precision_pickaxe"));
    }

    private static RecipeHolder<CraftingRecipe> example(Block block, @Nullable Shape from, boolean horizontal, Ingredient saws, Ingredient input, ItemStack result) {
        ShapedRecipePattern pattern = horizontal
                ? ShapedRecipePattern.of(Map.of('S', saws, 'B', input), "S", "B")
                : ShapedRecipePattern.of(Map.of('S', saws, 'B', input), "SB");
        CraftingRecipe recipe = new ShapedRecipe(new Recipe.CommonInfo(true),
                new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.BUILDING, ""), pattern, ItemStackTemplate.fromNonEmptyStack(result));
        String path = "jei/" + block.builtInRegistryHolder().key().identifier().getPath() + "_" + (from == null ? "block" : from.getSerializedName()) + (horizontal ? "_flat" : "_upright");
        return new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, SmolBlocks.id(path)), recipe);
    }
}
