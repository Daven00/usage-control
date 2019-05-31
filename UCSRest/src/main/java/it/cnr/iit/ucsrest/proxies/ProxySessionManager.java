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
package it.cnr.iit.ucsrest.proxies;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import it.cnr.iit.ucs.constants.CONNECTION;
import it.cnr.iit.ucs.properties.components.SessionManagerProperties;
import it.cnr.iit.ucs.sessionmanager.OnGoingAttributesInterface;
import it.cnr.iit.ucs.sessionmanager.SessionAttributes;
import it.cnr.iit.ucs.sessionmanager.SessionInterface;
import it.cnr.iit.ucs.sessionmanager.SessionManagerInterface;
import it.cnr.iit.ucsrest.rest.UCSRest;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.xacmlutilities.Attribute;

/**
 * This is the proxy to be used to communicate with the session manager.
 * <p>
 * The session manager is basically a database which can be implemented in
 * various forms:
 * <ol>
 * <li>Through an SQL database: in this case the SessionManager is not
 * distributed and it is local to the UCS</li>
 * <li>Through a NoSQL database: in this case the SessionManager is distributed
 * and</li>
 * <li>Through SOCKET: in this case the SessionManager is not in the same JVM of
 * the ContextHandler but offers a socket through which it can receive and send
 * messages</li>
 * <li>Through REST API: in this case the SessionManager is not in the same JVM
 * of the ContextHandler but offers however REST_APi to deal with it</li>
 * </ol>
 * The first two cases are indistinguishable also form the PROXY perspective, it
 * just knows that to deal with the session manager it can use the api.
 * </p>
 *
 * @author Antonio La Marra, Alessandro Rosetti
 *
 */
public class ProxySessionManager implements SessionManagerInterface {

    private static final Logger log = Logger.getLogger( ProxySessionManager.class.getName() );

    private SessionManagerProperties properties;
    private SessionManagerInterface sessionManager;

    private volatile boolean started = false;
    private volatile boolean initialized = false;

    public ProxySessionManager( SessionManagerProperties properties ) {
        Reject.ifNull( properties );
        Reject.ifNull( properties.getCommunicationType() );

        this.properties = properties;

        switch( getConnection() ) {
            case LOCAL:
                if( buildLocalSessionManager( properties ) ) {
                    initialized = true;
                }
                break;
            case SOCKET:
            case REST:
                log.log( Level.WARNING, CONNECTION.MSG_ERR_UNIMPLEMENTED, properties.getCommunicationType() );
                break;
            default:
                log.log( Level.SEVERE, CONNECTION.MSG_ERR_INCORRECT, properties.getCommunicationType() );
                return;
        }
    }

    private boolean buildLocalSessionManager( SessionManagerProperties properties ) {
        Optional<SessionManagerProperties> optSm = UCSRest.buildComponent( properties );

        if( optSm.isPresent() ) {
            sessionManager = (SessionManagerInterface) optSm.get();
            return true;
        }
        log.severe( "Error building Session Manager" );
        return false;
    }

    @Override
    public Boolean start() {
        if( !initialized ) {
            return false;
        }

        switch( getConnection() ) {
            case LOCAL:
                started = sessionManager.start();
                return started;
            case SOCKET:
            case REST:
                return false;
            default:
                log.log( Level.SEVERE, CONNECTION.MSG_ERR_INCORRECT, properties.getCommunicationType() );
                return false;
        }
    }

    @Override
    public Boolean stop() {
        if( initialized == false ) {
            return false;
        }

        switch( getConnection() ) {
            case LOCAL:
                started = !sessionManager.stop();
                return !started;
            case SOCKET:
            case REST:
                return false;
            default:
                log.log( Level.SEVERE, CONNECTION.MSG_ERR_INCORRECT, properties.getCommunicationType() );
                return false;
        }
    }

    @Override
    public Boolean createEntryForSubject( String sessionId, String policySet,
            String originalRequest, List<String> onGoingAttributesForSubject,
            String status, String pepURI, String myIP, String subjectName ) {
        if( !initialized || !started ) {
            return false;
        }

        switch( getConnection() ) {
            case LOCAL:
                return sessionManager.createEntryForSubject( sessionId,
                    policySet, originalRequest, onGoingAttributesForSubject, status,
                    pepURI, myIP, subjectName );
            case SOCKET:
            case REST:
                return false;
            default:
                return sessionManager.createEntryForSubject( sessionId,
                    policySet, originalRequest, onGoingAttributesForSubject, status,
                    pepURI, myIP, subjectName );
        }
    }

