package net.qiansiqi.denunciationdelight.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.qiansiqi.denunciationdelight.client.ClientSuicideTracker;

import java.util.function.Supplier;

public class SyncSuicideAnimationPacket {
    private final int entityId;
    private final int suicideType; // 0: GUN, 1: BLADE, 2: WALL_SLAM
    private final int totalTicks;

    public SyncSuicideAnimationPacket(int entityId, int suicideType, int totalTicks) {
        this.entityId = entityId;
        this.suicideType = suicideType;
        this.totalTicks = totalTicks;
    }

    public SyncSuicideAnimationPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.suicideType = buf.readInt();
        this.totalTicks = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeInt(this.suicideType);
        buf.writeInt(this.totalTicks);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientSuicideTracker.startAnimation(this.entityId, this.suicideType, this.totalTicks);
            });
        });
        context.setPacketHandled(true);
    }
}
