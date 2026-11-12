package com.workshopcraft.smolblocks.content;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/** Cell index is x | y << 1 | z << 2, each coordinate 0 or 1. */
public final class Cells {
    private static final VoxelShape[] SHAPES = new VoxelShape[256];

    private Cells() {}

    public static int index(int x, int y, int z) {
        return x | y << 1 | z << 2;
    }

    public static int x(int i) {
        return i & 1;
    }

    public static int y(int i) {
        return i >> 1 & 1;
    }

    public static int z(int i) {
        return i >> 2 & 1;
    }

    public static int coord(int i, Direction.Axis axis) {
        return switch (axis) {
            case X -> x(i);
            case Y -> y(i);
            case Z -> z(i);
        };
    }

    /** Neighbouring cell inside the same space, or -1 when the step leaves the space. */
    public static int neighbour(int i, Direction d) {
        int x = x(i) + d.getStepX(), y = y(i) + d.getStepY(), z = z(i) + d.getStepZ();
        if (x < 0 || x > 1 || y < 0 || y > 1 || z < 0 || z > 1) return -1;
        return index(x, y, z);
    }

    public static int mask(boolean spanX, boolean spanY, boolean spanZ, int cx, int cy, int cz) {
        int m = 0;
        for (int i = 0; i < 8; i++) {
            if ((spanX || x(i) == cx) && (spanY || y(i) == cy) && (spanZ || z(i) == cz)) m |= 1 << i;
        }
        return m;
    }

    public static VoxelShape shape(int mask) {
        VoxelShape s = SHAPES[mask & 255];
        if (s != null) return s;
        s = Shapes.empty();
        for (int i = 0; i < 8; i++) {
            if ((mask & 1 << i) == 0) continue;
            double x = x(i) * 0.5, y = y(i) * 0.5, z = z(i) * 0.5;
            s = Shapes.or(s, Shapes.box(x, y, z, x + 0.5, y + 0.5, z + 0.5));
        }
        s = s.optimize();
        SHAPES[mask & 255] = s;
        return s;
    }

    /** Splits a mask of one source block into as few pieces as a greedy pass finds. */
    public static List<Shape> decompose(int mask) {
        List<Shape> out = new ArrayList<>();
        for (Direction.Axis axis : Direction.Axis.values()) {
            for (int side = 0; side < 2; side++) {
                int plane = plane(axis, side);
                if ((mask & plane) == plane) {
                    out.add(axis == Direction.Axis.Y ? Shape.SLAB : Shape.VERTICAL_SLAB);
                    mask &= ~plane;
                }
            }
        }
        for (Direction.Axis axis : Direction.Axis.values()) {
            for (int i = 0; i < 8; i++) {
                if (coord(i, axis) != 0) continue;
                int line = mask(axis == Direction.Axis.X, axis == Direction.Axis.Y, axis == Direction.Axis.Z, x(i), y(i), z(i));
                if ((mask & line) == line) {
                    out.add(axis == Direction.Axis.Y ? Shape.VERTICAL_QUARTER : Shape.QUARTER);
                    mask &= ~line;
                }
            }
        }
        for (int i = 0; i < Integer.bitCount(mask); i++) out.add(Shape.EIGHTH);
        return out;
    }

    private static int plane(Direction.Axis axis, int side) {
        int m = 0;
        for (int i = 0; i < 8; i++) if (coord(i, axis) == side) m |= 1 << i;
        return m;
    }
}
