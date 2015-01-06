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

import groovy.text.SimpleTemplateEngine

includeTargets << grailsScript("_GrailsPackage")
includeTargets << grailsScript("_GrailsCompile")
includeTargets << new File(gdsflexPluginDir, "scripts/_GrailsGas3.groovy")
includeTargets << new File(gdsflexPluginDir, "scripts/_GrailsInstallFlexTemplates.groovy")

target (generateFlexApp: "generate Flex app") {
	depends(packageApp, installFlexTemplates, gas3)

	def domainDir = new File(basedir, "grails-app/domain")

	def domainClassList = list(domainDir)

	GroovyClassLoader loader = new GroovyClassLoader(rootLoader)
	loader.addURL(new File(classesDirPath).toURI().toURL())
	def GraniteConfigUtil = loader.parseClass(new File(gdsflexPluginDir, "src/groovy/org/granite/config/GraniteConfigUtil.groovy"))
	def as3Config = GraniteConfigUtil.getUserConfig()?.as3Config

	String targetDir = as3Config.srcDir ?: "${basedir}/grails-app/views/flex/"
	if (targetDir.endsWith("/")) {
		targetDir = targetDir[0..-2]
	}

	File outFile = new File(targetDir, "${grailsAppName}.mxml")
	int i = 0
	File bakFile = outFile
	while (bakFile.exists()) {
		bakFile = new File(targetDir, "${grailsAppName}.mxml.bak.${++i}")
	}
	if (i) {
		println "Backup existing Flex app to ${bakFile}"
		outFile.renameTo(bakFile)
	}

	outFile = new File(targetDir, "${grailsAppName}.mxml")

	def template = new SimpleTemplateEngine().createTemplate(new File(gdsflexPluginDir, "src/templates/mainflexapp.gsp"))
	template.make(domainClassList: domainClassList).writeTo(new FileWriter(outFile))

	println "Generated Flex app in ${targetDir}/${grailsAppName}.mxml"
}

private List list(File dir) {
	List names = []
	dir.eachFileRecurse { File f ->
		if (f.name.endsWith(".groovy")) {
			names << f.path.substring(dir.path.length() + 1, f.path.length() - 7).replace(File.separator, ".")
		}
	}
	names
}

setDefaultTarget generateFlexApp
