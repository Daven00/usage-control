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
package it.cnr.iit.ucsrest.rest;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import it.cnr.iit.ucs.constants.OperationName;
import it.cnr.iit.ucs.core.UCSCoreServiceBuilder;
import it.cnr.iit.ucs.message.endaccess.EndAccessMessage;
import it.cnr.iit.ucs.message.endaccess.EndAccessResponseMessage;
import it.cnr.iit.ucs.message.startaccess.StartAccessMessage;
import it.cnr.iit.ucs.message.startaccess.StartAccessResponseMessage;
import it.cnr.iit.ucs.message.tryaccess.TryAccessMessage;
import it.cnr.iit.ucs.message.tryaccess.TryAccessResponseMessage;
import it.cnr.iit.ucs.properties.UCSProperties;
import it.cnr.iit.ucs.ucs.UCSInterface;
import it.cnr.iit.utility.errorhandling.Reject;

/**
 * This class includes all the interfaces we will offer via rest.
 *
 * @author Antonio La Marra, Alessandro Rosetti
 */
@ApiModel(value = "UCS", description = "Usage Control System enforcement engine REST API")
@RestController
@RequestMapping("/")
public class UCSRestController {
	private static final Logger log = Logger.getLogger(UCSRestController.class.getName());

	@Autowired
	private UCSProperties properties;

	private UCSInterface ucs;

	@PostConstruct
	private void init() {
		ucs = new UCSCoreServiceBuilder().setProperties(properties).build();
	}

	@ApiOperation(httpMethod = "POST", value = "Receives request from PEP for tryaccess operation")
	@ApiResponses(value = { @ApiResponse(code = 500, message = "Invalid message received"),
			@ApiResponse(code = 200, message = "OK") })
	@PostMapping(value = OperationName.TRYACCESS_REST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	// TODO UCS-34 NOSONAR
	public TryAccessResponseMessage sendMessage(@RequestBody() TryAccessMessage message) {
		Reject.ifNull(message);
		log.log(Level.INFO, "TryAccess received {0}", System.currentTimeMillis());
		return (TryAccessResponseMessage) ucs.tryAccess(message);
	}

	@ApiOperation(httpMethod = "POST", value = "Receives request from PEP for startaccess operation")
	@ApiResponses(value = { @ApiResponse(code = 500, message = "Invalid message received"),
			@ApiResponse(code = 200, message = "OK") })
	@PostMapping(value = OperationName.STARTACCESS_REST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	// TODO UCS-34 NOSONAR
	public StartAccessResponseMessage sendMessage(@RequestBody() StartAccessMessage message) {
		Reject.ifNull(message);
		log.log(Level.INFO, "StartAccess received {0}", System.currentTimeMillis());
		return (StartAccessResponseMessage) ucs.startAccess(message);
	}

	@ApiOperation(httpMethod = "POST", value = "Receives request from PEP for endaccess operation")
	@ApiResponses(value = { @ApiResponse(code = 500, message = "Invalid message received"),
			@ApiResponse(code = 200, message = "OK") })
	@PostMapping(value = OperationName.ENDACCESS_REST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	// TODO UCS-34 NOSONAR
	public EndAccessResponseMessage sendMessage(@RequestBody() EndAccessMessage message) {
		Reject.ifNull(message);
		log.log(Level.INFO, "EndAccess received {0}", System.currentTimeMillis());
		return (EndAccessResponseMessage) ucs.endAccess(message);
	}

}
