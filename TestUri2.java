import org.springframework.web.util.UriComponentsBuilder;

public class TestUri2 {
    public static void main(String[] args) {
        String origins = "12.9,77.6|12.91,77.61";
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/routing/v1/distanceMatrix")
                .queryParam("origins", origins)
                .queryParam("destinations", "foo")
                .queryParam("mode", "driving")
                .queryParam("route_preference", "fastest");
                
        System.out.println("toUriString: " + builder.toUriString());
        System.out.println("build.encode.toUri: " + builder.build().encode().toUri());
    }
}
