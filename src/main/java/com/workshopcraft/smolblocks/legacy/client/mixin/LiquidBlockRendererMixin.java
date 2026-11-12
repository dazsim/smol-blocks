package com.workshopcraft.smolblocks.legacy.client.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.workshopcraft.smolblocks.content.MicroBlock;
import com.workshopcraft.smolblocks.content.MicroBlockEntity;
import com.workshopcraft.smolblocks.legacy.client.LegacyCells;
import com.workshopcraft.smolblocks.legacy.client.LegacyWaterTrim;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LiquidBlockRenderer.class)
abstract class LiquidBlockRendererMixin {
    @Inject(method = "tesselate", at = @At("HEAD"), cancellable = true)
    private void smolblocks$trimWater(BlockAndTintGetter level, BlockPos pos, VertexConsumer consumer, BlockState state, FluidState fluid, CallbackInfo ci) {
        if (consumer instanceof LegacyWaterTrim || !(state.getBlock() instanceof MicroBlock)) return;
        MicroBlockEntity.CellStates cells = LegacyCells.get(level, pos);
        int mask = cells == null ? 0 : cells.mask();
        if (mask == 0) return;
        ((LiquidBlockRenderer) (Object) this).tesselate(level, pos, new LegacyWaterTrim(consumer, mask, pos), state, fluid);
        ci.cancel();
    }
}
