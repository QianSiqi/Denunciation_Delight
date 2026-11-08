package net.qiansiqi.denunciationdelight.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.item.ItemStack;
import net.qiansiqi.denunciationdelight.client.model.NeckPlacardModel;

public class NeckPlacardArmorRenderer {
    private static NeckPlacardModel cachedModel = null;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static NeckPlacardModel getModel(HumanoidModel<?> original, ItemStack stack) {
        if (cachedModel == null) {
            cachedModel = new NeckPlacardModel(Minecraft.getInstance().getEntityModels().bakeLayer(NeckPlacardModel.LAYER_LOCATION));
        }

        // 同步原版生物躯干姿态与动画
        ((HumanoidModel) original).copyPropertiesTo((HumanoidModel) cachedModel);
        cachedModel.body.copyFrom(original.body);
        cachedModel.setItem(stack);

        // 仅躯干挂牌可见，头部及四肢全部关闭，杜绝产生白色四肢或覆盖原有身体衣服
        cachedModel.head.visible = false;
        cachedModel.hat.visible = false;
        cachedModel.body.visible = true;
        cachedModel.rightArm.visible = false;
        cachedModel.leftArm.visible = false;
        cachedModel.rightLeg.visible = false;
        cachedModel.leftLeg.visible = false;

        return cachedModel;
    }
}
