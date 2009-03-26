/*
  GRANITE DATA SERVICES
  Copyright (C) 2009 ADEQUATE SYSTEMS SARL

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

import grails.util.GrailsUtil
import org.codehaus.groovy.grails.plugins.support.GrailsPluginUtils
import org.granite.tide.hibernate.HibernateSessionManager
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean


class GdsflexGrailsPlugin {
    def version = 0.2
    def author = "William Drai"
    def authorEmail = "william.drai@graniteds.org"
    def title = "Integration between Grails and GraniteDS/Flex"
    def description = ""
    def documentation = "http://www.graniteds.org/"
	
	
	def doWithSpring = {
		tidePersistenceManagerTarget(HibernateSessionManager, ref("sessionFactory")) {
		}
		
		tidePersistenceManager(TransactionProxyFactoryBean) {
			transactionManager = ref("transactionManager")
			target = ref("tidePersistenceManagerTarget")
			transactionAttributes = ["*" : "PROPAGATION_REQUIRED,readOnly"]
		}
	}
	
    
    def doWithWebDescriptor = { xml ->
		
        // filters
        def filters = xml.filter
        filters[filters.size() - 1] + {
            filter {
                'filter-name'("AMFMessageFilter")
                'filter-class'("org.granite.messaging.webapp.AMFMessageFilter")
            }
        }
    
        // filter mappings
        def filterMappings = xml.'filter-mapping'
        filterMappings[filterMappings.size() - 1] + {
            'filter-mapping' {
                'filter-name'("AMFMessageFilter")
                'url-pattern'("/graniteamf/*")
            }
        }

        // servlets
        def servlets = xml.servlet
        servlets[servlets.size() - 1] + {
            servlet {
                'servlet-name'("AMFMessageServlet")
                'display-name'("AMFMessageServlet")
                'servlet-class'("org.granite.messaging.webapp.AMFMessageServlet")
                'load-on-startup'("1")
            }
			
			servlet {
				'servlet-name'("MXMLWebCompiler")
				'display-name'("MXML Web Compiler")
				'description'("GraniteDS Web Compiler")
				'servlet-class'("org.granite.grails.web.GrailsWebCompilerServlet")
				'load-on-startup'("1")
			}
        }
    
        // servlet mappings
        def servletMappings = xml.'servlet-mapping'
        servletMappings[servletMappings.size() - 1] + {
            'servlet-mapping' {
                'servlet-name'("AMFMessageServlet")
                'url-pattern'("/graniteamf/*")
            }
			
			'servlet-mapping' {
				'servlet-name'("MXMLWebCompiler")
				'url-pattern'("*.swf")
			}
        }
    }    
}
