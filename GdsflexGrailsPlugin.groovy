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

import grails.util.BuildSettings
import grails.util.Environment
import grails.util.Metadata

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

import org.codehaus.groovy.grails.web.servlet.GrailsDispatcherServlet
import org.granite.config.GraniteConfig
import org.granite.config.GraniteConfigListener
import org.granite.config.GraniteConfigUtil
import org.granite.config.ServletGraniteConfig
import org.granite.grails.integration.GrailsExternalizer
import org.granite.grails.integration.GrailsPersistenceManager
import org.granite.messaging.amf.io.util.externalizer.EnumExternalizer
import org.granite.spring.ServerFilter
import org.granite.tide.data.JDOPersistenceManager
import org.granite.tide.spring.TideDataPublishingAdvisor
import org.granite.tide.spring.security.AclIdentity
import org.granite.tide.spring.security.Identity
import org.springframework.aop.Advisor
import org.springframework.transaction.interceptor.TransactionInterceptor
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping

class GdsflexGrailsPlugin {
	def groupId = 'org.graniteds.grails'
	def version = "2.0.0.GA"
	def grailsVersion = '2.0 > *'
	def title = "GraniteDS Plugin"
	def description = "Integration between Grails and GraniteDS/Flex"
	def documentation = "http://www.granitedataservices.com/"
	def license = 'LGPL2.1'
	def organization = [name: 'GraniteDS', url: 'http://www.granitedataservices.com/']
	def developers = [
		[name: 'William Drai', email: 'william.drai@graniteds.org']
		[name: 'Ford Guo']
	]
	def issueManagement = [system: 'GITHUB', url: 'https://github.com/graniteds/grails-gdsflex/issues']
	def scm = [url: 'https://github.com/graniteds/grails-gdsflex']

	private static final config = GraniteConfigUtil.userConfig
	private static String sourceDir = config.as3Config.srcDir ?: "./grails-app/views/flex"
	private static modules = config.modules ?: []
	private static final String GRAVITY_ASYNC_SERVLET_NAME = "org.granite.gravity.servlet3.GravityAsyncServlet"

	private static GroovyClassLoader compilerLoader
	private static LinkedBlockingQueue lastModifiedQueue = new LinkedBlockingQueue()
	private static ExecutorService executor = Executors.newFixedThreadPool(1)

	def watchedResources = [
		"file:${sourceDir}/**/*.mxml",
		"file:${sourceDir}/**/*.css",
		"file:${sourceDir}/**/*.as"]

	def doWithSpring = {

		xmlns graniteds:"http://www.graniteds.org/config"
		graniteds."server-filter"("tide": "true")

		graniteUrlMapping(SimpleUrlHandlerMapping) {
			alwaysUseFullPath = true
			urlMap = [ '/graniteamf/*': ServerFilter.name ]
		}

		graniteds.'tide-data-publishing-advice'(mode: 'proxy', order: Integer.MAX_VALUE)

		grailsExternalizer(GrailsExternalizer)

		enumExternalizer(EnumExternalizer)

		if (manager?.hasGrailsPlugin("app-engine")) {
			tidePersistenceManager(JDOPersistenceManager, ref("persistenceManagerFactory"))
		}

		if (manager?.hasGrailsPlugin("hibernate")) {
			tidePersistenceManager(GrailsPersistenceManager, ref("transactionManager"))
		}

		def identityClass

		if (manager?.hasGrailsPlugin("spring-security-core")) {
			// Load classes dynamically in case plugin not present
			Class gssmswClass = Thread.currentThread().contextClassLoader.loadClass("org.granite.grails.security.GrailsSpringSecurity3MetadataSourceWrapper")
			Class gssiClass = Thread.currentThread().contextClassLoader.loadClass("org.granite.grails.security.GrailsSpringSecurity3Interceptor")

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
				// passwordEncoder = ref('passwordEncoder') // Don't use passwordEncoder any more, the User class encodes the incoming password itself
				sessionAuthenticationStrategy = ref('sessionAuthenticationStrategy')
			}

			identityClass = Identity
		}

		if (manager?.hasGrailsPlugin("spring-security-acl")) {
			identityClass = AclIdentity
		}

		if (config) {
			def conf = config.graniteConfig
			if (conf.springSecurityIdentityClass) {
				identityClass = conf.springSecurityIdentityClass
			}
		}

