package com.flarelabsmc.cotsl.common.storage.user;

import com.flarelabsmc.cotsl.common.network.packets.SendUserDataPacket;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.util.UUID;

@EventBusSubscriber
public class PermanentUserHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {}

    @SubscribeEvent
    public static void serverStarting(ServerStartingEvent event) {
        PermanentUserStorage.init(event.getServer());
    }

    @SubscribeEvent
    public static void playerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        try {
            UUID uuid = event.getEntity().getUUID();
            if (PermanentUserStorage.getUserData(uuid) != null) LOGGER.debug("Player data already exists for {}, with {}", uuid, PermanentUserStorage.getUserData(uuid).getCharacterData());
            PermanentUserStorage.registerPlayer(uuid);
        } catch (SQLException e) {
            LOGGER.error("Failed to register player data", e);
        }
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event) {
        try {
            PermanentUserStorage.disconnect();
        } catch (SQLException e) {
            LOGGER.error("Failed to disconnect from player database", e);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) throws SQLException {
        if (event.getLevel().isClientSide()) return;

        if (event.getEntity() instanceof Player player) {
            UUID uuid = player.getUUID();
            PermanentUser data = PermanentUserStorage.getUserData(uuid);
            if (data != null) {
                PacketDistributor.sendToAllPlayers(new SendUserDataPacket(data));
            }
        }
    }
}
