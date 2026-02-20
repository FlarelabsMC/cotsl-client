package com.flarelabsmc.cotsl.common.storage.type;

import java.sql.SQLException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.BaseDataType;
import com.j256.ormlite.support.DatabaseResults;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * SQL type for Record objects with primitive values.
 * Converts Record objects to string JSON and vice versa.
 */
public class RecordType extends BaseDataType {
    private static final RecordType INSTANCE = new RecordType();
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.module.paramnames.ParameterNamesModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    @SuppressWarnings("unused")
    public static RecordType getSingleton() {
        return INSTANCE;
    }

    private RecordType() {
        super(SqlType.STRING, new Class<?>[]{Record.class});
    }

    @Override
    public Object parseDefaultString(FieldType fieldType, String defaultStr) throws SQLException {
        try {
            return objectMapper.readValue(defaultStr, fieldType.getType());
        } catch (Exception e) {
            throw new SQLException("Failed to parse record: " + e.getMessage(), e);
        }
    }

    @Override
    public Object resultToSqlArg(FieldType fieldType, DatabaseResults results, int columnPos) throws SQLException {
        String json = results.getString(columnPos);
        if (json == null) return null;
        else try {
            return objectMapper.readValue(json, fieldType.getType());
        } catch (Exception e) {
            throw new SQLException("Failed to deserialize record: " + e.getMessage(), e);
        }
    }

    @Override
    public Object javaToSqlArg(FieldType fieldType, Object javaObject) throws SQLException {
        if (javaObject == null) return null;
        else try {
            return objectMapper.writeValueAsString(javaObject);
        } catch (Exception e) {
            throw new SQLException("Failed to serialize record: " + e.getMessage(), e);
        }
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}