    @Override
    public Boolean createEntryForResource( String sessionId, String policySet,
            String originalRequest, List<String> onGoingAttributesForObject,
            String status, String pepURI, String myIP, String objectName ) {
        if( !initialized || !started ) {
            return false;
        }

        switch( getConnection() ) {
            case LOCAL:
                return sessionManager.createEntryForResource( sessionId,
                    policySet, originalRequest, onGoingAttributesForObject, status,
                    pepURI, myIP, objectName );
            case SOCKET:
            case REST:
                return false;
            default:
                log.severe( "Incorrect communication medium : " + properties.getCommunicationType() );
                return false;
        }
    }

    @Override
    public Boolean createEntry( SessionAttributes parameterObject ) {
        // BEGIN parameter checking
        if( !initialized || !started ) {
            return false;
        }

        switch( getConnection() ) {
            case LOCAL:
                return sessionManager.createEntry( parameterObject );
            case SOCKET:
            case REST:
                return false;
            default:
                return sessionManager
                    .createEntry( parameterObject );
        }
    }

    @Override
    public Boolean createEntryForAction( String sessionId, String policySet,
            String originalRequest, List<String> onGoingAttributesForAction,
            String status, String pepURI, String myIP, String actionName ) {
        if( !initialized || !started ) {
            return false;
        }

        switch( getConnection() ) {
            case LOCAL:
                return sessionManager.createEntryForAction( sessionId,
                    policySet, originalRequest, onGoingAttributesForAction, status,
                    pepURI, myIP, actionName );
            case SOCKET:
            case REST:
                return false;
            default:
                return sessionManager.createEntryForAction( sessionId,
                    policySet, originalRequest, onGoingAttributesForAction, status,
                    pepURI, myIP, actionName );
        }
    }

    @Override
    public Boolean createEntryForEnvironment( String sessionId, String policySet,
            String originalRequest, List<String> onGoingAttributesForEnvironment,
            String status, String pepURI, String myIP ) {
        if( !initialized || !started ) {
            return false;
        }

        switch( getConnection() ) {
            case LOCAL:
                return sessionManager.createEntryForEnvironment( sessionId,
                    policySet, originalRequest, onGoingAttributesForEnvironment, status,
                    pepURI, myIP );
            case SOCKET:
            case REST:
                return false;
            default:
                return sessionManager.createEntryForEnvironment( sessionId,
                    policySet, originalRequest, onGoingAttributesForEnvironment, status,
                    pepURI, myIP );
        }
    }

    @Override
    public Boolean updateEntry( String sessionId, String status ) {
        if( !initialized || !started ) {
            return false;
        }

        switch( getConnection() ) {
            case LOCAL:
                return sessionManager.updateEntry( sessionId, status );
            case SOCKET:
            case REST:
                return false;
            default:
                return sessionManager.updateEntry( sessionId, status );
        }
    }

    @Override
    public Boolean deleteEntry( String sessionId ) {
        if( !initialized || !started ) {
            return false;
        }

        switch( getConnection() ) {
            case LOCAL:
                return sessionManager.deleteEntry( sessionId );
            case SOCKET:
            case REST:
                return false;
            default:
                return sessionManager.deleteEntry( sessionId );
        }
    }

    @Override
    public List<SessionInterface> getSessionsForAttribute( String attributeId ) {
        if( !initialized || !started ) {
            return new ArrayList<>();
        }

        switch( getConnection() ) {
            case LOCAL:
                return sessionManager.getSessionsForAttribute( attributeId );
            case SOCKET:
            case REST:
                return new ArrayList<>();
            default:
                return sessionManager.getSessionsForAttribute( attributeId );
        }
    }

    @Override
    public List<SessionInterface> getSessionsForSubjectAttributes(
            String subjectName, String attributeId ) {
        if( !initialized || !started ) {
            return new ArrayList<>();
        }

        switch( getConnection() ) {
            case LOCAL:
                return sessionManager
                    .getSessionsForSubjectAttributes( subjectName, attributeId );
            case SOCKET:
            case REST:
                return new ArrayList<>();
            default:
                return sessionManager
                    .getSessionsForSubjectAttributes( subjectName, attributeId );
        }
    }

