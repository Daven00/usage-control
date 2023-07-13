package it.cnr.iit.peprest.messagetrack;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import it.cnr.iit.ucs.message.Message;
import it.cnr.iit.ucs.message.endaccess.EndAccessMessage;
import it.cnr.iit.ucs.message.endaccess.EndAccessResponseMessage;
import it.cnr.iit.ucs.message.reevaluation.ReevaluationResponseMessage;
import it.cnr.iit.ucs.message.startaccess.StartAccessMessage;
import it.cnr.iit.ucs.message.startaccess.StartAccessResponseMessage;
import it.cnr.iit.ucs.message.tryaccess.TryAccessMessage;
import it.cnr.iit.ucs.message.tryaccess.TryAccessResponseMessage;
import it.cnr.iit.utility.errorhandling.Reject;

import oasis.names.tc.xacml.core.schema.wd_17.DecisionType;

/**
 *
 * @author Antonio La Marra
 *
 */
public class MessageStorage implements MessageStorageInterface, MessagesPerSession {

    private static final Logger log = Logger.getLogger( MessageStorage.class.getName() );

    private HashMap<String, MessageInformation> messageFlow = new HashMap<>();
    private HashMap<String, LinkedList<String>> messagesPerSession = new HashMap<>();

    @Override
    public Optional<CallerResponse> getMessageStatus( String messageId ) {
        if( messageId == null || messageId.isEmpty() ) {
            throw new IllegalArgumentException( "Invalid message id passed: " + messageId );
        }
        if( !messageFlow.containsKey( messageId ) ) {
            return Optional.empty();
        }
        return Optional.of( retrieveMessageStatus( messageId ) );
    }

    private CallerResponse retrieveMessageStatus( String messageId ) {
        if( messageFlow.get( messageId ).getCallerResponse().getStatus() != PEP_STATUS.TRYACCESS_PERMIT ) {
            return messageFlow.get( messageId ).getCallerResponse();
        } else {
            return retrieveLastMessageStatus( messageFlow.get( messageId ).getCallerResponse().getSessionId() );
        }
    }

    private CallerResponse retrieveLastMessageStatus( String sessionId ) {
        if( sessionId == null || sessionId.isEmpty() ) {
            throw new IllegalArgumentException( "TryAccessPermit message without sessionId" );
        }
        String messageId = messagesPerSession.get( sessionId ).getLast();
        return messageFlow.get( messageId ).getCallerResponse();
    }

    @Override
    public boolean addMessage( Message message ) {
        if( message == null ) {
            log.severe( "Message is null" );
            return false;
        }
        if( messageFlow.containsKey( message.getMessageId() ) ) {
            return mergeMessages( message );
        } else if( message instanceof TryAccessMessage ) {
            return addNewMessage( message );
        } else if( message instanceof StartAccessMessage ) {
            addMessageId( ( (StartAccessMessage) message ).getSessionId(), message.getMessageId() );
            return addNewMessage( message );
        } else if( message instanceof EndAccessMessage ) {
            addMessageId( ( (EndAccessMessage) message ).getSessionId(), message.getMessageId() );
            return addNewMessage( message );
        } else if( message instanceof ReevaluationResponseMessage ) {
            addMessageId( ( (ReevaluationResponseMessage) message ).getSessionId(), message.getMessageId() );
            return addNewMessage( message );
        } else {
            throw new IllegalArgumentException( "Invalid message" );
        }
    }

    private boolean mergeMessages( Message message ) {
        MessageInformation messageInformation = messageFlow.get( message.getMessageId() );
        if( message instanceof TryAccessResponseMessage ) {
            addMessagePerSession( (TryAccessResponseMessage) message );
            messageInformation.merge( (TryAccessResponseMessage) message );
        } else if( message instanceof StartAccessResponseMessage ) {
            messageInformation.merge( (StartAccessResponseMessage) message );
        } else if( message instanceof EndAccessResponseMessage ) {
            messageInformation.merge( (EndAccessResponseMessage) message );
        }
        return insert( messageInformation );
    }

    private boolean addNewMessage( Message message ) {
        MessageInformation messageInformation = null;
        if( message instanceof TryAccessMessage ) {
            messageInformation = MessageInformation.build( (TryAccessMessage) message );
        } else if( message instanceof StartAccessMessage ) {
            messageInformation = MessageInformation.build( (StartAccessMessage) message );
        } else if( message instanceof EndAccessMessage ) {
            messageInformation = MessageInformation.build( (EndAccessMessage) message );
        } else if( message instanceof ReevaluationResponseMessage ) {
            messageInformation = MessageInformation.build( (ReevaluationResponseMessage) message );
        }
        return insert( messageInformation );
    }

    private boolean insert( MessageInformation messageInformation ) {
        if( messageInformation == null ) {
            log.severe( "MessageInformation is null" );
            return false;
        }
        messageFlow.put( messageInformation.getMessageId(), messageInformation );
        return true;
    }

    /**
     * Retrieves the list of messages exchanged for a certain session id
     *
     * @param sessionId the session id to be considered
     * @return the list of message ids exchanged for that session
     */
    @Override
    public List<String> getMessagesPerSession( String sessionId ) {
        Reject.ifBlank( sessionId );
        if( !messagesPerSession.containsKey( sessionId ) ) {
            throw new IllegalArgumentException();
        }
        return messagesPerSession.get( sessionId );
    }

    private void addMessagePerSession( TryAccessResponseMessage message ) {
        if( message.getEvaluation().getResult().equals( DecisionType.PERMIT.value() ) ) {
            messagesPerSession.put( message.getSessionId(), new LinkedList<>() );
            addMessageId( message.getSessionId(), message.getMessageId() );
        }
    }

    private void addMessageId( String sessionId, String messageId ) {
        messagesPerSession.get( sessionId ).add( messageId );
    }

    public void clear() {
        messageFlow.clear();
        messagesPerSession.clear();
    }

}
