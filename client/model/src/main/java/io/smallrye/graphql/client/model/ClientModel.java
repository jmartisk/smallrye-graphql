package io.smallrye.graphql.client.model;

import java.util.Map;

/**
 * The model representing a client-side view of a `@GraphQLClientApi` interface and all
 * the operations that it contains.
*/
public class ClientModel {

    private Map<MethodSignature, Method> methods;

    public ClientModel() {
    }

    public Map<MethodSignature, Method> getMethods() {
        return methods;
    }

    public void setMethods(Map<MethodSignature, Method> methods) {
        this.methods = methods;
    }
}
