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

includeTargets << grailsScript("_GrailsCompile")
includeTargets << new File(gdsflexPluginDir, "scripts/_FlexCommon.groovy")

flexLoaderSet = false

target(initFlexProject: "Init flex project") {
	depends(compilePlugins)

	GroovyClassLoader loader = new GroovyClassLoader(rootLoader)
	loader.addURL(new File(pluginClassesDirPath).toURI().toURL())

	def as3Config = getConfig(loader)

	String sourceDir = as3Config.srcDir ?: "${basedir}/grails-app/views/flex"
	def modules = as3Config.modules ?: []

	ant.mkdir(dir: sourceDir)

	configureFlexCompilerClasspath()

	def FlexCompilerWrapper = Thread.currentThread().contextClassLoader.loadClass("FlexCompilerWrapper")
	FlexCompilerWrapper.init(flexSDK, basedir, gdsflexPluginDir, sourceDir, modules, grailsAppName)
}

target(flexCompile: "Compile the flex file to swf file") {
	depends(initFlexProject)

	ant.copy(todir: "${basedir}/web-app/WEB-INF", overwrite: false) {
		fileset(dir: "${gdsflexPluginDir}/src/web")
	}

	def FlexCompilerWrapper = Thread.currentThread().contextClassLoader.loadClass("FlexCompilerWrapper")
	FlexCompilerWrapper.compile(argsMap.params)
}

private getConfig(ClassLoader loader = Thread.currentThread().contextClassLoader) {
	def GraniteConfigUtil = loader.parseClass(new File("${gdsflexPluginDir}/src/groovy/org/granite/config/GraniteConfigUtil.groovy"))
	return GraniteConfigUtil.getUserConfig(loader)?.as3Config
}

def configureFlexCompilerClasspath() {
	if (flexLoaderSet) {
		println "Flex compiler classpath already set"
		return
	}

	GroovyClassLoader loader = new GroovyClassLoader()


	for (name in ['adt', 'asc', 'asdoc', 'batik-all-flex', 'commons-discovery', 'compc', 'mxmlc', 'mxmlc_ja', 'copylocale',
						'digest', 'flex-compiler-oem', 'fxgutils', 'optimizer', 'swfutils', 'velocity-dep-1.4-flex']) {
		loader.addURL(new File(flexSDK, "lib/${name}.jar").toURI().toURL())
	}

	for (name in ['Exception', 'Type', '', 'Wrapper']) {
		loader.parseClass(new File(gdsflexPluginDir, "scripts/flexcompiler/FlexCompiler${name}.groovy"))
	}

	flexLoaderSet = true

	println "Flex compiler classloader created"

	Thread.currentThread().contextClassLoader = loader
}
