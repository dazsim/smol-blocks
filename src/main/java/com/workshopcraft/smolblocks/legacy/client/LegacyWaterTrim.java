package com.workshopcraft.smolblocks.legacy.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.workshopcraft.smolblocks.content.Cells;
import net.minecraft.core.BlockPos;

/**
 * Vertex output for the water in a waterlogged micro block space. The fluid renderer writes quads one vertex
 * at a time; each quad is split at the cell boundaries and only the parts inside empty cells are passed on.
 */
public final class LegacyWaterTrim implements VertexConsumer {
    private static final float EPS = 1.0E-3F;
    private final VertexConsumer out;
    private final int mask, opaque;
    private final float baseX, baseY, baseZ;
    private final float[][] pos = new float[4][3];
    private final float[] u = new float[4], v = new float[4];
    private final int[] r = new int[4], g = new int[4], b = new int[4], a = new int[4];
    private final int[] lightU = new int[4], lightV = new int[4];
    private float x, y, z, cu, cv, nx, ny, nz;
    private int cr = 255, cg = 255, cb = 255, ca = 255, lu, lv;
    private int count;

    public LegacyWaterTrim(VertexConsumer out, int mask, int opaque, BlockPos at) {
        this.out = out;
        this.mask = mask;
        this.opaque = opaque;
        this.baseX = at.getX() & 15;
        this.baseY = at.getY() & 15;
        this.baseZ = at.getZ() & 15;
    }

    private void finish() {
        pos[count][0] = x;
        pos[count][1] = y;
        pos[count][2] = z;
        u[count] = cu;
        v[count] = cv;
        r[count] = cr;
        g[count] = cg;
        b[count] = cb;
        a[count] = ca;
        lightU[count] = lu;
        lightV[count] = lv;
        if (++count == 4) {
            count = 0;
            quad();
        }
    }

    private void quad() {
        int axis = planeAxis();
        float[] s = splits(pos[0], pos[3], pos[1], pos[2]);
        float[] t = splits(pos[0], pos[1], pos[3], pos[2]);
        for (int i = 0; i + 1 < s.length; i++) {
            for (int j = 0; j + 1 < t.length; j++) {
                float s0 = s[i], s1 = s[i + 1], t0 = t[j], t1 = t[j + 1];
                float[] c = lerp((s0 + s1) / 2, (t0 + t1) / 2);
                int cell = Cells.index(c[0] - baseX >= 0.5F ? 1 : 0, c[1] - baseY >= 0.5F ? 1 : 0, c[2] - baseZ >= 0.5F ? 1 : 0);
                float shift = 0.0F;
                if ((mask & 1 << cell) != 0) {
                    // A face on the outside of a see-through filled cell moves in to the water cell behind it
                    if ((opaque & 1 << cell) != 0 || (mask & 1 << (cell ^ 1 << axis)) != 0) continue;
                    float rel = c[axis] - (axis == 0 ? baseX : axis == 1 ? baseY : baseZ);
                    shift = rel < 0.5F ? 0.5F : axis == 1 ? 0.499F - rel : -0.5F;
                }
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

    /** Parameter values where the quad crosses a cell boundary, from the midpoint of edge a0-a1 to that of b0-b1. */
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
        float[] res = new float[5];
        for (int k = 0; k < 3; k++) res[k] = bilerp(pos[0][k], pos[1][k], pos[2][k], pos[3][k], s, t);
        res[3] = bilerp(u[0], u[1], u[2], u[3], s, t);
        res[4] = bilerp(v[0], v[1], v[2], v[3], s, t);
        return res;
    }

    // Vertex order is (0,0), (1,0), (1,1), (0,1) in s, t
    private static float bilerp(float c00, float c10, float c11, float c01, float s, float t) {
        float near = c00 + (c10 - c00) * s;
        float far = c01 + (c11 - c01) * s;
        return near + (far - near) * t;
    }

    private void emit(float s, float t, int axis, float shift) {
        float[] p = lerp(s, t);
        p[axis] += shift;
        // Colour and light are uniform enough across a water quad to take them from the nearest corner
        int k = s < 0.5F ? (t < 0.5F ? 0 : 3) : (t < 0.5F ? 1 : 2);
        //? if >=1.21 {
        out.addVertex(p[0], p[1], p[2]).setColor(r[k], g[k], b[k], a[k]).setUv(p[3], p[4]).setUv2(lightU[k], lightV[k]).setNormal(nx, ny, nz);
        //?} else {
        /*out.vertex(p[0], p[1], p[2]).color(r[k], g[k], b[k], a[k]).uv(p[3], p[4]).uv2(lightU[k], lightV[k]).normal(nx, ny, nz).endVertex();
        *///?}
    }

    //? if >=1.21 {
    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        cr = r;
        cg = g;
        cb = b;
        ca = a;
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        cu = u;
        cv = v;
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        lu = u;
        lv = v;
        return this;
    }

    // The fluid renderer sets the normal last on every vertex
    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        nx = x;
        ny = y;
        nz = z;
        finish();
        return this;
    }
    //?} else {
    /*@Override
    public VertexConsumer vertex(double x, double y, double z) {
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
        return this;
    }

    @Override
    public VertexConsumer color(int r, int g, int b, int a) {
        cr = r;
        cg = g;
        cb = b;
        ca = a;
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        cu = u;
        cv = v;
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        lu = u;
        lv = v;
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        nx = x;
        ny = y;
        nz = z;
        return this;
    }

    @Override
    public void endVertex() {
        finish();
    }

    @Override
    public void defaultColor(int r, int g, int b, int a) {
        out.defaultColor(r, g, b, a);
    }

    @Override
    public void unsetDefaultColor() {
        out.unsetDefaultColor();
    }
    *///?}
}
