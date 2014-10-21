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
import org.springframework.security.access.intercept.aopalliance.MethodSecurityInterceptor;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.granite.tide.data.JDOPersistenceManager
import org.granite.grails.integration.GrailsExternalizer;
import org.granite.grails.integration.GrailsPersistenceManager
import org.granite.messaging.amf.io.util.externalizer.EnumExternalizer;
import org.granite.tide.spring.TideDataPublishingAdvisor;
import org.granite.tide.spring.security.Identity
import org.granite.tide.spring.security.AclIdentity
import org.granite.config.ServletGraniteConfig
import org.granite.config.GraniteConfig
import org.granite.config.GraniteConfigUtil

import grails.util.Environment
import grails.util.BuildSettings


class GdsflexGrailsPlugin {
	def groupId = 'org.graniteds.grails'
    def version = "2.0.0-SNAPSHOT"
    def author = "William Drai, Ford Guo"
    def authorEmail = "william.drai@graniteds.org"
    def title = "Integration between Grails and GraniteDS/Flex"
    def description = ""
    def documentation = "http://www.graniteds.org/"
	
    private static final def config = GraniteConfigUtil.getUserConfig()
	private static def sourceDir = config?.as3Config.srcDir ?: "./grails-app/views/flex"
	private static def modules = config?.modules ?: []
	private static final String GRAVITY_ASYNC_SERVLET_NAME = "org.granite.gravity.servlet3.GravityAsyncServlet"
	
	private static GroovyClassLoader compilerLoader = null
    private static LinkedBlockingQueue lastModifiedQueue = new LinkedBlockingQueue()
    private static ExecutorService executor = Executors.newFixedThreadPool(1)
    
	def watchedResources = ["file:${sourceDir}/**/*.mxml",
	                        "file:${sourceDir}/**/*.css",
	                        "file:${sourceDir}/**/*.as"]
	                        
    
	def doWithSpring = {
		
		xmlns graniteds:"http://www.graniteds.org/config"
		graniteds."server-filter"("tide": "true")
		
		graniteUrlMapping(SimpleUrlHandlerMapping) {
			alwaysUseFullPath = true
			urlMap = [ '/graniteamf/*': 'org.granite.spring.ServerFilter' ]
		}
		
		graniteds.'tide-data-publishing-advice'('mode': 'proxy', order: Integer.MAX_VALUE)
		
		grailsExternalizer(GrailsExternalizer)
		
		enumExternalizer(EnumExternalizer)
		
        if (manager?.hasGrailsPlugin("app-engine")) {
			tidePersistenceManager(JDOPersistenceManager, ref("persistenceManagerFactory")) {
			}
        }
        
        if (manager?.hasGrailsPlugin("hibernate")) {
			tidePersistenceManager(GrailsPersistenceManager, ref("transactionManager")) {
			}
		}
		
		def identityClass = null
		
		if (manager?.hasGrailsPlugin("spring-security-core")) {
			// Load classes dynamically in case plugin not present
			Class gssmswClass = Thread.currentThread().getContextClassLoader().loadClass("org.granite.grails.security.GrailsSpringSecurity3MetadataSourceWrapper")
			Class gssiClass = Thread.currentThread().getContextClassLoader().loadClass("org.granite.grails.security.GrailsSpringSecurity3Interceptor")
			
			graniteObjectDefinitionSource(gssmswClass) {
				wrappedMetadataSource = ref('objectDefinitionSource')
			}
			
			graniteSecurityInterceptor(gssiClass) {
				authenticationManager = ref('authenticationManager')
				accessDecisionManager = ref('accessDecisionManager')
				securityMetadataSource = ref('graniteObjectDefinitionSource')
				runAsManager = ref('runAsManager')
			}

			graniteSecurityService(org.granite.spring.security.SpringSecurity3Service) {
				authenticationManager = ref('authenticationManager')
				authenticationTrustResolver = ref('authenticationTrustResolver')
				securityInterceptor = ref('graniteSecurityInterceptor')
				// passwordEncoder = ref('passwordEncoder') // Don't use passwordEncoder any more, Grails plugin encodes the incoming password itself
				sessionAuthenticationStrategy = ref('sessionAuthenticationStrategy')
			}
			
			identityClass = Identity
		}
		
		if (manager?.hasGrailsPlugin("spring-security-acl")) {
			identityClass = AclIdentity;
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
		
		if (manager?.hasGrailsPlugin("spring-security-core")) {
			graniteConfig.securityService = applicationContext.getBean("graniteSecurityService")
		}
		
		// Ensure order of transaction and data publish interceptors
		// Data publish interceptor must be called inside transaction interceptor
		def advisors = applicationContext.getBeansOfType(org.springframework.aop.Advisor.class).values()
		
		def txAdvisor = null
		def tideDataAdvisor = null
		for (Object advisor in advisors) {
			if (advisor instanceof TideDataPublishingAdvisor)
				tideDataAdvisor = advisor
			else if (advisor.advice instanceof TransactionInterceptor)
				txAdvisor = advisor
		}
		
		if (txAdvisor.order >= tideDataAdvisor.order) {
			if (txAdvisor.order == Integer.MAX_VALUE)
				txAdvisor.order = Integer.MAX_VALUE-1
			tideDataAdvisor.order = txAdvisor.order+1
		}
	}
    
    def doWithWebDescriptor = { xml ->
        // servlets
        def servlets = xml.servlet
        servlets[servlets.size() - 1] + {
            servlet {
                'servlet-name'("GraniteServlet")
                'display-name'("GraniteServlet")
                'servlet-class'("org.codehaus.groovy.grails.web.servlet.GrailsDispatcherServlet")
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
                'servlet-name'("GraniteServlet")
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
				'listener-class'("org.granite.config.GraniteConfigListener")
			}
		}		

        if (config) {
        	def conf = config.graniteConfig
			
			String gravityServletClassName = conf.gravityServletClassName ?: null
			
			// Defaults to servlet 3.0 when available
			ConfigObject buildConfig = GraniteConfigUtil.getBuildConfig()
			
			// Support for Tomcat 7 (Grails 2.x)
			if (!gravityServletClassName && buildConfig?.grails?.servlet?.version == "3.0")
				gravityServletClassName = GRAVITY_ASYNC_SERVLET_NAME
			
			if (!gravityServletClassName && Environment.current == Environment.DEVELOPMENT) {
				try {
					getClass().loadClass(GRAVITY_TOMCAT_SERVLET_NAME)
					gravityServletClassName = GRAVITY_TOMCAT_SERVLET_NAME
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
					if (GRAVITY_ASYNC_SERVLET_NAME.equals(gravityServletClassName))
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
    	
 		if (compilerLoader == null) {
 			ClassLoader loader = configureFlexCompilerClassPath(flexSDK, pluginDir)
 			
			File source = new File(sourceDir)
			if (!source.exists())
				source.mkdirs()
			
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
