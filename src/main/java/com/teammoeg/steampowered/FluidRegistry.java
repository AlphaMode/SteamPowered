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

package com.teammoeg.steampowered;

import com.tterrag.registrate.fabric.FluidData;
import com.tterrag.registrate.fabric.SimpleFlowableFluid;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;

public class FluidRegistry {
    public static final ResourceLocation STILL_STEAM_TEXTURE = new ResourceLocation(SteamPowered.MODID, "block/steam");
    public static final ResourceLocation FLOWING_STEAM_TEXTURE = new ResourceLocation(SteamPowered.MODID, "block/steam");

    public static FlowingFluid steam = Fluids.LAVA;//Registry.register(Registry.FLUID, SteamPowered.rl("steam"), new SimpleFlowableFluid.Still(FluidRegistry.PROPERTIES));
    public static FlowingFluid steamFlowing = Fluids.WATER;//Registry.register(Registry.FLUID, SteamPowered.rl("steam_flowing"), new SimpleFlowableFluid.Flowing(FluidRegistry.PROPERTIES));
    public static SimpleFlowableFluid.Properties PROPERTIES = new SimpleFlowableFluid.Properties(() -> steam, () -> steamFlowing, new FluidData.Builder());

    public static void register() {

    }
}
