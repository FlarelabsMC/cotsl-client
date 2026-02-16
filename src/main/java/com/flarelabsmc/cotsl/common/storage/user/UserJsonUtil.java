package com.flarelabsmc.cotsl.common.storage.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.GenericRawResults;

import java.sql.SQLException;
import java.util.function.Function;

public class UserJsonUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static <T, U, V> V getOrDefault(
            Dao<T, U> dao,
            U id,
            String tableName,
            String idColumn,
            String jsonColumn,
            String fieldPath,
            V defaultValue,
            Function<String, V> parser
    ) throws SQLException {
        GenericRawResults<String[]> results = dao.queryRaw(
                "SELECT json_extract(" + jsonColumn + ", '$." + fieldPath + "') FROM " + tableName + " WHERE " + idColumn + " = ?",
                id.toString()
        );
        String[] firstResult = results.getFirstResult();
        return firstResult != null && firstResult[0] != null ? parser.apply(firstResult[0]) : defaultValue;
    }

    public static <T, U> void update(
            Dao<T, U> dao,
            U id,
            String tableName,
            String idColumn,
            String jsonColumn,
            String fieldPath,
            Object value
    ) throws SQLException {
        dao.executeRaw(
                "UPDATE " + tableName + " SET " + jsonColumn + " = json_set(" + jsonColumn + ", '$." + fieldPath + "', ?) WHERE " + idColumn + " = ?",
                String.valueOf(value), id.toString()
        );
    }

    public static <T, U> String getStringOrDefault(Dao<T, U> dao, U id, String tableName, String idColumn, String jsonColumn, String fieldPath, String defaultValue) throws SQLException {
        return getOrDefault(dao, id, tableName, idColumn, jsonColumn, fieldPath, defaultValue, s -> s);
    }

    public static <T, U> int getIntOrDefault(Dao<T, U> dao, U id, String tableName, String idColumn, String jsonColumn, String fieldPath, int defaultValue) throws SQLException {
        return getOrDefault(dao, id, tableName, idColumn, jsonColumn, fieldPath, defaultValue, Integer::parseInt);
    }

    public static <T, U> boolean getBooleanOrDefault(Dao<T, U> dao, U id, String tableName, String idColumn, String jsonColumn, String fieldPath, boolean defaultValue) throws SQLException {
        return getOrDefault(dao, id, tableName, idColumn, jsonColumn, fieldPath, defaultValue, s -> s.equals("1") || Boolean.parseBoolean(s));
    }

    public static <T, U> double getDoubleOrDefault(Dao<T, U> dao, U id, String tableName, String idColumn, String jsonColumn, String fieldPath, double defaultValue) throws SQLException {
        return getOrDefault(dao, id, tableName, idColumn, jsonColumn, fieldPath, defaultValue, Double::parseDouble);
    }

    public static <T, U> long getLongOrDefault(Dao<T, U> dao, U id, String tableName, String idColumn, String jsonColumn, String fieldPath, long defaultValue) throws SQLException {
        return getOrDefault(dao, id, tableName, idColumn, jsonColumn, fieldPath, defaultValue, Long::parseLong);
    }

    public static <T, U> float getFloatOrDefault(Dao<T, U> dao, U id, String tableName, String idColumn, String jsonColumn, String fieldPath, float defaultValue) throws SQLException {
        return getOrDefault(dao, id, tableName, idColumn, jsonColumn, fieldPath, defaultValue, Float::parseFloat);
    }
}