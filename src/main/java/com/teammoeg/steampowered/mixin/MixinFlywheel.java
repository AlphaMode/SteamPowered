package com.teammoeg.steampowered.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.KineticNetwork;
import com.simibubi.create.content.contraptions.base.GeneratingKineticTileEntity;
import com.simibubi.create.content.contraptions.components.flywheel.FlywheelBlock;
import com.simibubi.create.content.contraptions.components.flywheel.FlywheelTileEntity;
import com.simibubi.create.content.contraptions.components.flywheel.engine.EngineBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(FlywheelTileEntity.class)
public abstract class MixinFlywheel extends GeneratingKineticTileEntity{
	public MixinFlywheel(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
		super(typeIn, pos, state);
	}
	@Shadow(remap=false)
	public abstract void setRotation(float speed, float capacity);
	@Inject(at=@At("HEAD"),method="tick", remap = false)
	public void sp$tick(CallbackInfo cbi) {
		Direction at=FlywheelBlock.getConnection(getBlockState());
		KineticNetwork nw=this.getOrCreateNetwork();
		if(nw!=null) {
			nw.updateCapacityFor(this,this.capacity);
		}
		if(at!=null) {
			if(!(this.getLevel().getBlockState(this.getBlockPos().relative(at,2)).getBlock() instanceof EngineBlock)) {
				FlywheelBlock.setConnection(getLevel(),getBlockPos(),getBlockState(),null);
				this.setRotation(0,0);
			}
		}else this.setRotation(0,0);
	}
}
