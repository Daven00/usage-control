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
package it.cnr.iit.ucs.contexthandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.cnr.iit.common.attributes.AttributeIds;
import it.cnr.iit.ucs.constants.STATUS;
import it.cnr.iit.ucs.exceptions.PolicyException;
import it.cnr.iit.ucs.exceptions.RequestException;
import it.cnr.iit.ucs.exceptions.SessionManagerException;
import it.cnr.iit.ucs.exceptions.StatusException;
import it.cnr.iit.ucs.message.attributechange.AttributeChangeMessage;
import it.cnr.iit.ucs.message.endaccess.EndAccessMessage;
import it.cnr.iit.ucs.message.endaccess.EndAccessResponseMessage;
import it.cnr.iit.ucs.message.reevaluation.ReevaluationResponseMessage;
import it.cnr.iit.ucs.message.startaccess.StartAccessMessage;
import it.cnr.iit.ucs.message.startaccess.StartAccessResponseMessage;
import it.cnr.iit.ucs.message.tryaccess.TryAccessMessage;
import it.cnr.iit.ucs.message.tryaccess.TryAccessResponseMessage;
import it.cnr.iit.ucs.pdp.PDPEvaluation;
import it.cnr.iit.ucs.properties.components.ContextHandlerProperties;
import it.cnr.iit.ucs.sessionmanager.OnGoingAttributesInterface;
import it.cnr.iit.ucs.sessionmanager.SessionAttributesBuilder;
import it.cnr.iit.ucs.sessionmanager.SessionInterface;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.xacml.Attribute;
import it.cnr.iit.xacml.Category;
import it.cnr.iit.xacml.PolicyTags;
import it.cnr.iit.xacml.wrappers.PolicyWrapper;
import it.cnr.iit.xacml.wrappers.RequestWrapper;
import oasis.names.tc.xacml.core.schema.wd_17.DecisionType;

/**
 * The context handler coordinates the ucs operations and spawns a thread in
 * charge of monitoring eventual changes in the value of the attributes.
 *
 * @author Antonio La Marra, Alessandro Rosetti
 */
public final class ContextHandler extends AbstractContextHandler {

	private static final Logger log = Logger.getLogger(ContextHandler.class.getName());

	@Deprecated
	public static final String PEP_ID_SEPARATOR = "#";

	public ContextHandler(ContextHandlerProperties properties) {
		super(properties);
	}

	/**
	 * TryAccess method invoked by the PEP
	 */
	@Override
	public TryAccessResponseMessage tryAccess(TryAccessMessage message) throws PolicyException, RequestException {
		log.log(Level.SEVERE, "TryAccess received at {0}", new Object[] { System.currentTimeMillis() });
		Reject.ifNull(message, "TryAccessMessage is null");

		PolicyWrapper policy = PolicyWrapper.build(getPap(), message);
		RequestWrapper request = RequestWrapper.build(message.getRequest(), getPipRegistry());
		request.update();

		String actionAttrValue = request.getRequestType().getAttribute(Category.ACTION.toString(),
				AttributeIds.ACTION_ID);

		log.severe("TryAccess policy: " + policy.getPolicy());
		log.severe("TryAccess request: " + request.getRequest());
		request.fatten(false);
		log.severe("TryAccess fattened request: " + request.getRequest());

		PDPEvaluation evaluation = getPdp().evaluate(request, policy, STATUS.TRY);
		Reject.ifNull(evaluation);
		log.severe("TryAccess response: " + evaluation.getResponse());

		String sessionId = generateSessionId();
		getObligationManager().translateObligations(evaluation, sessionId, STATUS.TRY);

		if (evaluation.isDecision(DecisionType.PERMIT) && actionAttrValue.contains("invoke")) {
			RequestWrapper origRequest = RequestWrapper.build(message.getRequest(), getPipRegistry());
			createSession(message, origRequest, policy, sessionId);
		}

		return buildTryAccessResponse(message, evaluation, sessionId);
	}

	private TryAccessResponseMessage buildTryAccessResponse(TryAccessMessage message, PDPEvaluation evaluation,
			String sessionId) {
		TryAccessResponseMessage response = new TryAccessResponseMessage(uri.getHost(), message.getSource(),
				message.getMessageId());
		response.setSessionId(sessionId);
		response.setEvaluation(evaluation);
		return response;
	}

	/**
	 * It creates a new session id
	 *
	 * @return session id to associate to the incoming session during the tryAccess
	 */
	private String generateSessionId() {
		return UUID.randomUUID().toString();
	}

