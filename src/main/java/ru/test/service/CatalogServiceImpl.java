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
    public Node addNode(Node node) {
        if (!checkNode(node)) return null;
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
    public List<Node> findNodes(String text, long page) {
        text = text.replaceAll("%", "\\\\%");
        text = text.replaceAll("_", "\\\\_");
        String findQuery = "SELECT `a`.`id`, `a`.`parent_id`, `a`.`name`, COUNT(`b`.`parent_id`) AS `children_count` " +
                "FROM `node` `a` " +
                "LEFT OUTER JOIN `node` `b` ON `a`.`id` = `b`.`parent_id` " +
                "WHERE `a`.`name` LIKE ? " +
                "GROUP BY `a`.`id` ORDER BY `name` ASC LIMIT ?, 25";
        List<Node> nodes = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(findQuery)) {
            statement.setString(1, "%" + text + "%");
            statement.setLong(2, page * 25);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Node node = new Node();
                node.setId(resultSet.getLong("id"));
                node.setParentId(resultSet.getLong("parent_id"));
                node.setName(resultSet.getString("name"));
                node.setChildrenCount(resultSet.getLong("children_count"));
                nodes.add(node);
            }
        } catch (SQLException e) {
            return null;
        }
        return nodes;
    }

    @Override
    public Node getNode(long nodeId) {
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
            e.printStackTrace();
            return null;
        }
        return node;
    }

    @Override
    public List<Node> getNodes(long parentId, long page) {
        String query;
        if (parentId == 0) {
            query = "SELECT `a`.`id`, `a`.`name`, COUNT(`b`.`parent_id`) AS `children_count` " +
                    "FROM `node` `a` " +
                    "LEFT OUTER JOIN `node` `b` ON `a`.`id` = `b`.`parent_id` " +
                    "WHERE `a`.`parent_id` IS NULL " +
                    "GROUP BY `a`.`id` ORDER BY `name` ASC LIMIT ?, 25";
        } else {
            query = "SELECT `a`.`id`, `a`.`name`, COUNT(`b`.`parent_id`) AS `children_count` " +
                    "FROM `node` `a` " +
                    "LEFT OUTER JOIN `node` `b` ON `a`.`id` = `b`.`parent_id` " +
                    "WHERE `a`.`parent_id` = ? " +
                    "GROUP BY `a`.`id` ORDER BY `name` ASC LIMIT ?, 25";
        }
        if (parentId > 0) {
            query = query.replaceAll(" LIMIT \\?, 25", "");
        }
        List<Node> list = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            if (parentId > 0) {
                statement.setLong(1, parentId);
                //statement.setLong(2, page * 25);
            } else {
                statement.setLong(1, page * 25);
            }
            ResultSet resultSet = statement.executeQuery();
            Node node;
            while (resultSet.next()) {
                node = new Node();
                node.setId(resultSet.getLong("id"));
                node.setParentId(parentId);
                node.setName(resultSet.getString("name"));
                node.setChildrenCount(resultSet.getLong("children_count"));
                node.setFields(new ArrayList<Field>());
                list.add(node);
            }
        } catch (SQLException e) {
            return null;
        }
        return list;
    }

    @Override
    public boolean removeNode(long id) {
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
    public Node updateNode(Node node) {
        if (!checkNode(node)) return null;
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

    @Override
    public void mock(long count) {
        //String countQuery = "SELECT COUNT(*) FROM `node`";
        String nodeQuery = "INSERT INTO `node`(`parent_id`, `name`) VALUES (?, ?)";
        String fieldQuery = "INSERT INTO `field`(`node_id`, `name`, `value`) VALUES (?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             //PreparedStatement countStatement = connection.prepareStatement(countQuery);
             PreparedStatement nodeStatement = connection.prepareStatement(nodeQuery);
             PreparedStatement fieldStatement = connection.prepareStatement(fieldQuery)) {
            //ResultSet resultSet = countStatement.executeQuery();
            //long current = resultSet.getLong(1);
            String[] names = {"node", "address", "data", "street", "home", "car", "home", "apple", "door", "tree"};
            int[] counts = new int[names.length];
            long N = 121;
            for (long i = 0; i < count; i++) {
                long parentId = ((i % N) - 1) / 3 + i / N * N;
                if (i % N == 0) {
                    nodeStatement.setNull(1, Types.BIGINT);
                } else {
                    nodeStatement.setLong(1, parentId + 1);
                }
                int index = (int) (Math.random() * names.length);
                nodeStatement.setString(2, names[index] + counts[index]++);
                nodeStatement.executeUpdate();
                int fieldsCount = (int) (Math.random() * 4);
                for (long j = 0; j <= fieldsCount; j++) {
                    fieldStatement.setLong(1, i + 1);
                    fieldStatement.setString(2, "filed" + j);
                    fieldStatement.setString(3, "value" + j);
                    fieldStatement.executeUpdate();
                }
            }
        } catch (SQLException ignored) {
        }
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

    private boolean checkNode(Node node) {
        if (node.getName().isEmpty()) {
            return false;
        }
        List<Field> fields = node.getFields();
        for (int i = 0; i < node.getFields().size(); i++) {
            if (fields.get(i).getName().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
