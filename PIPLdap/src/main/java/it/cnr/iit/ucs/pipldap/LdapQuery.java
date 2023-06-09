package it.cnr.iit.ucs.pipldap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;

import it.cnr.iit.ucs.pipldap.statics.LdapAuthorization;
import it.cnr.iit.utility.errorhandling.Reject;

public final class LdapQuery {

	private static String url;
	private static Integer port;
	private static String password;
	private static String bnddn;

	private static String dc = new String();
	private static String cn = new String();
	private static String ou = new String();

	private static LdapConnection connection;

	public static final String NOT_FOUND = "not found";

	private static Logger log = Logger.getLogger(LdapQuery.class.getName());

	private LdapQuery() {
	}

	static public void init() {
		url = LdapAuthorization.getHost();
		port = Integer.parseInt(LdapAuthorization.getPort());
		password = LdapAuthorization.getPassword();
		bnddn = LdapAuthorization.getBnddn();

		setDc(bnddn);
		setCn(bnddn);
		setOu(bnddn);

		connection = new LdapNetworkConnection(url, port);
		try {
			connection.bind(bnddn, password);
		} catch (LdapException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static Map<String, String> getAttributesByUsername(String username, String baseDn, String filter,
			String... attributes) {

		List<String> attrsList = Arrays.asList(attributes);
		Map<String, String> attrAndValue = new HashMap<>();

		try {
			SearchRequest req = new SearchRequestImpl();
			req.setScope(SearchScope.SUBTREE);
			req.addAttributes("*");
			req.setTimeLimit(0);
			req.setBase(new Dn(baseDn));
			req.setFilter(filter);

			SearchCursor searchCursor = connection.search(req);

			while (searchCursor.next()) {
				Response response = searchCursor.get();
				log.severe("in while in LdapQuery");

				if (response instanceof SearchResultEntry) {
					Entry resultEntry = ((SearchResultEntry) response).getEntry();
					log.severe("in first if in LdapQuery");

					String uid = resultEntry.getAttributes().stream().filter(el -> el.getId().equals("uid"))
							.map(el -> el.get().getString()).findFirst().orElse(NOT_FOUND);

					log.severe("uid in LdapQuery: " + uid);

					if (uid.equals(username)) {
						log.severe("printing attibutes: ");
						resultEntry.getAttributes().stream().forEach(el -> log.severe(el.getId()));

						for (String attr : attrsList) {

							String value = resultEntry.getAttributes().stream().filter(el -> el.getId().equals(attr))
									.map(el -> el.get().getString()).findFirst().orElse(NOT_FOUND);
							log.severe("value in for of LdapQuery=" + value);
							attrAndValue.put(attr, value);
						}
						break;
					}
				}
			}

			searchCursor.close();
			connection.close();

			attrAndValue.entrySet().stream().forEach(el -> log.severe(
					"attribute for user " + username + ": key -> " + el.getKey() + ", value -> " + el.getValue()));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return attrAndValue;

	}

	public static String queryForMemberOf(String baseDn, String filter, SearchScope level, String... attributes) {
		Reject.ifBlank(filter);

		try {

			SearchRequest req = new SearchRequestImpl();
			req.setScope(level);
			req.addAttributes(attributes);
			req.setTimeLimit(0);
			req.setBase(new Dn(baseDn));
			req.setFilter(filter);

			// Process the request
			SearchCursor searchCursor = connection.search(req);

			while (searchCursor.next()) {
				Response response = searchCursor.get();
				if (response instanceof SearchResultEntry) {
					Entry resultEntry = ((SearchResultEntry) response).getEntry();
					if (resultEntry.containsAttribute(attributes)) {
//						String memberOf = resultEntry.getAttributes().stream().findFirst().get().getString();
						String memberOf = resultEntry.getAttributes().stream().findFirst().get().getString();
						log.severe("role found is " + memberOf + " inside queryForMemberOf");
						return memberOf;
					}
				}
			}
			searchCursor.close();
			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return NOT_FOUND;
	}

	private static String setDc(String searchString) {
		Reject.ifBlank(searchString);

		List<String> dcElements = Arrays.asList(searchString.split("dc"));
		String dc = "dc" + dcElements.stream().filter(el -> !el.startsWith("ou=") && !el.startsWith("cn="))
				.collect(Collectors.joining("dc"));
		log.severe("setDc() -> " + dc);
		return dc;
	}

	private static String setCn(String searchString) {
		Reject.ifBlank(searchString);

		if (!searchString.contains("cn=")) {
			return "";
		}
		String cn = "cn" + searchString.split("cn")[1].split(",")[0];
		log.severe("setCn() -> " + cn);
		return cn;
	}

	private static String setOu(String searchString) {
		Reject.ifBlank(searchString);
		String ou = searchString.split(cn + ",")[1].split("," + dc)[0];
		log.severe("setOu() -> " + ou);
		return ou;
	}

}
