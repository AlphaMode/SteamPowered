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

import com.simibubi.create.foundation.block.BlockStressValues;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.teammoeg.steampowered.client.Particles;
import com.teammoeg.steampowered.registrate.SPBlocks;
import com.teammoeg.steampowered.registrate.SPItems;
import com.teammoeg.steampowered.registrate.SPTiles;
import com.tterrag.registrate.util.NonNullLazyValue;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraftforge.api.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class SteamPowered implements ModInitializer {

    public static final String MODID = "steampowered";

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MODID, path);
    }

    public static final CreativeModeTab itemGroup = FabricItemGroupBuilder.build(rl(MODID), () -> new ItemStack(SPBlocks.STEEL_FLYWHEEL.get()));

    public static final NonNullLazyValue<CreateRegistrate> registrate = CreateRegistrate.lazy(MODID);

    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void onInitialize() {
        FluidRegistry.register();
        BlockRegistry.register();
        ItemRegistry.register();
        Particles.register();
        SPBlocks.register();
        SPTiles.register();
        SPItems.register();
        SPTags.init();
        registrate.get().register();
        BlockStressValues.registerProvider(MODID,new SPStress());
        ModLoadingContext.registerConfig(MODID, ModConfig.Type.COMMON, SPConfig.COMMON_CONFIG);
        ModLoadingContext.registerConfig(MODID, ModConfig.Type.SERVER, SPConfig.SERVER_CONFIG);
    }
}
