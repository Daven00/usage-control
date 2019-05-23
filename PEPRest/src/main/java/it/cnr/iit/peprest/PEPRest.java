/*******************************************************************************
 * Copyright 2018 IIT-CNR
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package it.cnr.iit.peprest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.base.Throwables;

import it.cnr.iit.peprest.configuration.PEPProperties;
import it.cnr.iit.peprest.configuration.UCSProxyProperties;
import it.cnr.iit.peprest.messagetrack.MessageStorage;
import it.cnr.iit.peprest.messagetrack.MessageStorageInterface;
import it.cnr.iit.peprest.messagetrack.MessagesPerSession;
import it.cnr.iit.peprest.proxy.UCSProxy;
import it.cnr.iit.ucs.constants.OperationNames;
import it.cnr.iit.ucsinterface.message.EvaluatedResponse;
import it.cnr.iit.ucsinterface.message.MEAN;
import it.cnr.iit.ucsinterface.message.Message;
import it.cnr.iit.ucsinterface.message.endaccess.EndAccessMessage;
import it.cnr.iit.ucsinterface.message.reevaluation.ReevaluationResponse;
import it.cnr.iit.ucsinterface.message.startaccess.StartAccessMessage;
import it.cnr.iit.ucsinterface.message.tryaccess.TryAccessMessage;
import it.cnr.iit.ucsinterface.message.tryaccess.TryAccessResponse;
import it.cnr.iit.ucsinterface.pdp.PDPEvaluation;
import it.cnr.iit.ucsinterface.pep.PEPInterface;
import it.cnr.iit.ucsinterface.requestmanager.UCSCHInterface;
import it.cnr.iit.utility.Utility;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.xacmlutilities.wrappers.PolicyWrapper;
import it.cnr.iit.xacmlutilities.wrappers.RequestWrapper;

import oasis.names.tc.xacml.core.schema.wd_17.DecisionType;

/**
 * This is the PEP using rest
 *
 * @author Antonio La Marra, Alessandro Rosetti
 *
 */
@Component
public class PEPRest implements PEPInterface {

    private static final Logger log = Logger.getLogger( PEPRest.class.getName() );

    private static final String ERR_SEND_UCS_FAILED = "Unable to deliver messsage to UCS";

    // map of unanswered messages, the key is the id of the message
    private ConcurrentMap<String, Message> unansweredMap = new ConcurrentHashMap<>();
    private ConcurrentMap<String, Message> responsesMap = new ConcurrentHashMap<>();
    private MessageStorage messageStorage = new MessageStorage();

    @Autowired
    private PEPProperties pep;

    @Autowired
    private UCSCHInterface ucs;

    @Bean
    public PEPProperties getPEPProperties() {
        return new PEPProperties();
    }

    @Bean
    public UCSProxyProperties getUCSProxyProperties() {
        return new UCSProxyProperties();
    }

    @Bean
    public UCSCHInterface getUCSInterface() {
        return new UCSProxy();
    }

    public String tryAccess() {
        RequestWrapper request = RequestWrapper.build( Utility.readFileAbsPath( pep.getRequestPath() ) );
        PolicyWrapper policy = PolicyWrapper.build( Utility.readFileAbsPath( pep.getPolicyPath() ) );
        log.log( Level.INFO, "tryAccess at {0} ", System.currentTimeMillis() );
        TryAccessMessage message = buildTryAccessMessage( request, policy );
        return handleRequest( message );
    }

    public String startAccess( String sessionId ) {
        log.log( Level.INFO, "startAccess at {0} ", System.currentTimeMillis() );
        StartAccessMessage message = buildStartAccessMessage( sessionId );
        return handleRequest( message );
    }

    public String endAccess( String sessionId ) {
        log.log( Level.INFO, "endAccess at {0} ", System.currentTimeMillis() );
        EndAccessMessage message = buildEndAccessMessage( sessionId );
        return handleRequest( message );
    }

    @Override
    @Async
    public Message onGoingEvaluation( ReevaluationResponse message ) {
        Reject.ifNull( message );
        PDPEvaluation evaluation = message.getPDPEvaluation();
        Reject.ifNull( evaluation );
        log.log( Level.INFO, "onGoingEvaluation at {0} ", System.currentTimeMillis() );
        responsesMap.put( message.getMessageId(), message );
        messageStorage.addMessage( message );
        if( pep.getRevokeType().equals( "HARD" ) ) {
            log.log( Level.INFO, "endAcces sent at {0} ", System.currentTimeMillis() );
            EndAccessMessage endAccess = buildEndAccessMessage( evaluation.getSessionId(), null );
            handleRequest( endAccess );
        } else {
            // generic case to cater for multiple scenarios, e.g. pause/resume/pause/end etc...
            if( evaluation.isDecision( DecisionType.PERMIT ) ) {
                log.info( "RESUME EXECUTION" );
            } else if( evaluation.isDecision( DecisionType.DENY ) ) {
                log.info( "STOP EXECUTION" );
            }
        }
        message.setMotivation( "OK" );
        return message;
    }

