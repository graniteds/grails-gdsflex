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

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.granite.hibernate.ProxyFactory;


/**
 * @author Franck WOLFF
 */
public class GrailsProxyFactory extends ProxyFactory {

    public GrailsProxyFactory(String initializerClassName) {
    	super(initializerClassName);
    }

    @Override
    protected Type getIdentifierType(Class<?> persistentClass) {

        Type type = identifierTypes.get(persistentClass);
        if (type != null)
            return type;

        for (Class<?> clazz = persistentClass; clazz != Object.class && clazz != null; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
            	if ("id".equals(field.getName())) {
                    type = field.getGenericType();
                    break;
                }
            }
        }

        if (type == null) {
            PropertyDescriptor[] propertyDescriptors = null;
            try {
                BeanInfo info = Introspector.getBeanInfo(persistentClass);
                propertyDescriptors = info.getPropertyDescriptors();
            } catch (Exception e) {
                throw new IllegalArgumentException("Could not find id in: " + persistentClass, e);
            }

            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                Method method = propertyDescriptor.getReadMethod();
                if (method != null && "id".equals(method.getName())) {
                    type = method.getGenericReturnType();
                    break;
                }
                method = propertyDescriptor.getWriteMethod();
                if (method != null && "id".equals(method.getName())) {
                    type = method.getGenericParameterTypes()[0];
                    break;
                }
            }
        }

        if (type != null) {
            Type previousType = identifierTypes.putIfAbsent(persistentClass, type);
            if (previousType != null)
                type = previousType; // should be the same...
            return type;
        }

        throw new IllegalArgumentException("Could not find id in: " + persistentClass);
    }
}
