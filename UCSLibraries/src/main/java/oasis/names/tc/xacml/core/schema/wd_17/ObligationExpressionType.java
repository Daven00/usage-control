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
//
// Questo file è stato generato dall'architettura JavaTM per XML Binding (JAXB)
// Reference Implementation, v2.2.8-b130911.1802
// Vedere <a
// href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Qualsiasi modifica a questo file andrà persa durante la ricompilazione dello
// schema di origine.
// Generato il: 2017.04.24 alle 12:34:54 PM CEST
//

package oasis.names.tc.xacml.core.schema.wd_17;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Classe Java per ObligationExpressionType complex type.
 *
 * <p>
 * Il seguente frammento di schema specifica il contenuto previsto contenuto in
 * questa classe.
 *
 * <pre>
 * &lt;complexType name="ObligationExpressionType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:xacml:3.0:core:schema:wd-17}AttributeAssignmentExpression" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="ObligationId" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="FulfillOn" use="required" type="{urn:oasis:names:tc:xacml:3.0:core:schema:wd-17}EffectType" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType( XmlAccessType.FIELD )
@XmlType( name = "ObligationExpressionType" )
public class ObligationExpressionType {

    @XmlElement( name = "AttributeAssignmentExpression" )
    protected List<AttributeAssignmentExpressionType> attributeAssignmentExpression;
    @XmlAttribute( name = "ObligationId", required = true )
    // @XmlSchemaType(name = "anyURI")
    protected String obligationId;
    @XmlAttribute( name = "FulfillOn", required = true )
    protected EffectType fulfillOn;

    /**
     * Gets the value of the attributeAssignmentExpression property.
     * 
     * <p>
     * This accessor method returns a reference to the live list, not a snapshot.
     * Therefore any modification you make to the returned list will be present
     * inside the JAXB object. This is why there is not a <CODE>set</CODE> method
     * for the attributeAssignmentExpression property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * 
     * <pre>
     * getAttributeAssignmentExpression().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AttributeAssignmentExpressionType }
     * 
     * 
     */
    public List<AttributeAssignmentExpressionType> getAttributeAssignmentExpression() {
        if( attributeAssignmentExpression == null ) {
            attributeAssignmentExpression = new ArrayList<>();
        }
        return this.attributeAssignmentExpression;
    }

    /**
     * Recupera il valore della proprietà obligationId.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getObligationId() {
        return obligationId;
    }

    /**
     * Imposta il valore della proprietà obligationId.
     * 
     * @param value
     *          allowed object is {@link String }
     * 
     */
    public void setObligationId( String value ) {
        this.obligationId = value;
    }

    /**
     * Recupera il valore della proprietà fulfillOn.
     * 
     * @return possible object is {@link EffectType }
     * 
     */
    public EffectType getFulfillOn() {
        return fulfillOn;
    }

    /**
     * Imposta il valore della proprietà fulfillOn.
     * 
     * @param value
     *          allowed object is {@link EffectType }
     * 
     */
    public void setFulfillOn( EffectType value ) {
        this.fulfillOn = value;
    }

}
