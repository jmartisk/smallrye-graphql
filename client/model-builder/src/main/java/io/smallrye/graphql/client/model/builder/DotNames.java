package io.smallrye.graphql.client.model.builder;

import org.jboss.jandex.DotName;

public class DotNames {

    final static DotName QUERY = DotName.createSimple("org.eclipse.microprofile.graphql.Query");
    final static DotName MUTATION = DotName.createSimple("org.eclipse.microprofile.graphql.Mutation");
    final static DotName SUBSCRIPTION = DotName.createSimple("io.smallrye.graphql.api.Subscription");

    final static DotName NAME = DotName.createSimple("org.eclipse.microprofile.graphql.Name");

}
