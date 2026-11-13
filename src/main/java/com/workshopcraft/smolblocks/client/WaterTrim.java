package com.workshopcraft.smolblocks.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.workshopcraft.smolblocks.content.Cells;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * Fluid output for a waterlogged micro block space. Each water quad is split at the cell boundaries and
 * only the parts inside empty cells are kept, so water never shows over or inside a filled cell.
 */
public final class WaterTrim implements FluidRenderer.Output {
    private final FluidRenderer.Output output;
    private final int mask, opaque, neighbour;
    private final @Nullable Direction side;
    private final float baseX, baseY, baseZ;

    public WaterTrim(FluidRenderer.Output output, int mask, int opaque, BlockPos pos) {
        this(output, mask, opaque, pos, null, 0);
    }

    /** A pass for one side the renderer would skip, keeping what meets the see-through cells of the next space. */
    public WaterTrim(FluidRenderer.Output output, int mask, int opaque, BlockPos pos, @Nullable Direction side, int neighbour) {
        this.output = output;
        this.mask = mask;
        this.opaque = opaque;
        this.side = side;
        this.neighbour = neighbour;
        this.baseX = pos.getX() & 15;
        this.baseY = pos.getY() & 15;
        this.baseZ = pos.getZ() & 15;
    }

    @Override
    public VertexConsumer getBuilder(ChunkSectionLayer layer) {
        return new Trimmer(output.getBuilder(layer));
    }

    private final class Trimmer implements VertexConsumer {
        private static final float EPS = 1.0E-3F;
        private final VertexConsumer out;
        private final float[][] pos = new float[4][3];
        private final float[] u = new float[4], v = new float[4];
        private int count, color, overlay, light;
        private float nx, ny, nz;

        Trimmer(VertexConsumer out) {
            this.out = out;
        }

        @Override
        public void addVertex(float x, float y, float z, int color, float u, float v, int overlayCoords, int lightCoords, float nx, float ny, float nz) {
            pos[count][0] = x;
            pos[count][1] = y;
            pos[count][2] = z;
            this.u[count] = u;
            this.v[count] = v;
            this.color = color;
            this.overlay = overlayCoords;
            this.light = lightCoords;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
            if (++count == 4) {
                count = 0;
                quad();
            }
        }

        private void quad() {
            int axis = planeAxis();
            if (side != null && !Cells.onSide(side, axis, (pos[0][axis] + pos[2][axis]) / 2 - (axis == 0 ? baseX : axis == 1 ? baseY : baseZ)) && side != Direction.UP) return;
            float[] s = splits(pos[0], pos[3], pos[1], pos[2]);
            float[] t = splits(pos[0], pos[1], pos[3], pos[2]);
            for (int i = 0; i + 1 < s.length; i++) {
                for (int j = 0; j + 1 < t.length; j++) {
                    float s0 = s[i], s1 = s[i + 1], t0 = t[j], t1 = t[j + 1];
                    float[] c = lerp((s0 + s1) / 2, (t0 + t1) / 2);
                    int cell = Cells.index(c[0] - baseX >= 0.5F ? 1 : 0, c[1] - baseY >= 0.5F ? 1 : 0, c[2] - baseZ >= 0.5F ? 1 : 0);
                    float rel = c[axis] - (axis == 0 ? baseX : axis == 1 ? baseY : baseZ);
                    float shift = Cells.waterShift(mask, opaque, side, neighbour, cell, axis, rel);
                    if (Float.isNaN(shift)) continue;
                    emit(s0, t0, axis, shift);
                    emit(s1, t0, axis, shift);
                    emit(s1, t1, axis, shift);
                    emit(s0, t1, axis, shift);
                }
            }
        }

        /** Axis the quad faces along: the one its corners spread least on, so sloped water tops count as up. */
        private int planeAxis() {
            int axis = 1;
            float best = Float.MAX_VALUE;
            for (int k = 0; k < 3; k++) {
                float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
                for (float[] p : pos) {
                    lo = Math.min(lo, p[k]);
                    hi = Math.max(hi, p[k]);
                }
                if (hi - lo < best) {
                    best = hi - lo;
                    axis = k;
                }
            }
            return axis;
        }

        /**
         * Parameter values where the quad crosses a cell boundary, from 0 to 1. The direction runs from the
         * midpoint of edge a0-a1 to the midpoint of edge b0-b1.
         */
        private float[] splits(float[] a0, float[] a1, float[] b0, float[] b1) {
            int axis = 0;
            float best = -1.0F;
            float[] from = new float[3], to = new float[3];
            for (int k = 0; k < 3; k++) {
                from[k] = (a0[k] + a1[k]) / 2;
                to[k] = (b0[k] + b1[k]) / 2;
                float len = Math.abs(to[k] - from[k]);
                if (len > best) {
                    best = len;
                    axis = k;
                }
            }
            float base = axis == 0 ? baseX : axis == 1 ? baseY : baseZ;
            float c0 = from[axis] - base, c1 = to[axis] - base;
            if (best > EPS && (0.5F - c0) * (0.5F - c1) < 0 && Math.abs(0.5F - c0) > EPS && Math.abs(0.5F - c1) > EPS) {
                return new float[]{0.0F, (0.5F - c0) / (c1 - c0), 1.0F};
            }
            return new float[]{0.0F, 1.0F};
        }

        private float[] lerp(float s, float t) {
            float[] r = new float[5];
            for (int k = 0; k < 3; k++) r[k] = bilerp(pos[0][k], pos[1][k], pos[2][k], pos[3][k], s, t);
            r[3] = bilerp(u[0], u[1], u[2], u[3], s, t);
            r[4] = bilerp(v[0], v[1], v[2], v[3], s, t);
            return r;
        }

        // Vertex order is (0,0), (1,0), (1,1), (0,1) in s, t
        private static float bilerp(float c00, float c10, float c11, float c01, float s, float t) {
            float near = c00 + (c10 - c00) * s;
            float far = c01 + (c11 - c01) * s;
            return near + (far - near) * t;
        }

        private void emit(float s, float t, int axis, float shift) {
            float[] r = lerp(s, t);
            r[axis] += shift;
            // Stretch still-water sides up to the top of the space so they meet the water above
            if (side == Direction.UP && axis != 1 && r[1] - baseY > 0.5F) r[1] = baseY + 1.0F;
            out.addVertex(r[0], r[1], r[2], color, r[3], r[4], overlay, light, nx, ny, nz);
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return out.addVertex(x, y, z);
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            return out.setColor(r, g, b, a);
        }

        @Override
        public VertexConsumer setColor(int color) {
            return out.setColor(color);
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return out.setUv(u, v);
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return out.setUv1(u, v);
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return out.setUv2(u, v);
        }

        //? if >=26.3 {
        /*@Override
        public VertexConsumer setUv3(float u, float v) {
            return out.setUv3(u, v);
        }
        *///?}

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return out.setNormal(x, y, z);
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return out.setLineWidth(width);
        }
    }
}
