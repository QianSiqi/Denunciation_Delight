package net.qiansiqi.denunciationdelight.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.qiansiqi.denunciationdelight.DenunciationDelight;
import net.qiansiqi.denunciationdelight.item.NeckPlacardItem;

public class NeckPlacardModel extends HumanoidModel<LivingEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(DenunciationDelight.MOD_ID, "neck_placard"), "main");

    private ItemStack currentStack = ItemStack.EMPTY;

    public NeckPlacardModel(ModelPart root) {
        super(root);
        this.head.visible = false;
        this.hat.visible = false;
        this.rightArm.visible = false;
        this.leftArm.visible = false;
        this.rightLeg.visible = false;
        this.leftLeg.visible = false;
        this.body.visible = true;
    }

    public void setItem(ItemStack stack) {
        this.currentStack = stack;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // 仅保留空部位以满足 HumanoidModel 构造需求，不生成任何多余的原版身体立方体（杜绝把身体变白）
        partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);

        // body 部位：仅包含挂牌自身 (吊绳与白木板)，绝不包含原版 8x12x4 身体方块
        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        // 挂在脖子上的两条挂绳 (吊绳)
        body.addOrReplaceChild("left_rope", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(2.0F, -0.5F, -2.6F, 1.0F, 3.0F, 0.5F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        body.addOrReplaceChild("right_rope", CubeListBuilder.create()
                .texOffs(4, 0)
                .addBox(-3.0F, -0.5F, -2.6F, 1.0F, 3.0F, 0.5F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // 白色木板/硬纸板挂牌主体 (宽10, 高11, 厚0.8)
        body.addOrReplaceChild("placard_board", CubeListBuilder.create()
                .texOffs(0, 16)
                .addBox(-5.0F, 2.5F, -3.0F, 10.0F, 11.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.body.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        renderPlacardText(poseStack, packedLight);
    }

    private static float sanitizeFontScale(float value) {
        if (!Float.isFinite(value)) return 1.0F;
        return Math.max(0.5F, Math.min(2.5F, value));
    }

    private void renderPlacardText(PoseStack poseStack, int packedLight) {
        CompoundTag tag = this.currentStack.getTag();
        String line1 = "";
        String line2 = "";
        String line3 = "";
        boolean redCross = false;
        float fontScale = 1.0F;

        if (tag != null) {
            line1 = tag.getString(NeckPlacardItem.TAG_LINE1);
            line2 = tag.getString(NeckPlacardItem.TAG_LINE2);
            line3 = tag.getString(NeckPlacardItem.TAG_LINE3);
            redCross = tag.getBoolean(NeckPlacardItem.TAG_RED_CROSS);
            if (tag.contains(NeckPlacardItem.TAG_FONT_SIZE)) {
                fontScale = tag.getFloat(NeckPlacardItem.TAG_FONT_SIZE);
            }
        }
        fontScale = sanitizeFontScale(fontScale);

        // 如果没有单独写 Line2，但物品在铁砧改过名，则读取物品名称
        if (line2.isEmpty() && this.currentStack.hasCustomHoverName()) {
            line2 = this.currentStack.getHoverName().getString();
        }

        // 如果全部为空，默认不绘制
        if (line1.isEmpty() && line2.isEmpty() && line3.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        MultiBufferSource bufferSource = mc.renderBuffers().bufferSource();

        poseStack.pushPose();
        this.body.translateAndRotate(poseStack);

        // 与实体文字层保持一致：胸甲前表面外移 0.22 像素，避免穿模
        poseStack.translate(0.0F, 3.6F / 16.0F, -3.22F / 16.0F);

        float scale = 0.0165F * fontScale;
        poseStack.scale(scale, scale, scale);

        int light = 0xF000F0;
        float yStep = 10.5F * fontScale;
        drawAllLines(font, line1, line2, line3, redCross, yStep, poseStack, bufferSource, light);

        poseStack.popPose();
    }

    private void drawAllLines(Font font, String line1, String line2, String line3, boolean redCross, float yStep,
                              PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        // 第 1 行：身份/罪名 (醒目红字)
        if (!line1.isEmpty()) {
            int w1 = font.width(line1);
            font.drawInBatch(line1, -w1 / 2.0F, 0.0F, 0xFFDD0000, false,
                    poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, light);
        }

        // 第 2 行：名字 (黑字加粗)
        if (!line2.isEmpty()) {
            int w2 = font.width(line2);
            float y2 = yStep;
            font.drawInBatch(line2, -w2 / 2.0F, y2, 0xFF000000, false,
                    poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, light);

            // 红叉
            if (redCross) {
                poseStack.pushPose();
                poseStack.translate(0.0F, 0.0F, -0.05F);
                String cross = "§lX";
                int cw = font.width(cross);
                font.drawInBatch(cross, -cw / 2.0F, y2 - 1.0F, 0xFFFF0000, false,
                        poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, light);
                poseStack.popPose();
            }
        }

        // 第 3 行：口号/附注 (深色小字)
        if (!line3.isEmpty()) {
            int w3 = font.width(line3);
            float y3 = yStep * 2.0F;
            font.drawInBatch(line3, -w3 / 2.0F, y3, 0xFF222222, false,
                    poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, light);
        }
    }
}
