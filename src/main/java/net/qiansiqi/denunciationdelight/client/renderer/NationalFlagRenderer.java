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
import net.qiansiqi.denunciationdelight.DenunciationDelight;
import net.qiansiqi.denunciationdelight.block.NationalFlagBlock;
import net.qiansiqi.denunciationdelight.block.entity.NationalFlagBlockEntity;
import net.qiansiqi.denunciationdelight.flag.FlagRegistry;
import org.joml.Matrix4f;

public class NationalFlagRenderer implements BlockEntityRenderer<NationalFlagBlockEntity> {
    private static final ResourceLocation POLE_TEXTURE = new ResourceLocation(DenunciationDelight.MOD_ID, "textures/block/flagpole.png");

    public NationalFlagRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(NationalFlagBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof NationalFlagBlock)) return;

        Direction facing = state.getValue(NationalFlagBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));

        // 1. 渲染旗杆
        VertexConsumer poleConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(POLE_TEXTURE));
        Matrix4f poleMat = poseStack.last().pose();

        // 底部基座 (稳重方台)
        renderBox(poleConsumer, poleMat, -0.2F, 0.0F, -0.2F, 0.2F, 0.08F, 0.2F, packedLight, packedOverlay);

        // 主旗杆 (高耸至 2.85格)
        float pr = 0.045F; // 旗杆半径
        renderBox(poleConsumer, poleMat, -pr, 0.08F, -pr, pr, 2.85F, pr, packedLight, packedOverlay);

        // 杆顶金球/顶饰
        renderBox(poleConsumer, poleMat, -0.07F, 2.85F, -0.07F, 0.07F, 2.95F, 0.07F, packedLight, packedOverlay);

        // 2. 渲染国旗旗面 (大幅标准旗帜：宽 1.6格，高 1.06格，约 3:2 黄金比例)
        String countryId = FlagRegistry.canonicalize(be.getCountryId());
        if (countryId == null) countryId = "cn";
        ResourceLocation flagTexture = FlagRegistry.texture(countryId);
        VertexConsumer flagConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(flagTexture));
        Matrix4f flagMat = poseStack.last().pose();

        float x1 = pr;
        float x2 = pr + 1.6F;
        float y1 = 1.74F;
        float y2 = 2.80F;

        // 正反面稍微错开，避免两个共面四边形产生 z-fighting 闪烁
        float frontZ = 0.001F;
        float backZ = -0.001F;

        // 正面 (+Z)
        flagConsumer.vertex(flagMat, x1, y1, frontZ).color(1F, 1F, 1F, 1F).uv(0, 1).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex();
        flagConsumer.vertex(flagMat, x2, y1, frontZ).color(1F, 1F, 1F, 1F).uv(1, 1).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex();
        flagConsumer.vertex(flagMat, x2, y2, frontZ).color(1F, 1F, 1F, 1F).uv(1, 0).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex();
        flagConsumer.vertex(flagMat, x1, y2, frontZ).color(1F, 1F, 1F, 1F).uv(0, 0).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex();

        // 反面 (-Z)
        flagConsumer.vertex(flagMat, x1, y2, backZ).color(1F, 1F, 1F, 1F).uv(0, 0).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, -1).endVertex();
        flagConsumer.vertex(flagMat, x2, y2, backZ).color(1F, 1F, 1F, 1F).uv(1, 0).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, -1).endVertex();
        flagConsumer.vertex(flagMat, x2, y1, backZ).color(1F, 1F, 1F, 1F).uv(1, 1).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, -1).endVertex();
        flagConsumer.vertex(flagMat, x1, y1, backZ).color(1F, 1F, 1F, 1F).uv(0, 1).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, -1).endVertex();

        poseStack.popPose();
    }

    private void renderBox(VertexConsumer consumer, Matrix4f mat, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int light, int overlay) {
        consumer.vertex(mat, minX, minY, maxZ).color(0.8F, 0.8F, 0.8F, 1F).uv(0, 1).overlayCoords(overlay).uv2(light).normal(0, 0, 1).endVertex();
        consumer.vertex(mat, maxX, minY, maxZ).color(0.8F, 0.8F, 0.8F, 1F).uv(1, 1).overlayCoords(overlay).uv2(light).normal(0, 0, 1).endVertex();
        consumer.vertex(mat, maxX, maxY, maxZ).color(0.8F, 0.8F, 0.8F, 1F).uv(1, 0).overlayCoords(overlay).uv2(light).normal(0, 0, 1).endVertex();
        consumer.vertex(mat, minX, maxY, maxZ).color(0.8F, 0.8F, 0.8F, 1F).uv(0, 0).overlayCoords(overlay).uv2(light).normal(0, 0, 1).endVertex();

        consumer.vertex(mat, minX, minY, minZ).color(0.8F, 0.8F, 0.8F, 1F).uv(0, 1).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();
        consumer.vertex(mat, maxX, minY, minZ).color(0.8F, 0.8F, 0.8F, 1F).uv(1, 1).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();
        consumer.vertex(mat, maxX, maxY, minZ).color(0.8F, 0.8F, 0.8F, 1F).uv(1, 0).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();
        consumer.vertex(mat, minX, maxY, minZ).color(0.8F, 0.8F, 0.8F, 1F).uv(0, 0).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();
    }
}
