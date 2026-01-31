package cash.ice.zim.api.documentation;

import org.springframework.restdocs.generate.RestDocumentationGenerator;
import org.springframework.restdocs.operation.Operation;
import org.springframework.restdocs.snippet.TemplatedSnippet;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

public class IcecashEndpointDocumentation extends TemplatedSnippet {

    protected IcecashEndpointDocumentation() {
        this(null);
    }

    protected IcecashEndpointDocumentation(Map<String, Object> attributes) {
        super("endpoint", attributes);
    }

    public static IcecashEndpointDocumentation endpoint() {
        return new IcecashEndpointDocumentation();
    }

    @Override
    protected Map<String, Object> createModel(Operation operation) {
        Map<String, Object> model = new HashMap<>();
        model.put("method", operation.getRequest().getMethod());
        model.put("path", removeQueryStringIfPresent(extractUrlTemplate(operation)));
        return model;
    }

    private String removeQueryStringIfPresent(String urlTemplate) {
        int index = urlTemplate.indexOf('?');
        if (index == -1) {
            return urlTemplate;
        }
        return urlTemplate.substring(0, index);
    }

    private String extractUrlTemplate(Operation operation) {
        String urlTemplate = (String) operation.getAttributes()
                .get(RestDocumentationGenerator.ATTRIBUTE_NAME_URL_TEMPLATE);
        Assert.notNull(urlTemplate, "urlTemplate not found. If you are using MockMvc did "
                + "you use RestDocumentationRequestBuilders to build the request?");
        return urlTemplate;
    }
}
