package com.promc.payapi.storage;

import com.promc.payapi.api.order.Order;
import com.promc.payapi.api.storage.Storage;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.sql.*;
import java.util.UUID;

public abstract class SQLStorage implements Storage {

    protected final DataSource dataSource;

    public SQLStorage(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void insertOrder(@NotNull Order order) {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("INSERT INTO `" + TABLE_NAME + "` (`id`,`buyer`,`subject`,`total_fee`,`status`,`create_time`,`pay_time`,`pay_info`) VALUES (?,?,?,?,?,?,?,?);")
        ) {
            statement.setLong(1, order.getId());
            statement.setString(2, order.getBuyer().toString());
            statement.setString(3, order.getSubject());
            statement.setBigDecimal(4, order.getTotalFee());
            statement.setInt(5, order.getStatus());
            statement.setTimestamp(6, order.getCreateTime());
            statement.setTimestamp(7, null);
            statement.setString(8, null);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("数据库异常", e);
        }
    }

    @Override
    public Order selectOrderById(long orderId) {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT `id`,`buyer`,`subject`,`total_fee`,`status`,`create_time`,`pay_time`,`pay_info` FROM " + TABLE_NAME + " WHERE `id` = ?;")
        ) {
            statement.setLong(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Order order = new Order();
                    order.setId(resultSet.getLong(1));
                    order.setBuyer(UUID.fromString(resultSet.getString(2)));
                    order.setSubject(resultSet.getString(3));
                    order.setTotalFee(resultSet.getBigDecimal(4));
                    order.setStatus(resultSet.getInt(5));
                    order.setCreateTime(resultSet.getTimestamp(6));
                    order.setPayTime(resultSet.getTimestamp(7));
                    order.setPayInfo(resultSet.getString(8));
                    return order;
                }
                return null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean markPay(long orderId, @NotNull String info) {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("UPDATE `" + TABLE_NAME + "` SET `status` = 1, `pay_time` = ?, `pay_info` = ? WHERE `id` = ? AND `status` = 0;")
        ) {
            statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            statement.setString(2, info);
            statement.setLong(3, orderId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
