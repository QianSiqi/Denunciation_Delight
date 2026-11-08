package net.qiansiqi.denunciationdelight.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.qiansiqi.denunciationdelight.client.ClientInkedTracker;

import java.util.function.Supplier;

public class SyncInkedPacket {
    private final int entityId;
    private final int durationTicks;

    public SyncInkedPacket(int entityId, int durationTicks) {
        this.entityId = entityId;
        this.durationTicks = durationTicks;
    }

    public SyncInkedPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.durationTicks = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeInt(this.durationTicks);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientInkedTracker.setInked(this.entityId, this.durationTicks);
            });
        });
        context.setPacketHandled(true);
    }
}
