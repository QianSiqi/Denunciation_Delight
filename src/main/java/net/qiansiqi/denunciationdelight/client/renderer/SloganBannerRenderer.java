package net.qiansiqi.denunciationdelight.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.qiansiqi.denunciationdelight.DenunciationDelight;
import net.qiansiqi.denunciationdelight.block.SloganBannerBlock;
import net.qiansiqi.denunciationdelight.block.entity.SloganBannerBlockEntity;
import org.joml.Matrix4f;

public class SloganBannerRenderer implements BlockEntityRenderer<SloganBannerBlockEntity> {
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation(DenunciationDelight.MOD_ID, "textures/block/white.png");

    public SloganBannerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SloganBannerBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof SloganBannerBlock)) return;

        Direction facing = state.getValue(SloganBannerBlock.FACING);

        poseStack.pushPose();
        // 移动到方块中心并转向朝向
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        // 移向正面表面贴合位置
        poseStack.translate(0.0D, 0.0D, -0.45D);

        // 1. 渲染横幅底板 (整整一格宽高: 1.0 x 1.0)
        int bg = be.getBackgroundColor();
        float r = ((bg >> 16) & 0xFF) / 255.0F;
        float g = ((bg >> 8) & 0xFF) / 255.0F;
        float b = (bg & 0xFF) / 255.0F;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(WHITE_TEXTURE));
        Matrix4f mat = poseStack.last().pose();

        float hw = 0.50F; // 宽为整整一格 (左右各 0.5)
        float hh = 0.50F; // 高为整整一格 (上下各 0.5)

        // 正面四边形
        consumer.vertex(mat, -hw, -hh, 0.0F).color(r, g, b, 1.0F).uv(0, 0).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex();
        consumer.vertex(mat, hw, -hh, 0.0F).color(r, g, b, 1.0F).uv(1, 0).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex();
        consumer.vertex(mat, hw, hh, 0.0F).color(r, g, b, 1.0F).uv(1, 1).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex();
        consumer.vertex(mat, -hw, hh, 0.0F).color(r, g, b, 1.0F).uv(0, 1).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex();

        // 2. 检查多方块连通结构
        net.minecraft.world.level.Level level = be.getLevel();
        net.minecraft.core.BlockPos pos = be.getBlockPos();

        if (level == null) {
            poseStack.popPose();
            return;
        }

        // 面对横幅时，玩家眼中的向右方向（屏幕向右，对应局部坐标系 +X 轴）
        Direction rightDir = facing.getCounterClockWise();
        // 面对横幅时，玩家眼中的向左方向（屏幕向左，对应局部坐标系 -X 轴）
        Direction leftDir = facing.getClockWise();

        // 如果左侧或上方存在相同朝向的横幅方块，则当前方块不是主方块（Origin），只渲染底板，由主方块统一绘制整个结构的文字
        boolean hasLeft = isSameBanner(level, pos.relative(leftDir), facing);
        boolean hasUp = isSameBanner(level, pos.above(), facing);

        if (hasLeft || hasUp) {
            poseStack.popPose();
            return;
        }

        // 当前方块是主方块（左上角），沿局部 +X (rightDir) 探测连续宽度，沿局部 -Y (DOWN) 探测连续高度
        int width = 1;
        while (width < 32 && isSameBanner(level, pos.relative(rightDir, width), facing)) {
            width++;
        }

        int height = 1;
        while (height < 32 && isSameBanner(level, pos.below(height), facing)) {
            height++;
        }

        // 读取连通结构中的文字（优先使用主方块的文字）
        String text = be.getText();
        if (text == null || text.trim().isEmpty()) {
            text = findFirstText(level, pos, facing, rightDir, width, height);
        }

        if (text != null && !text.isEmpty()) {
            Font font = Minecraft.getInstance().font;
            float fontScale = be.getFontSize();
            int color = 0xFF000000 | be.getTextColor();

            // 多方块整体物理中心坐标 (以主方块为原点)
            float centerX = (width - 1) * 0.5F;
            float centerY = -(height - 1) * 0.5F;

            // 区分排版模式：
            // A. 纵向竖排多方块 (height > 1 且 width == 1) -> 自动竖排贯穿整条竖幅/对联
            if (height > 1 && width == 1) {
                renderVerticalMultiBanner(poseStack, bufferSource, font, text, fontScale, color, centerX, centerY, height, packedLight);
            } else {
                // B. 横向多方块大横幅 (width > 1) 或 单方块/多方块矩形 -> 水平居中贯穿
                renderHorizontalMultiBanner(poseStack, bufferSource, font, text, fontScale, color, centerX, centerY, width, packedLight);
            }
        }

        poseStack.popPose();
    }

    private void renderHorizontalMultiBanner(PoseStack poseStack, MultiBufferSource bufferSource, Font font,
                                             String text, float fontScale, int color,
                                             float centerX, float centerY, int width, int light) {
        // 多方块横幅自动允许更大更清晰的文字缩放
        float widthBoost = width > 1 ? Math.min(1.0F + (width - 1) * 0.35F, 2.5F) : 1.0F;
        float scale = 0.015F * fontScale * widthBoost;
        int textWidth = font.width(text);

        // 正向单层绘制 (正面标准朝向)
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 0.015D);
        poseStack.scale(scale, -scale, -scale);
        font.drawInBatch(text, -textWidth / 2.0F, -font.lineHeight / 2.0F, color, false,
                poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, light);
        poseStack.popPose();
    }

    private void renderVerticalMultiBanner(PoseStack poseStack, MultiBufferSource bufferSource, Font font,
                                           String text, float fontScale, int color,
                                           float centerX, float centerY, int height, int light) {
        char[] chars = text.toCharArray();
        if (chars.length == 0) return;

        float totalAvailablePixels = height * 54.0F;
        float charSpacing = Math.min(14.0F * fontScale, totalAvailablePixels / Math.max(chars.length, 1));
        float startY = -((chars.length - 1) * charSpacing) / 2.0F;
        float scale = 0.018F * fontScale;

        // 正向单层绘制
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 0.015D);
        poseStack.scale(scale, -scale, -scale);
        drawVerticalText(font, chars, startY, charSpacing, color, poseStack, bufferSource, light);
        poseStack.popPose();
    }

    private void drawVerticalText(Font font, char[] chars, float startY, float charSpacing, int color,
                                  PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        for (int i = 0; i < chars.length; i++) {
            String ch = String.valueOf(chars[i]);
            int cw = font.width(ch);
            font.drawInBatch(ch, -cw / 2.0F, startY + (i * charSpacing) - font.lineHeight / 2.0F, color, false,
                    poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, light);
        }
    }

    private boolean isSameBanner(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, Direction facing) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SloganBannerBlock) {
            return state.getValue(SloganBannerBlock.FACING) == facing;
        }
        return false;
    }

    private String findFirstText(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos origin,
                                 Direction facing, Direction rightDir, int width, int height) {
        for (int dx = 0; dx < width; dx++) {
            for (int dy = 0; dy < height; dy++) {
                net.minecraft.core.BlockPos p = origin.relative(rightDir, dx).below(dy);
                if (level.getBlockEntity(p) instanceof SloganBannerBlockEntity be) {
                    String t = be.getText();
                    if (t != null && !t.trim().isEmpty()) {
                        return t;
                    }
                }
            }
        }
        return "";
    }
}
