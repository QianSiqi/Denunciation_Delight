package net.qiansiqi.denunciationdelight.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.qiansiqi.denunciationdelight.DenunciationDelight;
import net.qiansiqi.denunciationdelight.block.WallPortraitBlock;
import net.qiansiqi.denunciationdelight.block.entity.WallPortraitBlockEntity;
import net.qiansiqi.denunciationdelight.client.ClientPortraitCache;
import org.joml.Matrix4f;

public class WallPortraitRenderer implements BlockEntityRenderer<WallPortraitBlockEntity> {
    private static final ResourceLocation FRAME_TEXTURE = new ResourceLocation(
            DenunciationDelight.MOD_ID, "textures/block/portrait_frame.png");

    public WallPortraitRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WallPortraitBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof WallPortraitBlock)) return;

        Level level = be.getLevel();
        if (level == null) return;

        Direction facing = state.getValue(WallPortraitBlock.FACING);
        Direction right = facing.getCounterClockWise();
        Direction left = right.getOpposite();
        BlockPos origin = be.getBlockPos();

        // 只让结构的左上角主方块绘制整组画面；其他方块直接返回，
        // 避免每个方块都画一遍导致多个 2×2 画框重叠（与 SloganBanner 同一思路）。
        if (isSamePortrait(level, origin.relative(left), facing)
                || isSamePortrait(level, origin.above(), facing)) {
            return;
        }

        int width = 1;
        while (width < 8 && isSamePortrait(level, origin.relative(right, width), facing)) {
            width++;
        }
        int height = 1;
        while (height < 8 && isSamePortrait(level, origin.below(height), facing)) {
            height++;
        }

        // 旧存档可能只有其中一块有图片 ID；从整个结构里找第一张有效图片。
        String portraitId = be.getPortraitId();
        if (portraitId.isEmpty()) {
            outer:
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    BlockPos pos = origin.relative(right, x).below(y);
                    if (level.getBlockEntity(pos) instanceof WallPortraitBlockEntity other
                            && !other.getPortraitId().isEmpty()) {
                        portraitId = other.getPortraitId();
                        break outer;
                    }
                }
            }
        }

        poseStack.pushPose();
        double centerX = (width - 1) * 0.5D;
        double centerY = -(height - 1) * 0.5D;
        // 先转到画像朝向，再按局部 X/Y 居中；否则东西向墙面会把水平偏移
        // 错误地加到世界 X 轴（墙的深度方向），整组画面就会浮在碰撞箱前面。
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.translate(centerX, centerY, -0.45D);

        Matrix4f mat = poseStack.last().pose();
        float outerW = width - 0.02F;
        float outerH = height - 0.02F;
        float innerW = width - 0.12F;
        float innerH = height - 0.12F;

        if (!portraitId.isEmpty()) {
            // 上传成功后自动绘制一圈木质画框；边框画在画面之前，避免与图片共面。
            VertexConsumer frame = bufferSource.getBuffer(
                    RenderType.entityCutoutNoCull(FRAME_TEXTURE));
            float frameZ = -0.002F;
            float ol = -outerW / 2.0F;
            float or = outerW / 2.0F;
            float ot = outerH / 2.0F;
            float ob = -outerH / 2.0F;
            float il = -innerW / 2.0F;
            float ir = innerW / 2.0F;
            float it = innerH / 2.0F;
            float ib = -innerH / 2.0F;

            drawQuad(frame, mat, ol, it, or, ot, frameZ, packedLight, packedOverlay);
            drawQuad(frame, mat, ol, ob, or, ib, frameZ, packedLight, packedOverlay);
            drawQuad(frame, mat, ol, ib, il, it, frameZ, packedLight, packedOverlay);
            drawQuad(frame, mat, ir, ib, or, it, frameZ, packedLight, packedOverlay);
        }

        ResourceLocation portraitTexture = ClientPortraitCache.getOrRequestTexture(portraitId);
        VertexConsumer image = bufferSource.getBuffer(
                RenderType.entityCutoutNoCull(portraitTexture));
        drawQuad(image, mat, -innerW / 2.0F, -innerH / 2.0F,
                innerW / 2.0F, innerH / 2.0F, 0.0F, packedLight, packedOverlay);

        poseStack.popPose();
    }

    private static boolean isSamePortrait(Level level, BlockPos pos, Direction facing) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof WallPortraitBlock
                && state.getValue(WallPortraitBlock.FACING) == facing;
    }

    private void drawQuad(VertexConsumer consumer, Matrix4f mat,
                          float x1, float y1, float x2, float y2, float z,
                          int packedLight, int packedOverlay) {
        consumer.vertex(mat, x1, y1, z).color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(1, 1).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex();
        consumer.vertex(mat, x2, y1, z).color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(0, 1).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex();
        consumer.vertex(mat, x2, y2, z).color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(0, 0).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex();
        consumer.vertex(mat, x1, y2, z).color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(1, 0).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex();
    }
}