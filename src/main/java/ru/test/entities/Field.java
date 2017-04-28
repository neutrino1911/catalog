package ru.test.entities;

public class Field {

    private long id;
    private long nodeId;
    private String name;
    private String value;

    public Field() {}

    public Field(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public Field(long id, long nodeId, String name, String value) {
        this.id = id;
        this.nodeId = nodeId;
        this.name = name;
        this.value = value;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getNodeId() {
        return nodeId;
    }

    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
