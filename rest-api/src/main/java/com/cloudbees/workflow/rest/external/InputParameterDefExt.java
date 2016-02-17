/*
 * The MIT License
 *
 * Copyright (c) 2015, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.cloudbees.workflow.rest.external;

import com.fasterxml.jackson.annotation.JsonInclude;
import hudson.model.BooleanParameterDefinition;
import hudson.model.ChoiceParameterDefinition;
import hudson.model.ParameterDefinition;
import hudson.model.PasswordParameterDefinition;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Input Parameter Definition.
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class InputParameterDefExt {

    private final String type;
    private final String name;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String description;
    private final Map<String, Object> definition;

    public InputParameterDefExt(@Nonnull ParameterDefinition definition) {
        this.type = definition.getType();
        this.name = definition.getName();
        this.description = definition.getDescription();
        this.definition = toDefinitionMap(definition);
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getDefinition() {
        return definition;
    }

    static Map<String, Object> toDefinitionMap(@Nonnull ParameterDefinition definition) {
        Map<String, Object> definitionMap = new HashMap<String, Object>();

        if (definition instanceof BooleanParameterDefinition) {
            definitionMap.put("defaultVal", ((BooleanParameterDefinition) definition).isDefaultValue());
        } else if (definition instanceof StringParameterDefinition) {
            definitionMap.put("defaultVal", ((StringParameterDefinition) definition).getDefaultValue());
        } else if (definition instanceof PasswordParameterDefinition) {
            definitionMap.put("defaultVal", ((PasswordParameterDefinition) definition).getDefaultValue());
        } else if (definition instanceof ChoiceParameterDefinition) {
            StringParameterValue defaultParameterValue = ((ChoiceParameterDefinition) definition).getDefaultParameterValue();
            if (defaultParameterValue != null) {
                definitionMap.put("defaultVal", defaultParameterValue.getValue());
            }
            definitionMap.put("choices", ((ChoiceParameterDefinition) definition).getChoices());
        }
        
        return definitionMap;
    }
}
