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

def configureGas3() {

	GroovyClassLoader loader = new GroovyClassLoader(rootLoader)
	loader.addURL(new File(classesDirPath).toURI().toURL())
	for (name in ['granite-generator', 'granite-generator-share', 'granite-grails-generator', 'jdo2-api-2.3-eb', 'appengine']) {
		rootLoader?.addURL(new File(gdsflexPluginDir, "scripts/lib/gas3/${name}.jar").toURI().toURL())
	}

	ant.taskdef(name: "gas3", classname: "org.granite.generator.ant.AntJavaAs3Task")
	def GraniteConfigUtil = loader.parseClass(new File(gdsflexPluginDir, "src/groovy/org/granite/config/GraniteConfigUtil.groovy"))
	GraniteConfigUtil
}

target(gas3: "Gas3") {
	depends(classpath)

	ant.path(id: "gas3.generate.classpath", compileClasspath)

	def GraniteConfigUtil = configureGas3()

	def as3Config = GraniteConfigUtil.getUserConfig()?.as3Config
	def generateServices = as3Config.generateServices
	def domainJar = as3Config.domainJar
	def extraClasses = as3Config.extraClasses
	def excludeClasses = as3Config.excludeClasses
	def entityBaseTemplate = as3Config.entityBaseTemplate
	if (!entityBaseTemplate) {
		entityBaseTemplate = StandardTemplateUris.TIDE_ENTITY_BASE
	}

	ant.path(id: "gas3.generate.classpath") {
		path(location: "${classesDirPath}")
		if (domainJar) {
			pathelement(location: domainJar)
		}
	}

	def domainDir = new File(basedir, "grails-app/domain")
	def servicesDir = new File(basedir, "grails-app/services")

	def domainFiles = list(domainDir)
	def servicesFiles
	if (generateServices) {
		servicesFiles = list(servicesDir)
	}

	if (domainFiles || servicesFiles || domainJar || extraClasses) {
		String targetDir = as3Config.srcDir ?: "${basedir}/grails-app/views/flex"
		if (targetDir.endsWith("/")) {
			targetDir = targetDir[0..-2]
		}

		File outDir = new File(targetDir)
		if (!outDir.exists()) {
			outDir.mkdirs()
		}

		ant.gas3(
			outputdir:           outDir,
			tide:               "true",
			as3TypeFactory:     "org.granite.grails.gas3.GrailsAs3TypeFactory",
			transformer:        "org.granite.grails.gas3.GrailsAs3GroovyTransformer",
			entitybasetemplate: "class:org/granite/grails/template/tideDomainClassBase.gsp",
			cla11sspathref:     "gas3.generate.classpath") {

			def toClass = { file, artifactDir -> file.path.substring(artifactDir.path.length() + 1).replace(".groovy", ".class") }
			def dotsToSlashes = { clazz -> clazz.replace('.', '/') + ".class" }
			fileset(dir: classesDirPath) {
				domainFiles.each { currentFile -> include(name: toClass(currentFile, domainDir)) }
				servicesFiles.each { currentFile -> include(name: toClass(currentFile, servicesDir)) }
				extraClasses.each { currentClass -> include(name: dotsToSlashes(currentClass)) }
				excludeClasses.each { currentClass -> exclude(name: dotsToSlashes(currentClass)) }
			}
			if (domainJar) {
				fileset(file: domainJar)
			}
		}
	}
}

private List list(File dir) {
	def files = []
	dir.eachFileRecurse { File f ->
		if (f.name.endsWith(".groovy")) {
			files << f
		}
	}
	files
}
