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

package com.teammoeg.steampowered.content.boiler;

import java.util.List;

import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.simibubi.create.lib.transfer.fluid.FluidStack;
import com.simibubi.create.lib.transfer.fluid.FluidTank;
import com.simibubi.create.lib.transfer.fluid.FluidTransferable;
import com.simibubi.create.lib.transfer.fluid.IFluidHandler;
import com.simibubi.create.lib.utility.LazyOptional;
import com.teammoeg.steampowered.FluidRegistry;
import com.teammoeg.steampowered.SPConfig;
import com.teammoeg.steampowered.content.burner.IHeatReceiver;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import org.jetbrains.annotations.Nullable;

public abstract class BoilerTileEntity extends BlockEntity implements IHeatReceiver, IHaveGoggleInformation, FluidTransferable {
    FluidTank input = new FluidTank(10000, s->s.getFluid() == Fluids.WATER);
    FluidTank output = new FluidTank(10000);
    private IFluidHandler ft = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            switch (tank) {
                case 0:
                    return input.getFluid();
                case 1:
                    return output.getFluid();
                default:
                    return null;
            }
        }

        @Override
        public long getTankCapacity(int tank) {
            return 10000;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank == 0 && stack.getFluid() == Fluids.WATER)
                return true;
            return false;
        }

        @Override
        public long fill(FluidStack resource, boolean action) {
            return input.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean action) {
            return output.drain(resource, action);
        }

        @Override
        public FluidStack drain(long maxDrain, boolean action) {
            return output.drain(maxDrain, action);
        }
    };
    int heatreceived;
    int lastheat;
    private LazyOptional<IFluidHandler> holder = LazyOptional.of(() -> ft);

    public BoilerTileEntity(BlockEntityType<?> p_i48289_1_, BlockPos pos, BlockState state) {
        super(p_i48289_1_, pos, state);
    }

    // Easy, easy
    public void readCustomNBT(CompoundTag nbt) {
        input.readFromNBT(nbt.getCompound("in"));
        output.readFromNBT(nbt.getCompound("out"));
        heatreceived = nbt.getInt("hu");
        lastheat=nbt.getInt("lasthu");
    }

    // Easy, easy
    public void writeCustomNBT(CompoundTag nbt) {
        nbt.put("in", input.writeToNBT(new CompoundTag()));
        nbt.put("out", output.writeToNBT(new CompoundTag()));
        nbt.putInt("hu", heatreceived);
        nbt.putInt("lasthu", lastheat);
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

    public void tick() {
    	
    	
        //debug
        if (this.level != null && !this.level.isClientSide) {
        	boolean flag=false;
        	if(lastheat!=heatreceived)
        		flag=true;
        	lastheat=heatreceived;
        	if(heatreceived!=0) {
	            int consume = Math.min(getHUPerTick(), heatreceived);
	            heatreceived = 0;
	            double waterconsume=(SPConfig.COMMON.steamPerWater.get()*10);
	            consume =  Math.min((int)(this.input.drain((int) Math.ceil(consume / waterconsume), false).getAmount() * waterconsume), consume);
	            this.output.fill(new FluidStack(FluidRegistry.steam.getSource(), consume / 10), false);
	            flag=true;
        	}
        	this.setChanged();
        	this.level.sendBlockUpdated(this.getBlockPos(),this.getBlockState(),this.getBlockState(), 3);

        }
    }

    @Override
    public void commitHeat(float value) {
        heatreceived = (int) value;

    }

    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        this.containedFluidTooltip(tooltip, isPlayerSneaking, LazyOptional.of(() -> input));
        this.containedFluidTooltip(tooltip, isPlayerSneaking, LazyOptional.of(() -> output));
        return true;
    }

    protected abstract int getHUPerTick();

    @Nullable
    @Override
    public IFluidHandler getFluidHandler(@Nullable Direction direction) {
        if (!this.holder.isPresent()) {
            this.refreshCapability();
        }
        return holder.resolve().get();
    }

    private void refreshCapability() {
        LazyOptional<IFluidHandler> oldCap = this.holder;
        this.holder = LazyOptional.of(() -> {
            return this.ft;
        });
        oldCap.invalidate();
    }
}
