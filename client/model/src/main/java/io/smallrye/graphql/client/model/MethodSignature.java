package io.smallrye.graphql.client.model;

import java.util.List;
import java.util.Objects;

public class MethodSignature {

    private String name;

    private List<String> argumentTypes;

    public MethodSignature(String name, List<String> argumentTypes) {
        this.name = name;
        this.argumentTypes = argumentTypes;
    }

    public String getName() {
        return name;
    }

    public List<String> getArgumentTypes() {
        return argumentTypes;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setArgumentTypes(List<String> argumentTypes) {
        this.argumentTypes = argumentTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MethodSignature that = (MethodSignature) o;
        return Objects.equals(name, that.name) && Objects.equals(argumentTypes, that.argumentTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, argumentTypes);
    }
}
