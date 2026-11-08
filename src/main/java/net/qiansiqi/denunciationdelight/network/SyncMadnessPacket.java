package net.qiansiqi.denunciationdelight.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.qiansiqi.denunciationdelight.madness.MadnessManager;

import java.util.function.Supplier;

public class SyncMadnessPacket {
    private final int madness;

    public SyncMadnessPacket(int madness) {
        this.madness = madness;
    }

    public SyncMadnessPacket(FriendlyByteBuf buf) {
        this.madness = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.madness);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                MadnessManager.setClientMadness(this.madness);
            });
        });
        context.setPacketHandled(true);
    }
}
