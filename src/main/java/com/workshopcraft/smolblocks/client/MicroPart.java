package com.workshopcraft.smolblocks.client;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record MicroPart(QuadCollection quads, Material.Baked particleMaterial) implements BlockStateModelPart {
    @Override
    public List<BakedQuad> getQuads(@Nullable Direction direction) {
        return quads.getQuads(direction);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public int materialFlags() {
        return quads.materialFlags();
    }
}