    @Override
    public List<SessionInterface> getSessionsForResourceAttributes(
            String objectName, String attributeId ) {
        if( !initialized || !started ) {
            return new ArrayList<>();
        }

        switch( getConnection() ) {
            case LOCAL:
                return sessionManager
                    .getSessionsForResourceAttributes( objectName, attributeId );
            case SOCKET:
            case REST:
                return new ArrayList<>();
            default:
                return sessionManager
                    .getSessionsForResourceAttributes( objectName, attributeId );
        }
    }

    @Override
    public List<SessionInterface> getSessionsForActionAttributes(
            String actionName, String attributeId ) {
        if( !initialized || !started ) {
            return new ArrayList<>();
        }

        switch( getConnection() ) {
            case LOCAL:
                return sessionManager
                    .getSessionsForActionAttributes( actionName, attributeId );
            case SOCKET:
            case REST:
                return new ArrayList<>();
            default:
                return sessionManager
                    .getSessionsForActionAttributes( actionName, attributeId );
        }
    }

    @Override
    public Optional<SessionInterface> getSessionForId( String sessionId ) {
        if( !initialized || !started ) {
            return Optional.empty();
        }

        switch( getConnection() ) {
            case LOCAL:
                return sessionManager.getSessionForId( sessionId );
            case SOCKET:
            case REST:
                return Optional.empty();
            default:
                return sessionManager.getSessionForId( sessionId );
        }
    }

    @Override
    public List<SessionInterface> getSessionsForStatus( String status ) {
        if( !initialized || !started ) {
            return new ArrayList<>();
        }

        switch( getConnection() ) {
            case LOCAL:
                return sessionManager.getSessionsForStatus( status );
            case SOCKET:
            case REST:
                return new ArrayList<>();
            default:
                return sessionManager.getSessionsForStatus( status );
        }
    }

    @Override
    public List<SessionInterface> getSessionsForEnvironmentAttributes(
            String attributeId ) {
        if( !initialized || !started ) {
            return new ArrayList<>();
        }

        switch( getConnection() ) {
            case LOCAL:
                return sessionManager
                    .getSessionsForEnvironmentAttributes( attributeId );
            case SOCKET:
            case REST:
                return new ArrayList<>();
            default:
                return sessionManager
                    .getSessionsForEnvironmentAttributes( attributeId );
        }
    }

    @Override
    public List<OnGoingAttributesInterface> getOnGoingAttributes( String sessionId ) {
        if( !initialized || !started ) {
            return new ArrayList<>();
        }

        switch( getConnection() ) {
            case LOCAL:
                return sessionManager.getOnGoingAttributes( sessionId );
            case SOCKET:
            case REST:
                return new ArrayList<>();
            default:
                return sessionManager.getOnGoingAttributes( sessionId );
        }
    }

    @Override
    public STATUS checkSession( String sessionId, Attribute attribute ) {
        if( !initialized || !started ) {
            return null;
        }

        switch( getConnection() ) {
            case LOCAL:
                return sessionManager.checkSession( sessionId, attribute );
            case SOCKET:
            case REST:
                return null;
            default:
                return sessionManager.checkSession( sessionId, attribute );
        }
    }

    @Override
    public boolean insertSession( SessionInterface session, Attribute attribute ) {
        if( !initialized || !started ) {
            return false;
        }

        switch( getConnection() ) {
            case LOCAL:
                return sessionManager.insertSession( session, attribute );
            case SOCKET:
            case REST:
                return false;
            default:
                return sessionManager.insertSession( session, attribute );
        }
    }

    @Override
    public boolean stopSession( SessionInterface session ) {
        if( !initialized || !started ) {
            return false;
        }

        switch( getConnection() ) {
            case LOCAL:
                return sessionManager.stopSession( session );
            case SOCKET:
            case REST:
                return false;
            default:
                return sessionManager.stopSession( session );
        }
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    protected CONNECTION getConnection() {
        return CONNECTION.valueOf( properties.getCommunicationType() );
    }

}