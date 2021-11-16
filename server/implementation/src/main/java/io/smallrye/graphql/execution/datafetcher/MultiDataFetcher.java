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

    public MultiDataFetcher(Operation operation) {
        super(operation);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <O> O invokeAndTransform(
            DataFetchingEnvironment dfe,
            DataFetcherResult.Builder<Object> UNUSED,
            Object[] transformedArguments) throws Exception {
        SmallRyeContext context = ((GraphQLContext) dfe.getContext()).get("context");
        try {
            SmallRyeContext.setContext(context);
            Multi<?> multi = operationInvoker.invoke(transformedArguments);

            return (O) multi

                    .onItem().transform((t) -> {
                        DataFetcherResult.Builder<Object> resultBuilder = DataFetcherResult.newResult();
                        resultBuilder.localContext(context);
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
                            System.out.println("APPLY THROWABLE: " + throwable.getMessage());
                            DataFetcherResult.Builder<Object> resultBuilder = DataFetcherResult.newResult();
                            resultBuilder.localContext(context);
                            eventEmitter.fireOnDataFetchError(dfe.getExecutionId().toString(), throwable);
                            if (throwable instanceof GraphQLException) {
                                GraphQLException graphQLException = (GraphQLException) throwable;
                                errorResultHelper.appendPartialResult(resultBuilder, dfe, graphQLException);
                                errorResultHelper.appendPartialResult(UNUSED, dfe, graphQLException);
                            } else if (throwable instanceof Exception) {
                                DataFetcherException dataFetcherException = SmallRyeGraphQLServerMessages.msg
                                        .dataFetcherException(operation, throwable);
                                errorResultHelper.appendException(UNUSED, dfe, dataFetcherException);
                                errorResultHelper.appendException(resultBuilder, dfe, dataFetcherException);
                            } else if (throwable instanceof Error) {
                                errorResultHelper.appendException(UNUSED, dfe, throwable);
                                errorResultHelper.appendException(resultBuilder, dfe, throwable);
                            }
                            // TODO FIXME
                            System.out.println("RESULT DATA = " + resultBuilder.build().getData());
                            System.out.println("RESULT ERRORS = " + resultBuilder.build().getErrors());
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
        System.out.println("INVOKE FAILURE, resultBuilder errors = " + resultBuilder.build().getErrors());
        return (O) Multi.createFrom()
                .item(resultBuilder::build);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<List<T>> load(List<K> keys, BatchLoaderEnvironment ble) {
        Object[] arguments = batchLoaderHelper.getArguments(keys, ble);
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        return ((Multi<List<T>>) operationInvoker.invokePrivileged(tccl, arguments))
                .toUni().runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .subscribe().asCompletionStage();
    }
}
