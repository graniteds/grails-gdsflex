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
    rootLoader?.addURL(new File("${flexSDK}/ant/lib/flexTasks.jar").toURI().toURL())
    
    Ant.taskdef(name: "html-wrapper", classname: "flex.ant.HtmlWrapperTask")
}


target(flexHtmlWrapper: "Flex html wrapper") {
    
    configureHtmlWrapper()
    
	Ant."html-wrapper"(title: "${grailsAppName}",
            file: "${grailsAppName}.html",
            application: "mainapp",
            swf: "${grailsAppName}",
            width: "100%",
            height: "100%",
            "version-major": "9",
            "version-minor": "0",
            "version-revision": "124",
            history: "true",
            template: "express-installation",
            output: "${basedir}/web-app/") {
	}
	
	Ant.copy(tofile: "${basedir}/grails-app/views/flexindex.gsp", file: "${basedir}/web-app/${grailsAppName}.html") {
	}
	
	println "html wrapper generated in web-app folder and flexindex.gsp in grails-app/views"
}
