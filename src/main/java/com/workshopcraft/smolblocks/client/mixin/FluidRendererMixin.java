package com.workshopcraft.smolblocks.client.mixin;

import com.workshopcraft.smolblocks.client.ClientCells;
import com.workshopcraft.smolblocks.client.HiddenWater;
import com.workshopcraft.smolblocks.client.WaterTrim;
import com.workshopcraft.smolblocks.content.Cells;
import com.workshopcraft.smolblocks.content.MicroBlock;
import com.workshopcraft.smolblocks.content.MicroBlockEntity;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
        if (output instanceof WaterTrim) return;
        boolean micro = state.getBlock() instanceof MicroBlock;
        MicroBlockEntity.CellStates cells = micro ? ClientCells.get(level, pos) : null;
        int mask = cells == null ? 0 : cells.mask(), opaque = cells == null ? 0 : cells.opaque();
        if (micro && mask == 0) return;
        FluidRenderer renderer = (FluidRenderer) (Object) this;
        // Faces against water in the next space are skipped by the renderer, but are needed where see-through pieces sit between
        for (Direction d : Direction.values()) {
            BlockPos next = pos.relative(d);
            BlockState nextState = level.getBlockState(next);
            if (!nextState.getFluidState().getType().isSame(fluid.getType())) continue;
            MicroBlockEntity.CellStates nextCells = nextState.getBlock() instanceof MicroBlock ? ClientCells.get(level, next) : null;
            int seeThrough = nextCells == null ? 0 : nextCells.mask() & ~nextCells.opaque() & Cells.face(d.getOpposite());
            if (seeThrough == 0 && (mask & ~opaque & Cells.face(d)) == 0) continue;
            renderer.tesselate(new HiddenWater(level, next), pos, new WaterTrim(output, mask, opaque, pos, d, seeThrough), state, fluid);
        }
        if (!micro) return;
        renderer.tesselate(level, pos, new WaterTrim(output, mask, opaque, pos), state, fluid);
        ci.cancel();
    }
}
