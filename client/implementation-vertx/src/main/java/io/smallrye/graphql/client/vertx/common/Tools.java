package io.smallrye.graphql.client.vertx.common;

import io.smallrye.graphql.client.vertx.dynamic.VertxDynamicGraphQLClient;
import io.vertx.core.MultiMap;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.function.Supplier;

public class Tools {

    private static final Logger log = Logger.getLogger(Tools.class);

    public static MultiMap mergeWithDynamicHeaders(MultiMap staticHeaders, Map<String, Supplier<String>> dynamicHeaders) {
        MultiMap result = new HeadersMultiMap();
        result.addAll(staticHeaders);
        dynamicHeaders.forEach((name, supplier) -> {
            String value = null;
            try {
                value = supplier.get();
            } catch(Exception e) {
                log.warn("Supplier for header " + name + " threw an exception. Ignoring the header", e);
            }
            if(value != null) {
                result.add(name, value);
            }
        });
        return result;
    }
}
