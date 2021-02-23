package io.smallrye.graphql.client.dynamic;

import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.graphql.client.Request;
import org.eclipse.microprofile.graphql.client.Response;
import org.eclipse.microprofile.graphql.client.core.Document;

import io.smallrye.mutiny.Uni;

public interface DynamicGraphQLClient extends AutoCloseable {

    Response executeSync(Document document) throws ExecutionException, InterruptedException;

    Response executeSync(Request request) throws ExecutionException, InterruptedException;

    Uni<Response> executeAsync(Document document);

    Uni<Response> executeAsync(Request request);

}
