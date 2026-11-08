package net.qiansiqi.denunciationdelight.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.qiansiqi.denunciationdelight.block.entity.NationalFlagBlockEntity;

import java.util.function.Supplier;

public class SelectFlagPacket {
    private final BlockPos pos;
    private final String countryId;

    public SelectFlagPacket(BlockPos pos, String countryId) {
        this.pos = pos;
        this.countryId = countryId;
    }

    public SelectFlagPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.countryId = buf.readUtf(32);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeUtf(this.countryId, 32);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (!NationalFlagBlockEntity.isValidCountry(this.countryId)) return;

            Level level = player.level();
            if (!level.isLoaded(this.pos)) return;
            if (player.distanceToSqr(Vec3.atCenterOf(this.pos)) > 64.0D) return;
            if (!level.mayInteract(player, this.pos)) return;

            BlockEntity be = level.getBlockEntity(this.pos);
            if (be instanceof NationalFlagBlockEntity flagBE) {
                flagBE.setCountryId(this.countryId);
            }
        });
        ctx.setPacketHandled(true);
    }
}
