package cash.ice;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

@RequiredArgsConstructor
public class GraphQLHelper {
    private final RestHelper restHelper;

    private final String graphQlUrl = RestHelper.host + "/graphql";

    public Wrapper call(String query) {
        return call(query, null);
    }

    public Wrapper call(String query, String token) {
        Map<String, Object> response = invokeGraphQLCall(query, token);
        if (response == null || (!response.containsKey("data") && !response.containsKey("errors"))) {
            throw new RuntimeException("Bad response: " + response);
        } else if (response.containsKey("errors")) {
            List<Map<String, Object>> errors = (List<Map<String, Object>>) response.get("errors");
            throw new GraphQLError(errors);
        } else {
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            return new Wrapper(data.entrySet().iterator().next().getValue());
        }
    }

    public GraphQLError callForError(String query) {
        return callForError(query, null);
    }

    public GraphQLError callForError(String query, String token) {
        return assertThrows(GraphQLError.class, () -> call(query, token));
    }

    private Map<String, Object> invokeGraphQLCall(String query, String token) {
        if (token == null) {
            return restHelper.getRestTemplate().postForObject(graphQlUrl, Map.of("query", query), Map.class);
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(Map.of("query", query), headers);
            ResponseEntity<Map> responseEntity = restHelper.getRestTemplate().exchange(graphQlUrl, HttpMethod.POST, httpEntity, Map.class);
            return (Map<String, Object>) responseEntity.getBody();
        }
    }

}
