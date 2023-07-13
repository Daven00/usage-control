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
package it.cnr.iit.ucs.pap;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import it.cnr.iit.ucs.exceptions.PAPException;
import it.cnr.iit.ucs.exceptions.PolicyException;
import it.cnr.iit.ucs.properties.components.PapProperties;
import it.cnr.iit.utility.FileUtility;
import it.cnr.iit.utility.errorhandling.Reject;
import it.cnr.iit.xacml.wrappers.PolicyWrapper;

import oasis.names.tc.xacml.core.schema.wd_17.PolicyType;

/**
 * The PAP is a storage of policies.
 * All the policies are stored inside a folder, where the name of the file corresponds
 * to the policy id.
 *
 * @author Antonio La Marra, Alessandro Rosetti
 *
 */
public class PolicyAdministrationPoint implements PAPInterface {

    private Logger log = Logger.getLogger( PolicyAdministrationPoint.class.getName() );

    private PapProperties properties;

    private static final String POLICY_FILE_EXTENSION = ".xml";

    public PolicyAdministrationPoint( PapProperties properties ) {
        Reject.ifNull( properties );
        this.properties = properties;
    }


    /**
     * Deletes the policy that has as id the policyId passed as parameter
     *
     * @param policyId the policyId to be used
     * @return true if deletion is successful (and the file does not exist anymore).
     * False, otherwise.
     */
    public boolean deletePolicy(String policyId) {
        String policyFile = properties.getPath() + File.separator + policyId + POLICY_FILE_EXTENSION;
        File pFile = new File(policyFile);
        return (pFile.delete() && !pFile.exists());
    }

    /**
     * Retrieves the policy that has as id the policyId passed as parameter
     *
     * @param policyId the policyId to be used
     * @return the policy in string format
     */
    @Override
    public String retrievePolicy( String policyId ) {
        Reject.ifBlank( policyId );
        Path path = getPolicyPath( policyId );
        try {
            return new String( Files.readAllBytes( path ) );
        } catch( Exception e ) {
            log.log( Level.SEVERE, "Error reading policy file : {0} -> {1}", new Object[] { path, e.getMessage() } );
        }
        return null;
    }

    /**
     * Adds a new policy
     *
     * @param policy the policy to be added
     * @return the policyId if everything goes OK, null otherwise
     */
    @Override
    public String addPolicy( String policy ) {
        Reject.ifBlank( policy );
        PolicyWrapper policyWrapper;
        try {
            policyWrapper = PolicyWrapper.build( policy );
        } catch( PolicyException e ) {
            return null;
        }

        PolicyType policyType = policyWrapper.getPolicyType();
        String id = policyType.getPolicyId();
        if( id == null || id.isEmpty() ) {
            return null;
        }

        Path policyPath = getPolicyPath( id );
        if( policyPath.toFile().exists() ) {
            log.log( Level.INFO, "Updating policy {0}", id );
        }
        if( writePolicy( policyPath, policy ) ) {
            return id;
        }
        return null;
    }

    private boolean writePolicy( Path path, String policy ) {
        // TODO UCS-33 NOSONAR
        try (FileOutputStream fos = new FileOutputStream( path.toString() )) {
            fos.write( policy.getBytes() );
            return true;
        } catch( Exception e ) {
            log.log( Level.SEVERE, "Error writing policy file : {0} -> {1}", new Object[] { path, e.getMessage() } );
            return false;
        }
    }

    private Path getPolicyPath( String policyId ) {
        // TODO UCS-33 NOSONAR
        return Paths.get( properties.getPath(), policyId + POLICY_FILE_EXTENSION );
    }

    /**
     * Retrieves the IDs of the policy stored
     *
     * @return the list of the IDs of the policies stored
     */
    @Override
    public List<String> listPolicies() throws PAPException {
        // TODO UCS-33 NOSONAR
        File directory = new File( properties.getPath() );
        File[] files = directory.listFiles( ( dir, name ) -> name.toLowerCase().endsWith( POLICY_FILE_EXTENSION ) );
        if (files != null) {
            return Arrays.asList( files ).parallelStream()
                .map( File::getName )
                .map( FileUtility::stripExtension )
                .collect( Collectors.toList() );
        } else {
            throw new PAPException("Error retrieving the list of policies");
        }
    }

    @Override
    public String getPath() {
        return properties.getPath();
    }

}
