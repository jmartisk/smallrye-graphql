package io.smallrye.graphql.scalar.number;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.scalar.CoercingUtil;
import graphql.scalar.GraphqlIntCoercing;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;

public class GraphQLJavaReproducer {

    public static void main(String[] args) {

        Converter converter = new Converter() {
            @Override
            public Object fromBigDecimal(BigDecimal bigDecimal) {
                System.out.println("1");
                return bigDecimal.intValueExact();
            }

            @Override
            public Object fromBigInteger(BigInteger bigInteger) {
                System.out.println("1");
                return bigInteger.intValue();
            }

            @Override
            public boolean isInRange(BigInteger value) {
                return (value.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0
                        || value.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) > 0);
            }
        };

        NumberCoercing coercing = new NumberCoercing("asd", converter, Integer.class);

        GraphQLInputType customNonNullInt = new GraphQLNonNull(GraphQLScalarType.newScalar()
                .name("MyInt").description("My custom int")
//                .coercing(new NumberCoercing("asd", converter, Integer.class))
                .coercing(new GraphqlIntCoercing())
                .build());

        GraphQLInputType type = new GraphQLNonNull(GraphQLScalarType.newScalar().name("MyInt").coercing(coercing).build());

        GraphQLArgument argument = GraphQLArgument
                .newArgument()
                .name("number")
//                .type(new GraphQLNonNull(Scalars.GraphQLInt))
//                .type(customNonNullInt)
//                .type(new IntegerScalar().getScalarType())
                .type(type)
                .build();

        GraphQLFieldDefinition echoNumberQuery = GraphQLFieldDefinition.newFieldDefinition()
                .name("echoNumber")
                .type(Scalars.GraphQLInt)
                .argument(argument)
                .build();
        FieldCoordinates coords = FieldCoordinates.coordinates("Query", "echoNumber");
        GraphQLCodeRegistry.Builder codeRegistry = GraphQLCodeRegistry
                .newCodeRegistry()
                .dataFetcher(coords, new DataFetcher<Integer>() {
                    @Override
                    public Integer get(DataFetchingEnvironment environment) throws Exception {
                        return environment.getArgument("number");
                    }
                });
        GraphQLObjectType.Builder queryRoot = GraphQLObjectType.newObject().name("Query").field(echoNumberQuery);
        GraphQLSchema schema = GraphQLSchema.newSchema().query(queryRoot).codeRegistry(codeRegistry.build()).build();
        GraphQL graphQL = GraphQL.newGraphQL(schema).build();

        ExecutionResult result = graphQL.execute("{ echoNumber(number: \"asdf\") }");
        System.out.println(result);
    }
}
