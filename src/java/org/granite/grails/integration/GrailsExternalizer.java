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

import groovy.lang.Closure;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.granite.messaging.amf.io.util.externalizer.DefaultExternalizer;
import org.granite.messaging.amf.io.util.externalizer.EnumExternalizer;
import org.granite.messaging.amf.io.util.externalizer.Externalizer;
import org.granite.util.PropertyDescriptor;
import org.granite.util.TypeUtil;
import org.granite.util.XMap;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author William Draï
 */
public class GrailsExternalizer extends DefaultExternalizer implements ApplicationContextAware {

	private GrailsApplication grailsApplication;
	private Externalizer delegate;
	private EnumExternalizer enumExternalizer;

	public static final Set<String> EVENTS = new HashSet<String>();
	static {
		for (String name : Arrays.asList("onLoad", "onSave", "beforeLoad", "beforeInsert", "afterInsert",
				"beforeUpdate", "afterUpdate", "beforeDelete", "afterDelete", "afterLoad")) {
			EVENTS.add(name);
		}
	};

	public static final String ERRORS = "org.springframework.validation.Errors";

	public void setApplicationContext(ApplicationContext springContext) {
		grailsApplication = (GrailsApplication)springContext.getBean("grailsApplication");
		GrailsPluginManager manager = (GrailsPluginManager)springContext.getBean("pluginManager");
		if (manager.hasGrailsPlugin("app-engine")) {
			delegate = new GrailsDataNucleusExternalizer(grailsApplication);
		}
		else {
			delegate = new GrailsHibernateExternalizer(grailsApplication);
		}

		enumExternalizer = new EnumExternalizer();
	}

	@Override
	public void configure(XMap properties) {
		super.configure(properties);

		enumExternalizer.configure(properties);
		if (delegate != null) {
			delegate.configure(properties);
		}
	}

	@Override
	public Object newInstance(String type, ObjectInput in)
		throws IOException, ClassNotFoundException, InstantiationException, InvocationTargetException, IllegalAccessException {

		Class<?> clazz = TypeUtil.forName(type);
		if (Enum.class.isAssignableFrom(clazz)) {
			return enumExternalizer.newInstance(type, in);
		}

		return delegate.newInstance(type, in);
	}

	@Override
	public void readExternal(Object o, ObjectInput in) throws IOException, ClassNotFoundException, IllegalAccessException {
		if (o instanceof Enum) {
			enumExternalizer.readExternal(o, in);
		}
		else {
			delegate.readExternal(o, in);
		}
	}

	@Override
	public void writeExternal(Object o, ObjectOutput out) throws IOException, IllegalAccessException {
		if (o instanceof Enum) {
			enumExternalizer.writeExternal(o, out);
		}
		else if (!Closure.class.isAssignableFrom(o.getClass())) {
			delegate.writeExternal(o, out);
		}
	}

	@Override
	public int accept(Class<?> clazz) {
		if (grailsApplication.isArtefactOfType("Domain", clazz) || "org.hibernate.proxy.HibernateProxy".equals(clazz.getName())) {
			return 100;
		}
		return -1;
	}

	public static boolean isIgnored(Field field) {
		if (EVENTS.contains(field.getName())) {
			return true;
		}
		if (field.getName().equals("errors") && field.getType().getName().equals(ERRORS)) {
			return true;
		}
		return false;
	}

	public static boolean isIgnored(PropertyDescriptor property) {
		if (EVENTS.contains(property.getName())) {
			return true;
		}
		return property.getName().equals("errors") && property.getPropertyType() != null &&
				property.getPropertyType().getName().equals(ERRORS);
	}
}
