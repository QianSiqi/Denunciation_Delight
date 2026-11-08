package net.qiansiqi.denunciationdelight.client.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.qiansiqi.denunciationdelight.DenunciationDelight;
import net.qiansiqi.denunciationdelight.client.model.ConeHatModel;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class ConeHatCurioRenderer implements ICurioRenderer {
    private static final ResourceLocation TEXTURE = new ResourceLocation(DenunciationDelight.MOD_ID, "textures/models/armor/cone_hat.png");
    private ConeHatModel model;

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack, SlotContext slotContext, PoseStack matrixStack,
            RenderLayerParent<T, M> renderLayerParent, MultiBufferSource renderTypeBuffer,
            int light, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch) {

        if (this.model == null) {
            this.model = new ConeHatModel(Minecraft.getInstance().getEntityModels().bakeLayer(ConeHatModel.LAYER_LOCATION));
        }

        if (renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel) {
            // 同步原版 HumanoidModel 的头部旋转、位置与动作姿态
            ((HumanoidModel) humanoidModel).copyPropertiesTo((HumanoidModel) this.model);
            this.model.head.copyFrom(humanoidModel.head);
            this.model.setItem(stack);

            this.model.head.visible = true;
            this.model.hat.visible = false;
            this.model.body.visible = false;
            this.model.rightArm.visible = false;
            this.model.leftArm.visible = false;
            this.model.rightLeg.visible = false;
            this.model.leftLeg.visible = false;

            // 注意：已通过 copyPropertiesTo 继承了头部潜行旋转与下移，严禁再次调用 translateIfSneaking / rotateIfSneaking

            VertexConsumer vertexConsumer = ItemRenderer.getArmorFoilBuffer(
                    renderTypeBuffer, RenderType.armorCutoutNoCull(TEXTURE), false, stack.hasFoil());
            this.model.renderToBuffer(matrixStack, vertexConsumer, light, 0, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
