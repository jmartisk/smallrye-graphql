package io.smallrye.graphql.tests.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;

import org.hamcrest.Matchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.RestAssured;
import io.smallrye.graphql.tests.GraphQLAssured;

@RunWith(Arquillian.class)
@RunAsClient
public class BeanValidationTest {

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "validation-test.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource(new StringAsset("smallrye.graphql.validation.enabled=true\n" +
                        "smallrye.graphql.schema.includeDirectives=true"),
                        "META-INF/microprofile-config.properties")
                .addClasses(ValidatingGraphQLApi.class, Person.class);
    }

    @ArquillianResource
    URL testingURL;

    @Test
    public void shouldAcceptValidPersonData() throws Exception {
        GraphQLAssured graphQLAssured = new GraphQLAssured(testingURL);

        String response = graphQLAssured
                .post("mutation {update(person: { firstName: \"Jane\", lastName: \"Doe\", age: 87 }) { firstName }}");

        assertThat(response).isEqualTo("{\"data\":{\"update\":{\"firstName\":\"Jane\"}}}");
    }

    @Test
    public void shouldFailInvalidPersonData() throws Exception {
        GraphQLAssured graphQLAssured = new GraphQLAssured(testingURL);

        String response = graphQLAssured
                .post("mutation {update(person: { firstName: \"*\", lastName: \"\", age: -1 }) { firstName }}");

        assertThat(response).isNotNull();
        assertThat(response).isNotEmpty();

        assertThat(response).contains("validation failed: update.person.firstName must match");
        assertThat(response).contains("validation failed: update.person.lastName must not be empty");
        assertThat(response).contains("validation failed: update.person.age must be greater than or equal to 0");

    }

    /*
     * Person contains a field `somethingEndingWithPercentageSign` that is constrained with a regex and should
     * end with a percentage sign. This breaks printing the schema with graphql-java 19.1
     * - see https://github.com/smallrye/smallrye-graphql/issues/1492
     * Here we verify that the schema can still be printed in such case.
     */
    @Test
    public void testSchemaWhenThereIsAConstraintContainingRegex() throws Exception {
        RestAssured.when()
                .get("/graphql/schema.graphql")
                .then()
                .body(Matchers.containsString("somethingEndingWithPercentageSign: String @constraint(pattern : \".+%\")"));
    }

}
