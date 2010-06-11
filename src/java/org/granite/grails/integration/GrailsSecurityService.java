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

import java.util.Map;

import javax.servlet.ServletContext;

import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.granite.context.GraniteContext;
import org.granite.logging.Logger;
import org.granite.messaging.service.security.AbstractSecurityContext;
import org.granite.messaging.service.security.AbstractSecurityService;
import org.granite.messaging.service.security.SecurityServiceException;
import org.granite.messaging.webapp.HttpGraniteContext;
import org.granite.util.ClassUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * @author William Draï
 */
public class GrailsSecurityService extends AbstractSecurityService {
    
	private static final Logger log = Logger.getLogger(GrailsSecurityService.class);
	
	private Map<String, String> conf;
	private AbstractSecurityService delegate;
	
	public GrailsSecurityService() {
	}

	public void configure(Map<String, String> params) {
		this.conf = params;
	}
	
    private AbstractSecurityService getDelegate() {
        GraniteContext context = GraniteContext.getCurrentInstance();
        ServletContext sc = ((HttpGraniteContext)context).getServletContext();
        ApplicationContext springContext = WebApplicationContextUtils.getRequiredWebApplicationContext(sc);
        
        GrailsPluginManager manager = (GrailsPluginManager)springContext.getBean("pluginManager");
        if (manager.hasGrailsPlugin("acegi")) {
        	try {
        		delegate = (AbstractSecurityService)ClassUtil.newInstance("org.granite.messaging.service.security.SpringSecurityService");
        		delegate.configure(conf);
        	}
        	catch (Exception e) {
        		log.error("Could not instantiate Spring Security service", e);
        	}
        }
        else
        	delegate = null;
        
        return delegate;
    }
    
	public Object authorize(AbstractSecurityContext context) throws Exception {
		AbstractSecurityService delegate = getDelegate();
		if (delegate != null)
			return delegate.authorize(context);
		
		return context.invoke();
	}

	public void login(Object credentials) throws SecurityServiceException {
		AbstractSecurityService delegate = getDelegate();
		if (delegate != null)
			delegate.login(credentials);
	}

	public void logout() throws SecurityServiceException {
		AbstractSecurityService delegate = getDelegate();
		if (delegate != null)
			delegate.logout();
	}
}
