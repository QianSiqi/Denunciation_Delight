package net.qiansiqi.denunciationdelight.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.qiansiqi.denunciationdelight.item.ConeHatItem;
import net.qiansiqi.denunciationdelight.item.ModItems;
import net.qiansiqi.denunciationdelight.item.NeckPlacardItem;

public class PlacardAndHatTextLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    public PlacardAndHatTextLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       T entity, float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {

        if (entity.isInvisible()) return;

        EntityModel<T> model = this.getParentModel();
        if (!(model instanceof HumanoidModel<?> humanoidModel)) {
            return;
        }

        // 1. 查找佩戴的高帽并渲染文字
        ItemStack hatStack = findConeHat(entity);
        if (!hatStack.isEmpty()) {
            renderHatText(poseStack, bufferSource, humanoidModel, hatStack);
        }

        // 2. 查找佩戴的挂牌并渲染文字
        ItemStack placardStack = findNeckPlacard(entity);
        if (!placardStack.isEmpty()) {
            renderPlacardText(poseStack, bufferSource, humanoidModel, placardStack);
        }
    }

    private ItemStack findConeHat(LivingEntity entity) {
        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (head.is(ModItems.CONE_HAT.get())) return head;

        if (ModList.get().isLoaded("curios")) {
            ItemStack curio = findInCurios(entity, ModItems.CONE_HAT.get());
            if (!curio.isEmpty()) return curio;
        }
        return ItemStack.EMPTY;
    }

    private ItemStack findNeckPlacard(LivingEntity entity) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.is(ModItems.NECK_PLACARD.get())) return chest;

        if (ModList.get().isLoaded("curios")) {
            ItemStack curio = findInCurios(entity, ModItems.NECK_PLACARD.get());
            if (!curio.isEmpty()) return curio;
        }
        return ItemStack.EMPTY;
    }

    private ItemStack findInCurios(LivingEntity entity, net.minecraft.world.item.Item targetItem) {
        try {
            return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(entity)
                    .map(inv -> inv.findFirstCurio(targetItem).map(slotResult -> slotResult.stack()).orElse(ItemStack.EMPTY))
                    .orElse(ItemStack.EMPTY);
        } catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static float clampFontScale(float value) {
        if (!Float.isFinite(value)) return 1.0F;
        return Math.max(0.5F, Math.min(2.5F, value));
    }

    private void renderHatText(PoseStack poseStack, MultiBufferSource bufferSource,
                               HumanoidModel<?> model, ItemStack stack) {
        CompoundTag tag = stack.getTag();
        String text = "";
        float fontScale = 1.0F;

        if (tag != null) {
            if (tag.contains(ConeHatItem.TAG_HAT_TEXT)) {
                text = tag.getString(ConeHatItem.TAG_HAT_TEXT);
            }
            if (tag.contains(ConeHatItem.TAG_FONT_SIZE)) {
                fontScale = tag.getFloat(ConeHatItem.TAG_FONT_SIZE);
            }
        }
        fontScale = clampFontScale(fontScale);

        if (text.isEmpty() && stack.hasCustomHoverName()) {
            text = stack.getHoverName().getString();
        }
        if (text.isEmpty()) return;

        Font font = Minecraft.getInstance().font;

        poseStack.pushPose();
        model.head.translateAndRotate(poseStack);

        // 原版 HumanoidModel 正面本来就是 -Z，屏幕向右是 +X，屏幕向下是 +Y！
        // 移至高帽圆锥正面外侧：起始 Y: -28 像素，Z: -1.80 像素
        poseStack.translate(0.0F, -28.0F / 16.0F, -1.80F / 16.0F);

        // 沿圆锥外壁倾斜 -7.8° (向下走向外倾斜贴合斜面)
        poseStack.mulPose(Axis.XP.rotationDegrees(-7.8F));

        // 正向缩放 scale(s, s, s)，绝对不翻转轴向，绝不触发背面剔除，字符正向书写，行号向下递增
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

    private void renderPlacardText(PoseStack poseStack, MultiBufferSource bufferSource,
                                  HumanoidModel<?> model, ItemStack stack) {
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
        if (line1.isEmpty() && line2.isEmpty() && line3.isEmpty()) {
            return;
        }

        Font font = Minecraft.getInstance().font;

        poseStack.pushPose();
        model.body.translateAndRotate(poseStack);

        // 原版 HumanoidModel 胸前正面就是 -Z 轴！
        // 木板表面在 Z = -3.0F 像素，平移至 Z = -3.22F 像素 (外移 0.22 像素)，彻底避免深度穿模
        poseStack.translate(0.0F, 3.6F / 16.0F, -3.22F / 16.0F);

        // 正向缩放 scale(s, s, s)，字形绝对正向，行从上向下正常排版
        float scale = 0.0165F * fontScale;
        poseStack.scale(scale, scale, scale);

        float yStep = 10.5F * fontScale;
        int light = 0xF000F0;

        // 第 1 行：身份/罪名 (醒目朱红)
        if (!line1.isEmpty()) {
            int w1 = font.width(line1);
            font.drawInBatch(line1, -w1 / 2.0F, 0.0F, 0xFFDD0000, false,
                    poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, light);
        }

        // 第 2 行：姓名/对象 (黑色大字)
        if (!line2.isEmpty()) {
            int w2 = font.width(line2);
            float y2 = yStep;
            font.drawInBatch(line2, -w2 / 2.0F, y2, 0xFF000000, false,
                    poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, light);

            // 大红叉：使用标准字符 "§lX"，微向前突 0.05 像素，清晰盖在名字上方
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

        // 第 3 行：批注/口号 (深色字)
        if (!line3.isEmpty()) {
            int w3 = font.width(line3);
            float y3 = yStep * 2.0F;
            font.drawInBatch(line3, -w3 / 2.0F, y3, 0xFF222222, false,
                    poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, light);
        }

        poseStack.popPose();
    }
}
