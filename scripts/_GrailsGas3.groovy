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

Ant.property(environment:"env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"

includeTargets << grailsScript("_GrailsCompile")

tmpPath = System.properties."java.io.tmpdir"+File.separator+"gdsflex-tmp"
as3Config = new ConfigSlurper().parse(
               new File("${basedir}/grails-app/conf/GraniteDSConfig.groovy").toURI().toURL()
            ).as3Config

target(gas3: "Gas3") {
	rootLoader?.addURL(new File("${gdsflexPluginDir}/scripts/lib/granite-generator.jar").toURI().toURL())
	rootLoader?.addURL(new File("${gdsflexPluginDir}/scripts/lib/granite-generator-share.jar").toURI().toURL())
	
	Ant.taskdef(name: "gas3", classname: "org.granite.generator.ant.AntJavaAs3Task")
		
	Ant.path(id: "gas3.compile.classpath", compileClasspath)
    println as3Config
    def domainJar = as3Config.domainJar
	def genClassPath =  domainJar?tmpPath:classesDirPath
	Ant.path(id: "gas3.generate.classpath") {
        path(location: genClassPath)
	}
	
    initGrailsApp()
	def domainClasses = grailsApp.getArtefacts('Domain')
	if (domainClasses.size()>0) {
        if(domainJar)  {
            Ant.mkdir(dir:tmpPath)
            Ant.unzip(dest:tmpPath,src:domainJar) {
                patternset() {
                    domainClasses.each{grailsClass->
                        include(name: grailsClass.fullName.replaceAll("\\.","/")+"*")
                    }                
                }
            }
        }
		Ant.gas3(outputdir: "${basedir}/grails-app/views", tide: "true", classpathref: "gas3.generate.classpath") {
			fileset(dir: genClassPath) {
				domainClasses.each{grailsClass->
                    include(name: grailsClass.fullName.replaceAll("\\.","/")+".class")
                }
			}
		}
	}
}

def initGrailsApp() {
    def builder =  new WebBeanBuilder()
    beanDefinitions = builder.beans {
        resourceHolder(org.codehaus.groovy.grails.commons.spring.GrailsResourceHolder) {
            resources = GrailsPluginUtils.getArtefactResources(basedir, resolveResources)
        }
        grailsResourceLoader(org.codehaus.groovy.grails.commons.GrailsResourceLoaderFactoryBean) {
            grailsResourceHolder = resourceHolder
        }
        grailsApplication(org.codehaus.groovy.grails.commons.DefaultGrailsApplication, ref("grailsResourceLoader"))
        pluginMetaManager(DefaultPluginMetaManager) {
            grailsApplication = ref('grailsApplication')
        }
    }
	def appCtx =  beanDefinitions.createApplicationContext()
    grailsApp = appCtx.grailsApplication
    PluginManagerHolder.pluginManager = null
    loadPlugins()
    pluginManager = PluginManagerHolder.pluginManager
    pluginManager.application = grailsApp
    pluginManager.doArtefactConfiguration()
    grailsApp.initialise()
}
