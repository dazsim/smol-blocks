package com.workshopcraft.smolblocks.legacy.client;

import com.workshopcraft.smolblocks.content.PieceItem;
import com.workshopcraft.smolblocks.content.Shape;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Item model for pieces: picks a model per source block and shape, built from the source block's quads. */
public class PieceBakedModel implements BakedModel {
    private record Key(BlockState state, Shape shape) {}

    private final BakedModel base;
    private final Map<Key, BakedModel> cache = new ConcurrentHashMap<>();
    // Vanilla keeps the empty constructor private, Forge and NeoForge open it up
    //? if fabric {
    /*private final ItemOverrides overrides = new ItemOverrides(null, null, List.of()) {
    *///?} else {
    private final ItemOverrides overrides = new ItemOverrides() {
    //?}
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            BlockState state = PieceItem.source(stack);
            Shape shape = PieceItem.shape(stack);
            if (state == null || shape == null) return base;
            return cache.computeIfAbsent(new Key(state, shape), PieceBakedModel.this::build);
        }
    };

    public PieceBakedModel(BakedModel base) {
        this.base = base;
    }

    private BakedModel build(Key key) {
        BlockState[] cells = new BlockState[8];
        Shape s = key.shape();
        int mask = s.canonicalMask();
        for (int i = 0; i < 8; i++) if ((mask & 1 << i) != 0) cells[i] = key.state();
        List<BakedQuad> quads = new ArrayList<>();
        LegacyQuads.build(cells, false, false, s.spanX ? 0 : 0.25F, s.spanY ? 0 : 0.25F, s.spanZ ? 0 : 0.25F, 0L,
                LegacyQuads.vanilla(state -> Boolean.TRUE)).values().forEach(f -> quads.addAll(f.all()));
        TextureAtlasSprite particle = LegacyQuads.model(key.state()).getParticleIcon();
        return new Shaped(List.copyOf(quads), particle, base.getTransforms());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return base.getQuads(state, side, rand);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return base.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return base.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    private record Shaped(List<BakedQuad> quads, TextureAtlasSprite particle, ItemTransforms transforms) implements BakedModel {
        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            return side == null ? quads : List.of();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }

        @Override
        public boolean isGui3d() {
            return true;
        }

        @Override
        public boolean usesBlockLight() {
            return true;
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return particle;
        }

        @Override
        public ItemTransforms getTransforms() {
            return transforms;
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }
    }
}
