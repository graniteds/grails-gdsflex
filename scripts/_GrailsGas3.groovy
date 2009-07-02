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


def configureGas3() {	
    GroovyClassLoader loader = new GroovyClassLoader(rootLoader)
    loader.addURL(new File(classesDirPath).toURI().toURL())
    Class groovyClass = loader.parseClass(new File("${gdsflexPluginDir}/src/groovy/org/granite/config/GraniteConfigUtil.groovy"))
    GroovyObject groovyObject = (GroovyObject) groovyClass.newInstance()
    
    rootLoader?.addURL(new File("${gdsflexPluginDir}/scripts/lib/gas3/granite-generator.jar").toURI().toURL())
    rootLoader?.addURL(new File("${gdsflexPluginDir}/scripts/lib/gas3/granite-generator-share.jar").toURI().toURL())
    rootLoader?.addURL(new File("${gdsflexPluginDir}/scripts/lib/gas3/jdo2-api-2.2.jar").toURI().toURL())
    rootLoader?.addURL(new File("${gdsflexPluginDir}/scripts/lib/gas3/appengine.jar").toURI().toURL())
    
    Ant.taskdef(name: "gas3", classname: "org.granite.generator.ant.AntJavaAs3Task")
	
    return groovyObject
}


target(gas3: "Gas3") {
    depends(classpath)
    
    Ant.path(id: "gas3.generate.classpath", compileClasspath)

    GroovyObject groovyObject = configureGas3()
    
    def as3Config = groovyObject.getUserConfig()?.as3Config
    def domainJar = as3Config.domainJar
    def extraClasses = as3Config.extraClasses
    
	Ant.path(id: "gas3.generate.classpath") {
		path(location: "${classesDirPath}")
		if (domainJar)
			pathelement(location: domainJar)
	}
	
	def domainDir = new File("${basedir}/grails-app/domain")

	files = []
	list(domainDir, files)
	
	if (!files.isEmpty() || domainJar || (extraClasses && !extraClasses.isEmpty())) {
        File outDir = new File("${basedir}/grails-app/views/flex")
        if (!outDir.exists())
            outDir.mkdirs()
        
		Ant.gas3(outputdir: outDir, 
			tide: "true", 
        	transformer: "org.granite.generator.as3.GrailsAs3GroovyTransformer",
			classpathref: "gas3.generate.classpath") {
			fileset(dir: "${classesDirPath}") {
				for (currentFile in files)
					include(name: currentFile.getPath().substring(domainDir.getPath().length()+1).replace(".groovy", ".class"))
				for (currentFile in extraClasses)
					include(name: currentFile.getPath().substring(domainDir.getPath().length()+1).replace(".groovy", ".class"))
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
