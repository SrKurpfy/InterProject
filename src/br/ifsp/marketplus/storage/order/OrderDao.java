package br.ifsp.marketplus.storage.order;

import br.ifsp.marketplus.model.Order;
import br.ifsp.marketplus.model.OrderProduct;
import br.ifsp.marketplus.storage.Dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class OrderDao implements Dao<UUID, Order> {

    private final Connection connection;

    private final OrderAdapter adapter;
    private final OrderProductAdapter productAdapter;

    public OrderDao(Connection connection) {
        this.connection = connection;
        this.adapter = new OrderAdapter();
        this.productAdapter = new OrderProductAdapter();
    }

    public void createTable() {
        try (PreparedStatement statement = connection.prepareStatement(
          "CREATE TABLE IF NOT EXISTS `orders` (" +
            "id           CHAR(36) NOT NULL PRIMARY KEY, " +
            "client_id    CHAR(36) NOT NULL, " +
            "total_price  DOUBLE NOT NULL, " +
            "emitted_date LONG NOT NULL, " +
            "FOREIGN KEY (client_id) REFERENCES clients(id));" +
            "" +
            "CREATE TABLE IF NOT EXISTS `order_products` (" +
            "product_id CHAR(36) NOT NULL, " +
            "order_id CHAR(36) NOT NULL, " +
            "amount   INT NOT NULL, " +
            "FOREIGN KEY (product_id) REFERENCES products(id), " +
            "FOREIGN KEY (order_id) REFERENCES orders(id));"
        )) {
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void insertOrUpdate(UUID key, Order order) {
        try (PreparedStatement statement = connection.prepareStatement(
          "INSERT INTO `orders` VALUES (?, ?, ?, ?);"
        )) {
            adapter.insert(statement, order);

            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        for (OrderProduct product : order.getProducts()) {
            try (PreparedStatement statement = connection.prepareStatement(
              "INSERT INTO `order_products` VALUES (?, ?, ?);"
            )) {
                productAdapter.insert(statement, product);

                statement.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public Collection<Order> getAll() {
        Set<Order> orders = new HashSet<>();

        try (PreparedStatement statement = connection.prepareStatement(
          "SELECT " +
            "orders.id AS o_id, " +
            "orders.client_id AS o_client_id, " +
            "orders.total_price AS o_total_price, " +
            "orders.emitted_date AS o_emitted_date, " +
            "order_products.product_id AS op_product_id, " +
            "order_products.order_id AS op_order_id, " +
            "order_products.amount AS op_amount, " +
            "FROM `orders` " +
            "INNER JOIN order_products ON orders.id = order_products.order_id;"
        )) {
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                orders.add(adapter.read(resultSet));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return orders;
    }

}