package ru.test.service;

import ru.test.Node;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Stateless
public class CatalogServiceImpl implements CatalogService {

    @EJB(lookup = "java:jboss/datasources/MysqlXADS")
    private DataSource dataSource;

    @Override
    public Node add(Map<String, String> params) {
        return null;
    }

    @Override
    public Node get(long id) {
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
        try(Connection connection = dataSource.getConnection();
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
        try(Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, id);
            int code = statement.executeUpdate();
            return code != 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
