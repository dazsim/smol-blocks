package com.workshopcraft.smolblocks.legacy.client;

import com.workshopcraft.smolblocks.SmolBlocks;
import com.workshopcraft.smolblocks.content.Cells;
import com.workshopcraft.smolblocks.content.Cuttable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds geometry for a set of cells from each source block's own model, for the versions that still use
 * packed vertex data. Every face quad of the source is clipped to the cell's share of that face, with UVs
 * interpolated, so a full set of pieces lines up with the original texture. Faces between two filled cells
 * are dropped, and blocks with a strip along the top of their sides show it on a lower cell whose top is open.
 */
public final class LegacyQuads {
    public static final int TINTS_PER_CELL = 4;
    private static final float EPS = 1.0E-4F;
    private static final int STRIDE = 8;
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Map<BlockState, Boolean> topStrip = new ConcurrentHashMap<>();

    private LegacyQuads() {}

    /** The quads of one source block, each with the render layer it belongs to. */
    public interface Source {
        void quads(BlockState state, Sink sink);
    }

    public interface Sink {
        void accept(Object layer, @Nullable Direction side, BakedQuad quad);
    }

    /** Quads of one layer: culled faces by direction, then the unculled ones. */
    public static final class Faces {
        @SuppressWarnings("unchecked")
        private final List<BakedQuad>[] lists = new List[7];
        private final List<BakedQuad> all = new ArrayList<>();

        Faces() {
            for (int i = 0; i < 7; i++) lists[i] = new ArrayList<>();
        }

        public List<BakedQuad> get(@Nullable Direction side) {
            return lists[side == null ? 6 : side.ordinal()];
        }

        public List<BakedQuad> all() {
            return all;
        }

        void add(@Nullable Direction side, BakedQuad q) {
            get(side).add(q);
            all.add(q);
        }
    }

    public static void clearCache() {
        topStrip.clear();
    }

