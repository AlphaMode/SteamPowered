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
import java.util.Random;

import com.simibubi.create.foundation.item.ItemDescription.Palette;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.lib.transfer.fluid.FluidStack;
import com.teammoeg.steampowered.client.ClientUtils;
import com.teammoeg.steampowered.client.Particles;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

public abstract class BoilerBlock extends Block implements SimpleWaterloggedBlock, EntityBlock {
	@Override
	public boolean canPlaceLiquid(BlockGetter w, BlockPos p, BlockState s, Fluid f) {
		BlockEntity te = w.getBlockEntity(p);
		if (te instanceof BoilerTileEntity) {
			BoilerTileEntity boiler = (BoilerTileEntity) te;
			if (boiler.input.fill(new FluidStack(f, 1000), true) == 1000)
				return true;
		}
		return false;
	}

	@Override
	public boolean placeLiquid(LevelAccessor w, BlockPos p, BlockState s, FluidState f) {
		BlockEntity te = w.getBlockEntity(p);
		if (te instanceof BoilerTileEntity) {
			BoilerTileEntity boiler = (BoilerTileEntity) te;
			if (boiler.input.fill(new FluidStack(f.getType(), 1000), true) == 1000) {
				boiler.input.fill(new FluidStack(f.getType(), 1000), false);
				return true;
			}
		}
		return false;
	}

	@Override
	public void animateTick(BlockState p_180655_1_, Level p_180655_2_, BlockPos p_180655_3_, Random p_180655_4_) {
		BlockEntity te = p_180655_2_.getBlockEntity(p_180655_3_);
		if (te instanceof BoilerTileEntity) {
			BoilerTileEntity boiler = (BoilerTileEntity) te;
			if (boiler.output.getFluidAmount()>=10000&&boiler.lastheat!=0) {//steam leaking
				double d0 = p_180655_3_.getX();
				double d1 = p_180655_3_.getY() + 1;
				double d2 = p_180655_3_.getZ();
				//if(p_180655_4_.nextDouble()<0.5D) {
					p_180655_2_.playLocalSound(d0, d1, d2, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.25F, 0.25F, false);
					int count=4;
					while(--count!=0)
					p_180655_2_.addParticle(Particles.STEAM, d0+p_180655_4_.nextFloat(), d1, d2+p_180655_4_.nextFloat(), 0.0D, 0.0D, 0.0D);
				//}
			}
		}
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState p_149740_1_) {
		return true;
	}

	public BoilerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public int getAnalogOutputSignal(BlockState b, Level w, BlockPos p) {
		BlockEntity te = w.getBlockEntity(p);
		if (te instanceof BoilerTileEntity) {
			BoilerTileEntity boiler = (BoilerTileEntity) te;
			return (int) (boiler.output.getFluidAmount() * 15 / boiler.output.getCapacity());
		}
		return super.getAnalogOutputSignal(b, w, p);
	}

	public abstract int getHuConsume();

	@Override
	public void appendHoverText(ItemStack i, BlockGetter w, List<Component> t, TooltipFlag f) {
		if (Screen.hasShiftDown()) {
			t.add(new TranslatableComponent("tooltip.steampowered.boiler.brief").withStyle(ChatFormatting.GOLD));
			if (ClientUtils.hasGoggles()) {
				t.add(new TranslatableComponent("tooltip.steampowered.boiler.danger").withStyle(ChatFormatting.RED));
				t.add(new TranslatableComponent("tooltip.steampowered.boiler.huconsume", this.getHuConsume())
						.withStyle(ChatFormatting.GOLD));
				t.add(new TranslatableComponent("tooltip.steampowered.boiler.waterconsume",
						((int) Math.ceil(this.getHuConsume() / 120.0))).withStyle(ChatFormatting.AQUA));
				t.add(new TranslatableComponent("tooltip.steampowered.boiler.steamproduce", this.getHuConsume() / 10)
						.withStyle(ChatFormatting.GOLD));
			}
		} else {
			t.add(TooltipHelper.holdShift(Palette.Gray, false));
		}
		if (Screen.hasControlDown()) {
			t.add(new TranslatableComponent("tooltip.steampowered.boiler.redstone").withStyle(ChatFormatting.RED));
		} else {
			t.add(Lang
					.translate("tooltip.holdForControls",
							Lang.translate("tooltip.keyCtrl").withStyle(ChatFormatting.GRAY))
					.withStyle(ChatFormatting.DARK_GRAY));
		}
		super.appendHoverText(i, w, t, f);
	}

	@Override
	public void stepOn(Level w, BlockPos bp, BlockState s, Entity e) {
		BlockEntity te = w.getBlockEntity(bp);
		if (te instanceof BoilerTileEntity && e instanceof LivingEntity) {
			if (((BoilerTileEntity) te).lastheat > 0 || (!((BoilerTileEntity) te).output.isEmpty())) {
				e.hurt(DamageSource.HOT_FLOOR, 2);
			}
		}
	}
}
