package net.qiansiqi.denunciationdelight.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.item.ItemStack;
import net.qiansiqi.denunciationdelight.client.model.ConeHatModel;

public class ConeHatArmorRenderer {
    private static ConeHatModel cachedModel = null;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static ConeHatModel getModel(HumanoidModel<?> original, ItemStack stack) {
        if (cachedModel == null) {
            cachedModel = new ConeHatModel(Minecraft.getInstance().getEntityModels().bakeLayer(ConeHatModel.LAYER_LOCATION));
        }

        ((HumanoidModel) original).copyPropertiesTo((HumanoidModel) cachedModel);
        cachedModel.head.copyFrom(original.head);
        cachedModel.setItem(stack);

        // 仅头部高帽可见，其它所有肢体全部关闭，不覆盖任何原版模型
        cachedModel.head.visible = true;
        cachedModel.hat.visible = false;
        cachedModel.body.visible = false;
        cachedModel.rightArm.visible = false;
        cachedModel.leftArm.visible = false;
        cachedModel.rightLeg.visible = false;
        cachedModel.leftLeg.visible = false;

        return cachedModel;
    }
}