    private TryAccessMessage buildTryAccessMessage( RequestWrapper request, PolicyWrapper policy ) {
        TryAccessMessage message = new TryAccessMessage( pep.getId(), pep.getBaseUri() );
        message.setPepUri( buildResponseApi( pep.getApiStatusChanged() ) );
        message.setPolicy( policy.getPolicy() );
        message.setRequest( request.getRequest() );
        message.setCallback( buildResponseApi( OperationNames.TRYACCESSRESPONSE_REST ), MEAN.REST );
        return message;
    }

    private StartAccessMessage buildStartAccessMessage( String sessionId ) {
        StartAccessMessage message = new StartAccessMessage( pep.getId(), pep.getBaseUri() );
        message.setSessionId( sessionId );
        message.setCallback( buildResponseApi( OperationNames.STARTACCESSRESPONSE_REST ), MEAN.REST );
        return message;
    }

    private EndAccessMessage buildEndAccessMessage( String sessionId ) {
        return buildEndAccessMessage( sessionId, buildResponseApi( OperationNames.ENDACCESSRESPONSE_REST ) );
    }

    private EndAccessMessage buildEndAccessMessage( String sessionId, String responseInterface ) {
        EndAccessMessage message = new EndAccessMessage( pep.getId(), pep.getBaseUri() );
        message.setSessionId( sessionId );
        message.setCallback( responseInterface, MEAN.REST );
        return message;
    }

    private String handleRequest( Message message ) {
        Reject.ifNull( message );
        if( ucs.sendMessageToCH( message ).isDelivered() ) {
            unansweredMap.put( message.getMessageId(), message );
            messageStorage.addMessage( message );
            return message.getMessageId();
        } else {
            throw Throwables.propagate( new IllegalAccessException( ERR_SEND_UCS_FAILED ) );
        }
    }

    @Override
    @Async
    public String receiveResponse( Message message ) {
        Reject.ifNull( message );
        try {
            responsesMap.put( message.getMessageId(), message );
            unansweredMap.remove( message.getMessageId() );
            messageStorage.addMessage( message );
            return handleResponse( message );
        } catch( Exception e ) { // NOSONAR
            log.log( Level.SEVERE, "Error occured while evaluating the response: {0}", e.getMessage() );
            throw Throwables.propagate( e );
        }
    }

    private String handleResponse( Message message ) {
        String response;
        if( message instanceof TryAccessResponse ) {
            response = handleTryAccessResponse( (TryAccessResponse) message );
        } else if( message instanceof EvaluatedResponse ) {
            response = ( (EvaluatedResponse) message ).getPDPEvaluation().getResult();
        } else {
            throw new IllegalArgumentException( "INVALID MESSAGE: " + message.toString() );
        }
        log.log( Level.INFO, "Evaluation {0} ", response );
        return response;
    }

    /**
     * Function that handles a tryAccessResponse
     *
     * @param response the response received by the UCS
     * @return a String stating the result of the evaluation or the ID of the startaccess message
     */
    private String handleTryAccessResponse( TryAccessResponse response ) {
        PDPEvaluation evaluation = response.getPDPEvaluation();
        if( evaluation.isDecision( DecisionType.PERMIT ) ) {
            return startAccess( response.getSessionId() );
        }
        return evaluation.getResult();
    }

    private final String buildResponseApi( String name ) {
        try {
            return new URL( new URL( pep.getBaseUri() ), name ).toString();
        } catch( MalformedURLException e ) {
            return null;
        }
    }

    /**
     * Retrieves the sessionId assigned in the tryAccessResponse
     * @param messageId the messageId assigned in the tryAccess request
     * @return an optional containing either the sessionId either nothing
     */
    public Optional<String> getSessionIdInTryAccess( String messageId ) {
        Reject.ifBlank( messageId );
        Optional<Message> message = getMessageFromId( messageId );
        if( message.isPresent() ) {
            TryAccessResponse response = (TryAccessResponse) message.get();
            return Optional.ofNullable( response.getSessionId() );
        }
        return Optional.empty();
    }

    /**
     * Retrieves the evaluation from the returned messageId
     * @param messageId the messageId assigned to that evaluation
     * @return an optional containing either the required evaluation or an empty one
     */
    public Optional<String> getEvaluationResult( String messageId ) {
        Reject.ifBlank( messageId );
        Optional<Message> optional = getMessageFromId( messageId );
        if( optional.isPresent() ) {
            Message message = optional.get();
            if( message instanceof EvaluatedResponse ) {
                String result = ( (EvaluatedResponse) message ).getPDPEvaluation().getResult();
                return Optional.of( result );
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves a message in the responses map
     * @param messageId the messageid assigned in the evaluation
     * @return an optional containing the message or nothing
     */
    private Optional<Message> getMessageFromId( String messageId ) {
        if( !responsesMap.containsKey( messageId ) ) {
            return Optional.empty();
        }
        return Optional.of( responsesMap.get( messageId ) );
    }

    public MessageStorageInterface getMessageStorage() {
        return messageStorage;
    }

    public MessagesPerSession getMessagesPerSession() {
        return messageStorage;
    }

    public void setMessageStorage( MessageStorage messageStorage ) {
        this.messageStorage = messageStorage;
    }

    public ConcurrentMap<String, Message> getResponses() {
        return responsesMap;
    }

    public ConcurrentMap<String, Message> getUnanswered() {
        return unansweredMap;
    }
}
