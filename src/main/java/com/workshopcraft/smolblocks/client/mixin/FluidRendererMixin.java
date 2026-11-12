package com.workshopcraft.smolblocks.client.mixin;

import com.workshopcraft.smolblocks.client.ClientCells;
import com.workshopcraft.smolblocks.client.WaterTrim;
import com.workshopcraft.smolblocks.content.MicroBlock;
import com.workshopcraft.smolblocks.content.MicroBlockEntity;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FluidRenderer.class)
abstract class FluidRendererMixin {
    @Inject(method = "tesselate", at = @At("HEAD"), cancellable = true)
    private void smolblocks$trimWater(BlockAndTintGetter level, BlockPos pos, FluidRenderer.Output output, BlockState state, FluidState fluid, CallbackInfo ci) {
        if (output instanceof WaterTrim || !(state.getBlock() instanceof MicroBlock)) return;
        MicroBlockEntity.CellStates cells = ClientCells.get(level, pos);
        int mask = cells == null ? 0 : cells.mask();
        if (mask == 0) return;
        ((FluidRenderer) (Object) this).tesselate(level, pos, new WaterTrim(output, mask, pos), state, fluid);
        ci.cancel();
    }
}
