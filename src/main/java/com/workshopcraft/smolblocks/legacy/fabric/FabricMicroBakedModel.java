package com.workshopcraft.smolblocks.legacy.fabric;

import com.workshopcraft.smolblocks.content.MicroBlockEntity;
import com.workshopcraft.smolblocks.legacy.client.LegacyCells;
import com.workshopcraft.smolblocks.legacy.client.LegacyQuads;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Micro block model for Fabric on the older versions, emitted through the Fabric renderer API. */
public class FabricMicroBakedModel implements BakedModel, FabricBakedModel {
    private static final int CACHE_LIMIT = 16384;
    private static final Map<Key, Map<Object, LegacyQuads.Faces>> cache = new ConcurrentHashMap<>();
    private static final Map<RenderType, RenderMaterial> materials = new ConcurrentHashMap<>();
    private static final LegacyQuads.Source SOURCE = LegacyQuads.vanilla(ItemBlockRenderTypes::getChunkRenderType);
    private static final Direction[] DIRECTIONS = Direction.values();

    private record Key(MicroBlockEntity.CellStates cells, long hidden) {}

    public FabricMicroBakedModel() {
        cache.clear();
        LegacyQuads.clearCache();
    }

    private static RenderMaterial material(RenderType type) {
        return materials.computeIfAbsent(type, t -> RendererAccess.INSTANCE.getRenderer().materialFinder()
                .blendMode(BlendMode.fromRenderLayer(t)).find());
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter level, BlockState state, BlockPos pos, Supplier<RandomSource> random, RenderContext context) {
        MicroBlockEntity.CellStates cells = LegacyCells.get(level, pos);
        if (cells == null) return;
        Key key = new Key(cells, LegacyCells.hidden(level, pos, cells));
        Map<Object, LegacyQuads.Faces> layers = cache.get(key);
        if (layers == null) {
            if (cache.size() > CACHE_LIMIT) cache.clear();
            layers = LegacyQuads.build(cells.states(), true, true, 0, 0, 0, key.hidden(), SOURCE);
            cache.put(key, layers);
        }
        QuadEmitter emitter = context.getEmitter();
        for (Map.Entry<Object, LegacyQuads.Faces> e : layers.entrySet()) {
            RenderMaterial material = material((RenderType) e.getKey());
            for (Direction d : DIRECTIONS) emit(emitter, e.getValue().get(d), material, d);
            emit(emitter, e.getValue().get(null), material, null);
        }
    }

    private static void emit(QuadEmitter emitter, List<BakedQuad> quads, RenderMaterial material, @Nullable Direction cull) {
        for (BakedQuad q : quads) {
            emitter.fromVanilla(q, material, cull);
            emitter.emit();
        }
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
        return LegacyQuads.model(Blocks.STONE.defaultBlockState()).getParticleIcon();
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
