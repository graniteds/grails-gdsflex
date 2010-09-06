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

tmpPath = System.properties."java.io.tmpdir" + "/gdsflex-tmp"


def configureGas3() {	
    GroovyClassLoader loader = new GroovyClassLoader(rootLoader)
    loader.addURL(new File(classesDirPath).toURI().toURL())
    Class groovyClass = loader.parseClass(new File("${gdsflexPluginDir}/src/groovy/org/granite/config/GraniteConfigUtil.groovy"))
    GroovyObject groovyObject = (GroovyObject)groovyClass.newInstance()
    
    rootLoader?.addURL(new File("${gdsflexPluginDir}/scripts/lib/gas3/granite-generator.jar").toURI().toURL())
    rootLoader?.addURL(new File("${gdsflexPluginDir}/scripts/lib/gas3/granite-generator-share.jar").toURI().toURL())
    rootLoader?.addURL(new File("${gdsflexPluginDir}/scripts/lib/gas3/granite-generator-grails.jar").toURI().toURL())
    rootLoader?.addURL(new File("${gdsflexPluginDir}/scripts/lib/gas3/jdo2-api-2.3-eb.jar").toURI().toURL())
    rootLoader?.addURL(new File("${gdsflexPluginDir}/scripts/lib/gas3/appengine.jar").toURI().toURL())
    
    Ant.taskdef(name: "gas3", classname: "org.granite.generator.ant.AntJavaAs3Task")
	
    return groovyObject
}


target(gas3: "Gas3") {
    depends(classpath)
    
    Ant.path(id: "gas3.generate.classpath", compileClasspath)

    GroovyObject groovyObject = configureGas3()
    
    def as3Config = groovyObject.getUserConfig()?.as3Config
    def generateServices = as3Config.generateServices
    def domainJar = as3Config.domainJar
    def extraClasses = as3Config.extraClasses
    def entityBaseTemplate = as3Config.entityBaseTemplate
    if (entityBaseTemplate == null || "".equals(entityBaseTemplate))
    	entityBaseTemplate = org.granite.generator.template.StandardTemplateUris.TIDE_ENTITY_BASE
    
	Ant.path(id: "gas3.generate.classpath") {
		path(location: "${classesDirPath}")
		if (domainJar)
			pathelement(location: domainJar)
	}
	
	def domainDir = new File("${basedir}/grails-app/domain")
	def servicesDir = new File("${basedir}/grails-app/services")
	
	domainFiles = []
	list(domainDir, domainFiles)
	servicesFiles = []
	if (generateServices)
		list(servicesDir, servicesFiles)
	
	if (!domainFiles.isEmpty() || !servicesFiles.isEmpty() || domainJar || (extraClasses && !extraClasses.isEmpty())) {
		def targetDir = as3Config.srcDir ?: "${basedir}/grails-app/views/flex"
		if (targetDir.endsWith("/"))
			targetDir = targetDir.substring(0, targetDir.length()-1)
	
        File outDir = new File(targetDir)
        if (!outDir.exists())
            outDir.mkdirs()
            
		Ant.gas3(outputdir: outDir, 
			tide: "true", 
			as3TypeFactory: "org.granite.grails.gas3.GrailsAs3TypeFactory",
        	transformer: "org.granite.grails.gas3.GrailsAs3GroovyTransformer",
        	entitybasetemplate: "class:org/granite/grails/template/tideDomainClassBase.gsp",
			
			classpathref: "gas3.generate.classpath") {
			fileset(dir: "${classesDirPath}") {
				for (currentFile in domainFiles)
					include(name: currentFile.getPath().substring(domainDir.getPath().length()+1).replace(".groovy", ".class"))
				for (currentFile in servicesFiles)
					include(name: currentFile.getPath().substring(servicesDir.getPath().length()+1).replace(".groovy", ".class"))
				for (currentClass in extraClasses)
					include(name: currentClass.replace('.', '/') + ".class")
			}
			if (domainJar)
				fileset(file: domainJar)
		}
	}
}

def list(File dir, List files) {
	dir.eachFileRecurse {
		if(it.getPath().endsWith(".groovy")) {
			files<<it
		}
	}
	
}
