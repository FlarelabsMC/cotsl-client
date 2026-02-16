package com.flarelabsmc.cotsl.common.storage.type;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.StringType;
import net.minecraft.resources.Identifier;

public class IdentifierType extends StringType {
    private static final IdentifierType INSTANCE = new IdentifierType();

    protected IdentifierType() {
        super(SqlType.STRING, new Class<?>[] { Identifier.class });
    }

    @SuppressWarnings("unused")
    public static IdentifierType getSingleton() {
        return INSTANCE;
    }

    @Override
    public Object javaToSqlArg(FieldType fieldType, Object javaObject) {
        Identifier rl = (Identifier) javaObject;
        return rl == null ? null : rl.toString();
    }

    @Override
    public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
        String str = (String) sqlArg;
        return str == null ? null : Identifier.parse(str);
    }
}

