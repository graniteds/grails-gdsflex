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

import org.codehaus.groovy.grails.web.context.ServletContextHolder;

import java.util.concurrent.*
import org.springframework.orm.hibernate3.AbstractSessionFactoryBean
import org.granite.tide.data.JDOPersistenceManager
import org.granite.grails.integration.GrailsPersistenceManager
import org.granite.tide.spring.security.Identity
import org.granite.config.ServletGraniteConfig
import org.granite.config.GraniteConfig
import org.granite.config.GraniteConfigUtil
import grails.util.Environment
import grails.util.BuildSettings


class GdsflexGrailsPlugin {
    def version = "0.9.1"
    def author = "William Drai, Ford Guo"
    def authorEmail = "william.drai@graniteds.org"
    def title = "Integration between Grails and GraniteDS/Flex"
    def description = ""
    def documentation = "http://www.graniteds.org/"
	
    private static final def config = GraniteConfigUtil.getUserConfig()
	private static def sourceDir = config?.as3Config.srcDir ?: "./grails-app/views/flex"
	private static def modules = config?.modules ?: []
	
	private static GroovyClassLoader compilerLoader = null
    private static LinkedBlockingQueue lastModifiedQueue = new LinkedBlockingQueue()
    private static ExecutorService executor = Executors.newFixedThreadPool(1)
    
