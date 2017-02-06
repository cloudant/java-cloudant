package experiment.cucumber;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

/**
 * Created by tomblench on 06/02/2017.
 */

@RunWith(Cucumber.class)
@CucumberOptions(features = "cloudant-client/src/test/resources")
public class RunCukesTest {

}
