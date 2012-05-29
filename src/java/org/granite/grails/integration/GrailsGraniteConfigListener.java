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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.granite.config.GraniteConfig;
import org.granite.config.GraniteConfigListener;
import org.granite.config.ServletGraniteConfig;
import org.granite.logging.Logger;
import org.granite.util.ClassUtil;

/**
 * @author William Drai
 */
public class GrailsGraniteConfigListener implements ServletContextListener {
	
	private static final Logger log = Logger.getLogger(GrailsGraniteConfigListener.class);
	
	private ServletContextListener graniteConfigListener = new GraniteConfigListener();
	
	@Override
    public void contextInitialized(ServletContextEvent sce) {
		graniteConfigListener.contextInitialized(sce);
		
		try {
			// Set proxy externalizer if Hibernate present
			ClassUtil.forName("org.hibernate.proxy.HibernateProxy");
			GraniteConfig graniteConfig = ServletGraniteConfig.getConfig(sce.getServletContext());
			graniteConfig.putExternalizersByInstanceOf("org.hibernate.proxy.HibernateProxy", "org.granite.grails.integration.GrailsExternalizer");
			
			log.info("Added externalizer for Hibernate proxies");
		}
		catch (Exception e) {
		}
	}
	
	@Override
    public void contextDestroyed(ServletContextEvent sce) {
		graniteConfigListener.contextDestroyed(sce);
	}
}
