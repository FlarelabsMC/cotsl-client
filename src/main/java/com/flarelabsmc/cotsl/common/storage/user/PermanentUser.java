package com.flarelabsmc.cotsl.common.storage.user;

import com.flarelabsmc.cotsl.common.storage.player.CharData;
import com.flarelabsmc.cotsl.common.storage.type.RecordType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.ApiStatus;

import java.util.UUID;

@DatabaseTable(tableName = "users")
public class PermanentUser {
    @DatabaseField(id = true)
    private UUID uuid;
    @DatabaseField(persisterClass = RecordType.class)
    private CharData characterData;

    public PermanentUser() {}

    public PermanentUser(UUID uuid, CharData charData) {
        this.uuid = uuid;
        this.characterData = charData;
    }

    public static PermanentUser init(UUID uuid) {
        PermanentUser user = new PermanentUser();
        CharData charData = CharData.init();
        user.uuid = uuid;
        user.characterData = charData;
        return user;
    }

    public static final StreamCodec<ByteBuf, PermanentUser> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, user -> user.uuid.toString(),
            CharData.STREAM_CODEC, PermanentUser::getCharacterData,
            (uuidStr, charData) -> new PermanentUser(UUID.fromString(uuidStr), charData)
    );

    public UUID getUUID() {
        return uuid;
    }

    public CharData getCharacterData() {
        return characterData != null ? characterData : CharData.init();
    }

    // ONLY USE FOR TESTING PURPOSES, THIS WILL CAUSE DATA INCONSISTENCY
    @ApiStatus.Internal
    public void setCharacterData(CharData characterData) {
        this.characterData = characterData;
    }
}
