package com.flarelabsmc.cotsl.common.network;

import com.flarelabsmc.cotsl.common.network.packets.RequestUserDataPacket;
import com.flarelabsmc.cotsl.common.network.packets.SendUserDataPacket;
import com.flarelabsmc.cotsl.common.storage.user.PermanentUser;
import com.flarelabsmc.cotsl.common.storage.user.PermanentUserStorage;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber
public class NetworkHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, PermanentUser> clientCache = new HashMap<>();
    private static final Set<UUID> pendingRequests = ConcurrentHashMap.newKeySet();

    public static PermanentUser getCachedUserData(UUID uuid) {
        return clientCache.get(uuid);
    }

    public static boolean tryAddPendingRequest(UUID uuid) {
        return pendingRequests.add(uuid);
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0.0");

        registrar.playToClient(
                SendUserDataPacket.TYPE,
                SendUserDataPacket.STREAM_CODEC,
                NetworkHandler::handleUserDataResponse
        );

        registrar.playToServer(
                RequestUserDataPacket.TYPE,
                RequestUserDataPacket.STREAM_CODEC,
                NetworkHandler::handleRequestUserData
        );
    }

    private static void handleUserDataResponse(SendUserDataPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            PermanentUser userData = packet.userData();
            PermanentUser existing = clientCache.get(userData.getUUID());

            if (existing != null) LOGGER.debug("Updating cached user data for UUID: {}", userData.getUUID());
            else LOGGER.debug("Caching new user data for UUID: {}", userData.getUUID());

            clientCache.put(userData.getUUID(), userData);
        });
    }

    private static void handleRequestUserData(RequestUserDataPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                var userData = PermanentUserStorage.getUserData(packet.uuid());
                if (userData != null) context.reply(new SendUserDataPacket(userData));
                else LOGGER.warn("No user data found for UUID: {}", packet.uuid());
            } catch (Exception e) {
                LOGGER.error("Failed to retrieve user data for UUID: {}", packet.uuid(), e);
            }
        });
    }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        clientCache.clear();
        pendingRequests.clear();
    }
}