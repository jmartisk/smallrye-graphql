package io.smallrye.graphql.client.dynamic.core;

import org.eclipse.microprofile.graphql.client.core.Enum;

public abstract class AbstractEnum implements Enum {

    private String value;

    public AbstractEnum() {
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
