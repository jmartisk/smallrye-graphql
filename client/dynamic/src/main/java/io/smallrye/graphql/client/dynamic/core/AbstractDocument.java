package io.smallrye.graphql.client.dynamic.core;

import java.util.List;

import org.eclipse.microprofile.graphql.client.core.Document;
import org.eclipse.microprofile.graphql.client.core.Operation;

public abstract class AbstractDocument implements Document {
    private List<Operation> operations;

    public AbstractDocument() {
    }

    public List<Operation> getOperations() {
        return operations;
    }

    public void setOperations(List<Operation> operations) {
        this.operations = operations;
    }
}
