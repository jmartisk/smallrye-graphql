package io.smallrye.graphql.client.core;

import static io.smallrye.graphql.client.core.utils.ServiceUtils.getNewInstanceOf;
import static java.util.Arrays.asList;

import java.util.List;

public interface InlineFragment extends FieldOrFragment {

    static InlineFragment on(String type, Field... fields) {
        InlineFragment fragment = getNewInstanceOf(InlineFragment.class);

        fragment.setType(type);
        fragment.setFields(asList(fields));

        return fragment;
    }

    String getType();

    void setType(String name);

    List<Field> getFields();

    void setFields(List<Field> fields);

}
