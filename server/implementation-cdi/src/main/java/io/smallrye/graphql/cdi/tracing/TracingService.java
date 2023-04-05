package io.smallrye.graphql.cdi.tracing;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.spi.CDI;

// import io.opentracing.Scope;
// import io.opentracing.Span;
// import io.opentracing.Tracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.smallrye.graphql.api.Context;
import io.smallrye.graphql.cdi.config.ConfigKey;
import io.smallrye.graphql.execution.event.Priorities;
import io.smallrye.graphql.spi.EventingService;

/**
 * Listening for event and create traces from it
 * <p>
 * FIXME: currently, this places the work of all fetchers inside an operation (execution)
 * under the same parent, which is the execution itself. It would be cool
 * to define some reasonable hierarchy between fetchers, so
 * for example, when evaluating a source method requires evaluating another source method,
 * the second one would be a child of the first one.
 *
 * @author Jan Martiska (jmartisk@redhat.com)
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
@Priority(Priorities.FIRST_IN_LAST_OUT)
public class TracingService implements EventingService {

    private static final Map<String, Stack<Span>> spans = new ConcurrentHashMap<>();
    private static final Map<String, Stack<Scope>> scopes = new ConcurrentHashMap<>();

    private Tracer tracer;

    @Override
    public void beforeExecute(Context context) {
        // FIXME: if operationName is not set in the request explicitly, this is empty
        String operationName = getOperationName(context);
        Span span = getTracer().spanBuilder(operationName)
                .setAttribute("graphql.executionId", context.getExecutionId())
                .setAttribute("graphql.operationType", getOperationNameString(context.getRequestedOperationTypes()))
                .setAttribute("graphql.operationName", context.getOperationName().orElse(EMPTY))
                .startSpan();

        spans.put(context.getExecutionId(), new Stack<>() {
            {
                push(span);
            }
        });
        scopes.put(context.getExecutionId(), new Stack<>() {
            {
                push(span.makeCurrent());
            }
        });
        //        System.err.println(
        //                "beforeExecute created:" + span.getSpanContext().getSpanId() + " operationName: " + context.getFieldName());
    }

    @Override
    public void afterExecute(Context context) {
        Span span = spans.get(context.getExecutionId()).pop();
        //        System.err.println(
        //                "afterExecute delete:" + span.getSpanContext().getSpanId() + " operationName: " + context.getFieldName());

        if (span != null) {
            scopes.get(context.getExecutionId()).pop().close();
            span.end();
        }
        // DELETE
        scopes.remove(context.getExecutionId());
        spans.remove(context.getExecutionId());
    }

    @Override
    public void errorExecute(Context context, Throwable t) {
        Span span = spans.get(context.getExecutionId()).pop();
        //        System.err.println(
        //                "errorExecute delete:" + span.getSpanContext().getSpanId() + " operationName: " + context.getFieldName());
        if (span != null) {
            span.recordException(t);
            span.setStatus(StatusCode.ERROR);
            scopes.get(context.getExecutionId()).pop().close();
            span.end();
        }
        scopes.remove(context.getExecutionId());
        spans.remove(context.getExecutionId());
    }

    @Override
    public void beforeDataFetch(Context context) {
        Span span = getTracer().spanBuilder(getOperationNameForParentType(context))
                .setParent(io.opentelemetry.context.Context.current().with(spans.get(context.getExecutionId()).peek()))
                .setAttribute("graphql.executionId", context.getExecutionId())
                .setAttribute("graphql.operationType", getOperationNameString(context.getOperationType()))
                .setAttribute("graphql.operationName", context.getOperationName().orElse(EMPTY))
                .setAttribute("graphql.parent", context.getParentTypeName().orElse(EMPTY))
                .setAttribute("graphql.field", context.getFieldName())
                .setAttribute("graphql.path", context.getPath())
                .startSpan();
        //        System.err.println(
        //                "beforeDataFetch created:" + span.getSpanContext().getSpanId() + " operationName: " + context.getFieldName());
        spans.get(context.getExecutionId()).push(span);
        scopes.get(context.getExecutionId()).push(span.makeCurrent());
    }

    // FIXME: is the fetcher is asynchronous, this typically ends its span before
    // the work is actually done - after the fetcher itself returns a future.
    // We currently don't have a way to find out when
    // the right moment to close this span is
    @Override
    public void afterDataFetch(Context context) {
        Span span = spans.get(context.getExecutionId()).pop();
        //        System.err.println(
        //                "afterDataFetch delete:" + span.getSpanContext().getSpanId() + " operationName: " + context.getFieldName());

        if (span != null) {
            scopes.get(context.getExecutionId()).pop().close();
            span.end();
        }
    }

    @Override
    public void errorDataFetch(Context context, Throwable t) {
        Span span = spans.get(context.getExecutionId()).pop();
        //        System.err.println(
        //                "errorDataFetch delete:" + span.getSpanContext().getSpanId() + " operationName: " + context.getFieldName());

        if (span != null) {
            logError(span, t);
            scopes.get(context.getExecutionId()).pop().close();
            span.end();
        }
    }

    @Override
    public String getConfigKey() {
        return ConfigKey.ENABLE_TRACING;
    }

    private Tracer getTracer() {
        if (tracer == null) {
            this.tracer = CDI.current().select(Tracer.class).get();
        }
        return tracer;
    }

    //    private Span getParentSpan(final Context context) {
    //        DataFetchingEnvironment dfe = context.unwrap(DataFetchingEnvironment.class);
    //        if (dfe.getGraphQlContext().hasKey(SPAN_CLASS)) {
    //            return dfe.getGraphQlContext().get(SPAN_CLASS);
    //        }
    //        return Span.current();
    //    }

    private void logError(Span span, Throwable throwable) {
        if (throwable instanceof InvocationTargetException || throwable instanceof CompletionException) {
            // Unwrap to get real exception
            throwable = throwable.getCause();
        }

        if (throwable != null) {
            span.recordException(throwable);
        }
        span.setStatus(StatusCode.ERROR);
        //        Map<String, Object> error = new HashMap<>();
        ////        if (throwable != null) {
        ////            error.put("error.object", throwable.getMessage());
        ////        }
        ////        error.put("event", "error");
        ////        // TODO: LOG INTO SPAN
        ////        span.setAttribute("error", true);
    }

    private String getOperationNameForParentType(Context context) {
        String parent = context.getParentTypeName().orElse(EMPTY);
        String name = PREFIX + ":" + parent + "." + context.getFieldName();
        return name;
    }

    private static String getOperationName(Context context) {
        if (context.getOperationName().isPresent()) {
            return PREFIX + ":" + context.getOperationName().get();
        }
        return PREFIX;
    }

    private String getOperationNameString(List<String> types) {
        return String.join(UNDERSCORE, types);
    }

    private String getOperationNameString(String... types) {
        return getOperationNameString(Arrays.asList(types));
    }

    private static final String UNDERSCORE = "_";
    private static final String EMPTY = "";
    private static final String PREFIX = "GraphQL";
}
