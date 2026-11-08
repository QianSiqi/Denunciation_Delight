package net.qiansiqi.denunciationdelight.portrait;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.qiansiqi.denunciationdelight.config.ModConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class ServerPortraitManager {
    private static final Map<String, byte[]> CACHE = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_ENTRIES = 32;
    private static final Pattern PORTRAIT_ID = Pattern.compile("^[0-9a-f]{16}$");
    private static final String UPLOAD_TICK_TAG = "DenunciationLastPortraitUpload";
    private static final int UPLOAD_COOLDOWN_TICKS = 40;

    private static File getStorageDir() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        File dir;
        if (server != null) {
            dir = server.getWorldPath(LevelResource.ROOT).resolve("saved_portraits").toFile();
        } else {
            dir = new File("saved_portraits");
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static String saveImage(byte[] data) {
        if (!isValidImageData(data)) {
            return "";
        }
        try {
            // 计算 SHA-256 作为唯一的 portraitId
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) { // 取前 16 位字符作为短 ID
                sb.append(String.format("%02x", hash[i]));
            }
            String portraitId = sb.toString();

            cacheImage(portraitId, data);

            File file = new File(getStorageDir(), portraitId + ".png");
            if (!file.exists()) {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(data);
                }
            }
            return portraitId;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static byte[] getImageData(String portraitId) {
        if (!isValidPortraitId(portraitId)) return null;
        if (CACHE.containsKey(portraitId)) {
            return CACHE.get(portraitId);
        }

        Path dir = getStorageDir().toPath().toAbsolutePath().normalize();
        Path file = dir.resolve(portraitId + ".png").normalize();
        if (!file.startsWith(dir) || !Files.isRegularFile(file)) {
            return null;
        }
        try {
            byte[] bytes = Files.readAllBytes(file);
            cacheImage(portraitId, bytes);
            return bytes;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 画像 ID 只允许 16 位小写十六进制字符，防止路径穿越。
     */
    private static void cacheImage(String portraitId, byte[] data) {
        if (!CACHE.containsKey(portraitId) && CACHE.size() >= MAX_CACHE_ENTRIES) {
            CACHE.clear();
        }
        CACHE.put(portraitId, data);
    }

    public static boolean isValidPortraitId(String portraitId) {
        return portraitId != null && PORTRAIT_ID.matcher(portraitId).matches();
    }

    /**
     * 只接受 PNG 文件头与合理尺寸，避免把任意文件/超大图写进世界目录。
     */
    public static boolean isValidImageData(byte[] data) {
        if (data == null || data.length < 24) {
            return false;
        }
        int maxKb = 2048;
        try {
            maxKb = ModConfig.MAX_PORTRAIT_KB.get();
        } catch (Throwable ignored) {
            // 测试/配置尚未加载时使用默认上限
        }
        int maxBytes = maxKb * 1024;
        if (data.length > maxBytes) {
            return false;
        }
        boolean pngSignature = (data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G'
                && (data[4] & 0xFF) == 0x0D && (data[5] & 0xFF) == 0x0A
                && (data[6] & 0xFF) == 0x1A && (data[7] & 0xFF) == 0x0A;
        if (!pngSignature || data[12] != 'I' || data[13] != 'H' || data[14] != 'D' || data[15] != 'R') {
            return false;
        }
        int width = ((data[16] & 0xFF) << 24) | ((data[17] & 0xFF) << 16)
                | ((data[18] & 0xFF) << 8) | (data[19] & 0xFF);
        int height = ((data[20] & 0xFF) << 24) | ((data[21] & 0xFF) << 16)
                | ((data[22] & 0xFF) << 8) | (data[23] & 0xFF);
        return width > 0 && height > 0 && width <= 512 && height <= 512;
    }

    /**
     * 简单的上传冷却，防止客户端在短时间内大量写入图片。
     */
    public static boolean tryAcquireUpload(Player player) {
        if (player == null) return false;
        MinecraftServer server = player.getServer();
        int now = server != null ? server.getTickCount() : player.tickCount;
        CompoundTag tag = player.getPersistentData();
        int last = tag.getInt(UPLOAD_TICK_TAG);
        if (last > 0 && now >= last && now - last < UPLOAD_COOLDOWN_TICKS) {
            return false;
        }
        tag.putInt(UPLOAD_TICK_TAG, now);
        return true;
    }
}