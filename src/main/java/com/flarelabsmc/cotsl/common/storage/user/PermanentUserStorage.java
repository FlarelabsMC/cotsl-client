package com.flarelabsmc.cotsl.common.storage.user;

import com.flarelabsmc.cotsl.common.network.packets.SendUserDataPacket;
import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;

public class PermanentUserStorage {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static DBAccess access;
    private static final int COMPRESSION_LEVEL = 3;

    public static void updateAndBroadcast(PermanentUser user, MinecraftServer server) throws SQLException {
        mergeUserData(user);
        PacketDistributor.sendToAllPlayers(new SendUserDataPacket(user));
        LOGGER.debug("Broadcasted updated user data for UUID: {}", user.getUUID());
    }

    public static void init(MinecraftServer server) {
        Path zestyPath = server.getWorldPath(LevelResource.ROOT).resolve("cotsl").resolve("players.db.zst");
        try {
            Files.createDirectories(zestyPath.getParent());
            ConnectionSource connection = new JdbcConnectionSource("jdbc:sqlite::memory:");
            TableUtils.createTableIfNotExists(connection, PermanentUser.class);
            access = new DBAccess(connection, zestyPath);
            if (Files.exists(zestyPath)) {
                Path tempPath = Files.createTempFile("players_tmp", ".db");
                try {
                    decompress(zestyPath, tempPath);
                    Dao<PermanentUser, UUID> dao = createDao();
                    String normalizedPath = tempPath.toString().replace("\\", "/");
                    dao.executeRaw("ATTACH DATABASE '" + normalizedPath + "' AS disk");
                    dao.executeRaw("INSERT OR REPLACE INTO users SELECT * FROM disk.users");
                    dao.executeRaw("DETACH DATABASE disk");

                    LOGGER.debug("Loaded existing user data into memory");
                } catch (Exception e) {
                    LOGGER.error("Failed to load existing database", e);
                } finally {
                    Files.deleteIfExists(tempPath);
                }
            } else LOGGER.debug("No existing database found, setup imminent");
            LOGGER.debug("Database initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize database", e);
        }
    }

    public static void disconnect() throws SQLException {
        try {
            if (access == null || !access.connection.isOpen("users")) return;
            Path tempPath = Files.createTempFile("players_temp", ".db");
            try {
                Dao<PermanentUser, UUID> dao = createDao();
                String normalizedPath = tempPath.toString().replace("\\", "/");
                dao.executeRaw("ATTACH DATABASE '" + normalizedPath + "' AS disk");
                dao.executeRaw("CREATE TABLE disk.users AS SELECT * FROM users");
                dao.executeRaw("DETACH DATABASE disk");
                compress(tempPath, access.compressedPath);
                LOGGER.info("Successfully saved player database to disk");
            } finally {
                Files.deleteIfExists(tempPath);
            }
            access.connection.close();
            access = null;
        } catch (Exception e) {
            LOGGER.error("Failed to save player database to disk", e);
            throw new SQLException(e);
        }
    }

    private static void compress(Path source, Path target) throws IOException {
        LOGGER.debug("Compressing user database...");
        long startTime = System.currentTimeMillis();
        long originalSize = Files.size(source);
        try (InputStream in = Files.newInputStream(source);
             OutputStream out = new ZstdOutputStream(Files.newOutputStream(target), COMPRESSION_LEVEL)) {
            in.transferTo(out);
        }
        long size = Files.size(target);
        long elapsed = System.currentTimeMillis() - startTime;
        LOGGER.debug("Compressed {} bytes to {} bytes in {}ms", originalSize, size, elapsed);
    }

    private static void decompress(Path source, Path target) throws IOException {
        LOGGER.debug("Decompressing user database...");
        long startTime = System.currentTimeMillis();
        try (InputStream in = new ZstdInputStream(Files.newInputStream(source));
             OutputStream out = Files.newOutputStream(target)) {
            in.transferTo(out);
        }
        long elapsed = System.currentTimeMillis() - startTime;
        LOGGER.debug("Decompressed {} bytes in {}ms", Files.size(target), elapsed);
    }

    public static Dao<PermanentUser, UUID> createDao() throws SQLException {
        if (access == null) throw new SQLException("Database not initialized");
        return DaoManager.createDao(access.connection, PermanentUser.class);
    }

    public static void registerPlayer(UUID uuid) throws SQLException {
        mergeUserData(PermanentUser.init(uuid));
    }

    public static PermanentUser getUserData(UUID uuid) throws SQLException {
        return createDao().queryForId(uuid);
    }

    public static void mergeUserData(PermanentUser user) throws SQLException {
        createDao().createOrUpdate(user);
    }

    public record DBAccess(ConnectionSource connection, Path compressedPath) { }
}