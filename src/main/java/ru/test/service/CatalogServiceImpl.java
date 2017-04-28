package ru.test.service;

import ru.test.entities.Node;

import javax.ejb.*;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

@Stateless
@TransactionManagement(value = TransactionManagementType.BEAN)
public class CatalogServiceImpl implements CatalogService {

    @EJB(lookup = "java:jboss/datasources/MysqlXADS")
    private DataSource dataSource;

    @Override
    public boolean add(Map<String, String> params) {
        Node node = new Node();
        node.setParentId(Integer.valueOf(params.get("parentId")));
        node.setName(escapeHtml4(params.get("name")));
        Map<String, String> map = new HashMap<>();
        for (String key : params.keySet()) {
            if (key.matches("f\\d+")) {
                String value = escapeHtml4(params.get(key.replace('f', 'v')));
                if (value != null) {
                    map.put(escapeHtml4(params.get(key)), value);
                }
            }
        }
        node.setData(map);
        String AIQuery = "SELECT `AUTO_INCREMENT` FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'catalog' AND TABLE_NAME = 'node'";
        String nodeQuery = "INSERT INTO `node`(`parent_id`, `name`) VALUES (?, ?)";
        String dataQuery = "INSERT INTO `data`(`node_id`, `field_name`, `value`) VALUES (?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement aiStatement = connection.prepareStatement(AIQuery);
             PreparedStatement nodeStatement = connection.prepareStatement(nodeQuery);
             PreparedStatement dataStatement = connection.prepareStatement(dataQuery)) {
            connection.setAutoCommit(false);
            ResultSet resultSet = aiStatement.executeQuery();
            resultSet.next();
            long nextId = resultSet.getLong(1);
            nodeStatement.setLong(1, node.getParentId());
            nodeStatement.setString(2, node.getName());
            nodeStatement.executeUpdate();
            for (Map.Entry<String, String> entry : node.getData().entrySet()) {
                dataStatement.setLong(1, nextId);
                dataStatement.setString(2, entry.getKey());
                dataStatement.setString(3, entry.getValue());
                dataStatement.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            return false;
        }
        return true;
    }

    @Override
    public Node get(long id) {
        String query = "SELECT `id`, `name` FROM `node` WHERE `id` = ?";

        return null;
    }

    @Override
    public List<Node> getTree(long parentId) {
        String query;
        if (parentId == 0) {
            query = "SELECT `id`, `name` FROM `node` WHERE `parent_id` IS NULL";
        } else {
            query = "SELECT `id`, `name` FROM `node` WHERE `parent_id` = ?";
        }
        List<Node> list = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            if (parentId > 0) {
                statement.setLong(1, parentId);
            }
            ResultSet resultSet = statement.executeQuery();
            Node node;
            while (resultSet.next()) {
                node = new Node();
                node.setId(resultSet.getLong("id"));
                node.setParentId(parentId);
                node.setName(resultSet.getString("name"));
                list.add(node);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public boolean remove(long id) {
        String query;
        if (id == 0) {
            query = "DELETE FROM `node` WHERE `id` IS NULL";
        } else {
            query = "DELETE FROM `node` WHERE `id` = ?";
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            if (id > 0) {
                statement.setLong(1, id);
            }
            int code = statement.executeUpdate();
            return code != 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
