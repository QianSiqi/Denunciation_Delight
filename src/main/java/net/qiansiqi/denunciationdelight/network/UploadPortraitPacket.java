package net.qiansiqi.denunciationdelight.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.qiansiqi.denunciationdelight.block.entity.WallPortraitBlockEntity;
import net.qiansiqi.denunciationdelight.config.ModConfig;
import net.qiansiqi.denunciationdelight.portrait.ServerPortraitManager;

import java.util.function.Supplier;

public class UploadPortraitPacket {
    private final BlockPos pos;
    private final byte[] imageData;

    public UploadPortraitPacket(BlockPos pos, byte[] imageData) {
        this.pos = pos;
        this.imageData = imageData;
    }

    public UploadPortraitPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.imageData = buf.readByteArray();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeByteArray(this.imageData);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            if (!ModConfig.ALLOW_PORTRAIT_UPLOAD.get()) {
                player.displayClientMessage(
                        Component.translatable("message.denunciationdelight.portrait_disabled"), true);
                return;
            }

            Level level = player.level();
            if (!level.isLoaded(this.pos)) return;
            // 必须先确认目标确实是画像方块，再执行任何写入
            if (!(level.getBlockEntity(this.pos) instanceof WallPortraitBlockEntity portraitBE)) return;
            if (player.distanceToSqr(Vec3.atCenterOf(this.pos)) > 64.0D) return;
            if (!level.mayInteract(player, this.pos)) return;
            // 限流 + PNG 文件头校验，避免任意文件落盘/磁盘塞满
            if (!ServerPortraitManager.tryAcquireUpload(player)) return;
            if (!ServerPortraitManager.isValidImageData(this.imageData)) return;

            String portraitId = ServerPortraitManager.saveImage(this.imageData);
            if (!portraitId.isEmpty()) {
                portraitBE.setPortraitId(portraitId);
            }
        });
        ctx.setPacketHandled(true);
    }
}
