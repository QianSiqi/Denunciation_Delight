package net.qiansiqi.denunciationdelight.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.qiansiqi.denunciationdelight.DenunciationDelight;
import net.qiansiqi.denunciationdelight.client.ClientInkedTracker;

public class InkSplatterLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation INK_TEXTURE =
            new ResourceLocation(DenunciationDelight.MOD_ID, "textures/models/armor/ink_splatter.png");

    public InkSplatterLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       T entity, float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {

        if (entity.isInvisible()) return;

        if (!ClientInkedTracker.isInked(entity)) return;

        float alpha = ClientInkedTracker.getAlpha(entity);
        if (alpha <= 0.01F) return;

        poseStack.pushPose();
        // 微外扩 0.3%，杜绝与生物原本皮肤与装备产生深度冲突与闪烁
        poseStack.scale(1.003F, 1.003F, 1.003F);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(INK_TEXTURE));
        this.getParentModel().renderToBuffer(poseStack, vertexConsumer, packedLight,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, alpha);
        poseStack.popPose();
    }
}
