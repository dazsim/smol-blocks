package com.workshopcraft.smolblocks.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
//? if neoforge {
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;
//?}

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MicroBlockEntity extends BlockEntity {
    //? if neoforge
    public static final ModelProperty<CellStates> CELLS = new ModelProperty<>();

    private final BlockState[] cells = new BlockState[8];

    public MicroBlockEntity(BlockPos pos, BlockState state) {
        super(SmolContent.MICRO_ENTITY, pos, state);
    }

    public @Nullable BlockState cell(int i) {
        return cells[i];
    }

    public int mask() {
        int m = 0;
        for (int i = 0; i < 8; i++) if (cells[i] != null) m |= 1 << i;
        return m;
    }

    public CellStates snapshot() {
        return new CellStates(cells.clone());
    }

    public void fill(int mask, BlockState source) {
        for (int i = 0; i < 8; i++) if ((mask & 1 << i) != 0) cells[i] = source;
        changed();
    }

    /** First filled cell, used for sounds, particles and hardness. */
    public @Nullable BlockState primary() {
        for (BlockState s : cells) if (s != null) return s;
        return null;
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        List<BlockState> list = new ArrayList<>(8);
        for (BlockState s : cells) list.add(s == null ? Blocks.AIR.defaultBlockState() : s);
        output.store("cells", BlockState.CODEC.listOf(), list);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        Arrays.fill(cells, null);
        List<BlockState> list = input.read("cells", BlockState.CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < Math.min(8, list.size()); i++) cells[i] = list.get(i).isAir() ? null : list.get(i);
        if (level != null && level.isClientSide()) {
            //? if neoforge
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    //? if neoforge {
    @Override
    public ModelData getModelData() {
        return ModelData.of(CELLS, snapshot());
    }
    //?}

    public record CellStates(BlockState[] states) {
        @Override
        public boolean equals(Object o) {
            return o instanceof CellStates c && Arrays.equals(states, c.states);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(states);
        }
    }
}
