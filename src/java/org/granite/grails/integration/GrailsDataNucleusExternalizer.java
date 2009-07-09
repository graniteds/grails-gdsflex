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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jdo.spi.Detachable;
import javax.jdo.spi.PersistenceCapable;
import javax.jdo.spi.StateManager;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.granite.context.GraniteContext;
import org.granite.datanucleus.DataNucleusExternalizer;
import org.granite.messaging.amf.io.util.ClassGetter;
import org.granite.messaging.amf.io.util.Property;
import org.granite.messaging.persistence.ExternalizablePersistentList;
import org.granite.messaging.persistence.ExternalizablePersistentMap;
import org.granite.messaging.persistence.ExternalizablePersistentSet;
import org.granite.util.ClassUtil;
import org.granite.util.StringUtil;

/**
 * @author William Draï
 */
public class GrailsDataNucleusExternalizer extends DataNucleusExternalizer {
	
	private static final Integer NULL_ID = Integer.valueOf(0);
	
	private GrailsApplication grailsApplication;
    

	GrailsDataNucleusExternalizer(GrailsApplication grailsApplication) {
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

        if (!isRegularEntity(o.getClass())) { // @Embeddable or others...
            super.writeExternal(o, out);
        }
        else {
        	Detachable pco = (Detachable)o;
        	preSerialize((PersistenceCapable)pco);
        	Object[] detachedState = getDetachedState(pco);
        	
        	// Pseudo-proxy created for uninitialized entities (see below).
        	if (detachedState != null && detachedState[0] == NULL_ID) {
            	// Write initialized flag.
            	out.writeObject(Boolean.FALSE);
            	// Write detached state.
        		out.writeObject(null);
        		// Write id.
        		out.writeObject(null);
        		return;
        	}

        	// Write initialized flag.
        	out.writeObject(Boolean.TRUE);
        	
        	if (detachedState != null) {
            	// Write detached state as a String, in the form of an hex representation
            	// of the serialized detached state.
    	        org.granite.util.Entity entity = new org.granite.util.Entity(pco);
    	        Object version = entity.getVersion();
    	        if (version != null)
    	        	detachedState[1] = version;
	        	byte[] binDetachedState = serializeDetachedState(detachedState);
	        	char[] hexDetachedState = StringUtil.bytesToHexChars(binDetachedState);
	            out.writeObject(new String(hexDetachedState));
        	}
        	else
        		out.writeObject(null);

            // Externalize entity fields.
            List<Property> fields = findOrderedFields(oClass);
        	Map<String, Boolean> loadedState = getLoadedState(detachedState, oClass);
            for (Property field : fields) {
                if (field.getName().equals("jdoDetachedState"))
                	continue;
                
                Object value = field.getProperty(o);
                if (value instanceof Closure) {
                	out.writeObject(null);
                	continue;
                }
                
                // Uninitialized associations.
                if (loadedState.containsKey(field.getName()) && !loadedState.get(field.getName())) {
            		Class<?> fieldClass = ClassUtil.classOfType(field.getType());
        			
            		// Create a "pseudo-proxy" for uninitialized entities: detached state is set to "0" (uninitialized flag).
            		if (Detachable.class.isAssignableFrom(fieldClass)) {
            			try {
            				value = fieldClass.newInstance();
            			} catch (Exception e) {
	                		throw new RuntimeException("Could not create DataNucleus pseudo-proxy for: " + field, e);
	                	}
            			setDetachedState((Detachable)value, new Object[] { NULL_ID, null, null, null });
            		}
            		// Create pseudo-proxy for collections (set or list).
            		else if (Collection.class.isAssignableFrom(fieldClass)) {
            			if (Set.class.isAssignableFrom(fieldClass))
            				value = new ExternalizablePersistentSet((Set<?>)null, false, false);
            			else
            				value = new ExternalizablePersistentList((List<?>)null, false, false);
            		}
            		// Create pseudo-proxy for maps.
            		else if (Map.class.isAssignableFrom(fieldClass)) {
            			value = new ExternalizablePersistentMap((Map<?, ?>)null, false, false);
            		}
                }
                
                // Initialized collections.
                else if (value instanceof Set) {
            		value = new ExternalizablePersistentSet(((Set<?>)value).toArray(), true, false);
                }
            	else if (value instanceof List) {
            		value = new ExternalizablePersistentList(((List<?>)value).toArray(), true, false);
            	}
                else if (value instanceof Map) {
                	value = new ExternalizablePersistentMap((Map<?, ?>)null, true, false);
                	((ExternalizablePersistentMap)value).setContentFromMap((Map<?, ?>)value);
                }
                out.writeObject(value);
            }
        }
    }
    
