package com.promc.payapi.storage;

import com.promc.payapi.api.storage.Storage;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class MySqlStorage extends SQLStorage {

    public MySqlStorage(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void createTable() {
        try (
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
        ) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + Storage.TABLE_NAME + "` (\n" +
                    "    `id` bigint NOT NULL COMMENT '订单号',\n" +
                    "    `subject` varchar(255) NOT NULL COMMENT '订单标题',\n" +
                    "    `buyer` varchar(255) NOT NULL COMMENT '买家名称',\n" +
                    "    `total_fee` float(11,2) NOT NULL COMMENT '订单金额',\n" +
                    "    `status` int(1) NOT NULL COMMENT '订单状态',\n" +
                    "    `create_time` datetime NOT NULL COMMENT '订单创建时间',\n" +
                    "    `pay_time` datetime DEFAULT NULL COMMENT '订单支付时间',\n" +
                    "    `pay_info` text DEFAULT NULL COMMENT '订单支付信息',\n" +
                    "    PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");
        } catch (SQLException e) {
            throw new IllegalStateException("数据库异常", e);
        }
    }
}
