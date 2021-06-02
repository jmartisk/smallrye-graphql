package io.smallrye.graphql.execution.datafetcher;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.dataloader.BatchLoaderEnvironment;
import org.eclipse.microprofile.graphql.GraphQLException;

import graphql.GraphQLContext;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.smallrye.graphql.SmallRyeGraphQLServerMessages;
import io.smallrye.graphql.bootstrap.Config;
import io.smallrye.graphql.execution.context.SmallRyeContext;
import io.smallrye.graphql.schema.model.Operation;
import io.smallrye.graphql.transformation.AbstractDataFetcherException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;

/**
 * Handle Stream calls with Multi
 *
 * @param <K>
 * @param <T>
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
public class MultiDataFetcher<K, T> extends AbstractDataFetcher<K, T> {

    public MultiDataFetcher(Operation operation, Config config) {
        super(operation, config);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <O> O invokeAndTransform(
            DataFetchingEnvironment dfe,
            DataFetcherResult.Builder<Object> resultBuilder,
            Object[] transformedArguments) throws Exception {
        System.out.println("Multi fetcher called with resultBuilder " + resultBuilder.hashCode());
        new Exception().printStackTrace();
        SmallRyeContext context = ((GraphQLContext) dfe.getContext()).get("context");
        try {
            SmallRyeContext.setContext(context);
            Multi<?> multi = reflectionHelper.invoke(transformedArguments);

            return (O) multi

                    .onItem().transform((t) -> {
                        //                        final DataFetcherResult.Builder<Object> resultBuilder = DataFetcherResult.newResult()
                        //                                .localContext(dfe.getContext());
                        try {
                            Object resultFromTransform = fieldHelper.transformResponse(t);
                            resultBuilder.data(resultFromTransform);
                            return (O) resultBuilder.build();
                        } catch (AbstractDataFetcherException abstractDataFetcherException) {
                            //Arguments or result couldn't be transformed
                            abstractDataFetcherException.appendDataFetcherResult(resultBuilder, dfe);
                            eventEmitter.fireOnDataFetchError(dfe.getExecutionId().toString(), abstractDataFetcherException);
                            return (O) resultBuilder.build();
                        }
                    })

                    .onFailure().recoverWithItem(new Function<Throwable, O>() {
                        public O apply(Throwable throwable) {
                            //                            final DataFetcherResult.Builder<Object> resultBuilder = DataFetcherResult.newResult()
                            //                                    .localContext(dfe.getContext());
                            System.out.println("RECOVERING FROM " + throwable);
                            System.out.println("data before recovering =" + resultBuilder.build().getData());
                            //                            resultBuilder.
                            resultBuilder.data(null);
                            resultBuilder.
                            eventEmitter.fireOnDataFetchError(dfe.getExecutionId().toString(), throwable);
                            if (throwable instanceof GraphQLException) {
                                GraphQLException graphQLException = (GraphQLException) throwable;
                                errorResultHelper.appendPartialResult(resultBuilder, dfe, graphQLException);
                            } else if (throwable instanceof Exception) {
                                DataFetcherException dataFetcherException = SmallRyeGraphQLServerMessages.msg
                                        .dataFetcherException(operation, throwable);
                                errorResultHelper.appendException(resultBuilder, dfe, dataFetcherException);
                                System.out.println("data after recovering =" + resultBuilder.build().getData());
                                System.out.println("errors after recovering = " + resultBuilder.build().getErrors());
                            } else if (throwable instanceof Error) {
                                errorResultHelper.appendException(resultBuilder, dfe, throwable);
                            }
                            return (O) resultBuilder.build();
                        }
                    });

        } finally {
            SmallRyeContext.remove();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <O> O invokeFailure(DataFetcherResult.Builder<Object> resultBuilder) {
        System.out.println("Invoke failure, errors = " + resultBuilder.build().getErrors());
        return (O) Multi.createFrom()
                .item(resultBuilder::build);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<List<T>> load(List<K> keys, BatchLoaderEnvironment ble) {
        Object[] arguments = batchLoaderHelper.getArguments(keys, ble);
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        return ((Multi<List<T>>) reflectionHelper.invokePrivileged(tccl, arguments))
                .toUni().runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .subscribe().asCompletionStage();
    }
}
