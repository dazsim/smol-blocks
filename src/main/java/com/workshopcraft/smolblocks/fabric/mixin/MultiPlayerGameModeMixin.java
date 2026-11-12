package com.workshopcraft.smolblocks.fabric.mixin;

import com.workshopcraft.smolblocks.content.PrecisionPickItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps the client from removing a whole space while the server breaks one piece of it. */
@Mixin(MultiPlayerGameMode.class)
abstract class MultiPlayerGameModeMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void smolblocks$keepSpace(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (minecraft.player != null && minecraft.level != null && PrecisionPickItem.breakPiece(minecraft.level, pos, minecraft.player)) cir.setReturnValue(false);
    }
}
