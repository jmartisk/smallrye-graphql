package io.smallrye.graphql.bootstrap;

import org.dataloader.BatchLoaderWithContext;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;

import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLSchema;
import io.smallrye.graphql.execution.batchloader.SourceBatchLoader;
import io.smallrye.graphql.execution.batchloader.SourceBatchLoaderHelper;
import io.smallrye.graphql.schema.model.Operation;

/**
 * Some context while bootstrapping
 * 
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
public class BootstrapContext {

    private static final ThreadLocal<BootstrapContext> current = new ThreadLocal<>();

    public static void start() {
        BootstrapContext registry = new BootstrapContext();
        current.set(registry);
    }

    public static void end() {
        current.remove();
    }

    public static void setGraphQLSchema(GraphQLSchema graphQLSchema) {
        getBootstrapContext().graphQLSchema = graphQLSchema;
    }

    public static void registerBatchLoader(Operation operation) {
        BatchLoaderWithContext<Object, Object> batchLoader = new SourceBatchLoader(operation);
        getBootstrapContext().dataLoaderRegistry.register(SourceBatchLoaderHelper.getName(operation),
                DataLoader.newDataLoader(batchLoader));
    }

    public static void registerDataLoader(String name, DataLoader<?, ?> dataLoader) {
        getBootstrapContext().dataLoaderRegistry.register(name, dataLoader);
    }

    public static GraphQLSchema getGraphQLSchema() {
        return getBootstrapContext().graphQLSchema;
    }

    public static DataLoaderRegistry getDataLoaderRegistry() {
        return getBootstrapContext().dataLoaderRegistry;
    }

    public static GraphQLCodeRegistry.Builder getCodeRegistryBuilder() {
        return getBootstrapContext().codeRegistryBuilder;
    }

    private static BootstrapContext getBootstrapContext() {
        BootstrapContext context = current.get();
        if (context != null) {
            return context;
        } else {
            throw new RuntimeException("Bootstrap context not available");
        }
    }

    private GraphQLSchema graphQLSchema;
    private final DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();
    private final GraphQLCodeRegistry.Builder codeRegistryBuilder = GraphQLCodeRegistry.newCodeRegistry();

    private BootstrapContext() {
    }

}
