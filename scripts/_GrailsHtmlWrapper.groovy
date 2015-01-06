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

tmpPath = System.properties."java.io.tmpdir" + "/gdsflex-tmp"

includeTargets << grailsScript("_GrailsCompile")
includeTargets << new File(gdsflexPluginDir, "scripts/_FlexCommon.groovy")

def configureHtmlWrapper() {
	GroovyClassLoader loader = new GroovyClassLoader(rootLoader)
	loader.addURL(new File(classesDirPath).toURI().toURL())
	def GraniteConfigUtil = loader.parseClass(new File(gdsflexPluginDir, "src/groovy/org/granite/config/GraniteConfigUtil.groovy"))

	rootLoader?.addURL(new File(flexSDK, "$ant/lib/flexTasks.jar").toURI().toURL())
	try {
		rootLoader?.addURL(new File("${pluginClassesDirPath}").toURI().toURL())
	}
	catch (MissingPropertyException e) {
		// Before Grails 1.3
		// rootLoader?.addURL(new File("${classesDirPath}").toURI().toURL())
	}

	ant.taskdef(name: "html-wrapper", classname: "flex.ant.HtmlWrapperTask")
	GraniteConfigUtil
}

target(flexHtmlWrapper: "Flex html wrapper") {

	def GraniteConfigUtil = configureHtmlWrapper()
	def as3Config = GraniteConfigUtil.getUserConfig()?.as3Config

	String targetPlayerVersionMajor = as3Config.versionMajor ?: "9"
	String targetPlayerVersionMinor = as3Config.versionMinor ?: "0"
	String targetPlayerVersionRevision = as3Config.versionRevision ?: "124"

	def targetDir = as3Config.srcDir ?: "${basedir}/grails-app/views/flex/"
	if (targetDir.endsWith("/")) {
		targetDir = targetDir[0..-2]
	}

	ant."html-wrapper"(
		title: grailsAppName,
		file: "${grailsAppName}.html",
		application: "mainapp",
		swf: grailsAppName,
		width: "100%",
		height: "100%",
		"version-major": targetPlayerVersionMajor,
		"version-minor": targetPlayerVersionMinor,
		"version-revision": targetPlayerVersionRevision,
		history: "true",
		output: "${basedir}/web-app/")

	ant.copy(tofile: "${basedir}/grails-app/views/flexindex.gsp", file: "${basedir}/web-app/${grailsAppName}.html")

	println "html wrapper generated in web-app folder and flexindex.gsp in grails-app/views"
}
