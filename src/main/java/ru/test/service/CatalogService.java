package ru.test.service;

import ru.test.Node;

import java.util.List;
import java.util.Map;

public interface CatalogService {

    boolean add (Map<String, String> params);
    Node get(long id);
    List<Node> getTree(long parentId);
    boolean remove(long id);
}
