/**
 * Available under MIT the license more info at: https://tldrlegal.com/license/mit-license
 *
 * MIT License
 *
 * Copyright 2021 MRH0
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction,
 * including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom t
 * he Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.teammoeg.steampowered.content.alternator;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Adapted from: Create: Crafts & Additions
 * @author MRH0
 */
public class InternalEnergyStorage extends SimpleEnergyStorage {
    private long capacity;

    public InternalEnergyStorage(int capacity) {
        super(capacity, capacity, capacity);
    }

    public InternalEnergyStorage(int capacity, int maxTransfer) {
        super(capacity, maxTransfer, maxTransfer);
    }

    public InternalEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    public InternalEnergyStorage(int capacity, int maxReceive, int maxExtract, int energy) {
        super(capacity, maxReceive, maxExtract);
        setEnergy(energy);
    }

    public CompoundTag write(CompoundTag nbt) {
        nbt.putLong("energy", amount);
        return nbt;
    }

    public void read(CompoundTag nbt) {
        setEnergy(nbt.getLong("energy"));
    }

    public CompoundTag write(CompoundTag nbt, String name) {
        nbt.putLong("energy_" + name, amount);
        return nbt;
    }

    public void read(CompoundTag nbt, String name) {
        setEnergy(nbt.getLong("energy_" + name));
    }

    @Override
    public boolean supportsExtraction() {
        return true;
    }

    @Override
    public boolean supportsInsertion() {
        return true;
    }

    public long internalConsumeEnergy(int consume) {
        long oenergy = amount;
        amount = Math.max(0, amount - consume);
        return oenergy - amount;
    }

    public long internalProduceEnergy(int produce) {
        long oenergy = amount;
        amount = Math.min(capacity, amount + produce);
        return oenergy - amount;
    }

    public void setEnergy(long energy) {
        this.amount = energy;
    }

    @Deprecated
    public void outputToSide(Level world, BlockPos pos, Direction side, int max) {
        BlockEntity te = world.getBlockEntity(pos.relative(side));
        if (te == null)
            return;
//        LazyOptional<EnergyStorage> opt = () -> EnergyStorage.SIDED.find(world, pos, side.getOpposite());
//        EnergyStorage ies = opt.orElse(null);
//        if (ies == null)
//            return;
        try (Transaction transaction = Transaction.openOuter()) {
            long ext = this.extract(max, transaction);
//            this.insert(ext - ies.insert(ext, transaction), transaction);
            transaction.commit();
        }
    }

    @Override
    public String toString() {
        return getAmount() + "/" + getCapacity();
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        return 0;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        return 0;
    }

    @Override
    public long getAmount() {
        return 0;
    }

    @Override
    public long getCapacity() {
        return 0;
    }
}
