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
import org.springframework.orm.hibernate3.AbstractSessionFactoryBean
import org.codehaus.groovy.grails.plugins.support.GrailsPluginUtils
import org.granite.tide.hibernate.HibernateSessionManager
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean
import org.granite.tide.spring.security.Identity
import org.granite.web.util.WebCompile


class GdsflexGrailsPlugin {
    def version = 0.3
    def author = "William Drai"
    def authorEmail = "william.drai@graniteds.org"
    def title = "Integration between Grails and GraniteDS/Flex"
    def description = ""
    def documentation = "http://www.graniteds.org/"
	
	def watchedResources = "file:./grails-app/views/mxml/**/*.mxml"
    
	def doWithSpring = {
        
		tidePersistenceManagerTarget(HibernateSessionManager, ref("sessionFactory")) {
		}
		
		tidePersistenceManager(TransactionProxyFactoryBean) {
			transactionManager = ref("transactionManager")
			target = ref("tidePersistenceManagerTarget")
			transactionAttributes = ["*" : "PROPAGATION_REQUIRED,readOnly"]
		}
		
        def config = graniteConfig
        if (config) {
        	def conf = config.graniteConfig
        	
        	if (conf.springSecurityAuthorizationEnabled) {
        		identity(conf.springSecurityIdentityClass)
        	}
        }
	}
	
	
	def doWithApplicationContext = { applicationContext ->
        def config = graniteConfig
        if (config) {
        	def conf = config.graniteConfig
        	if (conf.dataDispatchEnabled) {
        		applicationContext.getBeansOfType(AbstractSessionFactoryBean.class).values().each { sf ->
		        	def listeners = sf.sessionFactory.eventListeners
		        	[ "postInsert", "postUpdate", "postDelete"].each {
		            	addDataPublishListener(listeners, it)
		            }
		      	}
	        }	
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
        
        
        def config = graniteConfig
        if (config) {
        	def conf = config.graniteConfig

        	if (conf.gravityEnabled) {
	        	
	        	def listeners = xml.listener
	        	listeners[listeners.size() - 1] + {
	        		listener {
	        			'listener-class'("org.granite.config.GraniteConfigListener")
	        		}
	        	}
	        	
		        servlets = xml.servlet
		        servlets[servlets.size() - 1] + {
		            servlet {
		                'servlet-name'("GravityServlet")
		                'display-name'("GravityServlet")
		                'servlet-class'(conf.gravityServletClassName)
		                'load-on-startup'("1")
		            }
		        }
		    
		        // servlet mappings
		        servletMappings = xml.'servlet-mapping'
		        servletMappings[servletMappings.size() - 1] + {
		            'servlet-mapping' {
		                'servlet-name'("GravityServlet")
		                'url-pattern'("/gravityamf/*")
		            }
		        }
		   	}
        }
    }

    def onChange = { event ->
        println event
        if(event.source) {
            WebCompile.compile("grails-app/views/mxml",event.application.metadata['app.name'])
        }
    }
    
    
    static ConfigObject getGraniteConfig() {
    	WebCompile.init("web-app/WEB-INF")
    
		GroovyClassLoader classLoader = new GroovyClassLoader(GdsflexGrailsPlugin.getClassLoader())

		def slurper = new ConfigSlurper(GrailsUtil.environment)
		ConfigObject userConfig
		try {
			userConfig = slurper.parse(classLoader.loadClass('GraniteDSConfig'))
		}
		catch (e) {
		}

		ConfigObject config
		ConfigObject defaultConfig = slurper.parse(classLoader.loadClass('DefaultGraniteDSConfig'))
		if (userConfig) {
			config = defaultConfig.merge(userConfig)
		}
		else {
			config = defaultConfig
		}

		return config
    }
    
    
    def addDataPublishListener(listeners, type) {
        def previousListeners = listeners."${type}EventListeners"
        def newListeners = new Object[previousListeners.length + 1]
        System.arraycopy(previousListeners, 0, newListeners, 0, previousListeners.length)
        newListeners[-1] = new org.granite.tide.hibernate.HibernateDataPublishListener()
        listeners."${type}EventListeners" = newListeners
    }
}
