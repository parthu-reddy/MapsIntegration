import org.springframework.web.util.DefaultUriBuilderFactory;
import java.net.URI;

public class TestUri3 {
    public static void main(String[] args) {
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("https://api.olamaps.io");
        
        // Option A: Passing string without template
        URI uriA = factory.expand("/path?origins=12.9,77.6%7C12.91,77.61");
        System.out.println("A: " + uriA.toString());
        
        // Option B: Passing string with template variables
        URI uriB = factory.expand("/path?origins={origins}", "12.9,77.6|12.91,77.61");
        System.out.println("B: " + uriB.toString());
    }
}
