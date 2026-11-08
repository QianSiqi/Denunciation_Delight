package net.qiansiqi.denunciationdelight.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.qiansiqi.denunciationdelight.portrait.ServerPortraitManager;

import java.util.function.Supplier;

public class RequestPortraitPacket {
    private final String portraitId;

    public RequestPortraitPacket(String portraitId) {
        this.portraitId = portraitId;
    }

    public RequestPortraitPacket(FriendlyByteBuf buf) {
        this.portraitId = buf.readUtf(64);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.portraitId, 64);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && ServerPortraitManager.isValidPortraitId(this.portraitId)) {
                byte[] data = ServerPortraitManager.getImageData(this.portraitId);
                if (data != null && data.length > 0) {
                    ModNetworking.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                            new SyncPortraitPacket(this.portraitId, data));
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
