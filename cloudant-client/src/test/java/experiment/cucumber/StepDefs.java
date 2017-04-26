package experiment.cucumber;

/**
 * Created by tomblench on 06/02/2017.
 */

import com.cloudant.client.api.model.Response;

import static org.junit.Assert.assertEquals;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

import java.util.Map;

public class StepDefs {

    // shareddata gets injected by picocontainer
    private SharedData data;


    public StepDefs(SharedData data) {
        this.data = data;
    }


    @Given("^a document named ([^ ]+) with fields ([^ ]+) equals ([^ ]+) exists$")
    public void givenDocumentExists(String document, String fieldKey, String fieldValue) throws Throwable {
        SharedData.TestDocument doc = new SharedData.TestDocument();
        doc._id = document;
        doc.fields.put(fieldKey, fieldValue);
        Response response = data.client.database("db1", false).save(doc);
        data.createdDocument = doc;
        // TODO check response?
    }

    // TODO this is RYW
    @When("^I read a document named ([^ ]+)$")
    public void whenReadDocument(String document) throws Throwable {
        SharedData.TestDocument retrieved = data.client.database("db1", false).find(SharedData.TestDocument.class, data.createdDocument._id);
        data.retrievedDocument = retrieved;
    }

    @Then("^the document has the same id and fields")
    public void thenDocumentHasSameIdAndFields() throws Throwable {
        assertEquals(data.createdDocument._id, data.retrievedDocument._id);
        assertEquals(data.createdDocument.fields, data.retrievedDocument.fields);
    }

}