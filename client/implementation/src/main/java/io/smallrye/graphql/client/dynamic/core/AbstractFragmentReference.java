package io.smallrye.graphql.client.dynamic.core;

import java.util.Collections;
import java.util.List;

import io.smallrye.graphql.client.core.Argument;
import io.smallrye.graphql.client.core.Field;
import io.smallrye.graphql.client.core.FragmentReference;

public abstract class AbstractFragmentReference implements FragmentReference {

    private String name;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public List<Argument> getArguments() {
        throw new UnsupportedOperationException("Fragment references don't have arguments");
    }

    @Override
    public void setArguments(List<Argument> arguments) {
        throw new UnsupportedOperationException("Fragment references don't have arguments");
    }

    @Override
    public List<Field> getFields() {
        return Collections.emptyList();
    }

    @Override
    public void setFields(List<Field> fields) {
        throw new UnsupportedOperationException("Fragment references don't have subfields");
    }

}
