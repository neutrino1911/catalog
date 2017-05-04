package ru.test.service;

import ru.test.entities.Field;
import ru.test.entities.Node;

import javax.ejb.*;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Stateless
@TransactionManagement(value = TransactionManagementType.BEAN)
public class CatalogServiceImpl implements CatalogService {

    @EJB(lookup = "java:jboss/datasources/MysqlXADS")
    private DataSource dataSource;

    @Override
    public Node add(Node node) {
        String AIQuery = "SELECT `AUTO_INCREMENT` FROM INFORMATION_SCHEMA.TABLES" +
                " WHERE TABLE_SCHEMA = 'catalog' AND TABLE_NAME = 'node'";
        String nodeQuery = "INSERT INTO `node`(`parent_id`, `name`) VALUES (?, ?)";
        String fieldQuery = "INSERT INTO `field`(`node_id`, `name`, `value`) VALUES (?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement AIStatement = connection.prepareStatement(AIQuery);
             PreparedStatement nodeStatement = connection.prepareStatement(nodeQuery);
             PreparedStatement dataStatement = connection.prepareStatement(fieldQuery)) {
            connection.setAutoCommit(false);
            ResultSet resultSet = AIStatement.executeQuery();
            resultSet.next();
            long nextId = resultSet.getLong(1);
            node.setId(nextId);
            if (node.getParentId() == 0) {
                nodeStatement.setNull(1, Types.BIGINT);
            } else {
                nodeStatement.setLong(1, node.getParentId());
            }
            nodeStatement.setString(2, node.getName());
            nodeStatement.executeUpdate();
            if (node.getFields() != null) {
                for (Field field : node.getFields()) {
                    dataStatement.setLong(1, nextId);
                    dataStatement.setString(2, field.getName());
                    dataStatement.setString(3, field.getValue());
                    dataStatement.executeUpdate();
                }
            }
            node.setFields(new ArrayList<Field>());
            connection.commit();
        } catch (SQLException e) {
            return null;
        }
        return node;
    }

    @Override
    public List<Node> find(String text) {
        String findQuery = "SELECT `id`, `parent_id`, `name` FROM `node` WHERE `name` LIKE ?";
        List<Node> nodes = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement nodeStatement = connection.prepareStatement(findQuery)) {
            nodeStatement.setString(1, "%" + text + "%");
            ResultSet resultSet = nodeStatement.executeQuery();
            while (resultSet.next()) {
                Node node = new Node();
                node.setId(resultSet.getLong("id"));
                node.setParentId(resultSet.getLong("parent_id"));
                node.setName(resultSet.getString("name"));
                nodes.add(node);
            }
        } catch (SQLException e) {
            return null;
        }
        return nodes;
    }

    @Override
    public Node get(long nodeId) {
        String nodeQuery = "SELECT `parent_id`, `name` FROM `node` WHERE `id` = ?";
        String fieldQuery = "SELECT `id`, `name`, `value` FROM `field` WHERE `node_id` = ?";
        Node node = new Node(nodeId);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement nodeStatement = connection.prepareStatement(nodeQuery);
             PreparedStatement dataStatement = connection.prepareStatement(fieldQuery)) {
            nodeStatement.setLong(1, nodeId);
            ResultSet resultSet = nodeStatement.executeQuery();
            resultSet.next();
            node.setParentId(resultSet.getLong("parent_id"));
            node.setName(resultSet.getString("name"));
            dataStatement.setLong(1, nodeId);
            resultSet = dataStatement.executeQuery();
            List<Field> list = new ArrayList<>();
            while (resultSet.next()) {
                list.add(new Field(
                        resultSet.getLong("id"),
                        nodeId,
                        resultSet.getString("name"),
                        resultSet.getString("value")
                ));
            }
            node.setFields(list);
        } catch (SQLException e) {
            return null;
        }
        return node;
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
                node.setFields(new ArrayList<Field>());
                list.add(node);
            }
        } catch (SQLException e) {
            return null;
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
        return removeById(query, id);
    }

    @Override
    public boolean removeField(long id) {
        String query = "DELETE FROM `field` WHERE `id` = ?";
        return removeById(query, id);
    }

    @Override
    public Node update(Node node) {
        String nodeQuery = "UPDATE `node` SET `parent_id` = ?, `name` = ? WHERE `id` = ?";
        String fieldUpdateQuery = "UPDATE `field` SET `name` = ?, `value` = ? WHERE `id` = ?";
        String fieldInsertQuery = "INSERT INTO `field`(`node_id`, `name`, `value`) VALUES (?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement nodeStatement = connection.prepareStatement(nodeQuery);
             PreparedStatement fieldUpdateStatement = connection.prepareStatement(fieldUpdateQuery);
             PreparedStatement fieldInsertStatement = connection.prepareStatement(fieldInsertQuery)) {
            connection.setAutoCommit(false);
            if (node.getParentId() == 0) {
                nodeStatement.setNull(1, Types.BIGINT);
            } else {
                nodeStatement.setLong(1, node.getParentId());
            }
            nodeStatement.setString(2, node.getName());
            nodeStatement.setLong(3, node.getId());
            nodeStatement.executeUpdate();
            if (node.getFields() != null) {
                for (Field field : node.getFields()) {
                    if (field.getId() > 0) {
                        fieldUpdateStatement.setString(1, field.getName());
                        fieldUpdateStatement.setString(2, field.getValue());
                        fieldUpdateStatement.setLong(3, field.getId());
                        int code = fieldUpdateStatement.executeUpdate();
                        if (code == 0) {
                            return null;
                        }
                    } else {
                        fieldInsertStatement.setLong(1, node.getId());
                        fieldInsertStatement.setString(2, field.getName());
                        fieldInsertStatement.setString(3, field.getValue());
                        fieldInsertStatement.executeUpdate();
                    }
                }
            }
            node.setFields(new ArrayList<Field>());
            connection.commit();
        } catch (SQLException e) {
            return null;
        }
        return node;
    }

    private boolean removeById(String query, long id) {
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
