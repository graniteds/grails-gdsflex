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

import grails.util.Environment


Ant.property(environment:"env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"

includeTargets << grailsScript("_GrailsCompile")
includeTargets << new File("${gdsflexPluginDir}/scripts/_FlexCommon.groovy")


private def getConfig(ClassLoader loader = Thread.currentThread().getContextClassLoader()) {

    Class groovyClass = loader.parseClass(new File("${gdsflexPluginDir}/src/groovy/org/granite/config/GraniteConfigUtil.groovy"))
    GroovyObject groovyObject = (GroovyObject)groovyClass.newInstance()
    
    return groovyObject.getUserConfig(loader)?.as3Config
}


flexLoaderSet = false

def configureFlexCompilerClasspath() {
	if (flexLoaderSet) {
		println "Flex compiler classpath already set" 
		return
	}
	
	GroovyClassLoader loader = new GroovyClassLoader()
	
	loader.addURL(new File("${flexSDK}/lib/adt.jar").toURI().toURL())
	loader.addURL(new File("${flexSDK}/lib/asc.jar").toURI().toURL())
	loader.addURL(new File("${flexSDK}/lib/asdoc.jar").toURI().toURL())
	loader.addURL(new File("${flexSDK}/lib/batik-all-flex.jar").toURI().toURL())
	loader.addURL(new File("${flexSDK}/lib/commons-discovery.jar").toURI().toURL())
	loader.addURL(new File("${flexSDK}/lib/compc.jar").toURI().toURL())
	loader.addURL(new File("${flexSDK}/lib/mxmlc.jar").toURI().toURL())
	loader.addURL(new File("${flexSDK}/lib/mxmlc_ja.jar").toURI().toURL())
	loader.addURL(new File("${flexSDK}/lib/copylocale.jar").toURI().toURL())
	loader.addURL(new File("${flexSDK}/lib/digest.jar").toURI().toURL())
	loader.addURL(new File("${flexSDK}/lib/flex-compiler-oem.jar").toURI().toURL())
	loader.addURL(new File("${flexSDK}/lib/fxgutils.jar").toURI().toURL())
	loader.addURL(new File("${flexSDK}/lib/optimizer.jar").toURI().toURL())
	loader.addURL(new File("${flexSDK}/lib/swfutils.jar").toURI().toURL())
	loader.addURL(new File("${flexSDK}/lib/velocity-dep-1.4-flex.jar").toURI().toURL())
	
	loader.parseClass(new File("${gdsflexPluginDir}/scripts/flexcompiler/FlexCompilerException.groovy"))
	loader.parseClass(new File("${gdsflexPluginDir}/scripts/flexcompiler/FlexCompilerType.groovy"))
	loader.parseClass(new File("${gdsflexPluginDir}/scripts/flexcompiler/FlexCompiler.groovy"))
	loader.parseClass(new File("${gdsflexPluginDir}/scripts/flexcompiler/FlexCompilerWrapper.groovy"))
			
	flexLoaderSet = true
	
	println "Flex compiler classloader created"
	
	Thread.currentThread().setContextClassLoader(loader)
}

target(initFlexProject: "Init flex project") {
    depends(compilePlugins)
    
    GroovyClassLoader loader = new GroovyClassLoader(rootLoader)
	loader.addURL(new File("${pluginClassesDirPath}").toURI().toURL())
    
	def as3Config = getConfig(loader)	
    
	def sourceDir = as3Config.srcDir ?: "${basedir}/grails-app/views/flex"
	def modules = as3Config.modules ?: []
	
	Ant.mkdir(dir: sourceDir)
	
    configureFlexCompilerClasspath()
	
    Class wrapperClass = Thread.currentThread().getContextClassLoader().loadClass("FlexCompilerWrapper")
	java.lang.reflect.Method wrapperInit = wrapperClass.getMethod("init", Object.class, Object.class, Object.class, Object.class, Object.class, Object.class)
	wrapperInit.invoke(null, flexSDK, basedir, gdsflexPluginDir, sourceDir, modules, grailsAppName)
}


target(flexCompile: "Compile the flex file to swf file") {
    depends(initFlexProject)
    
	Ant.copy(todir: "${basedir}/web-app/WEB-INF", overwrite: false) {
	    fileset(dir: "${gdsflexPluginDir}/src/web")
	}
    
    Class wrapperClass = Thread.currentThread().getContextClassLoader().loadClass("FlexCompilerWrapper")
	java.lang.reflect.Method wrapperCompile = wrapperClass.getMethod("compile", Object.class)
	wrapperCompile.invoke(null, argsMap["params"])
}
