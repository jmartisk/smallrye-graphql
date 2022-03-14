package io.smallrye.graphql.execution.datafetcher;

import static io.smallrye.graphql.SmallRyeGraphQLServerMessages.msg;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.dataloader.BatchLoaderEnvironment;
import org.eclipse.microprofile.context.ThreadContext;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.smallrye.graphql.execution.context.SmallRyeContext;
import io.smallrye.graphql.schema.model.Operation;

/**
 * The default, built in data fetcher
 *
 * @param <K>
 * @param <T>
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
public class DefaultDataFetcher<K, T> extends AbstractDataFetcher<K, T> {

    public DefaultDataFetcher(Operation operation) {
        super(operation);
    }

    @Override
    public <T> T invokeAndTransform(DataFetchingEnvironment dfe, DataFetcherResult.Builder<Object> resultBuilder,
            Object[] transformedArguments) throws Exception {
        SmallRyeContext context = contextHelper.getSmallRyeContext(dfe);
        try {
            SmallRyeContext.setContext(context);
            Object resultFromMethodCall = operationInvoker.invoke(transformedArguments);
            Object resultFromTransform = fieldHelper.transformOrAdaptResponse(resultFromMethodCall, dfe);
            resultBuilder.data(resultFromTransform);
            return (T) resultBuilder.build();
        } finally {
            SmallRyeContext.remove();
        }
    }

    @Override
    public <T> T invokeFailure(DataFetcherResult.Builder<Object> resultBuilder) {
        return (T) resultBuilder.build();
    }

    @Override
    public CompletionStage<List<T>> load(List<K> keys, BatchLoaderEnvironment ble) {
        System.out.println("---------------------------------");
        System.out.println("DefaultDataFetcher.load, ble = " + ble);
        System.out.println("keys = " + keys + " (size " + keys.size() + ")");
        final Object[] arguments = batchLoaderHelper.getArguments(keys, ble);
        final DataFetchingEnvironment dfe = batchLoaderHelper.getDataFetchingEnvironment(ble);
        final SmallRyeContext smallRyeContext = contextHelper.getSmallRyeContext(dfe);
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        ThreadContext threadContext = ThreadContext.builder().build();
        try {
            SmallRyeContext.setContext(smallRyeContext);
            System.out.println("Setting SRC " + smallRyeContext.hashCode() + " on thread " + Thread.currentThread().getName());

            CompletableFuture<List<T>> reflectionSupplier = CompletableFuture.supplyAsync(() -> {
                try {
                    return (List<T>) operationInvoker.invokePrivileged(tccl, arguments);
                } catch (Exception e) {
                    if (e instanceof RuntimeException && e.getCause() != null && !(e.getCause() instanceof RuntimeException)) {
                        throw msg.dataFetcherException(operation, e.getCause());
                    } else if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    } else {
                        throw msg.dataFetcherException(operation, e);
                    }
                }
            }, threadContext.currentContextExecutor());

            return threadContext
                    .withContextCapture(reflectionSupplier);
        } finally {
            SmallRyeContext.remove();
        }
    }
}
