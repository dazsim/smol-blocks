package com.workshopcraft.smolblocks.neoforge.mixin;

import com.workshopcraft.smolblocks.neoforge.WaterTrim;
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
        MicroBlockEntity.CellStates cells = level.getModelData(pos).get(MicroBlockEntity.CELLS);
        int mask = 0;
        if (cells != null) {
            for (int i = 0; i < 8; i++) if (cells.states()[i] != null) mask |= 1 << i;
        } else if (level.getBlockEntity(pos) instanceof MicroBlockEntity be) {
            mask = be.mask();
        }
        if (mask == 0) return;
        ((FluidRenderer) (Object) this).tesselate(level, pos, new WaterTrim(output, mask, pos), state, fluid);
        ci.cancel();
    }
}
