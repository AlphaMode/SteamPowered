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

package com.teammoeg.steampowered.content.engine;

import com.simibubi.create.content.contraptions.components.flywheel.FlywheelBlock;
import com.simibubi.create.content.contraptions.components.flywheel.FlywheelTileEntity;
import com.simibubi.create.content.contraptions.components.flywheel.engine.EngineBlock;
import com.simibubi.create.content.contraptions.components.flywheel.engine.EngineTileEntity;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.simibubi.create.lib.transfer.TransferUtil;
import com.simibubi.create.lib.transfer.fluid.FluidTank;
import com.simibubi.create.lib.transfer.fluid.FluidTransferable;
import com.simibubi.create.lib.transfer.fluid.IFluidHandler;
import com.simibubi.create.lib.utility.LazyOptional;
import com.teammoeg.steampowered.FluidRegistry;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public abstract class SteamEngineTileEntity extends EngineTileEntity implements IHaveGoggleInformation, FluidTransferable {

    private FluidTank tank;
    private LazyOptional<IFluidHandler> holder = LazyOptional.of(() -> tank);
    private int heatup=0;
    public SteamEngineTileEntity(BlockEntityType<? extends SteamEngineTileEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.refreshCapability();
        this.tank = new FluidTank(this.getSteamStorage(), fluidStack -> {
            Tag<Fluid> steamTag = FluidTags.getAllTags().getTag(new ResourceLocation("c", "steam"));
            if (steamTag != null) return fluidStack.getFluid().is(steamTag);
            else return fluidStack.getFluid() == FluidRegistry.steam;
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (level != null && !level.isClientSide) {
            BlockState state = this.level.getBlockState(this.worldPosition);
            if (!tank.isEmpty()&&tank.drain(this.getSteamConsumptionPerTick(), false).getAmount() >= this.getSteamConsumptionPerTick()) {
                this.level.setBlockAndUpdate(this.worldPosition, state.setValue(SteamEngineBlock.LIT, true));
                if(heatup>=60) {
                    this.appliedCapacity = this.getGeneratingCapacity();
                    this.appliedSpeed = this.getGeneratingSpeed();
                    this.refreshWheelSpeed();
                }else
                	heatup++;
            }else {
	        	if(heatup>0)
	        		heatup--;
	            this.level.setBlockAndUpdate(this.worldPosition, state.setValue(SteamEngineBlock.LIT, false));
	            this.appliedCapacity = 0;
	            this.appliedSpeed = 0;
	            this.refreshWheelSpeed();
            }
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (tank.isEmpty() || tank.getFluidAmount() < this.getSteamConsumptionPerTick()) {
            tooltip.add(componentSpacing.plainCopy().append(new TranslatableComponent("tooltip.steampowered.steam_engine.not_enough_steam").withStyle(ChatFormatting.RED)));
        }else if(heatup<20) {
        	tooltip.add(componentSpacing.plainCopy().append(new TranslatableComponent("tooltip.steampowered.steam_engine.heating").withStyle(ChatFormatting.YELLOW)));
        } else {
            tooltip.add(componentSpacing.plainCopy().append(new TranslatableComponent("tooltip.steampowered.steam_engine.running").withStyle(ChatFormatting.GREEN)));
        }
        return this.containedFluidTooltip(tooltip, isPlayerSneaking, TransferUtil.getFluidHandler(this));
    }

    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        tank.readFromNBT(compound.getCompound("TankContent"));
        heatup=compound.getInt("heatup");
    }

    public void write(CompoundTag compound, boolean clientPacket) {
        compound.put("TankContent", tank.writeToNBT(new CompoundTag()));
        compound.putInt("heatup",heatup);
        super.write(compound, clientPacket);
    }

    @Nonnull
    @Override
    public IFluidHandler getFluidHandler(@Nullable Direction direction) {
        if (!this.holder.isPresent()) {
            this.refreshCapability();
        }
        return TransferUtil.getFluidHandler(this).resolve().get();
    }

    private void refreshCapability() {
        LazyOptional<IFluidHandler> oldCap = this.holder;
        this.holder = LazyOptional.of(() -> {
            return this.tank;
        });
        oldCap.invalidate();
    }

    public void attachWheel() {
        Direction engineFacing = (Direction) this.getBlockState().getValue(EngineBlock.FACING);
        BlockPos wheelPos = this.worldPosition.relative(engineFacing, 2);
        BlockState wheelState = this.level.getBlockState(wheelPos);
        if (this.getFlywheel() == wheelState.getBlock()) {
            Direction wheelFacing = (Direction) wheelState.getValue(FlywheelBlock.HORIZONTAL_FACING);
            if (wheelFacing.getAxis() == engineFacing.getClockWise().getAxis()) {
                if (!FlywheelBlock.isConnected(wheelState) || FlywheelBlock.getConnection(wheelState) == engineFacing.getOpposite()) {
                    BlockEntity te = this.level.getBlockEntity(wheelPos);
                    if (!te.isRemoved()) {
                        if (te instanceof FlywheelTileEntity) {
                            if (!FlywheelBlock.isConnected(wheelState)) {
                                FlywheelBlock.setConnection(this.level, te.getBlockPos(), te.getBlockState(), engineFacing.getOpposite());
                            }

                            this.poweredWheel = (FlywheelTileEntity) te;
                            this.refreshWheelSpeed();
                        }

                    }
                }
            }
        }
    }

    public abstract Block getFlywheel();

    public abstract float getGeneratingCapacity();

    public abstract float getGeneratingSpeed();

    public abstract int getSteamConsumptionPerTick();

    public abstract int getSteamStorage();
}
