package io.smallrye.graphql.client.model.builder;

import com.sun.security.ntlm.Client;
import io.smallrye.graphql.client.model.ClientModel;
import io.smallrye.graphql.client.model.Method;
import io.smallrye.graphql.client.model.MethodSignature;
import io.smallrye.graphql.client.model.OperationType;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.smallrye.graphql.client.model.builder.DotNames.MUTATION;
import static io.smallrye.graphql.client.model.builder.DotNames.NAME;
import static io.smallrye.graphql.client.model.builder.DotNames.QUERY;
import static io.smallrye.graphql.client.model.builder.DotNames.SUBSCRIPTION;

/**
 * Utility class for building `ClientModel` instances from a Jandex index.
 */
public class JandexClientModelBuilder {

    public ClientModel fromApiClass(DotName dotName, IndexView index) {
        ClassInfo clazz = index.getClassByName(dotName);
        ClientModel model = new ClientModel();

        Map<MethodSignature, Method> methods = new HashMap<>();
        for (MethodInfo methodInfo : clazz.methods()) {
            if(!isGraphQLOperation(methodInfo)) {
                continue;
            }

            // build the method model
            Method methodModel = new Method();
            OperationType operationType = getOperationType(methodInfo);
            methodModel.setOperationType(operationType);
            methodModel.setOperationName(getOperationName(methodInfo, operationType));

            // add the method model to the client model
            List<String> parameterTypes = methodInfo.parameters().stream().map(type -> type.name().toString()).collect(Collectors.toList());
            methods.put(new MethodSignature(methodInfo.name(), parameterTypes), methodModel);
        }
        model.setMethods(methods);

        return model;
    }

    private boolean isGraphQLOperation(MethodInfo methodInfo) {
        short flags = methodInfo.flags();
        if(!Modifier.isInterface(flags) || !Modifier.isPublic(flags)) {
            return false;
        }
        // TODO: any more checks needed?
        return true;
    }

    private OperationType getOperationType(MethodInfo methodInfo) {
        List<AnnotationInstance> annotations = methodInfo.annotations().stream()
            .filter(a -> a.name().equals(QUERY)
                || a.name().equals(SUBSCRIPTION)
                || a.name().equals(MUTATION))
            .collect(Collectors.toList());
        if(annotations.isEmpty()) {
            return OperationType.QUERY;
        }
        if(annotations.size() > 1) {
            throw new IllegalArgumentException("Conflicting annotations found on method " + methodInfo + ": " + annotations);
        }
        AnnotationInstance theAnnotation = annotations.get(0);
        if(theAnnotation.name().equals(QUERY)) {
            return OperationType.QUERY;
        }
        if(theAnnotation.name().equals(MUTATION)) {
            return OperationType.MUTATION;
        }
        if(theAnnotation.name().equals(SUBSCRIPTION)) {
            return OperationType.SUBSCRIPTION;
        }
        // this should never happen
        throw new IllegalStateException();
    }

    private String getOperationName(MethodInfo methodInfo, OperationType operationType) {
        AnnotationInstance opAnnotation = null;
        switch (operationType) {
            case QUERY:
                opAnnotation = methodInfo.annotation(QUERY);
                break;
            case MUTATION:
                opAnnotation = methodInfo.annotation(MUTATION);
                break;
            case SUBSCRIPTION:
                opAnnotation = methodInfo.annotation(SUBSCRIPTION);
                break;
        }
        if(opAnnotation != null && !opAnnotation.value().asString().isEmpty()) {
            return opAnnotation.value().asString();
        }
        AnnotationInstance nameAnnotation = methodInfo.annotation(NAME);
        if(nameAnnotation != null && nameAnnotation.value().asString().isEmpty()) {
            return nameAnnotation.value().asString();
        }
        String name = methodInfo.name();
        if (name.startsWith("get") && name.length() > 3 && Character.isUpperCase(name.charAt(3)))
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        return name;
    }


}
