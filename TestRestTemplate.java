import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

public class TestRestTemplate {
    public static void main(String[] args) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(Collections.singletonList(new ClientHttpRequestInterceptor() {
            @Override
            public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
                URI uri = UriComponentsBuilder.fromUri(request.getURI())
                        .queryParam("api_key", "j6gXHJsn9rB5wABmU1l8pTbkhJkvv8rmu9gj4hdz")
                        .build()
                        .toUri();
                
                HttpRequest modifiedRequest = new HttpRequestWrapper(request) {
                    @Override
                    public URI getURI() {
                        return uri;
                    }
                };
                System.out.println("Final URI: " + uri);
                return execution.execute(modifiedRequest, body);
            }
        }));

        String origins = "12.935,77.625|12.936,77.626";
        String restaurantCoords = "12.937,77.627";
        
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://api.olamaps.io/routing/v1/distanceMatrix")
                .queryParam("origins", origins)
                .queryParam("destinations", restaurantCoords)
                .queryParam("mode", "driving")
                .queryParam("route_preference", "fastest");
        
        try {
            Map response = restTemplate.getForObject(builder.toUriString(), Map.class);
            System.out.println(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