    public static BakedModel model(BlockState state) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
    }

    /** Source quads straight from the vanilla model, all in one layer picked per block. */
    public static Source vanilla(java.util.function.Function<BlockState, Object> layer) {
        return (state, sink) -> {
            BakedModel model = model(state);
            Object l = layer.apply(state);
            for (Direction d : DIRECTIONS) for (BakedQuad q : model.getQuads(state, d, RandomSource.create(42L))) sink.accept(l, d, q);
            for (BakedQuad q : model.getQuads(state, null, RandomSource.create(42L))) sink.accept(l, null, q);
        };
    }

    /** Bit for an outer face of a cell that a neighbouring block hides. */
    public static long hiddenBit(int cell, Direction d) {
        return 1L << (d.ordinal() << 3 | cell);
    }

    /**
     * @param remapTint  give each cell its own tint index range so the block colour can look up that cell's block
     * @param worldFaces put outer faces in their cull direction (block rendering) instead of all unculled (items)
     * @param hidden     outer faces covered by neighbouring blocks, see {@link #hiddenBit}
     */
    public static Map<Object, Faces> build(@Nullable BlockState[] cells, boolean remapTint, boolean worldFaces,
                                           float ox, float oy, float oz, long hidden, Source source) {
        Map<Object, Faces> out = new LinkedHashMap<>();
        for (int i = 0; i < 8; i++) {
            BlockState s = cells[i];
            if (s == null) continue;
            int cell = i;
            source.quads(s, (layer, side, q) -> {
                Direction d = side == null ? q.getDirection() : side;
                emit(out.computeIfAbsent(layer, k -> new Faces()), cells, cell, q, d, remapTint, worldFaces, ox, oy, oz, hidden);
            });
        }
        return out;
    }

    private static void emit(Faces out, @Nullable BlockState[] cells, int cell, BakedQuad q, Direction d,
                             boolean remapTint, boolean worldFaces, float ox, float oy, float oz, long hidden) {
        int n = Cells.neighbour(cell, d);
        BlockState self = cells[cell];
        if (n >= 0 && cells[n] != null && (Cuttable.solid(cells[n]) || self.skipRendering(cells[n], d))) return;
        if (n < 0 && (hidden & hiddenBit(cell, d)) != 0) return;
        boolean lift = false;
        if (d.getAxis() != Direction.Axis.Y && Cells.y(cell) == 0 && hasTopStrip(self)) {
            lift = cells[Cells.neighbour(cell, Direction.UP)] != self;
        }
        BakedQuad clipped = clip(q, d, cell, remapTint ? cell : -1, ox, oy, oz, lift);
        if (clipped == null) return;
        // The block state only reports opaque cells as occluding, so see-through cells are never culled by neighbours
        out.add(worldFaces && n < 0 && Cuttable.solid(self) ? d : null, clipped);
    }

    private static boolean hasTopStrip(BlockState state) {
        Boolean known = topStrip.get(state);
        if (known != null) return known;
        boolean strip = state.is(SmolBlocks.TOP_STRIP) || hasSideOverlay(state);
        topStrip.put(state, strip);
        return strip;
    }

    // A tinted layer drawn over an untinted side, the way grass blocks colour their side strip
    private static boolean hasSideOverlay(BlockState state) {
        boolean tinted = false, plain = false;
        for (BakedQuad q : model(state).getQuads(state, Direction.NORTH, RandomSource.create(42L))) {
            if (q.getTintIndex() >= 0) tinted = true;
            else plain = true;
        }
        return tinted && plain;
    }

    private static float coord(int[] v, int vertex, int axis) {
        return Float.intBitsToFloat(v[vertex * STRIDE + axis]);
    }

    /** @param lift sample the upper half of the source face for a lower cell, so a top strip shows on it */
    private static @Nullable BakedQuad clip(BakedQuad q, Direction d, int cell, int tintCell, float ox, float oy, float oz, boolean lift) {
        int[] v = q.getVertices();
        int axis = d.getAxis().ordinal();
        int a = (axis + 1) % 3, b = (axis + 2) % 3;
        float plane = d.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0F : 0.0F;
        for (int i = 0; i < 4; i++) {
            if (Math.abs(coord(v, i, axis) - plane) > EPS) return null;
        }
        float minA = Float.MAX_VALUE, maxA = -Float.MAX_VALUE, minB = Float.MAX_VALUE, maxB = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            minA = Math.min(minA, coord(v, i, a));
            maxA = Math.max(maxA, coord(v, i, a));
            minB = Math.min(minB, coord(v, i, b));
            maxB = Math.max(maxB, coord(v, i, b));
        }
        if (maxA - minA < EPS || maxB - minB < EPS) return null;
        int[] corner = new int[4];
        for (int i = 0; i < 4; i++) {
            boolean hiA = coord(v, i, a) > (minA + maxA) / 2, hiB = coord(v, i, b) > (minB + maxB) / 2;
            corner[(hiA ? 1 : 0) | (hiB ? 2 : 0)] = i;
        }
        float lo = 0.5F * cellCoord(cell, a), loB = 0.5F * cellCoord(cell, b);
        float cellA0 = Math.max(minA, lo), cellA1 = Math.min(maxA, lo + 0.5F);
        float cellB0 = Math.max(minB, loB), cellB1 = Math.min(maxB, loB + 0.5F);
        if (cellA1 - cellA0 < EPS || cellB1 - cellB0 < EPS) return null;
        float facePlane = 0.5F * cellCoord(cell, axis) + (plane > 0 ? 0.5F : 0.0F);
        float liftA = 0.0F, liftB = 0.0F;
        if (lift) {
            // Only whole-face quads can be shifted, partial ones keep their own position
            if (a == 1 && minA < EPS && maxA > 1 - EPS) liftA = 0.5F;
            if (b == 1 && minB < EPS && maxB > 1 - EPS) liftB = 0.5F;
        }
        float[] offset = {ox, oy, oz};
        int[] out = v.clone();
        for (int i = 0; i < 4; i++) {
            float na = coord(v, i, a) > (minA + maxA) / 2 ? cellA1 : cellA0;
            float nb = coord(v, i, b) > (minB + maxB) / 2 ? cellB1 : cellB0;
            float s = (na + liftA - minA) / (maxA - minA), t = (nb + liftB - minB) / (maxB - minB);
            float[] p = new float[3];
            p[axis] = facePlane;
            p[a] = na;
            p[b] = nb;
            for (int k = 0; k < 3; k++) out[i * STRIDE + k] = Float.floatToRawIntBits(p[k] + offset[k]);
            out[i * STRIDE + 4] = Float.floatToRawIntBits(lerp(v, corner, 4, s, t));
            out[i * STRIDE + 5] = Float.floatToRawIntBits(lerp(v, corner, 5, s, t));
        }
        int tint = q.getTintIndex();
        if (tintCell >= 0 && tint >= 0 && tint < TINTS_PER_CELL) tint = tintCell * TINTS_PER_CELL + tint;
        //? if forge || neoforge {
        return new BakedQuad(out, tint, q.getDirection(), q.getSprite(), q.isShade(), q.hasAmbientOcclusion());
        //?} else {
        /*return new BakedQuad(out, tint, q.getDirection(), q.getSprite(), q.isShade());
        *///?}
    }

    private static int cellCoord(int cell, int axis) {
        return switch (axis) {
            case 0 -> Cells.x(cell);
            case 1 -> Cells.y(cell);
            default -> Cells.z(cell);
        };
    }

    private static float lerp(int[] v, int[] corner, int offset, float s, float t) {
        float c00 = Float.intBitsToFloat(v[corner[0] * STRIDE + offset]), c10 = Float.intBitsToFloat(v[corner[1] * STRIDE + offset]);
        float c01 = Float.intBitsToFloat(v[corner[2] * STRIDE + offset]), c11 = Float.intBitsToFloat(v[corner[3] * STRIDE + offset]);
        float bottom = c00 + (c10 - c00) * s;
        float top = c01 + (c11 - c01) * s;
        return bottom + (top - bottom) * t;
    }
}
