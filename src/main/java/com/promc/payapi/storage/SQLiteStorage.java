package com.promc.payapi.storage;

import com.promc.payapi.api.storage.Storage;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteStorage extends SQLStorage {
    public SQLiteStorage(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void createTable() {
        try (
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
        ) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + Storage.TABLE_NAME + "` (\n" +
                    "    `id` INTEGER PRIMARY KEY,\n" +
                    "    `subject` TEXT NOT NULL,\n" +
                    "    `buyer` TEXT NOT NULL,\n" +
                    "    `total_fee` NUMERIC NOT NULL,\n" +
                    "    `status` INTEGER NOT NULL,\n" +
                    "    `create_time` TIMESTAMP NOT NULL,\n" +
                    "    `pay_time` TIMESTAMP DEFAULT NULL,\n" +
                    "    `pay_info` TEXT DEFAULT NULL\n" +
                    ");");
        } catch (SQLException e) {
            throw new IllegalStateException("数据库异常", e);
        }
    }
}
