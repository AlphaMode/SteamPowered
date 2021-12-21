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

package com.teammoeg.steampowered.content.alternator;

import java.util.List;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.lib.utility.LazyOptional;
import com.teammoeg.steampowered.SPConfig;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Adapted from: Create: Crafts & Additions under the MIT License
 * @author MRH0
 * @author yuesha-yc
 */
public class DynamoTileEntity extends KineticTileEntity {

    public InternalEnergyStorage energy;
    private LazyOptional<EnergyStorage> lazyEnergy;
    private boolean redstoneLocked = false;
    
    public static final int MAX_FE_OUT = SPConfig.COMMON.dynamoFeMaxOut.get(); // FE Output
    public static final int FE_CAPACITY = SPConfig.COMMON.dynamoFeCapacity.get(); // FE Storage
    public static final int IMPACT = SPConfig.COMMON.dynamoImpact.get(); // Impact on network
    public static final double EFFICIENCY = SPConfig.COMMON.dynamoEfficiency.get(); // Efficiency

    public DynamoTileEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        energy = new InternalEnergyStorage(FE_CAPACITY, 0, MAX_FE_OUT);
        lazyEnergy = LazyOptional.of(() -> energy);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (this.getBlockState().getValue(DynamoBlock.REDSTONE_LOCKED)) {
            tooltip.add(new TextComponent(spacing).append(new TranslatableComponent("tooltip.steampowered.dynamo.locked").withStyle(ChatFormatting.RED)));
            return true;
        }
		tooltip.add(new TextComponent(spacing).append(new TranslatableComponent("tooltip.steampowered.energy.production").withStyle(ChatFormatting.GRAY)));
		tooltip.add(new TextComponent(spacing).append(new TextComponent(" " + format(getEnergyProductionRate((int) (isSpeedRequirementFulfilled() ? getSpeed() : 0))) + "fe/t ") // fix
		        .withStyle(ChatFormatting.AQUA)).append(Lang.translate("gui.goggles.at_current_speed").withStyle(ChatFormatting.DARK_GRAY)));
		return super.addToGoggleTooltip(tooltip, isPlayerSneaking);
    }

    private static String format(int n) {
        if (n > 1000000)
            return Math.round(n / 100000d) / 10d + "M";
        if (n > 1000)
            return Math.round(n / 100d) / 10d + "K";
        return n + "";
    }

    @Override
    public float calculateStressApplied() {
        if (getBlockState().getValue(DynamoBlock.REDSTONE_LOCKED)) {
            this.lastStressApplied = 0;
            return 0;
        }
		this.lastStressApplied = IMPACT;
		return IMPACT;
    }

    public boolean isEnergyInput(Direction side) {
        return false;
    }

    public boolean isEnergyOutput(Direction side) {
        return side != getBlockState().getValue(DynamoBlock.FACING).getOpposite();
    }

    @Override
    public void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        energy.read(compound);
        redstoneLocked = compound.getBoolean("redstonelocked");
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        energy.write(compound);
        compound.putBoolean("redstonelocked", redstoneLocked);
    }

    private boolean firstTickState = true;

    @Override
    public void tick() {
        super.tick();
        if (level != null && level.isClientSide())
            return;

        if (this.getBlockState().getValue(DynamoBlock.REDSTONE_LOCKED))
            return;

        if (firstTickState)
            firstTick();
        firstTickState = false;

        if (Math.abs(getSpeed()) > 0 && isSpeedRequirementFulfilled())
            energy.internalProduceEnergy(getEnergyProductionRate((int) getSpeed()));

        for (Direction d : Direction.values()) {
            if (!isEnergyOutput(d))
                continue;
            EnergyStorage ies = getCachedEnergy(d);
            if (ies == null)
                continue;
            try (Transaction transaction = Transaction.openOuter()) {
                long ext = energy.extract(ies.insert(MAX_FE_OUT, transaction), transaction);
                ies.insert(ext, transaction);
                transaction.commit();
            }
        }
    }

    public static int getEnergyProductionRate(int rpm) {
        rpm = Math.abs(rpm);
        return (int) (Math.abs(rpm) * EFFICIENCY);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        lazyEnergy.invalidate();
    }

    public void firstTick() {
        updateCache();
    }

    public void updateCache() {
        if (level.isClientSide())
            return;
        for (Direction side : Direction.values()) {
            BlockEntity te = level.getBlockEntity(worldPosition.relative(side));
            if (te == null) {
                setCache(side, LazyOptional.empty());
                continue;
            }
//            LazyOptional<EnergyStorage> le = () -> EnergyStorage.SIDED.find(te.getLevel(), te.getBlockPos(), side.getOpposite());
//            setCache(side, le);
        }
    }

    private LazyOptional<EnergyStorage> escacheUp = LazyOptional.empty();
    private LazyOptional<EnergyStorage> escacheDown = LazyOptional.empty();
    private LazyOptional<EnergyStorage> escacheNorth = LazyOptional.empty();
    private LazyOptional<EnergyStorage> escacheEast = LazyOptional.empty();
    private LazyOptional<EnergyStorage> escacheSouth = LazyOptional.empty();
    private LazyOptional<EnergyStorage> escacheWest = LazyOptional.empty();

    public void setCache(Direction side, LazyOptional<EnergyStorage> storage) {
        switch (side) {
            case DOWN:
                escacheDown = storage;
                break;
            case EAST:
                escacheEast = storage;
                break;
            case NORTH:
                escacheNorth = storage;
                break;
            case SOUTH:
                escacheSouth = storage;
                break;
            case UP:
                escacheUp = storage;
                break;
            case WEST:
                escacheWest = storage;
                break;
        }
    }

    public EnergyStorage getCachedEnergy(Direction side) {
        switch (side) {
            case DOWN:
                return escacheDown.orElse(null);
            case EAST:
                return escacheEast.orElse(null);
            case NORTH:
                return escacheNorth.orElse(null);
            case SOUTH:
                return escacheSouth.orElse(null);
            case UP:
                return escacheUp.orElse(null);
            case WEST:
                return escacheWest.orElse(null);
        }
        return null;
    }
}
