package com.workshopcraft.smolblocks.client;

import com.workshopcraft.smolblocks.content.Cells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds geometry for a set of cells from each source block's own model. Every face quad of the source
 * is clipped to the cell's share of that face, with UVs interpolated, so a full set of pieces lines up
 * with the original texture. Faces between two filled cells are dropped.
 */
public final class MicroQuads {
    public static final int TINTS_PER_CELL = 4;
    private static final float EPS = 1.0E-4F;

    private MicroQuads() {}

    public static BlockStateModelSet models() {
        return Minecraft.getInstance().getModelManager().getBlockStateModelSet();
    }

    /**
     * @param remapTint  give each cell its own tint index range so the block tint source can look up that cell's block
     * @param worldFaces put outer faces in their cull direction (block rendering) instead of all unculled (items)
     */
    public static QuadCollection build(@Nullable BlockState[] cells, boolean remapTint, boolean worldFaces, Vector3fc offset) {
        BlockStateModelSet set = models();
        QuadCollection.Builder out = new QuadCollection.Builder();
        List<BlockStateModelPart> parts = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            BlockState s = cells[i];
            if (s == null) continue;
            BlockStateModel model = set.get(s);
            parts.clear();
            model.collectParts(RandomSource.create(42L), parts);
            for (BlockStateModelPart part : parts) {
                for (Direction d : Direction.values()) {
                    for (BakedQuad q : part.getQuads(d)) emit(out, cells, i, q, d, remapTint, worldFaces, offset);
                }
                for (BakedQuad q : part.getQuads(null)) emit(out, cells, i, q, q.direction(), remapTint, worldFaces, offset);
            }
        }
        return out.build();
    }

    private static void emit(QuadCollection.Builder out, @Nullable BlockState[] cells, int cell, BakedQuad q, Direction d,
                             boolean remapTint, boolean worldFaces, Vector3fc offset) {
        int n = Cells.neighbour(cell, d);
        if (n >= 0 && cells[n] != null) return;
        BakedQuad clipped = clip(q, d, cell, remapTint ? cell : -1, offset);
        if (clipped == null) return;
        if (worldFaces && n < 0) out.addCulledFace(d, clipped);
        else out.addUnculledFace(clipped);
    }

    private static @Nullable BakedQuad clip(BakedQuad q, Direction d, int cell, int tintCell, Vector3fc offset) {
        int axis = d.getAxis().ordinal();
        int a = (axis + 1) % 3, b = (axis + 2) % 3;
        float plane = d.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0F : 0.0F;
        for (int v = 0; v < 4; v++) {
            if (Math.abs(q.position(v).get(axis) - plane) > EPS) return null;
        }
        float minA = Float.MAX_VALUE, maxA = -Float.MAX_VALUE, minB = Float.MAX_VALUE, maxB = -Float.MAX_VALUE;
        for (int v = 0; v < 4; v++) {
            Vector3fc p = q.position(v);
            minA = Math.min(minA, p.get(a));
            maxA = Math.max(maxA, p.get(a));
            minB = Math.min(minB, p.get(b));
            maxB = Math.max(maxB, p.get(b));
        }
        if (maxA - minA < EPS || maxB - minB < EPS) return null;
        int[] corner = new int[4];
        for (int v = 0; v < 4; v++) {
            Vector3fc p = q.position(v);
            boolean hiA = p.get(a) > (minA + maxA) / 2, hiB = p.get(b) > (minB + maxB) / 2;
            corner[(hiA ? 1 : 0) | (hiB ? 2 : 0)] = v;
        }
        float lo = 0.5F * cellCoord(cell, a), loB = 0.5F * cellCoord(cell, b);
        float cellA0 = Math.max(minA, lo), cellA1 = Math.min(maxA, lo + 0.5F);
        float cellB0 = Math.max(minB, loB), cellB1 = Math.min(maxB, loB + 0.5F);
        if (cellA1 - cellA0 < EPS || cellB1 - cellB0 < EPS) return null;
        float facePlane = 0.5F * cellCoord(cell, axis) + (plane > 0 ? 0.5F : 0.0F);

        Vector3f[] pos = new Vector3f[4];
        long[] uv = new long[4];
        for (int v = 0; v < 4; v++) {
            Vector3fc p = q.position(v);
            float na = p.get(a) > (minA + maxA) / 2 ? cellA1 : cellA0;
            float nb = p.get(b) > (minB + maxB) / 2 ? cellB1 : cellB0;
            float s = (na - minA) / (maxA - minA), t = (nb - minB) / (maxB - minB);
            Vector3f np = new Vector3f();
            np.setComponent(axis, facePlane);
            np.setComponent(a, na);
            np.setComponent(b, nb);
            pos[v] = np.add(offset);
            uv[v] = lerpUV(q, corner, s, t);
        }
        BakedQuad.MaterialInfo info = q.materialInfo();
        if (tintCell >= 0 && info.tintIndex() >= 0 && info.tintIndex() < TINTS_PER_CELL) {
            //? if neoforge {
            info = new BakedQuad.MaterialInfo(info.sprite(), info.layer(), info.itemRenderType(),
                    tintCell * TINTS_PER_CELL + info.tintIndex(), info.shade(), info.lightEmission(), info.ambientOcclusion());
            //?} else {
            /*info = new BakedQuad.MaterialInfo(info.sprite(), info.layer(), info.itemRenderType(),
                    tintCell * TINTS_PER_CELL + info.tintIndex(), info.shade(), info.lightEmission());
            *///?}
        }
        //? if neoforge {
        return new BakedQuad(pos[0], pos[1], pos[2], pos[3], uv[0], uv[1], uv[2], uv[3], q.direction(), info, q.bakedNormals(), q.bakedColors());
        //?} else {
        /*return new BakedQuad(pos[0], pos[1], pos[2], pos[3], uv[0], uv[1], uv[2], uv[3], q.direction(), info);
        *///?}
    }

    private static int cellCoord(int cell, int axis) {
        return switch (axis) {
            case 0 -> Cells.x(cell);
            case 1 -> Cells.y(cell);
            default -> Cells.z(cell);
        };
    }

    private static long lerpUV(BakedQuad q, int[] corner, float s, float t) {
        long c00 = q.packedUV(corner[0]), c10 = q.packedUV(corner[1]), c01 = q.packedUV(corner[2]), c11 = q.packedUV(corner[3]);
        float u = bilerp(UVPair.unpackU(c00), UVPair.unpackU(c10), UVPair.unpackU(c01), UVPair.unpackU(c11), s, t);
        float v = bilerp(UVPair.unpackV(c00), UVPair.unpackV(c10), UVPair.unpackV(c01), UVPair.unpackV(c11), s, t);
        return UVPair.pack(u, v);
    }

    private static float bilerp(float c00, float c10, float c01, float c11, float s, float t) {
        float bottom = c00 + (c10 - c00) * s;
        float top = c01 + (c11 - c01) * s;
        return bottom + (top - bottom) * t;
    }
}
