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
package wd_17;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

@Root
public class AdviceExpressionType {

    @ElementList( name = "AttributeAssignmentExpression", required = false)
    protected List<AttributeAssignmentExpressionType> attributeAssignmentExpression;
    @Attribute( name = "AdviceId", required = true )
    protected String adviceId;
    @Attribute( name = "AppliesTo", required = true )
    protected EffectType appliesTo;

    public List<AttributeAssignmentExpressionType> getAttributeAssignmentExpression() {
        if( attributeAssignmentExpression == null ) {
            attributeAssignmentExpression = new ArrayList<>();
        }
        return this.attributeAssignmentExpression;
    }

    public String getAdviceId() {
        return adviceId;
    }

    public void setAdviceId( String value ) {
        this.adviceId = value;
    }

    public EffectType getAppliesTo() {
        return appliesTo;
    }

    public void setAppliesTo( EffectType value ) {
        this.appliesTo = value;
    }

}
