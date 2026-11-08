package net.qiansiqi.denunciationdelight.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.qiansiqi.denunciationdelight.block.WallPortraitBlock;
import net.qiansiqi.denunciationdelight.block.entity.WallPortraitBlockEntity;
import net.qiansiqi.denunciationdelight.client.ClientPortraitCache;
import org.joml.Matrix4f;

public class WallPortraitRenderer implements BlockEntityRenderer<WallPortraitBlockEntity> {

    public WallPortraitRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WallPortraitBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof WallPortraitBlock)) return;

        Direction facing = state.getValue(WallPortraitBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.translate(0.0D, 0.0D, -0.45D); // 移向正面表面

        ResourceLocation portraitTexture = ClientPortraitCache.getOrRequestTexture(be.getPortraitId());
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(portraitTexture));
        Matrix4f mat = poseStack.last().pose();

        float hw = 0.44F;
        float hh = 0.44F;

        // 绘制双面可见的画面矩形
        consumer.vertex(mat, -hw, -hh, 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F).uv(1, 1).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex();
        consumer.vertex(mat, hw, -hh, 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F).uv(0, 1).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex();
        consumer.vertex(mat, hw, hh, 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F).uv(0, 0).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex();
        consumer.vertex(mat, -hw, hh, 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F).uv(1, 0).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex();

        poseStack.popPose();
    }
}
