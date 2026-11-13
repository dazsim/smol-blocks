package com.workshopcraft.smolblocks.content;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

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

    /** A piece taken out of a mask: its shape and the cells it covers. */
    public record Piece(Shape shape, int mask) {}

    private static final Direction.Axis[] ORDER = {Direction.Axis.Y, Direction.Axis.X, Direction.Axis.Z};

    /** Greedily splits a mask into the largest pieces, slabs first. */
    public static List<Piece> decompose(int mask) {
        List<Piece> out = new ArrayList<>();
        for (Direction.Axis axis : ORDER) {
            for (int side = 0; side < 2; side++) {
                int plane = plane(axis, side);
                if ((mask & plane) == plane) {
                    out.add(new Piece(axis == Direction.Axis.Y ? Shape.SLAB : Shape.VERTICAL_SLAB, plane));
                    mask &= ~plane;
                }
            }
        }
        for (Direction.Axis axis : ORDER) {
            for (int i = 0; i < 8; i++) {
                if (coord(i, axis) != 0) continue;
                int line = mask(axis == Direction.Axis.X, axis == Direction.Axis.Y, axis == Direction.Axis.Z, x(i), y(i), z(i));
                if ((mask & line) == line) {
                    out.add(new Piece(axis == Direction.Axis.Y ? Shape.VERTICAL_QUARTER : Shape.QUARTER, line));
                    mask &= ~line;
                }
            }
        }
        for (int i = 0; i < 8; i++) if ((mask & 1 << i) != 0) out.add(new Piece(Shape.EIGHTH, 1 << i));
        return out;
    }

    /** Cells touching one face of the space. */
    public static int face(Direction d) {
        return plane(d.getAxis(), d.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0);
    }

    /**
     * Where a piece of a water face in a cell goes, as a shift along the face axis, or NaN to drop it. A face on
     * the outside of a see-through filled cell moves in to the water cell behind it. With a side set, the face is
     * one the fluid renderer skips because the next space holds water too, so it only stays where it meets a
     * see-through piece of that space.
     */
    public static float waterShift(int mask, int opaque, @Nullable Direction side, int neighbour, int cell, int axis, float rel) {
        if ((mask & 1 << cell) != 0) {
            if ((opaque & 1 << cell) != 0 || (mask & 1 << (cell ^ 1 << axis)) != 0) return Float.NaN;
            return rel < 0.5F ? 0.5F : axis == 1 ? 0.499F - rel : -0.5F;
        }
        if (side == null) return 0.0F;
        // Still-water sides and bottom from the pass that hid the water above
        if (side == Direction.UP && axis != 1) return 0.0F;
        if ((neighbour & 1 << (cell ^ 1 << axis)) == 0) return Float.NaN;
        // Water above means this water is full height, which the renderer could not see
        return side == Direction.UP ? 0.999F - rel : 0.0F;
    }

    /** Whether a quad facing along an axis, at rel along it, lies on the given side of the space. */
    public static boolean onSide(Direction side, int axis, float rel) {
        return side.getAxis().ordinal() == axis && rel >= 0.5F == (side.getAxisDirection() == Direction.AxisDirection.POSITIVE);
    }

    private static int plane(Direction.Axis axis, int side) {
        int m = 0;
        for (int i = 0; i < 8; i++) if (coord(i, axis) == side) m |= 1 << i;
        return m;
    }
}