	def watchedResources = ["file:${sourceDir}/**/*.mxml",
	                        "file:${sourceDir}/**/*.css",
	                        "file:${sourceDir}/**/*.as"]
	                        
    
	def doWithSpring = {
	
        if (manager?.hasGrailsPlugin("app-engine")) {
			tidePersistenceManager(JDOPersistenceManager, ref("persistenceManagerFactory")) {
			}
        }
        
        if (manager?.hasGrailsPlugin("hibernate")) {
			tidePersistenceManager(GrailsPersistenceManager, ref("transactionManager")) {
			}
		}
		
		def identityClass = null
		
		if (manager?.hasGrailsPlugin("acegi")) {			
			graniteSecurityInterceptor(org.granite.grails.security.GrailsAcegiInterceptor) {
				authenticationManager = ref('authenticationManager')
				accessDecisionManager = ref('accessDecisionManager')
				objectDefinitionSource = ref('objectDefinitionSource')
			}
			
			graniteSecurityService(org.granite.messaging.service.security.SpringSecurityService) {
				securityInterceptor = ref('graniteSecurityInterceptor')
			}
			
        	identityClass = org.granite.tide.spring.security.Identity
		}
		
		if (manager?.hasGrailsPlugin("spring-security-core")) {
			graniteObjectDefinitionSource(org.granite.grails.security.GrailsSpringSecurity3MetadataSourceWrapper) {
				wrappedMetadataSource = ref('objectDefinitionSource')
			}
		
			graniteSecurityInterceptor(org.granite.grails.security.GrailsSpringSecurity3Interceptor) {
				authenticationManager = ref('authenticationManager')
				accessDecisionManager = ref('accessDecisionManager')
				securityMetadataSource = ref('graniteObjectDefinitionSource')
				runAsManager = ref('runAsManager')
			}
			
			graniteSecurityService(org.granite.spring.security.SpringSecurity3Service) {
				authenticationManager = ref('authenticationManager')
				authenticationTrustResolver = ref('authenticationTrustResolver')
				securityInterceptor = ref('graniteSecurityInterceptor')
				passwordEncoder = ref('passwordEncoder')
				sessionAuthenticationStrategy = ref('sessionAuthenticationStrategy')
			}
			
			identityClass = org.granite.tide.spring.security.Identity3
		}
		
		if (config) {
			def conf = config.graniteConfig
			
			if (conf.springSecurityIdentityClass)
				identityClass = conf.springSecurityIdentityClass
		}

		if (identityClass != null)
			identity(identityClass)
	}
	
	
	def doWithApplicationContext = { applicationContext ->
		
		GraniteConfig graniteConfig = ServletGraniteConfig.loadConfig(applicationContext.servletContext)
		
		if (manager?.hasGrailsPlugin("acegi") || manager?.hasGrailsPlugin("spring-security-core")) {
			graniteConfig.securityService = applicationContext.getBean("graniteSecurityService")
		}
		
        if (config) {
        	def conf = config.graniteConfig
        	if (conf.dataDispatchEnabled && manager?.hasGrailsPlugin("hibernate")) {        	
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
			
	        if (manager?.hasGrailsPlugin("app-engine")) {
				servlet {
					'servlet-name'("WebSWFServlet")
					'display-name'("Web SWF")
					'description'("GraniteDS Web SWF")
					'servlet-class'("org.granite.grails.web.GrailsGAEWebSWFServlet")
					'load-on-startup'("1")
				}
	        }
	        else {
				servlet {
					'servlet-name'("WebSWFServlet")
					'display-name'("Web SWF")
					'description'("GraniteDS Web SWF")
					'servlet-class'("org.granite.grails.web.GrailsWebSWFServlet")
					'load-on-startup'("1")
				}
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
				'servlet-name'("WebSWFServlet")
				'url-pattern'("*.swf")
			}
        }
		
		def listeners = xml.'listener'
		listeners[listeners.size() - 1] + {
			listener {
				'listener-class'("org.granite.grails.integration.GrailsGraniteConfigListener")
			}
		}		

        if (config) {
        	def conf = config.graniteConfig
			
        	if (conf.gravityEnabled) {
				String gravityServletClassName = conf.gravityServletClassName ?: null
				
				// Defaults to servlet 3.0 when available
				ConfigObject buildConfig = GraniteConfigUtil.getBuildConfig()
				
				// Support for Tomcat 6 (Grails 1.3.x) and Tomcat 7 (Grails 2.x)
				if (!gravityServletClassName && buildConfig?.grails?.servlet?.version == "3.0")
					gravityServletClassName = "org.granite.gravity.servlet3.GravityAsyncServlet"
				
				if (!gravityServletClassName && Environment.current == Environment.DEVELOPMENT) {
					try {
						getClass().loadClass("org.granite.gravity.tomcat.GravityTomcatServlet")
						gravityServletClassName = "org.granite.gravity.tomcat.GravityTomcatServlet"
					}
					catch (Exception e) {
					}
				}
			
				if (!gravityServletClassName)
					throw new RuntimeException("Gravity enabled but no suitable servlet class defined in GraniteDSConfig.groovy")
				
				servlets = xml.servlet
		        servlets[servlets.size() - 1] + {
		            servlet {
		                'servlet-name'("GravityServlet")
		                'display-name'("GravityServlet")
		                'servlet-class'(gravityServletClassName)
		                'load-on-startup'("1")
						if (org.granite.gravity.servlet3.GravityAsyncServlet == "org.granite.gravity.servlet3.GravityAsyncServlet")
							'async-supported'("true")
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
		if (Environment.current == Environment.DEVELOPMENT) {
	        if (event.source && config?.as3Config.autoCompileFlex) {
	        	flexCompile(event)
	        }
		}
    }
    
    def flexCompile(event) {
		def buildConfig = GraniteConfigUtil.getBuildConfig()
		
		def flexSDK = System.getenv("FLEX_HOME")
		if (buildConfig.flex.sdk)
			flexSDK = buildConfig.flex.sdk
		
		if (!flexSDK) {
			println "No Flex SDK specified. Either set FLEX_HOME in your environment or specify flex.sdk in your grails-app/conf/BuildConfig.groovy file"
			System.exit(1)
		}
		
		println "Using Flex SDK: ${flexSDK}"
		
		String grailsHome = System.getProperty("grails.home")
		BuildSettings settings = new BuildSettings(new File(grailsHome))
    	
    	File pluginDir = lookupPluginDir(buildConfig, settings)
    	// println "Plugin dir: ${pluginDir}"
    	
 		if (compilerLoader == null) {
 			ClassLoader loader = configureFlexCompilerClassPath(flexSDK, pluginDir)
 			
			File source = new File(sourceDir)
			if (!source.exists())
				source.mkdirs()
				
			// println "Source dir: ${source.canonicalPath}"
			
			Class wrapperClass = loader.loadClass("FlexCompilerWrapper")
			java.lang.reflect.Method wrapperInit = wrapperClass.getMethod("init", Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class)
			wrapperInit.invoke(null, flexSDK, settings.baseDir, pluginDir, source.canonicalPath, modules, event.application.metadata['app.name'], loader)
			
			compilerLoader = loader
		}
    	
    	long lastModified = event.source.lastModified() / 500L 
    	if (!lastModifiedQueue.contains(lastModified)) {
    		lastModifiedQueue.offer(lastModified)
    		executor.execute({
    			println "Flex incremental compilation for ${event.source.filename}"
    			
    			Thread.currentThread().setContextClassLoader(compilerLoader)
    			if (!lastModifiedQueue.isEmpty()) {
        			lastModifiedQueue.clear()
    				Class wrapperClass = Thread.currentThread().getContextClassLoader().loadClass("FlexCompilerWrapper")
					java.lang.reflect.Method wrapperCompile = wrapperClass.getMethod("incrementalCompile", Object.class)
					wrapperCompile.invoke(null, event.source.file)
    			}
    		} as Runnable)
    	}
    }

    
    def addDataPublishListener(listeners, type) {
        def previousListeners = listeners."${type}EventListeners"
        def newListeners = new Object[previousListeners.length + 1]
        System.arraycopy(previousListeners, 0, newListeners, 0, previousListeners.length)
        newListeners[-1] = Class.forName("org.granite.tide.hibernate.HibernateDataPublishListener").newInstance()
        listeners."${type}EventListeners" = newListeners
    }
    
    
    static def configureFlexCompilerClassPath(flexSDK, pluginDir) {
		GroovyClassLoader loader = new GroovyClassLoader()
		
		loader.addURL(new File("${flexSDK}/lib/adt.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/afe.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/aglj32.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/asc.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/asdoc.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/batik_ja.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/batik-awt-util.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/batik-bridge.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/batik-css.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/batik-dom.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/batik-ext.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/batik-gvt.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/batik-parser.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/batik-script.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/batik-svg-dom.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/batik-svggen.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/batik-transcoder.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/batik-util.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/batik-xml.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/commons-discovery.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/compc.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/mxmlc.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/mxmlc_ja.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/copylocale.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/digest.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/flex-compiler-oem.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/flex-fontkit.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/flex-messaging-common.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/mm-velocity-1.4.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/optimizer.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/rideau.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/swfutils.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/xmlParserAPIs.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/xercesImpl.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/xercesPatch.jar").toURI().toURL())
		loader.addURL(new File("${flexSDK}/lib/xalan.jar").toURI().toURL())
	
		loader.parseClass(new File("${pluginDir}/scripts/flexcompiler/FlexCompilerException.groovy"))
		loader.parseClass(new File("${pluginDir}/scripts/flexcompiler/FlexCompilerType.groovy"))
		loader.parseClass(new File("${pluginDir}/scripts/flexcompiler/FlexCompiler.groovy"))
		loader.parseClass(new File("${pluginDir}/scripts/flexcompiler/FlexCompilerWrapper.groovy"))

		return loader
	}
	
    private static File lookupPluginDir(buildConfig, settings) {
    	if (buildConfig?.grails?.plugin?.location?.gdsflex)
    		return new File(buildConfig?.grails?.plugin?.location?.gdsflex.toString());

        File[] dirs = listPluginDirs(settings.getProjectPluginsDir())
        if (dirs.length > 0)
        	return dirs[0]
        
        dirs = listPluginDirs(settings.getGlobalPluginsDir())
        return dirs.length > 0 ? dirs[0] : null;
    }
    
    private static File[] listPluginDirs(File dir) {
        File[] dirs = dir.listFiles({ path -> 
            return path.isDirectory() &&
                    (!path.getName().startsWith(".") && path.getName().startsWith('gdsflex-'))
        } as FileFilter)
		
        return dirs == null ? new File[0] : dirs;
    }
}
