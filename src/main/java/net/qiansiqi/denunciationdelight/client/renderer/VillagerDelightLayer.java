package net.qiansiqi.denunciationdelight.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.qiansiqi.denunciationdelight.DenunciationDelight;
import net.qiansiqi.denunciationdelight.client.ClientInkedTracker;
import net.qiansiqi.denunciationdelight.client.model.ConeHatModel;
import net.qiansiqi.denunciationdelight.client.model.NeckPlacardModel;
import net.qiansiqi.denunciationdelight.item.ConeHatItem;
import net.qiansiqi.denunciationdelight.item.ModItems;
import net.qiansiqi.denunciationdelight.item.NeckPlacardItem;

public class VillagerDelightLayer<T extends AbstractVillager, M extends VillagerModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation HAT_TEXTURE = new ResourceLocation(DenunciationDelight.MOD_ID, "textures/models/armor/cone_hat.png");
    private static final ResourceLocation PLACARD_TEXTURE = new ResourceLocation(DenunciationDelight.MOD_ID, "textures/models/armor/neck_placard.png");
    private static final ResourceLocation VILLAGER_INK_TEXTURE = new ResourceLocation(DenunciationDelight.MOD_ID, "textures/models/armor/villager_ink_splatter.png");

    private ConeHatModel hatModel;
    private NeckPlacardModel placardModel;

    public VillagerDelightLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       T villager, float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {

        if (villager.isInvisible()) return;

        M villagerModel = this.getParentModel();
        Font font = Minecraft.getInstance().font;

        // 1. 渲染村民高帽 (3D 模型 + 竖排文字)
        ItemStack hatStack = villager.getItemBySlot(EquipmentSlot.HEAD);
        if (hatStack.is(ModItems.CONE_HAT.get())) {
            renderVillagerHat(poseStack, bufferSource, packedLight, villagerModel, font, hatStack);
        }

        // 2. 渲染村民挂牌 (3D 模型 + 3 行文字 + 大红叉)
        ItemStack placardStack = villager.getItemBySlot(EquipmentSlot.CHEST);
        if (placardStack.is(ModItems.NECK_PLACARD.get())) {
            renderVillagerPlacard(poseStack, bufferSource, packedLight, villagerModel, font, placardStack);
        }

        // 3. 渲染村民专属泼墨状态
        if (ClientInkedTracker.isInked(villager)) {
            float alpha = ClientInkedTracker.getAlpha(villager);
            if (alpha > 0.01F) {
                poseStack.pushPose();
                poseStack.scale(1.003F, 1.003F, 1.003F);
                VertexConsumer inkBuffer = bufferSource.getBuffer(RenderType.entityTranslucent(VILLAGER_INK_TEXTURE));
                villagerModel.renderToBuffer(poseStack, inkBuffer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, alpha);
                poseStack.popPose();
            }
        }
    }

    private static float clampFontScale(float value) {
        if (!Float.isFinite(value)) return 1.0F;
        return Math.max(0.5F, Math.min(2.5F, value));
    }

    private void renderVillagerHat(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                   M villagerModel, Font font, ItemStack stack) {
        if (this.hatModel == null) {
            this.hatModel = new ConeHatModel(Minecraft.getInstance().getEntityModels().bakeLayer(ConeHatModel.LAYER_LOCATION));
        }

        poseStack.pushPose();
        villagerModel.getHead().translateAndRotate(poseStack);

        // 村民头部高 10 像素，普通玩家高 8 像素，高帽向上偏移 2 像素贴合村民头顶
        poseStack.translate(0.0F, -2.0F / 16.0F, 0.0F);

        // 绘制 3D 高帽本体
        this.hatModel.head.visible = true;
        VertexConsumer vertexConsumer = ItemRenderer.getArmorFoilBuffer(
                bufferSource, RenderType.armorCutoutNoCull(HAT_TEXTURE), false, stack.hasFoil());
        this.hatModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        // 绘制高帽正面竖排文字
        CompoundTag tag = stack.getTag();
        String text = "";
        float fontScale = 1.0F;
        if (tag != null) {
            text = tag.getString(ConeHatItem.TAG_HAT_TEXT);
            if (tag.contains(ConeHatItem.TAG_FONT_SIZE)) {
                fontScale = tag.getFloat(ConeHatItem.TAG_FONT_SIZE);
            }
        }
        fontScale = clampFontScale(fontScale);
        if (text.isEmpty() && stack.hasCustomHoverName()) {
            text = stack.getHoverName().getString();
        }

        if (!text.isEmpty()) {
            poseStack.pushPose();
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

        poseStack.popPose();
    }

    private void renderVillagerPlacard(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                       M villagerModel, Font font, ItemStack stack) {
        if (this.placardModel == null) {
            this.placardModel = new NeckPlacardModel(Minecraft.getInstance().getEntityModels().bakeLayer(NeckPlacardModel.LAYER_LOCATION));
        }

        poseStack.pushPose();
        ModelPart bodyPart = villagerModel.root().getChild("body");
        bodyPart.translateAndRotate(poseStack);

        // 村民长袍较厚，挂牌前移贴合村民长袍前胸
        poseStack.translate(0.0F, 1.0F / 16.0F, -1.0F / 16.0F);

        // 绘制 3D 挂牌本体 (吊绳 + 白木板)
        this.placardModel.body.visible = true;
        VertexConsumer vertexConsumer = ItemRenderer.getArmorFoilBuffer(
                bufferSource, RenderType.armorCutoutNoCull(PLACARD_TEXTURE), false, stack.hasFoil());
        this.placardModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        // 绘制挂牌正面文字
        CompoundTag tag = stack.getTag();
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
        fontScale = clampFontScale(fontScale);
        if (line2.isEmpty() && stack.hasCustomHoverName()) {
            line2 = stack.getHoverName().getString();
        }

        if (!line1.isEmpty() || !line2.isEmpty() || !line3.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.0F, 3.6F / 16.0F, -3.22F / 16.0F);

            float scale = 0.0165F * fontScale;
            poseStack.scale(scale, scale, scale);

            float yStep = 10.5F * fontScale;
            int light = 0xF000F0;

            if (!line1.isEmpty()) {
                int w1 = font.width(line1);
                font.drawInBatch(line1, -w1 / 2.0F, 0.0F, 0xFFDD0000, false,
                        poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, light);
            }

            if (!line2.isEmpty()) {
                int w2 = font.width(line2);
                float y2 = yStep;
                font.drawInBatch(line2, -w2 / 2.0F, y2, 0xFF000000, false,
                        poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, light);

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

            if (!line3.isEmpty()) {
                int w3 = font.width(line3);
                float y3 = yStep * 2.0F;
                font.drawInBatch(line3, -w3 / 2.0F, y3, 0xFF222222, false,
                        poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, light);
            }

            poseStack.popPose();
        }

        poseStack.popPose();
    }
}
