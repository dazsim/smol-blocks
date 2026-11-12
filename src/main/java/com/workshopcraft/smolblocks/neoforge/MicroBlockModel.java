package com.workshopcraft.smolblocks.neoforge;

import com.mojang.serialization.MapCodec;
import com.workshopcraft.smolblocks.client.MicroPart;
import com.workshopcraft.smolblocks.client.MicroQuads;
import com.workshopcraft.smolblocks.content.MicroBlockEntity;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MicroBlockModel implements DynamicBlockStateModel {
    private static final int CACHE_LIMIT = 4096;
    private final Map<MicroBlockEntity.CellStates, MicroPart> cache = new ConcurrentHashMap<>();

    private static MicroBlockEntity.@Nullable CellStates cells(BlockAndTintGetter level, BlockPos pos) {
        return level.getModelData(pos).get(MicroBlockEntity.CELLS);
    }

    private MicroPart part(MicroBlockEntity.CellStates cells) {
        MicroPart part = cache.get(cells);
        if (part != null) return part;
        if (cache.size() > CACHE_LIMIT) cache.clear();
        QuadCollection quads = MicroQuads.build(cells.states(), true, true, new Vector3f());
        part = new MicroPart(quads, particle(cells));
        cache.put(cells, part);
        return part;
    }

    private static Material.Baked particle(MicroBlockEntity.@Nullable CellStates cells) {
        if (cells != null) {
            for (BlockState s : cells.states()) if (s != null) return MicroQuads.models().getParticleMaterial(s);
        }
        return MicroQuads.models().getParticleMaterial(Blocks.STONE.defaultBlockState());
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        MicroBlockEntity.CellStates cells = cells(level, pos);
        if (cells != null) parts.add(part(cells));
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        MicroBlockEntity.CellStates cells = cells(level, pos);
        return cells == null ? Boolean.FALSE : cells;
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return particle(cells(level, pos));
    }

    @Override
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        MicroBlockEntity.CellStates cells = cells(level, pos);
        return cells == null ? 0 : part(cells).materialFlags();
    }

    @Override
    public Material.Baked particleMaterial() {
        return particle(null);
    }

    @Override
    public int materialFlags() {
        return 0;
    }

    public record Unbaked() implements CustomUnbakedBlockStateModel {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<Unbaked> codec() {
            return CODEC;
        }

        @Override
        public BlockStateModel bake(ModelBaker baker) {
            return new MicroBlockModel();
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
        }
    }
}
