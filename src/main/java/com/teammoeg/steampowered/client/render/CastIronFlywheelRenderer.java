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

package com.teammoeg.steampowered.client.render;

import com.jozufozu.flywheel.backend.Backend;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.content.contraptions.base.HorizontalKineticBlock;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.base.KineticTileEntityRenderer;
import com.simibubi.create.content.contraptions.components.flywheel.FlywheelBlock;
import com.simibubi.create.content.contraptions.components.flywheel.FlywheelTileEntity;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.teammoeg.steampowered.block.SPBlockPartials;
import com.teammoeg.steampowered.mixin.FlywheelTileEntityAccess;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class CastIronFlywheelRenderer extends KineticTileEntityRenderer {
    public CastIronFlywheelRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    protected void renderSafe(KineticTileEntity te, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(te, partialTicks, ms, buffer, light, overlay);
        if (!Backend.getInstance().canUseInstancing(te.getLevel())) {
            BlockState blockState = te.getBlockState();
            FlywheelTileEntity wte = (FlywheelTileEntity) te;
            // Mixin starts
            FlywheelTileEntityAccess access = (FlywheelTileEntityAccess) wte;
            float speed = access.getVisualSpeed().get(partialTicks) * 3.0F / 10.0F;
            float angle = access.getAngle() + speed * partialTicks;
            // Mixin ends
            VertexConsumer vb = buffer.getBuffer(RenderType.solid());
            if (FlywheelBlock.isConnected(blockState)) {
                Direction connection = FlywheelBlock.getConnection(blockState);
                light = LevelRenderer.getLightColor(te.getLevel(), blockState, te.getBlockPos().relative(connection));
                float rotation = connection.getAxis() == Direction.Axis.X ^ connection.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? -angle : angle;
                boolean flip = blockState.getValue(FlywheelBlock.CONNECTION) == FlywheelBlock.ConnectionState.LEFT;
                this.transformConnector(this.rotateToFacing(CachedBufferer.partial(SPBlockPartials.CAST_IRON_FLYWHEEL_UPPER_ROTATING, blockState), connection), true, true, rotation, flip).light(light).renderInto(ms, vb);
                this.transformConnector(this.rotateToFacing(CachedBufferer.partial(SPBlockPartials.CAST_IRON_FLYWHEEL_LOWER_ROTATING, blockState), connection), false, true, rotation, flip).light(light).renderInto(ms, vb);
                this.transformConnector(this.rotateToFacing(CachedBufferer.partial(SPBlockPartials.CAST_IRON_FLYWHEEL_UPPER_SLIDING, blockState), connection), true, false, rotation, flip).light(light).renderInto(ms, vb);
                this.transformConnector(this.rotateToFacing(CachedBufferer.partial(SPBlockPartials.CAST_IRON_FLYWHEEL_LOWER_SLIDING, blockState), connection), false, false, rotation, flip).light(light).renderInto(ms, vb);
            }

            this.renderFlywheel(te, ms, light, blockState, angle, vb);
        }
    }

    private void renderFlywheel(KineticTileEntity te, PoseStack ms, int light, BlockState blockState, float angle, VertexConsumer vb) {
        BlockState referenceState = blockState.rotate(Rotation.CLOCKWISE_90);
        Direction facing = referenceState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        SuperByteBuffer wheel = CachedBufferer.partialFacing(SPBlockPartials.CAST_IRON_FLYWHEEL, referenceState, facing);
        kineticRotationTransform(wheel, te, blockState.getValue(HorizontalKineticBlock.HORIZONTAL_FACING).getAxis(), AngleHelper.rad((double) angle), light);
        wheel.renderInto(ms, vb);
    }

    protected SuperByteBuffer getRotatedModel(KineticTileEntity te) {
        return CachedBufferer.partialFacing(AllBlockPartials.SHAFT_HALF, te.getBlockState(), te.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());
    }

    protected SuperByteBuffer transformConnector(SuperByteBuffer buffer, boolean upper, boolean rotating, float angle, boolean flip) {
        float shift = upper ? 0.25F : -0.125F;
        float offset = upper ? 0.25F : 0.25F;
        float radians = (float) ((double) (angle / 180.0F) * 3.141592653589793D);
        float shifting = Mth.sin(radians) * shift + offset;
        float maxAngle = upper ? -5.0F : -15.0F;
        float minAngle = upper ? -45.0F : 5.0F;
        float barAngle = 0.0F;
        if (rotating) {
            barAngle = Mth.lerp((Mth.sin((float) ((double) radians + 1.5707963267948966D)) + 1.0F) / 2.0F, minAngle, maxAngle);
        }

        float pivotX = (upper ? 8.0F : 3.0F) / 16.0F;
        float pivotY = (upper ? 8.0F : 2.0F) / 16.0F;
        float pivotZ = (upper ? 23.0F : 21.5F) / 16.0F;
        buffer.translate(pivotX, pivotY, pivotZ + shifting);
        if (rotating) {
            buffer.rotate(Direction.EAST, AngleHelper.rad((double) barAngle));
        }

        buffer.translate(-pivotX, -pivotY, -pivotZ);
        if (flip && !upper) {
            buffer.translate(0.5625F, 0.0F, 0.0F);
        }

        return buffer;
    }

    protected SuperByteBuffer rotateToFacing(SuperByteBuffer buffer, Direction facing) {
        buffer.rotateCentered(Direction.UP, AngleHelper.rad((double) AngleHelper.horizontalAngle(facing)));
        return buffer;
    }
}
