package io.smallrye.graphql.client.dynamic.core;

import java.util.List;

import org.eclipse.microprofile.graphql.client.core.InputObject;
import org.eclipse.microprofile.graphql.client.core.InputObjectField;

public abstract class AbstractInputObject implements InputObject {
    private List<InputObjectField> inputObjectFields;

    public AbstractInputObject() {
    }

    public List<InputObjectField> getInputObjectFields() {
        return inputObjectFields;
    }

    public void setInputObjectFields(List<InputObjectField> inputObjectFields) {
        this.inputObjectFields = inputObjectFields;
    }
}
