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

package com.teammoeg.steampowered.client;

import com.teammoeg.steampowered.FluidRegistry;
import com.teammoeg.steampowered.block.SPBlockPartials;
import com.teammoeg.steampowered.ponder.SPPonderIndex;
import com.teammoeg.steampowered.registrate.SPBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

import net.minecraft.client.renderer.RenderType;

public class SteamPoweredClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SPBlockPartials.clientInit();
        SPPonderIndex.register();
        setupRenderType();
        registerParticleFactories();
    }

    public static void registerParticleFactories() {
        ParticleFactoryRegistry.getInstance().register(Particles.STEAM, SteamParticle.Factory::new);
    }
    public static void setupRenderType() {
        BlockRenderLayerMap.INSTANCE.putFluid(FluidRegistry.steam, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putFluid(FluidRegistry.steamFlowing, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(SPBlocks.DYNAMO.get(), RenderType.cutoutMipped());
    }
}
