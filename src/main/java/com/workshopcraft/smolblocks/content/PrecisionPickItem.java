package com.workshopcraft.smolblocks.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
//? if >=1.21.2 {
import net.minecraft.world.item.Item;
//?} else {
/*import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
*///?}

/** A pickaxe that takes a micro block space apart one piece at a time. On anything else it is a plain pickaxe. */
//? if >=1.21.2 {
public class PrecisionPickItem extends Item {
    public PrecisionPickItem(Properties properties) {
        super(properties);
    }
//?} else if >=1.21 {
/*public class PrecisionPickItem extends PickaxeItem {
    public PrecisionPickItem(Tier tier, Properties properties) {
        super(tier, properties.attributes(PickaxeItem.createAttributes(tier, 1.0F, -2.8F)));
    }
*///?} else {
/*public class PrecisionPickItem extends PickaxeItem {
    public PrecisionPickItem(Tier tier, Properties properties) {
        super(tier, 1, -2.8F, properties);
    }
*///?}

    public static boolean held(Player player) {
        return player.getMainHandItem().getItem() instanceof PrecisionPickItem;
    }

    /** The filled cell the player is looking at in a space, or -1. */
    public static int aimedCell(Player player, BlockPos pos) {
        //? if >=1.20.5 {
        double reach = player.blockInteractionRange();
        //?} else {
        /*double reach = player.isCreative() ? 5.0 : 4.5;
        *///?}
        return cellAt(pos, player.pick(reach + 1.0, 1.0F, false));
    }

    /** The cell a hit on this space lands in, nudged inside along the hit face, or -1 for a hit elsewhere. */
    public static int cellAt(BlockPos pos, @Nullable HitResult hit) {
        if (!(hit instanceof BlockHitResult bh) || hit.getType() != HitResult.Type.BLOCK || !bh.getBlockPos().equals(pos)) return -1;
        Direction d = bh.getDirection();
        Vec3 p = bh.getLocation().subtract(d.getStepX() * 0.001, d.getStepY() * 0.001, d.getStepZ() * 0.001)
                .subtract(pos.getX(), pos.getY(), pos.getZ());
        return Cells.index(p.x >= 0.5 ? 1 : 0, p.y >= 0.5 ? 1 : 0, p.z >= 0.5 ? 1 : 0);
    }

    /**
     * Breaks only the aimed piece of a space with more than one piece in it. Returns true when the normal
     * break must not go ahead; the last piece breaks the usual way, leaving air or water. The client only
     * holds back its own prediction and waits for the server.
     */
    public static boolean breakPiece(Level level, BlockPos pos, Player player) {
        if (!held(player) || !(level.getBlockEntity(pos) instanceof MicroBlockEntity be) || Integer.bitCount(be.mask()) <= 1) return false;
        if (level.isClientSide()) return true;
        int cell = aimedCell(player, pos);
        BlockState source = cell < 0 ? null : be.cell(cell);
        if (source == null) return true;
        ItemStack tool = player.getMainHandItem();
        BlockState space = level.getBlockState(pos);
        if (!player.isCreative() && level instanceof ServerLevel server && MicroBlock.wouldDrop(source, player, server, pos, tool)) {
            Block.popResource(level, pos, PieceItem.create(source, Shape.EIGHTH, 1));
        }
        level.levelEvent(2001, pos, Block.getId(source));
        if (!player.isCreative()) tool.mineBlock(level, space, pos, player);
        be.clear(cell);
        return true;
    }
}
