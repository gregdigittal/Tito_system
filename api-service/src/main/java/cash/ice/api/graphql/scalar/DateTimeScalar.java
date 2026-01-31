package cash.ice.api.graphql.scalar;

import cash.ice.common.utils.Tool;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.*;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.function.Function;

import static graphql.scalars.util.Kit.typeName;

public final class DateTimeScalar {
    public static final GraphQLScalarType INSTANCE;

    static {
        Coercing<LocalDateTime, String> coercing = new Coercing<>() {
            @Override
            public String serialize(Object input) throws CoercingSerializeException {
                TemporalAccessor temporalAccessor;
                if (input instanceof TemporalAccessor) {
                    temporalAccessor = (TemporalAccessor) input;
                } else if (input instanceof String) {
                    temporalAccessor = parseLocalDateTime(input.toString(), CoercingSerializeException::new);
                } else {
                    throw new CoercingSerializeException("Expected a 'String' or 'java.time.temporal.TemporalAccessor' but was '" + typeName(input) + "'.");
                }
                try {
                    return Tool.DATE_FORMATTER.format(temporalAccessor);
                } catch (DateTimeException e) {
                    throw new CoercingSerializeException("Unable to turn TemporalAccessor into full date because of : '" + e.getMessage() + "'.");
                }
            }

            @Override
            public LocalDateTime parseValue(Object input) throws CoercingParseValueException {
                TemporalAccessor temporalAccessor;
                if (input instanceof TemporalAccessor) {
                    temporalAccessor = (TemporalAccessor) input;
                } else if (input instanceof String) {
                    temporalAccessor = parseLocalDateTime(input.toString(), CoercingParseValueException::new);
                } else {
                    throw new CoercingParseValueException("Expected a 'String' or 'java.time.temporal.TemporalAccessor' but was '" + typeName(input) + "'.");
                }
                try {
                    return LocalDateTime.from(temporalAccessor);
                } catch (DateTimeException e) {
                    throw new CoercingParseValueException("Unable to turn TemporalAccessor into full date because of : '" + e.getMessage() + "'.");
                }
            }

            @Override
            public LocalDateTime parseLiteral(Object input) throws CoercingParseLiteralException {
                if (!(input instanceof StringValue)) {
                    throw new CoercingParseLiteralException("Expected AST type 'StringValue' but was '" + typeName(input) + "'.");
                }
                return parseLocalDateTime(((StringValue) input).getValue(), CoercingParseLiteralException::new);
            }

            @Override
            public Value<?> valueToLiteral(Object input) {
                String s = serialize(input);
                return StringValue.newStringValue(s).build();
            }

            private LocalDateTime parseLocalDateTime(String s, Function<String, RuntimeException> exceptionMaker) {
                try {
                    TemporalAccessor temporalAccessor = Tool.DATE_FORMATTER.parse(s);
                    return LocalDateTime.from(temporalAccessor);
                } catch (DateTimeParseException e) {
                    throw exceptionMaker.apply("Invalid DateTime value : '" + s + "'. because of : '" + e.getMessage() + "'");
                }
            }
        };

        INSTANCE = GraphQLScalarType.newScalar()
                .name("DateTime")
                .description("Simple DateTime Scalar. eg. '2022-10-14 15:45:00'")
                .coercing(coercing)
                .build();
    }

    private DateTimeScalar() {
    }
}
