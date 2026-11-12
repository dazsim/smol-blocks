package com.workshopcraft.smolblocks.legacy.forgelike;

import com.workshopcraft.smolblocks.content.MicroBlockEntity;
import com.workshopcraft.smolblocks.legacy.client.LegacyCells;
import com.workshopcraft.smolblocks.legacy.client.LegacyQuads;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
//? if neoforge {
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
//?} else {
/*import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
*///?}

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Micro block model for Forge and NeoForge on the older versions. The block entity hands over its cells as
 * model data, faces hidden by neighbours are worked out per chunk build, and the baked quads are cached per
 * distinct combination and split by the render type of each source block.
 */
public class MicroBakedModel implements BakedModel {
    private static final ModelProperty<Long> HIDDEN = new ModelProperty<>();
    private static final int CACHE_LIMIT = 16384;
    private static final Map<Key, Built> cache = new ConcurrentHashMap<>();

    private record Key(MicroBlockEntity.CellStates cells, long hidden) {}

    private record Built(Map<Object, LegacyQuads.Faces> layers, ChunkRenderTypeSet types, TextureAtlasSprite particle) {}

    private static final LegacyQuads.Source SOURCE = (state, sink) -> {
        BakedModel model = LegacyQuads.model(state);
        for (RenderType type : model.getRenderTypes(state, RandomSource.create(42L), ModelData.EMPTY)) {
            for (Direction d : Direction.values()) {
                for (BakedQuad q : model.getQuads(state, d, RandomSource.create(42L), ModelData.EMPTY, type)) sink.accept(type, d, q);
            }
            for (BakedQuad q : model.getQuads(state, null, RandomSource.create(42L), ModelData.EMPTY, type)) sink.accept(type, null, q);
        }
    };

    public MicroBakedModel() {
        cache.clear();
        LegacyQuads.clearCache();
    }

    private static TextureAtlasSprite particle(BlockState state) {
        return LegacyQuads.model(state).getParticleIcon(ModelData.EMPTY);
    }

    private static @Nullable Built built(ModelData data) {
        MicroBlockEntity.CellStates cells = data.get(MicroBlockEntity.CELLS);
        Long hidden = data.get(HIDDEN);
        if (cells == null) return null;
        Key key = new Key(cells, hidden == null ? 0L : hidden);
        Built b = cache.get(key);
        if (b != null) return b;
        if (cache.size() > CACHE_LIMIT) cache.clear();
        Map<Object, LegacyQuads.Faces> layers = LegacyQuads.build(cells.states(), true, true, 0, 0, 0, key.hidden(), SOURCE);
        List<RenderType> types = new ArrayList<>();
        for (Object layer : layers.keySet()) types.add((RenderType) layer);
        BlockState first = Blocks.STONE.defaultBlockState();
        for (BlockState s : cells.states()) {
            if (s != null) {
                first = s;
                break;
            }
        }
        b = new Built(layers, ChunkRenderTypeSet.of(types), particle(first));
        cache.put(key, b);
        return b;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
        MicroBlockEntity.CellStates cells = data.get(MicroBlockEntity.CELLS);
        if (cells == null) cells = LegacyCells.get(level, pos);
        if (cells == null) return data;
        return data.derive().with(MicroBlockEntity.CELLS, cells).with(HIDDEN, LegacyCells.hidden(level, pos, cells)).build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType type) {
        Built b = built(data);
        if (b == null) return List.of();
        if (type != null) {
            LegacyQuads.Faces faces = b.layers().get(type);
            return faces == null ? List.of() : faces.get(side);
        }
        List<BakedQuad> all = new ArrayList<>();
        for (LegacyQuads.Faces faces : b.layers().values()) all.addAll(faces.get(side));
        return all;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        Built b = built(data);
        return b == null ? ChunkRenderTypeSet.none() : b.types();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        Built b = built(data);
        return b == null ? getParticleIcon() : b.particle();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return List.of();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return false;
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
        return particle(Blocks.STONE.defaultBlockState());
    }

    @Override
    public ItemTransforms getTransforms() {
        return ItemTransforms.NO_TRANSFORMS;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
