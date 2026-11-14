package net.qiansiqi.denunciationdelight.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.fml.loading.FMLPaths;
import net.qiansiqi.denunciationdelight.DenunciationDelight;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 书页内容加载器。
 *
 * 优先读取 config/denunciationdelight/selected_works.json；
 * 首次打开时如果文件不存在，会把内置的占位内容复制过去，方便整合包作者替换。
 * 模组本身不内置受版权保护的正文。
 */
public final class SelectedWorksContent {
    private static final ResourceLocation DEFAULT_RESOURCE =
            new ResourceLocation(DenunciationDelight.MOD_ID, "selected_works/pages.json");

    private SelectedWorksContent() {
    }

    public record Page(String heading, String body) {
    }

    public record Book(String title, List<Page> pages) {
    }

    public static Book load() {
        Book config = loadConfig();
        if (config != null && !config.pages().isEmpty()) return config;

        Book resource = loadResource();
        if (resource != null && !resource.pages().isEmpty()) return resource;

        return fallback();
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve("denunciationdelight").resolve("selected_works.json");
    }

    private static Book loadConfig() {
        Path path = configPath();
        if (!Files.exists(path)) {
            copyDefaultToConfig(path);
        }
        if (!Files.exists(path)) return null;

        try (InputStream stream = Files.newInputStream(path)) {
            return parse(stream);
        } catch (Exception e) {
            DenunciationDelight.LOGGER.error("Failed to read selected works config {}", path, e);
            return null;
        }
    }

    private static void copyDefaultToConfig(Path path) {
        try {
            Files.createDirectories(path.getParent());
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager()
                    .getResource(DEFAULT_RESOURCE);
            if (resource.isEmpty()) return;
            try (InputStream stream = resource.get().open()) {
                Files.copy(stream, path);
            }
        } catch (IOException e) {
            DenunciationDelight.LOGGER.error("Failed to create selected works config template", e);
        }
    }

    private static Book loadResource() {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager()
                    .getResource(DEFAULT_RESOURCE);
            if (resource.isEmpty()) return null;
            try (InputStream stream = resource.get().open()) {
                return parse(stream);
            }
        } catch (Exception e) {
            DenunciationDelight.LOGGER.error("Failed to read selected works resource", e);
            return null;
        }
    }

    private static Book parse(InputStream stream) {
        JsonObject root = JsonParser.parseReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        String title = root.has("title") && root.get("title").isJsonPrimitive()
                ? root.get("title").getAsString() : "毛泽东选集";
        List<Page> pages = new ArrayList<>();
        if (root.has("pages") && root.get("pages").isJsonArray()) {
            JsonArray array = root.getAsJsonArray("pages");
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                JsonObject page = element.getAsJsonObject();
                String heading = page.has("heading") ? page.get("heading").getAsString() : "";
                String body = page.has("body") ? page.get("body").getAsString() : "";
                if (!heading.isBlank() || !body.isBlank()) {
                    pages.add(new Page(heading, body));
                }
            }
        }
        return new Book(title, Collections.unmodifiableList(pages));
    }

    private static Book fallback() {
        List<Page> pages = List.of(
                new Page("说明", "这是一本装饰性书籍。模组本身不包含《毛泽东选集》的原文，也不附带任何未授权文本。"),
                new Page("添加内容", "将你拥有版权或使用授权的文本写入 config/denunciationdelight/selected_works.json。\n\n"
                        + "每个 page 包含 heading 和 body，body 支持换行。"),
                new Page("格式示例", "{\n  \"title\": \"毛泽东选集\",\n  \"pages\": [\n"
                        + "    {\"heading\": \"标题\", \"body\": \"正文\"}\n  ]\n}")
        );
        return new Book("毛泽东选集", pages);
    }
}