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

Ant.property(environment:"env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"

includeTargets << grailsScript ( "Init" )

target ('default': "Gas3") {
     gas3()
}                     

target(gas3: "Gas3") {
    depends(init, classpath)

	rootLoader?.addURL(new File("${gdsflexPluginDir}/scripts/lib/granite-generator.jar").toURI().toURL())
	rootLoader?.addURL(new File("${gdsflexPluginDir}/scripts/lib/granite-generator-share.jar").toURI().toURL())
	
//	Ant.path(id: 'gas3.classpath') {
//		fileset(dir: "${gdsflexPluginDir}/scripts/lib", includes: "*.jar")
//	}       
	
	Ant.taskdef(name: "gas3", classname: "org.granite.generator.ant.AntJavaAs3Task")
		
	Ant.path(id: "gas3.compile.classpath", compileClasspath)
	
	Ant.path(id: "gas3.generate.classpath") {
		path(location: "${classesDirPath}")
	}
	
	def domainDir = new File("${basedir}/grails-app/domain")

	files = new ArrayList();
	list(domainDir, files);
	
	Ant.gas3(outputdir: "${basedir}/grails-app/views", tide: "true", classpathref: "gas3.generate.classpath") {
		fileset(dir: "${classesDirPath}") {
			for (currentFile in files)
				include(name: currentFile.getPath().substring(domainDir.getPath().length()+1).replace(".groovy", ".class"))
		}
	}
}

def list(File dir, List files) {
	fs = dir.listFiles()
	
	for (f in fs) {
		if (f.isDirectory())
			list(f, files)
		else if (f.getPath().endsWith(".groovy"))
			files.add(f);
	}
}
