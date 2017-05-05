package ru.test.service;

import ru.test.entities.Node;

import java.util.List;

public interface CatalogService {

    Node add (Node node);
    List<Node> find(String text, long page);
    Node get(long id);
    List<Node> getTree(long parentId, long page);
    boolean remove(long id);
    boolean removeField(long id);
    Node update(Node node);
    void mock(long count);
}
