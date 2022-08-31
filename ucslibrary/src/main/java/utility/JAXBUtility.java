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
package utility;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

/**
 * This is the class devoted to store the utility required to deal with the
 * JAXB.
 *
 * @author antonio
 *
 */
public final class JAXBUtility {

    private JAXBUtility() {

    }

    // constant that represents the schema we're using
    public static final String SCHEMA = "urn:oasis:names:tc:xacml:3.0:core:schema:wd-17";

    /**
     * Takes an object which skeleton has been provided by the xjc utility and
     * marshals it into a string that represents the xml
     *
     * @param clazz
     *          the class to which the object belongs
     * @param object
     *          the object itself
     * @param name
     *          the name of the class of the object
     * @param schema
     *          the schema to be used, it can be null
     * @return a String that represents the xml of the object
     * @throws JAXBException
     */
    public static final <T> String marshalToString( Class<T> clazz, T object,
            String name, String schema ) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance( clazz );
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );

        QName qName = new QName( schema, name );
        JAXBElement<T> elem = new JAXBElement<>( qName, clazz, null, object );

        StringWriter stringWriter = new StringWriter();
        jaxbMarshaller.marshal( elem, stringWriter );

        return stringWriter.getBuffer().toString();
    }

    /**
     * Takes a String that represents the content of an xml and converts it into
     * one of the objects provided by the xjc tool.
     *
     * @param clazz
     *          the class to which the object belongs
     * @param xmlString
     *          the xml in string format
     * @return the object built up after unmarshalling, null otherwise
     * @throws JAXBException
     */
    public static final <T> T unmarshalToObject( Class<T> clazz, String xmlString )
            throws JAXBException {

//        System.out.println("CLAZZ: " + clazz.getName());
//        System.out.println("XMLSTRING: " + xmlString);

        JAXBContext jaxbContext = JAXBContext.newInstance( clazz );
//        System.out.println("JAXBCONTEXT created");
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
//        System.out.println("Unmarshaller created");
        Source stream = new StreamSource(
            new ByteArrayInputStream( xmlString.getBytes() ) );
        JAXBElement<T> element = jaxbUnmarshaller.unmarshal( stream, clazz );

        return element.getValue();
    }

}
