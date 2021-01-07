package com.josejo911.semanticControl;

public class Operation {
    private String operation;
    private String type;

    public Operation(String operation, String type) {
        this.operation = operation;
        this.type = type;
    }

    public Operation(String operation) {
        this.operation = operation;
        this.type = null;
    }

    public String getOperation() { return operation; }
    public String getType() { return type; }

    public void setType(String type) { this.type = type; }
    public void setOperation(String operation) { this. operation = operation; }

    public String toString() {
        return operation + ", t:" + type;
    }
}