	/**
	 * This function creates a new session in the session manager.
	 *
	 * @param message   the message
	 * @param request   the original request, not the fat one because, whenever we
	 *                  need to re-evaluate the request we will retrieval from the
	 *                  various PIPs a fresh value
	 * @param policy    the policy
	 * @param sessionId the sessionId
	 */
	private void createSession(TryAccessMessage message, RequestWrapper request, PolicyWrapper policy,
			String sessionId) {
		log.log(Level.SEVERE, "Creating a new session : {0} ", sessionId);

		String pepUri = uri.getHost() + PEP_ID_SEPARATOR + message.getSource();

		// retrieve the id of ongoing attributes
		List<Attribute> onGoingAttributes = policy.getAttributesForCondition(PolicyTags.getCondition(STATUS.START));
		log.severe("list of ongoing attributes:");
		onGoingAttributes.stream().forEach(el -> log.severe(el.getAttributeId()));

		try {
			SessionAttributesBuilder sessionAttributeBuilder = new SessionAttributesBuilder();
			sessionAttributeBuilder
					.setOnGoingAttributesForSubject(getAttributeIdsForCategory(onGoingAttributes, Category.SUBJECT))
					.setOnGoingAttributesForAction(getAttributeIdsForCategory(onGoingAttributes, Category.ACTION))
					.setOnGoingAttributesForResource(getAttributeIdsForCategory(onGoingAttributes, Category.RESOURCE))
					.setOnGoingAttributesForEnvironment(
							getAttributeIdsForCategory(onGoingAttributes, Category.ENVIRONMENT));
			sessionAttributeBuilder.setSubjectName(request.getRequestType().getAttributeValue(Category.SUBJECT))
					.setResourceName(request.getRequestType().getAttributeValue(Category.RESOURCE))
					.setActionName(request.getRequestType().getAttributeValue(Category.ACTION));
			sessionAttributeBuilder.setSessionId(sessionId).setPolicySet(policy.getPolicy())
					.setOriginalRequest(request.getRequest()).setStatus(STATUS.TRY.name()).setPepURI(pepUri)
					.setMyIP(uri.getHost());

			log.severe("sessionAttributeBuilder: ");
			log.severe(new ObjectMapper().writeValueAsString(sessionAttributeBuilder.build()));
			if (!getSessionManager().createEntry(sessionAttributeBuilder.build())) {
				log.log(Level.SEVERE, "Session \"{0}\" has not been stored correctly", sessionId);
			}
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SessionManagerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Retrieves the AttributeIDs of the attributes used for the ongoing evaluation
	 *
	 * @param onGoingAttributes the list of attributes used for ongoing evaluation
	 * @param category          the category of the attributes
	 * @return the list of the string representing the IDs of the attributes
	 */
	private List<String> getAttributeIdsForCategory(List<Attribute> onGoingAttributes, Category category) {
		ArrayList<String> attributeIds = new ArrayList<>();
		for (Attribute attribute : onGoingAttributes) {
			if (attribute.getCategory() == category) {
				attributeIds.add(attribute.getAttributeId());
			}
		}
		return attributeIds;
	}

	/**
	 * startAccess method invoked by PEP
	 */
	@Override
	public StartAccessResponseMessage startAccess(StartAccessMessage message)
			throws StatusException, PolicyException, RequestException {
		log.log(Level.SEVERE, "StartAccess begin scheduling at {0}", System.currentTimeMillis());

		Optional<SessionInterface> optSession = getSessionManager().getSessionForId(message.getSessionId());
		Reject.ifAbsent(optSession, "StartAccess: no session for id " + message.getSessionId());
		SessionInterface session = optSession.get(); // NOSONAR

		// Check if the session has the correct status
		if (!session.isStatus(STATUS.TRY.name())) {
			log.log(Level.SEVERE, "StartAccess: wrong status for session {0}", message.getSessionId());
			throw new StatusException(
					"StartAccess: tryaccess must be performed yet for session " + message.getSessionId());
		}

		PolicyWrapper policy = PolicyWrapper.build(session.getPolicySet());
		RequestWrapper request = RequestWrapper.build(session.getOriginalRequest(), getPipRegistry());
		request.update();

		log.severe("StartAccess policy: " + policy.getPolicy());
		log.severe("StartAccess request: " + request.getRequest());
		request.fatten(true);
		log.severe("StartAccess fattened request: " + request.getRequest());

		PDPEvaluation evaluation = getPdp().evaluate(request, policy, STATUS.START);
		Reject.ifNull(evaluation);
		log.severe("StartAccess response: " + evaluation.getResponse());

//		getObligationManager().translateObligations(evaluation, message.getSessionId(), STATUS.TRY);
		getObligationManager().translateObligations(evaluation, message.getSessionId(), STATUS.START);

		if (evaluation.isDecision(DecisionType.PERMIT)) {
			if (!getSessionManager().updateEntry(message.getSessionId(), STATUS.START.name())) {
				log.log(Level.SEVERE, "StartAccess error, sessionId {0} status update failed", message.getSessionId());

			}

			optSession = getSessionManager().getSessionForId(message.getSessionId());
			Reject.ifAbsent(optSession, "StartAccess: no session for id " + message.getSessionId());
			session = optSession.get(); // NOSONAR
		} else {
			List<Attribute> attributes = policy.getAttributesForCondition(PolicyTags.getCondition(STATUS.START));
			if (revoke(session, attributes) && !getSessionManager().deleteEntry(message.getSessionId())) {
				log.log(Level.SEVERE, "StartAccess error, sessionId {0} deletion failed", message.getSessionId());
			}
		}

		return buildStartAccessResponse(message, evaluation);
	}

	private StartAccessResponseMessage buildStartAccessResponse(StartAccessMessage message, PDPEvaluation evaluation) {
		StartAccessResponseMessage response = new StartAccessResponseMessage(message.getDestination(),
				message.getSource(), message.getMessageId());
		response.setEvaluation(evaluation);
		return response;
	}

	/**
	 * This is the code for the revoke. A revoke is always triggered by and
	 * EndAccess, in this function, all the attributes are un-subscribed.
	 */
	private synchronized boolean revoke(SessionInterface session, List<Attribute> attributes) {
		log.log(Level.SEVERE, "Revoke begins at {0}", System.currentTimeMillis());

		attributes.stream().forEach(el -> log.severe("called revoke for attribute " + el.getAttributeId()));
		boolean otherSessions = attributesToUnsubscribe(session.getId(), (ArrayList<Attribute>) attributes);
		if (!otherSessions) {
			getPipRegistry().unsubscribeAll(attributes);
		}
		if (!getSessionManager().deleteEntry(session.getId())) {
			log.log(Level.SEVERE, "EndAccess: errors during entry deletion for sessionId {0}", session.getId());
			return false;
		}

		log.log(Level.SEVERE, "Revoke ends at {0}", System.currentTimeMillis());
		return true;
	}

	/**
	 * This function checks if there are attributes to be unsubscribed. The first
	 * step is to retrieve the list of ongoing attributes, then we have to
	 * unsubscribe all those attributes that are not needed anymore.
	 *
	 * @param sessionId  the id of the session we're revoking
	 * @param attributes the JSON object to be filled by this function
	 * @return true if there are attributes to unsubscribe, false otherwise
	 */
	private boolean attributesToUnsubscribe(String sessionId, ArrayList<Attribute> attributes) {
		String subjectName = "";
		String resourceName = "";
		String actionName = "";
		// retrieve on going attributes for both subject and object
		Collection<OnGoingAttributesInterface> onGoingAttributes = getSessionManager().getOnGoingAttributes(sessionId);
		List<OnGoingAttributesInterface> subjectOnGoingAttributesList = new LinkedList<>();
		List<OnGoingAttributesInterface> resourceOnGoingAttributesList = new LinkedList<>();
		List<OnGoingAttributesInterface> actionOnGoingAttributesList = new LinkedList<>();
		List<OnGoingAttributesInterface> environmentOnGoingAttributesList = new LinkedList<>();

		// build attribute lists for subject, resource, action and environment
		if (onGoingAttributes != null && !onGoingAttributes.isEmpty()) {
			// fill the correspondent list of ongoingAttributes
			for (OnGoingAttributesInterface attribute : onGoingAttributes) {
				if (attribute.getSubjectName() != null && !attribute.getSubjectName().equals("null")) {
					subjectOnGoingAttributesList.add(attribute);
					subjectName = attribute.getSubjectName();
				} else if (attribute.getResourceName() != null && !attribute.getResourceName().equals("null")) {
					resourceOnGoingAttributesList.add(attribute);
					resourceName = attribute.getResourceName();
				} else if (attribute.getActionName() != null && !attribute.getActionName().equals("null")) {
					actionOnGoingAttributesList.add(attribute);
					actionName = attribute.getActionName();
				} else {
					environmentOnGoingAttributesList.add(attribute);
				}
			}
		}

		// builds up the JSON object that is needed to perform unsubscribe
		boolean otherSessions = true;
		if (onGoingAttributes != null && !onGoingAttributes.isEmpty()) {
			otherSessions = buildOnGoingAttributes(Category.RESOURCE, attributes, resourceName, otherSessions,
					resourceOnGoingAttributesList);
			otherSessions = buildOnGoingAttributes(Category.SUBJECT, attributes, subjectName, otherSessions,
					subjectOnGoingAttributesList);
			otherSessions = buildOnGoingAttributes(Category.ACTION, attributes, actionName, otherSessions,
					actionOnGoingAttributesList);
			otherSessions = buildOnGoingAttributes(Category.ENVIRONMENT, attributes, "", otherSessions,
					environmentOnGoingAttributesList);
		}
		return otherSessions;
	}

	private boolean buildOnGoingAttributes(Category category, ArrayList<Attribute> attributes, String name,
			boolean otherSessions, List<OnGoingAttributesInterface> listOngoingAttributes) {
		for (OnGoingAttributesInterface attribute : listOngoingAttributes) {
			List<SessionInterface> sessionList = getSessionListForCategory(category, attribute.getAttributeId(), name);
			if (sessionList == null || sessionList.isEmpty() || sessionList.size() == 1) {
				otherSessions = false;
				attributes.add(buildAttribute(attribute, name));
			}
		}
		return otherSessions;
	}

	private List<SessionInterface> getSessionListForCategory(Category category, String id, String name) {
		log.severe("in getSessionListForCategory: category=" + category.toString() + " id=" + id + " name=" + name);

		switch (category) {
		case ENVIRONMENT:
			return getSessionManager().getSessionsForEnvironmentAttributes(id);
		case ACTION:
			return getSessionManager().getSessionsForActionAttributes(name, id);
		case SUBJECT:
			return getSessionManager().getSessionsForSubjectAttributes(name, id);
		case RESOURCE:
			return getSessionManager().getSessionsForResourceAttributes(name, id);
		default:
			log.severe("Invalid attribute passed");
			return new ArrayList<>();
		}
	}

	private Attribute buildAttribute(OnGoingAttributesInterface ongoingAttribute, String name) {
		Attribute attribute = new Attribute();
		attribute.setAttributeId(ongoingAttribute.getAttributeId());
		if (!name.isEmpty()) {
			attribute.setAdditionalInformations(name);
		}
		return attribute;
	}

	/**
	 * endAccess method invoked by PEP
	 */
	@Override
	public EndAccessResponseMessage endAccess(EndAccessMessage message)
			throws StatusException, RequestException, PolicyException {
		// IMPORTANT: THE ENDACCESS OPERATION CANNOT WORK WITH THE CURRENTS POLICIES
		// BECAUSE
		// THE DecisionTime="post" IS MISSING
		// THE ENDACCESS WILL RETURN ALWAYS A DENY, NO MATTERS THE REQUEST

		log.log(Level.SEVERE, "EndAccess begins at {0}", System.currentTimeMillis());
		Reject.ifNull(message, "EndAccessMessage is null");

		Optional<SessionInterface> optSession = getSessionManager().getSessionForId(message.getSessionId());
		Reject.ifAbsent(optSession, "EndAccess: no session for id " + message.getSessionId());
		SessionInterface session = optSession.get(); // NOSONAR

		// Check if the session has the correct status
//		log.severe("session.getStatus()=" + session.getStatus());
//		if (!(session.isStatus(STATUS.START.name()) || session.isStatus(STATUS.REVOKE.name()))) {
//			log.log(Level.SEVERE, "EndAccess: wrong status for session {0}", message.getSessionId());
//			throw new StatusException("EndAccess: wrong status for session " + message.getSessionId());
//		}

		PolicyWrapper policy = PolicyWrapper.build(session.getPolicySet());
		RequestWrapper request = RequestWrapper.build(session.getOriginalRequest(), getPipRegistry());
		request.update();

		log.severe("EndAccess policy: " + policy.getPolicy());
		log.severe("EndAccess request: " + request.getRequest());
		request.fatten(false);
		log.severe("EndAccess fatten request: " + request.getRequest());

		PDPEvaluation evaluation = getPdp().evaluate(request, policy, STATUS.END);
		Reject.ifNull(evaluation);
		log.severe("EndAccess response:" + evaluation.getResponse());

		log.log(Level.SEVERE, "EndAccess evaluated at {0} pdp response : {1}",
				new Object[] { System.currentTimeMillis(), evaluation.getResult() });

		getObligationManager().translateObligations(evaluation, message.getSessionId(), STATUS.END);

		List<Attribute> attributes = policy.getAttributesForCondition(PolicyTags.getCondition(STATUS.START));
		// Yes, START, because the policy has not any post condition, so we need to
		// retrieve the ongoing
		// access must be revoked
		if (revoke(session, attributes)) {
			log.log(Level.SEVERE, "EndAccess evaluation with revoke ends at {0}", System.currentTimeMillis());
		}

		return buildEndAccessResponse(message, evaluation);
	}

	private EndAccessResponseMessage buildEndAccessResponse(EndAccessMessage message, PDPEvaluation evaluation) {
		EndAccessResponseMessage response = new EndAccessResponseMessage(message.getDestination(), message.getSource(),
				message.getMessageId());
		response.setEvaluation(evaluation);
		return response;
	}

	/**
	 * This is the function where the effective reevaluation takes place.
	 */
	public boolean reevaluateSessions(Attribute attribute) {
		try {
			log.severe("ReevaluateSessions for  attributeId : " + attribute.getAttributeId());
			List<SessionInterface> sessionList = getSessionListForCategory(attribute.getCategory(),
					attribute.getAttributeId(), attribute.getAdditionalInformations());
			if (sessionList != null) {
				for (SessionInterface session : sessionList) {
					reevaluate(session);
				}
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			log.severe("Error in Reevaluate sessions : " + e.getMessage());
		}
		return false;
	}

	public synchronized void reevaluate(SessionInterface session) throws PolicyException, RequestException {
		log.log(Level.SEVERE, "Reevaluation begins at {0}", System.currentTimeMillis());

		PolicyWrapper policy = PolicyWrapper.build(session.getPolicySet());
		RequestWrapper request = RequestWrapper.build(session.getOriginalRequest(), getPipRegistry());
		request.update();

		log.severe("Reevaluation policy: " + policy.getPolicy());
		log.severe("Reevaluation request: " + request.getRequest());
		request.fatten(false);
		log.severe("Reevaluation fatten request: " + request.getRequest());

		PDPEvaluation evaluation = getPdp().evaluate(request, policy, STATUS.START);
		Reject.ifNull(evaluation);
		log.severe("Reevaluation response:" + evaluation.getResponse());

		getObligationManager().translateObligations(evaluation, session.getId(), STATUS.END);

		log.log(Level.SEVERE, "Reevaluate evaluated at {0} pdp response : {1}",
				new Object[] { System.currentTimeMillis(), evaluation.getResult() });

		if (session.isStatus(STATUS.START.name()) && evaluation.isDecision(DecisionType.DENY)) {
			log.log(Level.SEVERE, "Revoke at {0}", System.currentTimeMillis());
			getSessionManager().updateEntry(session.getId(), STATUS.REVOKE.name());

		} else if (session.isStatus(STATUS.REVOKE.name()) && evaluation.isDecision(DecisionType.PERMIT)) {
			log.log(Level.SEVERE, "Resume at {0}", System.currentTimeMillis());
			getSessionManager().updateEntry(session.getId(), STATUS.START.name());
		} else {
			log.log(Level.SEVERE, "Reevaluation ends without change at {0}", System.currentTimeMillis());
			return;
		}

		ReevaluationResponseMessage response = buildReevaluationResponse(session, evaluation);
		getRequestManager().sendReevaluation(response);
		log.log(Level.SEVERE, "Reevaluation ends changing status at {0}", System.currentTimeMillis());
	}

	private ReevaluationResponseMessage buildReevaluationResponse(SessionInterface session, PDPEvaluation evaluation) {
		String[] destSplitted = session.getPepId().split(PEP_ID_SEPARATOR);
		ReevaluationResponseMessage response = new ReevaluationResponseMessage(uri.getHost(), destSplitted[0]);
		response.setPepId(destSplitted[destSplitted.length - 1]);
		response.setSessionId(session.getId());
		response.setEvaluation(evaluation);
		return response;
	}

	@Override
	public void attributeChanged(AttributeChangeMessage message) {
		log.log(Level.SEVERE, "Attribute changed received at {0}", System.currentTimeMillis());
		for (Attribute attribute : message.getAttributes()) {
			if (!reevaluateSessions(attribute)) {
				log.log(Level.SEVERE, "Error handling attribute changes");
			}
		}

	}

	@Override
	public String enrichRequest(String request) {
		RequestWrapper requestWrapper;
		try {
			requestWrapper = RequestWrapper.build(request, getPipRegistry());
			requestWrapper.fatten(false);
			return requestWrapper.getRequest();
		} catch (RequestException e) {
			e.printStackTrace();
		}

		return null;

	}

}
