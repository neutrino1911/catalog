package ru.test.service;

import ru.test.entities.Node;

import java.util.List;

public interface CatalogService {

    Node addNode(Node node);
    List<Node> findNodes(String text, long page);
    Node getNode(long id);
    List<Node> getNodes(long parentId, long page);
    boolean removeNode(long id);
    boolean removeField(long id);
    Node updateNode(Node node);
    void mock(long count);
}
