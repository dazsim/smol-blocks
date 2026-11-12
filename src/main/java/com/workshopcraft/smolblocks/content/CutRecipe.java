package com.workshopcraft.smolblocks.content;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.2 {
import com.mojang.serialization.MapCodec;
import net.minecraft.network.codec.StreamCodec;
//?} else {
/*import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
*///?}
//? if >=1.21 {
import net.minecraft.world.item.crafting.CraftingInput;
//?} else {
/*import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.CraftingContainer;
*///?}
//? if >=1.21 && <1.21.2
/*import net.minecraft.core.HolderLookup;*/
//? if <1.21
/*import net.minecraft.core.RegistryAccess;*/

/**
 * Saw plus one block or piece. Saw directly above or below the piece cuts horizontally,
 * anywhere else (beside or diagonal) cuts vertically.
 */
public class CutRecipe extends CustomRecipe {
    //? if >=1.21.2 {
    public static final CutRecipe INSTANCE = new CutRecipe();
    public static final RecipeSerializer<CutRecipe> SERIALIZER = new RecipeSerializer<>(MapCodec.unit(INSTANCE), StreamCodec.unit(INSTANCE));
    //?} else if >=1.21 {
    /*public static final RecipeSerializer<CutRecipe> SERIALIZER = new SimpleCraftingRecipeSerializer<>(CutRecipe::new);

    public CutRecipe(CraftingBookCategory category) {
        super(category);
    }
    *///?} else {
    /*public static final RecipeSerializer<CutRecipe> SERIALIZER = new SimpleCraftingRecipeSerializer<>(CutRecipe::new);

    public CutRecipe(Identifier id, CraftingBookCategory category) {
        super(id, category);
    }
    *///?}

    private record Match(int saw, int target, BlockState source, Shape result) {}

    //? if >=1.21 {
    private static Match find(CraftingInput input) {
        int size = input.size(), w = input.width();
    //?} else {
    /*private static Match find(CraftingContainer input) {
        int size = input.getContainerSize(), w = input.getWidth();
    *///?}
        int saw = -1, target = -1;
        for (int i = 0; i < size; i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof SawItem) {
                if (saw >= 0) return null;
                saw = i;
            } else {
                if (target >= 0) return null;
                target = i;
            }
        }
        if (saw < 0 || target < 0) return null;
        ItemStack stack = input.getItem(target);
        BlockState source;
        Shape from = null;
        if (stack.getItem() instanceof PieceItem) {
            source = PieceItem.source(stack);
            from = PieceItem.shape(stack);
            if (source == null || from == null) return null;
        } else {
            source = Cuttable.sourceOf(stack);
            if (source == null) return null;
        }
        if (!((SawItem) input.getItem(saw).getItem()).canCut(source)) return null;
        boolean horizontal = saw % w == target % w;
        Shape result = Shape.cut(from, horizontal);
        return result == null ? null : new Match(saw, target, source, result);
    }

    //? if >=1.21 {
    @Override
    public boolean matches(CraftingInput input, Level level) {
        return find(input) != null;
    }

    //? if >=1.21.2 {
    @Override
    public ItemStack assemble(CraftingInput input) {
    //?} else {
    /*@Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
    *///?}
        Match m = find(input);
        return m == null ? ItemStack.EMPTY : PieceItem.create(m.source, m.result, 2);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> out = NonNullList.withSize(input.size(), ItemStack.EMPTY);
    //?} else {
    /*@Override
    public boolean matches(CraftingContainer input, Level level) {
        return find(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
        Match m = find(input);
        return m == null ? ItemStack.EMPTY : PieceItem.create(m.source, m.result, 2);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer input) {
        NonNullList<ItemStack> out = NonNullList.withSize(input.getContainerSize(), ItemStack.EMPTY);
    *///?}
        Match m = find(input);
        if (m == null) return out;
        ItemStack saw = input.getItem(m.saw).copyWithCount(1);
        int damage = saw.getDamageValue() + 1;
        if (damage < saw.getMaxDamage()) {
            saw.setDamageValue(damage);
            out.set(m.saw, saw);
        }
        return out;
    }

    //? if <1.21.2 {
    /*@Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }
    *///?}

    @Override
    public RecipeSerializer<CutRecipe> getSerializer() {
        return SERIALIZER;
    }
}
