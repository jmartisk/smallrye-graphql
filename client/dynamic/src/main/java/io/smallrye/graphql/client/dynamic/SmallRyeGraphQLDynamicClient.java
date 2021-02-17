package io.smallrye.graphql.client.dynamic;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.eclipse.microprofile.graphql.client.Error;
import org.eclipse.microprofile.graphql.client.Request;
import org.eclipse.microprofile.graphql.client.Response;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class SmallRyeGraphQLDynamicClient implements AutoCloseable {

    private final WebClient webClient;
    private final String url;

    SmallRyeGraphQLDynamicClient(WebClientOptions options, Vertx vertx, String url) {
        webClient = WebClient.create(vertx, options);
        this.url = url;
    }

    public Response executeSync(Request request) throws ExecutionException, InterruptedException {
        HttpResponse<Buffer> result = webClient.postAbs(url)
                .putHeader("Content-Type", "application/json")
                .sendBuffer(Buffer.buffer(request.toJson()))
                .toCompletionStage()
                .toCompletableFuture()
                .get();
        return readFrom(result.body());
    }

    public Uni<Response> executeAsync(Request request) {
        return Uni.createFrom().completionStage(
                webClient.postAbs(url)
                        .putHeader("Content-Type", "application/json")
                        .sendBuffer(Buffer.buffer(request.toJson()))
                        .toCompletionStage())
                .map(response -> readFrom(response.body()));
    }

    @Override
    public void close() {
        webClient.close();
    }

    private ResponseImpl readFrom(Buffer input) {
        // FIXME: is there a more performant way to read the input?
        JsonReader jsonReader = Json.createReader(new StringReader(input.toString()));
        JsonObject jsonResponse = jsonReader.readObject();
        JsonObject data = null;
        if (jsonResponse.containsKey("data")) {
            if (!jsonResponse.isNull("data")) {
                data = jsonResponse.getJsonObject("data");
            } else {
                //                log.warn("No data in GraphQLResponse");
            }
        }

        List<Error> errors = null;
        if (jsonResponse.containsKey("errors")) {
            JsonArray rawErrors = jsonResponse.getJsonArray("errors");
            Jsonb jsonb = JsonbBuilder.create();
            errors = jsonb.fromJson(
                    rawErrors.toString(),
                    new ArrayList<ErrorImpl>() {
                    }.getClass().getGenericSuperclass());
            try {
                jsonb.close();
            } catch (Exception ignore) {
            } // Ugly!!!
        }

        return new ResponseImpl(data, errors);
    }

    // TODO: defining custom HTTP headers, etc
    public static class Builder {

        private Vertx vertx;
        private String url;

        public Builder vertx(Vertx vertx) {
            this.vertx = vertx;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public SmallRyeGraphQLDynamicClient build() {
            WebClientOptions options = new WebClientOptions();
            if (url == null) {
                throw new IllegalArgumentException("URL is required");
            }
            Vertx toUseVertx = vertx != null ? vertx : Vertx.vertx();
            return new SmallRyeGraphQLDynamicClient(options, toUseVertx, url);
        }

    }
}
