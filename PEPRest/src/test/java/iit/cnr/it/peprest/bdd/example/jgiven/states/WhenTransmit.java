package iit.cnr.it.peprest.bdd.example.jgiven.states;

import static org.junit.Assert.*;

import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ExpectedScenarioState;
import com.tngtech.jgiven.annotation.ProvidedScenarioState;

import iit.cnr.it.peprest.bdd.example.jgiven.Message;
import iit.cnr.it.peprest.bdd.example.jgiven.PEPRestService;

public class WhenTransmit extends Stage<WhenTransmit> {

    PEPRestService pEPRestService;

    @ExpectedScenarioState
    String originNode;
    @ExpectedScenarioState
    String destinationNode;

    @ProvidedScenarioState
    Message message;

    public WhenTransmit(){
        pEPRestService = new PEPRestService();
    }

    public WhenTransmit we_declare_the_nodes() {
        assertNotNull( pEPRestService );
        pEPRestService.selectNodes(originNode, destinationNode);
        return self();
    }

    public WhenTransmit insert_message_content(float money) {
        assertNotNull( pEPRestService );
        message = pEPRestService.transmitMessage(money);
        return self();
    }
}
