package com.workshopcraft.smolblocks.neoforge;

import com.mojang.serialization.MapCodec;
import com.workshopcraft.smolblocks.client.MicroPart;
import com.workshopcraft.smolblocks.client.MicroQuads;
import com.workshopcraft.smolblocks.content.Cells;
import com.workshopcraft.smolblocks.content.MicroBlock;
import com.workshopcraft.smolblocks.content.MicroBlockEntity;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

/**
 * One baked part per distinct cell combination and set of neighbour hidden faces, shared by every block
 * state and cleared when models rebake.
 */
public class MicroBlockModel implements DynamicBlockStateModel {
    private static final int CACHE_LIMIT = 16384;
    private static final Map<Key, MicroPart> cache = new ConcurrentHashMap<>();

    private record Key(MicroBlockEntity.CellStates cells, long hidden) {}

    public static void clearCache() {
        cache.clear();
        MicroQuads.clearCache();
    }

    private static MicroBlockEntity.@Nullable CellStates cells(BlockAndTintGetter level, BlockPos pos) {
        MicroBlockEntity.CellStates cells = level.getModelData(pos).get(MicroBlockEntity.CELLS);
        if (cells == null && level.getBlockEntity(pos) instanceof MicroBlockEntity be) cells = be.snapshot();
        return cells;
    }

    /**
     * Outer faces covered by the next space over: the touching cell of a neighbouring micro block, or a
     * full block. Glass next to glass hides the shared face the way vanilla glass blocks do.
     */
    private static long hidden(BlockAndTintGetter level, BlockPos pos, MicroBlockEntity.CellStates cells) {
        long hidden = 0L;
        BlockState[] own = cells.states();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (Direction d : Direction.values()) {
            at.setWithOffset(pos, d);
            BlockState next = level.getBlockState(at);
            if (next.isAir()) continue;
            BlockState[] other = null;
            if (next.getBlock() instanceof MicroBlock) {
                MicroBlockEntity.CellStates nc = cells(level, at);
                if (nc == null) continue;
                other = nc.states();
            }
            for (int i = 0; i < 8; i++) {
                BlockState self = own[i];
                if (self == null || Cells.neighbour(i, d) >= 0) continue;
                BlockState touching = other == null ? next : other[i ^ 1 << d.getAxis().ordinal()];
                if (touching != null && (touching.isSolidRender() || self.skipRendering(touching, d))) {
                    hidden |= MicroQuads.hiddenBit(i, d);
                }
            }
        }
        return hidden;
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
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        MicroBlockEntity.CellStates cells = cells(level, pos);
        if (cells != null) parts.add(part(cells, hidden(level, pos, cells)));
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        MicroBlockEntity.CellStates cells = cells(level, pos);
        return cells == null ? Boolean.FALSE : new Key(cells, hidden(level, pos, cells));
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return particle(cells(level, pos));
    }

    @Override
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        MicroBlockEntity.CellStates cells = cells(level, pos);
        return cells == null ? 0 : part(cells, hidden(level, pos, cells)).materialFlags();
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
