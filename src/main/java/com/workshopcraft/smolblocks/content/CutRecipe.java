package com.workshopcraft.smolblocks.content;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Saw plus one block or piece. Saw directly above or below the piece cuts horizontally,
 * anywhere else (beside or diagonal) cuts vertically.
 */
public class CutRecipe extends CustomRecipe {
    public static final CutRecipe INSTANCE = new CutRecipe();
    public static final RecipeSerializer<CutRecipe> SERIALIZER = new RecipeSerializer<>(MapCodec.unit(INSTANCE), StreamCodec.unit(INSTANCE));

    private record Match(int saw, int target, BlockState source, Shape result) {}

    private static Match find(CraftingInput input) {
        int saw = -1, target = -1;
        for (int i = 0; i < input.size(); i++) {
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
            source = stack.get(SmolContent.SOURCE);
            from = stack.get(SmolContent.SHAPE);
            if (source == null || from == null) return null;
        } else {
            source = Cuttable.sourceOf(stack);
            if (source == null) return null;
        }
        int w = input.width();
        boolean horizontal = saw % w == target % w;
        Shape result = Shape.cut(from, horizontal);
        return result == null ? null : new Match(saw, target, source, result);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return find(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Match m = find(input);
        return m == null ? ItemStack.EMPTY : PieceItem.create(m.source, m.result, 2);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> out = NonNullList.withSize(input.size(), ItemStack.EMPTY);
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

    @Override
    public RecipeSerializer<CutRecipe> getSerializer() {
        return SERIALIZER;
    }
}
