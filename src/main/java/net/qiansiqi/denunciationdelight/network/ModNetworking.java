package net.qiansiqi.denunciationdelight.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.qiansiqi.denunciationdelight.DenunciationDelight;

public class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DenunciationDelight.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private static int nextId() {
        return packetId++;
    }

    public static void register() {
        INSTANCE.messageBuilder(UpdatePlacardPacket.class, nextId())
                .encoder(UpdatePlacardPacket::toBytes)
                .decoder(UpdatePlacardPacket::new)
                .consumerMainThread(UpdatePlacardPacket::handle)
                .add();

        INSTANCE.messageBuilder(UpdateSloganBannerPacket.class, nextId())
                .encoder(UpdateSloganBannerPacket::toBytes)
                .decoder(UpdateSloganBannerPacket::new)
                .consumerMainThread(UpdateSloganBannerPacket::handle)
                .add();

        INSTANCE.messageBuilder(UploadPortraitPacket.class, nextId())
                .encoder(UploadPortraitPacket::toBytes)
                .decoder(UploadPortraitPacket::new)
                .consumerMainThread(UploadPortraitPacket::handle)
                .add();

        INSTANCE.messageBuilder(RequestPortraitPacket.class, nextId())
                .encoder(RequestPortraitPacket::toBytes)
                .decoder(RequestPortraitPacket::new)
                .consumerMainThread(RequestPortraitPacket::handle)
                .add();

        INSTANCE.messageBuilder(SyncPortraitPacket.class, nextId())
                .encoder(SyncPortraitPacket::toBytes)
                .decoder(SyncPortraitPacket::new)
                .consumerMainThread(SyncPortraitPacket::handle)
                .add();

        INSTANCE.messageBuilder(SelectFlagPacket.class, nextId())
                .encoder(SelectFlagPacket::toBytes)
                .decoder(SelectFlagPacket::new)
                .consumerMainThread(SelectFlagPacket::handle)
                .add();

        INSTANCE.messageBuilder(UpdateConeHatPacket.class, nextId())
                .encoder(UpdateConeHatPacket::toBytes)
                .decoder(UpdateConeHatPacket::new)
                .consumerMainThread(UpdateConeHatPacket::handle)
                .add();

        INSTANCE.messageBuilder(SyncInkedPacket.class, nextId())
                .encoder(SyncInkedPacket::toBytes)
                .decoder(SyncInkedPacket::new)
                .consumerMainThread(SyncInkedPacket::handle)
                .add();

        INSTANCE.messageBuilder(SyncMadnessPacket.class, nextId())
                .encoder(SyncMadnessPacket::toBytes)
                .decoder(SyncMadnessPacket::new)
                .consumerMainThread(SyncMadnessPacket::handle)
                .add();

        INSTANCE.messageBuilder(SyncSuicideAnimationPacket.class, nextId())
                .encoder(SyncSuicideAnimationPacket::toBytes)
                .decoder(SyncSuicideAnimationPacket::new)
                .consumerMainThread(SyncSuicideAnimationPacket::handle)
                .add();
    }

    public static void sendToServer(Object message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToTracking(MSG message, net.minecraft.world.entity.Entity entity) {
        INSTANCE.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), message);
    }

    public static <MSG> void sendToPlayer(MSG message, net.minecraft.server.level.ServerPlayer player) {
        INSTANCE.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), message);
    }
}
