package net.qiansiqi.denunciationdelight.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 服务端/整合包可调的玩法开关。
 * 内容主题包含自裁过场，默认开启但提供关闭项。
 */
public final class ModConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_MADNESS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SUICIDE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_INK;
    public static final ForgeConfigSpec.BooleanValue ALLOW_PORTRAIT_UPLOAD;
    public static final ForgeConfigSpec.IntValue MAX_PORTRAIT_KB;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Denunciation Delight gameplay settings").push("gameplay");
        ENABLE_MADNESS = builder
                .comment("Enable the madness value, wearing penalties and high-madness effects.")
                .define("enableMadness", true);
        ENABLE_SUICIDE = builder
                .comment("Allow madness 100 to trigger the suicide cutscene. Disable if this content is not wanted.")
                .define("enableSuicide", true);
        ENABLE_INK = builder
                .comment("Enable ink bottle effects: blindness, slow, inked overlay and madness gain.")
                .define("enableInk", true);
        builder.pop();

        builder.comment("Wall portrait upload settings").push("portraits");
        ALLOW_PORTRAIT_UPLOAD = builder
                .comment("Allow players to upload images to wall portraits.")
                .define("allowPortraitUpload", true);
        MAX_PORTRAIT_KB = builder
                .comment("Maximum accepted PNG size in KiB.")
                .defineInRange("maxPortraitSizeKb", 2048, 64, 8192);
        builder.pop();

        SPEC = builder.build();
    }

    private ModConfig() {
    }
}