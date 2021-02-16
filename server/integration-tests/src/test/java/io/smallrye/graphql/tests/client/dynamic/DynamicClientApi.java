package io.smallrye.graphql.tests.client.dynamic;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

@GraphQLApi
@ApplicationScoped
public class DynamicClientApi {

    @Query
    public Dummy simple() {
        Dummy ret = new Dummy();
        ret.setInteger(30);
        ret.setString("asdf");
        return ret;
    }
}
