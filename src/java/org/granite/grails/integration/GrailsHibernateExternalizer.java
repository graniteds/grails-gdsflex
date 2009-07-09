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

import java.io.IOException;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.List;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.granite.context.GraniteContext;
import org.granite.hibernate.HibernateExternalizer;
import org.granite.messaging.amf.io.util.ClassGetter;
import org.granite.messaging.amf.io.util.Property;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;

/**
 * @author William Draï
 */
public class GrailsHibernateExternalizer extends HibernateExternalizer {
	
	private GrailsApplication grailsApplication;
    

	GrailsHibernateExternalizer(GrailsApplication grailsApplication) {
		this.grailsApplication = grailsApplication;
	}
    
	@Override
	protected boolean isRegularEntity(Class<?> clazz) {
		return grailsApplication.isArtefactOfType("Domain", clazz);
	}

    @Override
    public void writeExternal(Object o, ObjectOutput out) throws IOException, IllegalAccessException {

        ClassGetter classGetter = GraniteContext.getCurrentInstance().getGraniteConfig().getClassGetter();
        Class<?> oClass = classGetter.getClass(o);

        if (o instanceof HibernateProxy) {        	
            HibernateProxy proxy = (HibernateProxy)o;

            // Only write initialized flag & detachedState & entity id if proxy is uninitialized.
            if (proxy.getHibernateLazyInitializer().isUninitialized()) {
            	String detachedState = getProxyDetachedState(proxy);
            	Serializable id = proxy.getHibernateLazyInitializer().getIdentifier();
            	
            	// Write initialized flag.
            	out.writeObject(Boolean.FALSE);
            	// Write detachedState.
            	out.writeObject(detachedState);
            	// Write entity id.
                out.writeObject(id);
                return;
            }

            // Proxy is initialized, get the underlying persistent object.
            o = proxy.getHibernateLazyInitializer().getImplementation();
        }

        if (!isRegularEntity(o.getClass())) { // @Embeddable or others...
            super.writeExternal(o, out);
        }
        else {
            // Write initialized flag.
            out.writeObject(Boolean.TRUE);
            // Write detachedState.
            out.writeObject(null);

            // Externalize entity fields.
            List<Property> fields = findOrderedFields(oClass, false);
            for (Property field : fields) {
                Object value = field.getProperty(o);
                if (value instanceof PersistentCollection)
                	value = newExternalizableCollection((PersistentCollection)value);
                if (!(value instanceof Closure))
                	out.writeObject(value);
                else
                	out.writeObject(null);
            }
        }
    }
}
