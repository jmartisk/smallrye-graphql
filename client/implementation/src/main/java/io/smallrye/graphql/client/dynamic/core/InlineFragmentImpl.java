package io.smallrye.graphql.client.dynamic.core;

import io.smallrye.graphql.client.core.Field;
import io.smallrye.graphql.client.core.exceptions.BuildException;

public class InlineFragmentImpl extends AbstractInlineFragment {

    @Override
    public String build() throws BuildException {
        StringBuilder builder = new StringBuilder();
        builder.append("... on ").append(getType()).append(" {");

        Field[] fields = this.getFields().toArray(new Field[0]);
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            builder.append(field.build());
            if (i < fields.length - 1) {
                builder.append(" ");
            }
        }

        builder.append("}");
        return builder.toString();
    }

}
