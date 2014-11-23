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

import java.util.List;

import javax.servlet.ServletContext;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.granite.context.GraniteContext;
import org.granite.datanucleus.DataNucleusClassGetter;
import org.granite.hibernate4.HibernateClassGetter;
import org.granite.messaging.amf.io.util.ClassGetter;
import org.granite.messaging.amf.io.util.DefaultClassGetter;
import org.granite.messaging.webapp.HttpGraniteContext;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * @author William Draï
 */
public class GrailsClassGetter extends DefaultClassGetter {

	private GrailsApplication grailsApplication;
	private ClassGetter delegate;

	private ClassGetter getDelegate() {
		if (delegate == null) {
			GraniteContext context = GraniteContext.getCurrentInstance();
			ServletContext sc = ((HttpGraniteContext)context).getServletContext();
			ApplicationContext springContext = WebApplicationContextUtils.getRequiredWebApplicationContext(sc);

			grailsApplication = (GrailsApplication)springContext.getBean("grailsApplication");
			GrailsPluginManager manager = (GrailsPluginManager)springContext.getBean("pluginManager");

			if (manager.hasGrailsPlugin("app-engine")) {
				delegate = new DataNucleusClassGetter();
			}
			else {
				delegate = new HibernateClassGetter();
			}
		}
		return delegate;
	}

	@Override
	public List<Object[]> getFieldValues(Object obj, Object dest) {
		return getDelegate().getFieldValues(obj, dest);
	}

	@Override
	public void initialize(Object owner, String propertyName, Object propertyValue) {
		getDelegate().initialize(owner, propertyName, propertyValue);
	}

	@Override
	public Class<?> getClass(Object o) {
		return getDelegate().getClass(o);
	}

	@Override
	public boolean isEntity(Object o) {
		return grailsApplication.isArtefactOfType("Domain", o.getClass());
	}

	@Override
	public boolean isInitialized(Object owner, String propertyName, Object propertyValue) {
		return getDelegate().isInitialized(owner, propertyName, propertyValue);
	}
}
