package io.smallrye.graphql.tests.client.dynamic;

import static org.eclipse.microprofile.graphql.client.core.Field.field;
import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.time.Duration;
import java.util.concurrent.ExecutionException;

import javax.json.JsonObject;

import org.eclipse.microprofile.graphql.client.core.Document;
import org.eclipse.microprofile.graphql.client.core.Operation;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.smallrye.graphql.client.dynamic.RequestImpl;
import io.smallrye.graphql.client.dynamic.SmallRyeGraphQLDynamicClient;

@RunWith(Arquillian.class)
@RunAsClient
public class DynamicClientTest {

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "validation-test.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addClasses(DynamicClientApi.class, Dummy.class);
    }

    @ArquillianResource
    URL testingURL;

    private static SmallRyeGraphQLDynamicClient client;

    @Before
    public void prepare() {
        client = new SmallRyeGraphQLDynamicClient.Builder()
                .url(testingURL.toString() + "graphql")
                .build();
    }

    @After
    public void cleanup() {
        client.close();
    }

    @Test
    public void testSimpleQuerySync() throws ExecutionException, InterruptedException {
        Document document = Document.document(
                Operation.operation("simple",
                        field("string"),
                        field("integer")));
        // TODO: what is the spec-compliant way to build a Request object?
        JsonObject data = client.executeSync(new RequestImpl(document.build())).getData();
        assertEquals("asdf", data.getJsonObject("simple").getString("string"));
        assertEquals(30, data.getJsonObject("simple").getInt("integer"));
    }

    @Test
    public void testSimpleQueryAsync() {
        Document document = Document.document(
                Operation.operation("simple",
                        field("string"),
                        field("integer")));
        JsonObject data = client.executeAsync(new RequestImpl(document.build()))
                .await().atMost(Duration.ofSeconds(30)).getData();
        assertEquals("asdf", data.getJsonObject("simple").getString("string"));
        assertEquals(30, data.getJsonObject("simple").getInt("integer"));
    }

}
