package com.fooddelivery.mapsintegration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@Configuration
public class OlaMapsClientConfig {

    @Value("${olamaps.api.key}")
    private String apiKey;

    @Bean
    public RestTemplate olaMapsRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new OlaMapsAuthInterceptor(apiKey));
        return restTemplate;
    }

    private static class OlaMapsAuthInterceptor implements ClientHttpRequestInterceptor {
        private final String apiKey;

        public OlaMapsAuthInterceptor(String apiKey) {
            this.apiKey = apiKey;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            HttpHeaders headers = request.getHeaders();
            headers.set("Accept", "application/json");
            
            if (headers.getFirst("x-request-id") == null) {
                headers.set("x-request-id", UUID.randomUUID().toString());
            }
            if (headers.getFirst("x-correlation-id") == null) {
                headers.set("x-correlation-id", UUID.randomUUID().toString());
            }

            URI uri = UriComponentsBuilder.fromUri(request.getURI())
                    .queryParam("api_key", apiKey)
                    .build()
                    .toUri();

            HttpRequest modifiedRequest = new HttpRequestWrapper(request) {
                @Override
                public URI getURI() {
                    return uri;
                }
                @Override
                public HttpHeaders getHeaders() {
                    return headers;
                }
            };

            return execution.execute(modifiedRequest, body);
        }
    }
}