    private static void preSerialize(PersistenceCapable o) {
    	try {
    		Class<?> baseClass = o.getClass();
    		while (baseClass.getSuperclass() != Object.class &&
    			   baseClass.getSuperclass() != null &&
    			   PersistenceCapable.class.isAssignableFrom(baseClass.getSuperclass())) {
    			baseClass = baseClass.getSuperclass();
    		}
	    	Field f = baseClass.getDeclaredField("jdoStateManager");
	    	f.setAccessible(true);
	    	StateManager sm = (StateManager)f.get(o);
	    	if (sm != null) {
	    		setDetachedState((Detachable)o, null);
	    		sm.preSerialize(o);
	    	}
    	}
    	catch (Exception e) {
    		throw new RuntimeException("Cannot access jdoDetachedState for detached object", e);
    	}
    }
    
    private static Object[] getDetachedState(javax.jdo.spi.Detachable o) {
    	try {
    		Class<?> baseClass = o.getClass();
    		while (baseClass.getSuperclass() != Object.class && baseClass.getSuperclass() != null && PersistenceCapable.class.isAssignableFrom(baseClass.getSuperclass()))
    			baseClass = baseClass.getSuperclass();
	    	Field f = baseClass.getDeclaredField("jdoDetachedState");
	    	f.setAccessible(true);
	    	return (Object[])f.get(o);
    	}
    	catch (Exception e) {
    		throw new RuntimeException("Cannot access jdoDetachedState for detached object", e);
    	}
    }
    
    private static void setDetachedState(javax.jdo.spi.Detachable o, Object[] detachedState) {
    	try {
    		Class<?> baseClass = o.getClass();
    		while (baseClass.getSuperclass() != Object.class && baseClass.getSuperclass() != null && PersistenceCapable.class.isAssignableFrom(baseClass.getSuperclass()))
    			baseClass = baseClass.getSuperclass();
	    	Field f = baseClass.getDeclaredField("jdoDetachedState");
	    	f.setAccessible(true);
	    	f.set(o, detachedState);
    	}
    	catch (Exception e) {
    		throw new RuntimeException("Cannot access jdoDetachedState for detached object", e);
    	}
    }
    
    static Map<String, Boolean> getLoadedState(Detachable pc, Class<?> clazz) {
    	return getLoadedState(getDetachedState(pc), clazz);    	
    }
    
    static Map<String, Boolean> getLoadedState(Object[] detachedState, Class<?> clazz) {
    	try {
    		BitSet loaded = detachedState != null ? (BitSet)detachedState[2] : null;
	    	
	        List<String> fieldNames = new ArrayList<String>();
	    	for (Class<?> c = clazz; c != null && PersistenceCapable.class.isAssignableFrom(c); c = c.getSuperclass()) { 
	        	Field pcFieldNames = c.getDeclaredField("jdoFieldNames");
	        	pcFieldNames.setAccessible(true);
	        	fieldNames.addAll(0, Arrays.asList((String[])pcFieldNames.get(null)));
	    	}
	        
	    	Map<String, Boolean> loadedState = new HashMap<String, Boolean>();
	    	for (int i = 0; i < fieldNames.size(); i++)
	    		loadedState.put(fieldNames.get(i), (loaded != null && loaded.size() > i ? loaded.get(i) : true));
	    	return loadedState;
    	}
    	catch (Exception e) {
    		throw new RuntimeException("Could not get loaded state for: " + detachedState);
    	}
    }
}
