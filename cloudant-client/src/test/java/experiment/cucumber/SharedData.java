package experiment.cucumber;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by tomblench on 06/02/2017.
 */
public class SharedData {

    public TestDocument createdDocument;
    public TestDocument retrievedDocument;

    public CloudantClient client;

    // TODO this would probably live in some common test utils class
    public static class TestDocument {
        public String _id;
        public String _rev;
        public Map<String, String> fields;
        public TestDocument() {
            fields = new HashMap<String, String>();
        }
    }

    public SharedData() throws Exception {
        System.out.println("Hello from shared data");
        client = ClientBuilder.url(new URL("http://localhost:5984")).build();
        // TODO this should be in the right before/after steps to create/delete databases
        client.deleteDB("db1");
        client.database("db1", true);
    }
}
