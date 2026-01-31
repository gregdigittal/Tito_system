package cash.ice;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class GraphQLError extends RuntimeException {
    private final String errorMessage;
    private final String errorCode;
    private final String classification;

    public GraphQLError(List<Map<String, Object>> errors) {
        super(String.format("%s. In %s GraphQL method. Response: %s", errors.getFirst().get("message"), errors.getFirst().get("path"), errors));
        this.errorMessage = (String) errors.getFirst().get("message");
        this.errorCode = ((Map<String, String>) errors.getFirst().get("extensions")).get("errorCode");
        this.classification = ((Map<String, String>) errors.getFirst().get("extensions")).get("classification");
    }

    public GraphQLError print(String comment) {
        System.out.printf("%s: %s%n", comment, this);
        return this;
    }
}
