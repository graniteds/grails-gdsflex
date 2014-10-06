/*
  GRANITE DATA SERVICES
  Copyright (C) 2011 GRANITE DATA SERVICES S.A.S.

  This file is part of Granite Data Services.

  Granite Data Services is free software; you can redistribute it and/or modify
  it under the terms of the GNU Library General Public License as published by
  the Free Software Foundation; either version 2 of the License, or (at your
  option) any later version.

  Granite Data Services is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE. See the GNU Library General Public License
  for more details.

  You should have received a copy of the GNU Library General Public License
  along with this library; if not, see <http://www.gnu.org/licenses/>.
*/

package org.granite.grails.integration;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.granite.hibernate4.ProxyFactory;


/**
 * @author Franck WOLFF
 */
public class GrailsProxyFactory extends ProxyFactory {

    public GrailsProxyFactory(String initializerClassName) {
    	super(initializerClassName);
    }

    @Override
    protected Object[] getIdentifierInfo(Class<?> persistentClass) {

        Object[] infos = identifierInfos.get(persistentClass);
        if (infos != null)
            return infos;
        
        Type type = null;
        Method getter = null;
        
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
            	getter = method;
                type = method.getGenericReturnType();
                break;
            }
        }

        if (type != null) {
            Object[] previousInfos = identifierInfos.putIfAbsent(persistentClass, new Object[] { type, getter });
            if (previousInfos != null)
                infos = previousInfos; // should be the same...
            return infos;
        }

        throw new IllegalArgumentException("Could not find id in: " + persistentClass);
    }
}
