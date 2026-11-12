package com.workshopcraft.smolblocks.fabric.client;

import com.workshopcraft.smolblocks.client.ClientCells;
import com.workshopcraft.smolblocks.client.MicroPart;
import com.workshopcraft.smolblocks.client.MicroQuads;
import com.workshopcraft.smolblocks.content.MicroBlockEntity;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/** Fabric renderer API model for micro block spaces, sharing the quad builder and cache key with NeoForge. */
public class FabricMicroModel implements BlockStateModel {
    private static final int CACHE_LIMIT = 16384;
    private static final Map<Key, MicroPart> cache = new ConcurrentHashMap<>();

    private record Key(MicroBlockEntity.CellStates cells, long hidden) {}

    public static void clearCache() {
        cache.clear();
        MicroQuads.clearCache();
    }

    private static MicroPart part(MicroBlockEntity.CellStates cells, long hidden) {
        Key key = new Key(cells, hidden);
        MicroPart part = cache.get(key);
        if (part != null) return part;
        if (cache.size() > CACHE_LIMIT) cache.clear();
        QuadCollection quads = MicroQuads.build(cells.states(), true, true, new Vector3f(), hidden);
        part = new MicroPart(quads, particle(cells));
        cache.put(key, part);
        return part;
    }

    private static Material.Baked particle(MicroBlockEntity.@Nullable CellStates cells) {
        if (cells != null) {
            for (BlockState s : cells.states()) if (s != null) return MicroQuads.models().getParticleMaterial(s);
        }
        return MicroQuads.models().getParticleMaterial(Blocks.STONE.defaultBlockState());
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cullTest) {
        MicroBlockEntity.CellStates cells = ClientCells.get(level, pos);
        if (cells == null) return;
        MicroPart part = part(cells, ClientCells.hidden(level, pos, cells));
        for (Direction d : Direction.values()) {
            if (cullTest.test(d)) continue;
            for (BakedQuad q : part.getQuads(d)) {
                emitter.fromBakedQuad(q);
                emitter.cullFace(d);
                emitter.emit();
            }
        }
        for (BakedQuad q : part.getQuads(null)) {
            emitter.fromBakedQuad(q);
            emitter.emit();
        }
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        MicroBlockEntity.CellStates cells = ClientCells.get(level, pos);
        return cells == null ? Boolean.FALSE : new Key(cells, ClientCells.hidden(level, pos, cells));
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return particle(ClientCells.get(level, pos));
    }

    @Override
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        MicroBlockEntity.CellStates cells = ClientCells.get(level, pos);
        return cells == null ? 0 : part(cells, ClientCells.hidden(level, pos, cells)).materialFlags();
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
    }

    @Override
    public Material.Baked particleMaterial() {
        return particle(null);
    }

    @Override
    public int materialFlags() {
        return 0;
    }

    public static final class Unbaked implements BlockStateModel.Unbaked {
        @Override
        public BlockStateModel bake(ModelBaker baker) {
            return new FabricMicroModel();
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
        }
    }
}
