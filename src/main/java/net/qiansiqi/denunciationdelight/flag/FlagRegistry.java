package net.qiansiqi.denunciationdelight.flag;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.qiansiqi.denunciationdelight.DenunciationDelight;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 国旗注册表：统一维护 ID、中文/英文名、贴图路径和旧存档别名。
 * 数据来自 assets/denunciationdelight/flags.json，避免在代码里散落 250+ 个字符串。
 */
public final class FlagRegistry {
    private static final Map<String, FlagDef> BY_ID = new LinkedHashMap<>();
    private static final List<FlagDef> ALL;
    private static final Map<String, String> ALIASES = Map.of(
            "china", "cn",
            "usa", "us",
            "uk", "gb",
            "russia", "ru",
            "france", "fr",
            "germany", "de"
    );

    static {
        loadFromResource();
        if (BY_ID.isEmpty()) {
            // 资源异常时的最小兜底，避免整个模组无法启动
            add("cn", "China", "中国");
            add("us", "United States", "美国");
            add("gb", "United Kingdom", "英国");
            add("ru", "Russia", "俄罗斯");
            add("fr", "France", "法国");
            add("de", "Germany", "德国");
            add("un", "United Nations", "联合国");
            add("ussr", "Soviet Union", "苏联");
        }
        ALL = Collections.unmodifiableList(new ArrayList<>(BY_ID.values()));
    }

    private FlagRegistry() {
    }

    private static void loadFromResource() {
        try (InputStream stream = FlagRegistry.class.getResourceAsStream(
                "/assets/denunciationdelight/flags.json")) {
            if (stream == null) {
                DenunciationDelight.LOGGER.error("flags.json is missing");
                return;
            }
            JsonArray array = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                String id = getString(object, "id");
                String en = getString(object, "en");
                String zh = getString(object, "zh");
                if (id == null || id.isBlank() || en == null) continue;
                add(id.toLowerCase(Locale.ROOT), en, zh == null ? en : zh);
            }
        } catch (Exception e) {
            DenunciationDelight.LOGGER.error("Failed to load flags.json", e);
        }
    }

    private static String getString(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsString() : null;
    }

    private static void add(String id, String en, String zh) {
        BY_ID.putIfAbsent(id, new FlagDef(id, en, zh));
    }

    public static List<FlagDef> all() {
        return ALL;
    }

    /**
     * 把旧存档的 china/usa/uk/... 映射到新的 ISO ID；未知 ID 返回 null。
     */
    public static String canonicalize(String id) {
        if (id == null) return null;
        String lower = id.toLowerCase(Locale.ROOT);
        String alias = ALIASES.get(lower);
        if (alias != null) return alias;
        return BY_ID.containsKey(lower) ? lower : null;
    }

    public static boolean isValidCountry(String id) {
        return canonicalize(id) != null;
    }

    public static FlagDef get(String id) {
        String canonical = canonicalize(id);
        return canonical == null ? null : BY_ID.get(canonical);
    }

    public static ResourceLocation texture(String id) {
        String canonical = canonicalize(id);
        if (canonical == null) canonical = "cn";
        return new ResourceLocation(DenunciationDelight.MOD_ID,
                "textures/flag/" + canonical + ".png");
    }

    public record FlagDef(String id, String en, String zh) {
        public String displayName(boolean chinese) {
            return chinese && zh != null && !zh.isBlank() ? zh : en;
        }
    }
}