package io.smallrye.graphql.client.dynamic.core;

import org.eclipse.microprofile.graphql.client.core.Operation;

public class DocumentImpl extends AbstractDocument {
    @Override
    // TODO: use StringJoiner
    public String build() {
        StringBuilder builder = new StringBuilder();

        for (Operation operation : this.getOperations()) {
            builder.append(operation.build());
        }

        // FIXME PART 2 - what is the correct format? this works with SmallRye but fails the TCK
        return "{" + builder.toString() + "}";
        //        return builder.toString();
    }
}
