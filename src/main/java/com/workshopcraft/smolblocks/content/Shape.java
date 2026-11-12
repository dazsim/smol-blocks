package com.workshopcraft.smolblocks.content;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * Piece shapes in a canonical frame. Spans say which axes the piece fills completely;
 * placement picks the cell on the other axes and turns horizontal pieces to face the player.
 */
public enum Shape implements StringRepresentable {
    SLAB("slab", 4, true, false, true),
    VERTICAL_SLAB("vertical_slab", 4, true, true, false),
    QUARTER("quarter", 2, true, false, false),
    VERTICAL_QUARTER("vertical_quarter", 2, false, true, false),
    EIGHTH("eighth", 1, false, false, false);

    public static final Codec<Shape> CODEC = StringRepresentable.fromEnum(Shape::values);
    public static final StreamCodec<ByteBuf, Shape> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Shape::ordinal);

    private final String name;
    public final int cells;
    public final boolean spanX, spanY, spanZ;

    Shape(String name, int cells, boolean spanX, boolean spanY, boolean spanZ) {
        this.name = name;
        this.cells = cells;
        this.spanX = spanX;
        this.spanY = spanY;
        this.spanZ = spanZ;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** Pieces from cutting a full block (shape null) or this shape. Horizontal means the saw sat above or below. */
    public static Shape cut(Shape from, boolean horizontal) {
        if (from == null) return horizontal ? SLAB : VERTICAL_SLAB;
        return switch (from) {
            case SLAB -> QUARTER;
            case VERTICAL_SLAB -> horizontal ? QUARTER : VERTICAL_QUARTER;
            case QUARTER, VERTICAL_QUARTER -> EIGHTH;
            case EIGHTH -> null;
        };
    }

    /** Cell mask in the canonical frame, sitting at the low corner. */
    public int canonicalMask() {
        return Cells.mask(spanX, spanY, spanZ, 0, 0, 0);
    }
}
