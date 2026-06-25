import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;

public class Test {
    public static void main(String[] args) throws Exception {
        URI original = new URI("https://api.olamaps.io/routing/v1/distanceMatrix?origins=12.9,77.6%7C13.0,77.7&destinations=12.9,77.6");
        URI uri = UriComponentsBuilder.fromUri(original)
                    .queryParam("api_key", "12345")
                    .build()
                    .toUri();
        System.out.println(uri.toString());
    }
}
