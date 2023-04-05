package io.smallrye.graphql.cdi.tracing;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.smallrye.graphql.api.Context;
import io.smallrye.graphql.cdi.config.ConfigKey;
import io.smallrye.graphql.execution.event.Priorities;
import io.smallrye.graphql.spi.EventingService;

/**
 * Listening for operation start/end event and create traces from it
 * 
 * @author Jan Martiska (jmartisk@redhat.com)
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
@Priority(Priorities.FIRST_IN_LAST_OUT)
public class TracingService implements EventingService {

    private Logger LOG = Logger.getLogger(TracingService.class);
    private static final Map<String, Span> spans = new ConcurrentHashMap<>();
    private static final Map<String, Scope> scopes = new ConcurrentHashMap<>();

    private Tracer tracer;

    @Override
    public void beforeExecute(Context context) {
        String operationName = getOperationName(context);
        Span span = getTracer().spanBuilder(operationName)
                .setAttribute("graphql.executionId", context.getExecutionId())
                .setAttribute("graphql.operationType", getOperationNameString(context.getRequestedOperationTypes()))
                .setAttribute("graphql.operationName", context.getOperationName().orElse(EMPTY))
                .startSpan();
        LOG.trace("Start span " + span.getSpanContext().getSpanId());

        spans.put(context.getExecutionId(), span);
        scopes.put(context.getExecutionId(), span.makeCurrent());
    }

    @Override
    public void afterExecute(Context context) {
        Span span = spans.remove(context.getExecutionId());

        if (span != null) {
            LOG.trace("Finish span " + span.getSpanContext().getSpanId());
            scopes.remove(context.getExecutionId()).close();
            span.end();
        }
    }

    @Override
    public void errorExecute(Context context, Throwable t) {
        Span span = spans.remove(context.getExecutionId());
        if (span != null) {
            LOG.trace("Exceptionally finish span " + span.getSpanContext().getSpanId());
            span.recordException(t);
            span.setStatus(StatusCode.ERROR);
            scopes.remove(context.getExecutionId()).close();
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