		if (identityClass) {
			identity(identityClass)
		}
	}

	def doWithApplicationContext = { applicationContext ->

		GraniteConfig graniteConfig = ServletGraniteConfig.loadConfig(applicationContext.servletContext)

		if (manager?.hasGrailsPlugin("spring-security-core")) {
			graniteConfig.securityService = applicationContext.graniteSecurityService
		}

		// Ensure order of transaction and data publish interceptors
		// Data publish interceptor must be called inside transaction interceptor
		def advisors = applicationContext.getBeansOfType(Advisor).values()

		def txAdvisor
		def tideDataAdvisor
		for (advisor in advisors) {
			if (advisor instanceof TideDataPublishingAdvisor) {
				tideDataAdvisor = advisor
			}
			else if (advisor.advice instanceof TransactionInterceptor) {
				txAdvisor = advisor
			}
		}

		if (txAdvisor.order >= tideDataAdvisor.order) {
			if (txAdvisor.order == Integer.MAX_VALUE) {
				txAdvisor.order = Integer.MAX_VALUE - 1
			}
			tideDataAdvisor.order = txAdvisor.order + 1
		}
	}

	def doWithWebDescriptor = { xml ->
		// servlets
		def servlets = xml.servlet
		servlets[servlets.size() - 1] + {
			servlet {
				'servlet-name'("GraniteServlet")
				'display-name'("GraniteServlet")
				'servlet-class'(GrailsDispatcherServlet.name)
				'load-on-startup'("1")
			}

			String className = manager?.hasGrailsPlugin("app-engine") ? 'GrailsGAEWebSWFServlet' : 'GrailsWebSWFServlet'

			servlet {
				'servlet-name'("WebSWFServlet")
				'display-name'("Web SWF")
				description("GraniteDS Web SWF")
				'servlet-class'("org.granite.grails.web." + className)
				'load-on-startup'("1")
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
				'listener-class'(GraniteConfigListener.name)
			}
		}

		if (config) {
			def conf = config.graniteConfig

			String gravityServletClassName = conf.gravityServletClassName ?: null

			// Defaults to servlet 3.0 when available
			ConfigObject buildConfig = GraniteConfigUtil.buildConfig

			// Support for Tomcat 7 (Grails 2.x)
			if (!gravityServletClassName && buildConfig?.grails?.servlet?.version == "3.0") {
				gravityServletClassName = GRAVITY_ASYNC_SERVLET_NAME
			}

			if (!gravityServletClassName && Environment.isDevelopmentMode()) {
				try {
					getClass().loadClass(GRAVITY_TOMCAT_SERVLET_NAME)
					gravityServletClassName = GRAVITY_TOMCAT_SERVLET_NAME
				}
				catch (ignored) {}
			}

			if (!gravityServletClassName) {
				throw new RuntimeException("Gravity enabled but no suitable servlet class defined in GraniteDSConfig.groovy")
			}

			servlets = xml.servlet
			servlets[servlets.size() - 1] + {
				servlet {
					'servlet-name'("GravityServlet")
					'display-name'("GravityServlet")
					'servlet-class'(gravityServletClassName)
					'load-on-startup'("1")
					if (GRAVITY_ASYNC_SERVLET_NAME.equals(gravityServletClassName)) {
						'async-supported'("true")
					}
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
		if (Environment.isDevelopmentMode() && event.source && config?.as3Config.autoCompileFlex) {
			flexCompile(event)
		}
	}

	private flexCompile(event) {
		def buildConfig = GraniteConfigUtil.buildConfig

		def flexSDK = System.getenv("FLEX_HOME")
		if (buildConfig.flex.sdk) {
			flexSDK = buildConfig.flex.sdk
		}

		if (!flexSDK) {
			println "No Flex SDK specified. Either set FLEX_HOME in your environment or specify flex.sdk in your grails-app/conf/BuildConfig.groovy file"
			System.exit(1)
		}

		println "Using Flex SDK: ${flexSDK}"

		String grailsHome = System.getProperty("grails.home")
		BuildSettings settings = new BuildSettings(new File(grailsHome))

		File pluginDir = lookupPluginDir(buildConfig, settings)

		if (!compilerLoader) {
			ClassLoader loader = configureFlexCompilerClassPath(flexSDK, pluginDir)

			File source = new File(sourceDir)
			if (!source.exists()) {
				source.mkdirs()
			}

			def FlexCompilerWrapper = loader.loadClass("FlexCompilerWrapper")
			FlexCompilerWrapper.init(flexSDK, settings.baseDir, pluginDir, source.canonicalPath, modules, Metadata.current.getApplicationName(), loader)

			compilerLoader = loader
		}

		long lastModified = event.source.lastModified() / 500L
		if (!lastModifiedQueue.contains(lastModified)) {
			lastModifiedQueue.offer(lastModified)
			executor.execute({
				println "Flex incremental compilation for ${event.source.filename}"

				Thread.currentThread().contextClassLoader = compilerLoader
				if (lastModifiedQueue) {
					lastModifiedQueue.clear()
					def FlexCompilerWrapper = compilerLoader.loadClass("FlexCompilerWrapper")
					FlexCompilerWrapper.incrementalCompile(event.source.file)
				}
			} as Runnable)
		}
	}

	static GroovyClassLoader configureFlexCompilerClassPath(flexSDK, pluginDir) {
		GroovyClassLoader loader = new GroovyClassLoader()

		for (name in ['adt', 'afe', 'aglj32', 'asc', 'asdoc', 'batik_ja', 'batik-awt-util', 'batik-bridge',
		              'batik-css', 'batik-dom', 'batik-ext', 'batik-gvt', 'batik-parser', 'batik-script',
		              'batik-svg-dom', 'batik-svggen', 'batik-transcoder', 'batik-util', 'batik-xml',
		              'commons-discovery', 'compc', 'mxmlc', 'mxmlc_ja', 'copylocale', 'digest',
		              'flex-compiler-oem', 'flex-fontkit', 'flex-messaging-common', 'mm-velocity-1.4',
		              'optimizer', 'rideau', 'swfutils', 'xmlParserAPIs', 'xercesImpl', 'xercesPatch', 'xalan']) {
			loader.addURL(new File(flexSDK, "lib/${name}.jar").toURI().toURL())
		}

		for (name in ['Exception', 'Type', '', 'Wrapper']) {
			loader.parseClass(new File(pluginDir, "scripts/flexcompiler/FlexCompiler${name}.groovy"))
		}

		loader
	}

	private static File lookupPluginDir(buildConfig, settings) {
		if (buildConfig?.grails?.plugin?.location?.gdsflex) {
			return new File(buildConfig.grails.plugin.location.gdsflex.toString())
		}

		File[] dirs = listPluginDirs(settings.projectPluginsDir)
		if (dirs) {
			return dirs[0]
		}

		dirs = listPluginDirs(settings.globalPluginsDir)
		dirs ? dirs[0] : null
	}

	private static File[] listPluginDirs(File dir) {
		File[] dirs = dir.listFiles({ path ->
			path.directory && (!path.name.startsWith(".") && path.name.startsWith('gdsflex-'))
		} as FileFilter)

		dirs ?: [] as File[]
	}
}
