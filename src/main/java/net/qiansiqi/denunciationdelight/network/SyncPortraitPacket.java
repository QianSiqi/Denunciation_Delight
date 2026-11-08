package net.qiansiqi.denunciationdelight.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.qiansiqi.denunciationdelight.client.ClientPortraitCache;

import java.util.function.Supplier;

public class SyncPortraitPacket {
    private final String portraitId;
    private final byte[] imageData;

    public SyncPortraitPacket(String portraitId, byte[] imageData) {
        this.portraitId = portraitId;
        this.imageData = imageData;
    }

    public SyncPortraitPacket(FriendlyByteBuf buf) {
        this.portraitId = buf.readUtf(64);
        this.imageData = buf.readByteArray();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.portraitId, 64);
        buf.writeByteArray(this.imageData);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ClientPortraitCache.registerImage(this.portraitId, this.imageData);
        });
        ctx.setPacketHandled(true);
    }
}
