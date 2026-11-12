package com.workshopcraft.smolblocks.content;

import com.workshopcraft.smolblocks.SmolBlocks;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class SawItem extends Item {
    /** Saw tiers in rising order. A block in a tier's tag needs a saw of that tier or higher. */
    public enum Tier {
        IRON(null),
        DIAMOND(SmolBlocks.NEEDS_DIAMOND_SAW);

        private final @Nullable TagKey<Block> needs;

        Tier(@Nullable TagKey<Block> needs) {
            this.needs = needs;
        }
    }

    private final Tier tier;

    public SawItem(Tier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public boolean canCut(BlockState state) {
        for (Tier t : Tier.values()) {
            if (t.ordinal() > tier.ordinal() && t.needs != null && state.is(t.needs)) return false;
        }
        return true;
    }
}
