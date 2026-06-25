import org.springframework.web.util.DefaultUriBuilderFactory;
import java.net.URI;

public class TestUri4 {
    public static void main(String[] args) {
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("https://api.olamaps.io");
        
        String url = "/path?origins=12.9,77.6|12.91,77.61";
        URI uri = factory.expand(url);
        System.out.println("Result: " + uri.toString());
    }
}
