/*
  GRANITE DATA SERVICES
  Copyright (C) 2007-2008 ADEQUATE SYSTEMS SARL

  This file is part of Granite Data Services.

  Granite Data Services is free software; you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation; either version 3 of the License, or (at your
  option) any later version.

  Granite Data Services is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
  for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this library; if not, see <http://www.gnu.org/licenses/>.
*/

package org.granite.grails.integration;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.granite.context.GraniteContext;
import org.granite.datanucleus.DataNucleusExternalizer;
import org.granite.messaging.amf.io.convert.Converters;
import org.granite.messaging.amf.io.util.FieldProperty;
import org.granite.messaging.amf.io.util.MethodProperty;
import org.granite.messaging.amf.io.util.Property;
import org.granite.messaging.amf.io.util.externalizer.annotation.ExternalizedProperty;
import org.granite.messaging.amf.io.util.externalizer.annotation.IgnoredProperty;
import org.granite.util.ClassUtil;

/**
 * @author William Draï
 */
public class GrailsDataNucleusExternalizer extends DataNucleusExternalizer {
	
	private GrailsApplication grailsApplication;
    

	GrailsDataNucleusExternalizer(GrailsApplication grailsApplication) {
		this.grailsApplication = grailsApplication;
	}
    
    
	@Override
	protected boolean isRegularEntity(Class<?> clazz) {
		return grailsApplication.isArtefactOfType("Domain", clazz);
	}

	@Override
	protected boolean isValueIgnored(Object value) {
		return value instanceof Closure;
	}

    
    private boolean isRoot(Class<?> clazz) {
        return clazz.getSuperclass() != null && 
        	(clazz.getSuperclass().equals(GroovyObject.class) ||
            clazz.getSuperclass().equals(Object.class) ||
            Modifier.isAbstract(clazz.getSuperclass().getModifiers()));
    }
    
    @Override
    public List<Property> findOrderedFields(final Class<?> clazz, boolean returnSettersWhenAvailable) {
        List<Property> fields = !dynamicClass ? (returnSettersWhenAvailable ? orderedSetterFields.get(clazz) : orderedFields.get(clazz)) : null;

        if (fields == null) {
        	if (dynamicClass)
        		Introspector.flushFromCaches(clazz);
            PropertyDescriptor[] propertyDescriptors = ClassUtil.getProperties(clazz);
            Converters converters = GraniteContext.getCurrentInstance().getGraniteConfig().getConverters();

            fields = new ArrayList<Property>();
            
            List<Property> idVersionFields = new ArrayList<Property>();

            Set<String> allFieldNames = new HashSet<String>();
            for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {

                List<Property> newFields = new ArrayList<Property>();

                // Standard declared fields.
                for (Field field : c.getDeclaredFields()) {
                    if (!allFieldNames.contains(field.getName()) &&
                        !Modifier.isTransient(field.getModifiers()) &&
                        !Modifier.isStatic(field.getModifiers()) &&
                        !field.isAnnotationPresent(IgnoredProperty.class) &&
                        !GrailsExternalizer.EVENTS.contains(field.getName())) {

                    	boolean found = false;
                    	if (returnSettersWhenAvailable && propertyDescriptors != null) {
                    		for (PropertyDescriptor pd : propertyDescriptors) {
                    			if (pd.getName().equals(field.getName()) && pd.getWriteMethod() != null) {
                        			if ("id".equals(field.getName()) || "version".equals(field.getName())) { 
                        				if (c == clazz)
                            				idVersionFields.add(new MethodProperty(converters, field.getName(), pd.getWriteMethod(), pd.getReadMethod()));
                        			}
                        			else
                        				newFields.add(new MethodProperty(converters, field.getName(), pd.getWriteMethod(), pd.getReadMethod()));
                    				found = true;
                    				break;
                    			}
                    		}
                    	}
                		if (!found) {
                			if ("id".equals(field.getName()) || "version".equals(field.getName())) { 
                				if (c == clazz)
                					idVersionFields.add(new FieldProperty(converters, field));
                			}
                			else
                				newFields.add(new FieldProperty(converters, field));
                		}
                    }
                    allFieldNames.add(field.getName());
                }

                // Getter annotated  by @ExternalizedProperty.
                if (propertyDescriptors != null) {
                    for (PropertyDescriptor property : propertyDescriptors) {
                        Method getter = property.getReadMethod();
                        if (getter != null &&
                            getter.isAnnotationPresent(ExternalizedProperty.class) &&
                            getter.getDeclaringClass().equals(c) &&
                            !allFieldNames.contains(property.getName()) &&
                            !GrailsExternalizer.EVENTS.contains(property.getName())) {

                            newFields.add(new MethodProperty(
                                converters,
                                property.getName(),
                                null,
                                getter
                            ));
                            allFieldNames.add(property.getName());
                        }
                    }
                }

                if (isRoot(c))
                	newFields.addAll(idVersionFields);
                
                Collections.sort(newFields, new Comparator<Property>() {
                    public int compare(Property o1, Property o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });

                fields.addAll(0, newFields);
            }

            if (!dynamicClass) {
	            List<Property> previousFields = (returnSettersWhenAvailable ? orderedSetterFields : orderedFields).putIfAbsent(clazz, fields);
	            if (previousFields != null)
	                fields = previousFields;
            }
        }

        return fields;
    }
}
