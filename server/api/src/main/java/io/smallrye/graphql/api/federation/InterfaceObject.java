package io.smallrye.graphql.api.federation;

import static io.smallrye.graphql.api.DirectiveLocation.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

import org.eclipse.microprofile.graphql.Description;

import io.smallrye.common.annotation.Experimental;
import io.smallrye.graphql.api.Directive;

@Directive(on = { OBJECT })
@Retention(RUNTIME)
@Description("Indicates that an object definition serves as an abstraction of another subgraph's" +
        " entity interface. This abstraction enables a subgraph to automatically contribute fields" +
        " to all entities that implement a particular entity interface.\n" +
        "During composition, the fields of every @interfaceObject are added both to their" +
        " corresponding interface definition and to all entity types that implement that interface.")
@Experimental("SmallRye GraphQL Federation is still subject to change.")
public @interface InterfaceObject {
}
