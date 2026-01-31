package cash.ice.api.graphql;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class IcecashGraphQLError implements GraphQLError {
    private String errorCode;
    private String message;
    private List<SourceLocation> locations;
    private ErrorClassification errorType;
    private List<Object> path;
    private Map<String, Object> extensions;

    public IcecashGraphQLError(String message) {
        this.message = message;
    }
}
