package it.cnr.iit.peprest.messagetrack;

/**
 * This object holds many useful pieces of information:
 * <ol>
 * <li>The status of the message</li>
 * <li>The id that was assigned to the session initialized by the caller, if any, by which the caller can manage its
 * session</li>
 * <li>The id of the derived message (only after a tryAccess) that makes it easier for the caller to track the
 * evaluation of its session</li>
 * </ol>
 *
 * @author Antonio La Marra
 *
 */
public class CallerResponse {

    private PEP_STATUS status;
    private String sessionId;
    private String derivedMessageId;

    public PEP_STATUS getStatus() {
        return status;
    }

    public void setStatus( PEP_STATUS status ) {
        this.status = status;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId( String sessionId ) {
        this.sessionId = sessionId;
    }

    public String getDerivedMessageId() {
        return derivedMessageId;
    }

    public void setDerivedMessageId( String derivedMessageId ) {
        this.derivedMessageId = derivedMessageId;
    }

}
