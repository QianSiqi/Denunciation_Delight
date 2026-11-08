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
import net.qiansiqi.denunciationdelight.item.ConeHatItem;

public class ConeHatModel extends HumanoidModel<LivingEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(DenunciationDelight.MOD_ID, "cone_hat"), "main");

    private ItemStack currentStack = ItemStack.EMPTY;

    public ConeHatModel(ModelPart root) {
        super(root);
        this.body.visible = false;
        this.rightArm.visible = false;
        this.leftArm.visible = false;
        this.rightLeg.visible = false;
        this.leftLeg.visible = false;
        this.hat.visible = false;
    }

    public void setItem(ItemStack stack) {
        this.currentStack = stack;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // 仅保留空部位以满足 HumanoidModel 构造需求，不生成任何多余的原版身体/头套立方体（杜绝遮挡玩家脸部）
        partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);

        // head 部位：仅包含高帽自身的所有立方体，不包含原版 8x8x8 头盔方块
        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        // 1. 基座沿圈 (略宽于头部，贴合在头顶，绝不遮盖脸部)
        head.addOrReplaceChild("cone_base", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.3F, -9.2F, -4.3F, 8.6F, 1.2F, 8.6F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // 2. 超精细 16 阶超尖平滑圆锥体 (细腻平滑过渡，高耸尖锐，尖端细至 0.2)
        head.addOrReplaceChild("cone_s1", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.88F, -11.00F, -3.88F, 7.76F, 1.80F, 7.76F), PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("cone_s2", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.63F, -12.80F, -3.63F, 7.27F, 1.80F, 7.27F), PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("cone_s3", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.39F, -14.60F, -3.39F, 6.78F, 1.80F, 6.78F), PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("cone_s4", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.15F, -16.40F, -3.15F, 6.29F, 1.80F, 6.29F), PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("cone_s5", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2.90F, -18.20F, -2.90F, 5.81F, 1.80F, 5.81F), PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("cone_s6", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2.66F, -20.00F, -2.66F, 5.32F, 1.80F, 5.32F), PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("cone_s7", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2.42F, -21.80F, -2.42F, 4.83F, 1.80F, 4.83F), PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("cone_s8", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2.17F, -23.60F, -2.17F, 4.34F, 1.80F, 4.34F), PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("cone_s9", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.93F, -25.40F, -1.93F, 3.86F, 1.80F, 3.86F), PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("cone_s10", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.68F, -27.20F, -1.68F, 3.37F, 1.80F, 3.37F), PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("cone_s11", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.44F, -29.00F, -1.44F, 2.88F, 1.80F, 2.88F), PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("cone_s12", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.20F, -30.80F, -1.20F, 2.39F, 1.80F, 2.39F), PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("cone_s13", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-0.95F, -32.60F, -0.95F, 1.91F, 1.80F, 1.91F), PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("cone_s14", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-0.71F, -34.40F, -0.71F, 1.42F, 1.80F, 1.42F), PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("cone_s15", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-0.47F, -36.20F, -0.47F, 0.93F, 1.80F, 0.93F), PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("cone_s16", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-0.22F, -38.00F, -0.22F, 0.44F, 1.80F, 0.44F), PartPose.offset(0.0F, 0.0F, 0.0F));

        // 3. 极细尖端 (0.2 x 0.2，高达 -39.8 像素)
        head.addOrReplaceChild("cone_tip", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-0.10F, -39.80F, -0.10F, 0.20F, 1.80F, 0.20F), PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.head.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        renderHatText(poseStack, packedLight);
    }

    /**
     * 直接由护甲模型绘制文字，避免只依赖实体渲染层时出现“字不见”的问题。
     */
    private void renderHatText(PoseStack poseStack, int packedLight) {
        CompoundTag tag = this.currentStack.getTag();
        String text = "";
        float fontScale = 1.0F;

        if (tag != null) {
            text = tag.getString(ConeHatItem.TAG_HAT_TEXT);
            if (tag.contains(ConeHatItem.TAG_FONT_SIZE)) {
                fontScale = tag.getFloat(ConeHatItem.TAG_FONT_SIZE);
            }
        }
        if (!Float.isFinite(fontScale)) fontScale = 1.0F;
        fontScale = Math.max(0.5F, Math.min(2.5F, fontScale));

        if (text.isEmpty() && this.currentStack.hasCustomHoverName()) {
            text = this.currentStack.getHoverName().getString();
        }
        if (text.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        MultiBufferSource bufferSource = mc.renderBuffers().bufferSource();

        poseStack.pushPose();
        this.head.translateAndRotate(poseStack);
        poseStack.translate(0.0F, -28.0F / 16.0F, -1.80F / 16.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-7.8F));

        float scale = 0.016F * fontScale;
        poseStack.scale(scale, scale, scale);

        float charSpacing = 10.0F * fontScale;
        int light = 0xF000F0;
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            String ch = String.valueOf(chars[i]);
            int cw = font.width(ch);
            font.drawInBatch(ch, -cw / 2.0F, i * charSpacing, 0xFF000000, false,
                    poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, light);
        }
        poseStack.popPose();
    }
}
