package org.example.catholicsouvenircustomorder.config;

import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.elasticsearch.autoconfigure.Rest5ClientBuilderCustomizer;
import org.springframework.context.annotation.Configuration;
@Configuration
public class ElasticsearchConfig implements Rest5ClientBuilderCustomizer {

    @Value("${spring.elasticsearch.api-key}")
    private String apiKey;

    @Override
    public void customize(co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder builder) {
    }

    @Override
    public void customize(HttpAsyncClientBuilder httpAsyncClientBuilder) {
        httpAsyncClientBuilder.addRequestInterceptorFirst(
                (request, entity, context) ->
                        request.addHeader("Authorization", "ApiKey " + apiKey.trim())
        );
    }
}
