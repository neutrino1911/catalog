package ru.test.service;

import ru.test.entities.Node;

import java.util.List;

public interface CatalogService {

    Node add (Node node);
    Node get(long id);
    List<Node> getTree(long parentId);
    boolean remove(long id);
    boolean removeField(long id);
    Node update(Node node);
}
