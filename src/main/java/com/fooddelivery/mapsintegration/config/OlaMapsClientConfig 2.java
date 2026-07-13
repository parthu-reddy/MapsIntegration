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

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class OlaMapsClientConfig {

    @Value("${olamaps.api.key}")
    private String apiKey;

    @Value("${olamaps.api.base-url:https://api.olamaps.io}")
    private String baseUrl;

    @Bean
    public RestTemplate olaMapsRestTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        RestTemplate restTemplate = new RestTemplate(factory);
        org.springframework.web.util.DefaultUriBuilderFactory defaultUriBuilderFactory = new org.springframework.web.util.DefaultUriBuilderFactory(baseUrl);
        restTemplate.setUriTemplateHandler(defaultUriBuilderFactory);
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

            URI originalUri = request.getURI();
            String appendStr = (originalUri.getRawQuery() == null ? "?" : "&") + "api_key=" + apiKey;
            URI newUri = URI.create(originalUri.toString() + appendStr);

            HttpRequest modifiedRequest = new HttpRequestWrapper(request) {
                @Override
                public URI getURI() {
                    return newUri;
                }
                @Override
                public HttpHeaders getHeaders() {
                    return headers;
                }
            };
            log.debug("Executing OLA Maps Request: {}", newUri.toString());

            return execution.execute(modifiedRequest, body);
        }
    }
}
