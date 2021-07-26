package io.smallrye.graphql.client.core;

import static io.smallrye.graphql.client.core.utils.ServiceUtils.getNewInstanceOf;
import static java.util.Arrays.asList;

public interface InlineFragment extends Field {

    static InlineFragment on(String type, Field... fields) {
        InlineFragment fragment = getNewInstanceOf(InlineFragment.class);

        fragment.setType(type);
        fragment.setFields(asList(fields));

        return fragment;
    }

    String getType();

    void setType(String name);

}
