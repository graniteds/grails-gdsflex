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
import grails.spring.WebBeanBuilder
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import org.codehaus.groovy.grails.plugins.DefaultPluginMetaManager
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.springframework.core.io.FileSystemResourceLoader
import org.springframework.mock.web.MockServletContext
import org.springframework.util.ClassUtils
import groovyjarjarasm.asm.*
import java.lang.reflect.*
import javax.persistence.*


Ant.property(environment:"env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"

tmpPath = System.properties."java.io.tmpdir"+"/gdsflex-tmp"

includeTargets << grailsScript("_GrailsCompile")
includeTargets << new File("${gdsflexPluginDir}/scripts/_FlexCommon.groovy")


def configureHtmlWrapper() {	    
    GroovyClassLoader loader = new GroovyClassLoader(rootLoader)
    loader.addURL(new File(classesDirPath).toURI().toURL())
    Class groovyClass = loader.parseClass(new File("${gdsflexPluginDir}/src/groovy/org/granite/config/GraniteConfigUtil.groovy"))
    GroovyObject groovyObject = (GroovyObject)groovyClass.newInstance()
	
    rootLoader?.addURL(new File("${flexSDK}/ant/lib/flexTasks.jar").toURI().toURL())	
	try {
		rootLoader?.addURL(new File("${pluginClassesDirPath}").toURI().toURL())
	}
	catch (groovy.lang.MissingPropertyException e) {
		// Before Grails 1.3
		// rootLoader?.addURL(new File("${classesDirPath}").toURI().toURL())
	}

    Ant.taskdef(name: "html-wrapper", classname: "flex.ant.HtmlWrapperTask")
	
	return groovyObject
}


target(flexHtmlWrapper: "Flex html wrapper") {
    
    GroovyObject groovyObject = configureHtmlWrapper()
	
    def as3Config = groovyObject.getUserConfig()?.as3Config
    
    def targetPlayerVersionMajor = as3Config.versionMajor ?: "9"
    def targetPlayerVersionMinor = as3Config.versionMinor ?: "0"
    def targetPlayerVersionRevision = as3Config.versionRevision ?: "124"
    
	def targetDir = as3Config.srcDir ?: "${basedir}/grails-app/views/flex/"
	if (targetDir.endsWith("/"))
		targetDir = targetDir.substring(0, targetDir.length()-1)
    
	Ant."html-wrapper"(title: "${grailsAppName}",
            file: "${grailsAppName}.html",
            application: "mainapp",
            swf: "${grailsAppName}",
            width: "100%",
            height: "100%",
            "version-major": targetPlayerVersionMajor,
            "version-minor": targetPlayerVersionMinor,
            "version-revision": targetPlayerVersionRevision,
            history: "true",
            output: "${basedir}/web-app/") {
	}
	
	Ant.copy(tofile: "${basedir}/grails-app/views/flexindex.gsp", file: "${basedir}/web-app/${grailsAppName}.html") {
	}
	
	println "html wrapper generated in web-app folder and flexindex.gsp in grails-app/views"
}
