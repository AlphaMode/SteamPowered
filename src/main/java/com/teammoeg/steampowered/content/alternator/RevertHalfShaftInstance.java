package com.teammoeg.steampowered.content.alternator;

import com.jozufozu.flywheel.api.MaterialManager;
import com.simibubi.create.content.contraptions.base.HalfShaftInstance;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class RevertHalfShaftInstance extends HalfShaftInstance {

	public RevertHalfShaftInstance(MaterialManager modelManager, KineticTileEntity tile) {
		super(modelManager, tile);
	}
	@Override
    protected Direction getShaftDirection() {
        return blockState.getValue(BlockStateProperties.FACING).getOpposite();
    }

}
