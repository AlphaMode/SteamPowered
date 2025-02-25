/*
 * Copyright (c) 2021 TeamMoeg
 *
 * This file is part of Steam Powered.
 *
 * Steam Powered is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Steam Powered is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Steam Powered. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.steampowered.content.burner;

import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.simibubi.create.lib.transfer.item.IItemHandler;
import com.simibubi.create.lib.transfer.item.ItemStackHandler;
import com.simibubi.create.lib.transfer.item.ItemTransferable;
import com.simibubi.create.lib.utility.BurnUtil;
import com.simibubi.create.lib.utility.LazyOptional;
import com.teammoeg.steampowered.SPConfig;
import org.jetbrains.annotations.Nullable;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BurnerTileEntity extends BlockEntity implements IHaveGoggleInformation, ItemTransferable {
    private ItemStackHandler inv = new ItemStackHandler() {

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (BurnUtil.getBurnTime(stack) != 0) return true;
            return false;
        }

    };
    int HURemain;
    private LazyOptional<IItemHandler> holder = LazyOptional.of(() -> inv);

    public BurnerTileEntity(BlockEntityType<?> p_i48289_1_, BlockPos pos, BlockState state) {
        super(p_i48289_1_, pos, state);
    }

    // Easy, easy
    public void readCustomNBT(CompoundTag nbt) {
        inv.deserializeNBT(nbt.getCompound("inv"));
        HURemain = nbt.getInt("hu");
    }

    // Easy, easy
    public void writeCustomNBT(CompoundTag nbt) {
        nbt.put("inv", inv.serializeNBT());
        nbt.putInt("hu", HURemain);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        readCustomNBT(nbt);
    }

    @Override
    public void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        writeCustomNBT(nbt);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        CompoundTag nbt = new CompoundTag();
        this.writeCustomNBT(nbt);
        return ClientboundBlockEntityDataPacket.create(this, (blockEntity -> nbt));
    }

//    @Override
//    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
//        this.readCustomNBT(pkt.getTag());
//    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag nbt = super.getUpdateTag();
        writeCustomNBT(nbt);
        return nbt;
    }

    @Nullable
    @Override
    public IItemHandler getItemHandler(@Nullable Direction direction) {
        if (!this.holder.isPresent()) {
            this.refreshCapability();
        }
        return holder.resolve().get();
    }

    private void refreshCapability() {
        LazyOptional<IItemHandler> oldCap = this.holder;
        this.holder = LazyOptional.of(() -> {
            return this.inv;
        });
        oldCap.invalidate();
    }

    public void tick() {
        if (level != null && !level.isClientSide) {
            BlockState state = this.level.getBlockState(this.worldPosition);
            int emit = getHuPerTick();
            while (HURemain < emit && this.consumeFuel()) ;
            if (HURemain < emit) {
                if (HURemain > 0) {
                    emitHeat(HURemain);
                    HURemain = 0;
                }
                this.level.setBlockAndUpdate(this.worldPosition, state.setValue(BurnerBlock.LIT, false));
            } else {
                HURemain -= emit;
                emitHeat(emit);
                this.level.setBlockAndUpdate(this.worldPosition, state.setValue(BurnerBlock.LIT, true));
            }
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
            this.setChanged();
        }
    }

    private boolean consumeFuel() {
    	if(this.getBlockState().getValue(BurnerBlock.REDSTONE_LOCKED))return false;
        int time = BurnUtil.getBurnTime(inv.getStackInSlot(0));
        if (time <= 0) return false;
        inv.getStackInSlot(0).shrink(1);
        HURemain += time * SPConfig.COMMON.HUPerFuelTick.get()*getEfficiency();//2.4HU/t

        return true;
    }

    protected void emitHeat(float value) {
        BlockEntity receiver = level.getBlockEntity(this.getBlockPos().above());
        if (receiver instanceof IHeatReceiver) {
            ((IHeatReceiver) receiver).commitHeat(value);
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(componentSpacing.plainCopy().append(new TranslatableComponent("tooltip.steampowered.burner.hu", HURemain).withStyle(ChatFormatting.GOLD)));
        if(!inv.getStackInSlot(0).isEmpty())
        tooltip.add(componentSpacing.plainCopy().append(new TranslatableComponent("tooltip.steampowered.burner.item", inv.getStackInSlot(0).getCount(), inv.getStackInSlot(0).getItem().getName(inv.getStackInSlot(0))).withStyle(ChatFormatting.GRAY)));
        return true;
    }

    /*
     * HU per tick max, 10HU=1 steam
     * */
    protected abstract int getHuPerTick();
    protected abstract double getEfficiency();
}
