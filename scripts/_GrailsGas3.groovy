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


Ant.property(environment:"env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"

tmpPath = System.properties."java.io.tmpdir"+File.separator+"gdsflex-tmp"
as3Config = [:]
if(new File("${basedir}/grails-app/conf/GraniteDSConfig.groovy").exists()) {
    as3Config = new ConfigSlurper().parse(
            new File("${basedir}/grails-app/conf/GraniteDSConfig.groovy").toURI().toURL()
            ).as3Config
}
rootLoader?.addURL(new File("${gdsflexPluginDir}/scripts/lib/granite-generator.jar").toURI().toURL())
rootLoader?.addURL(new File("${gdsflexPluginDir}/scripts/lib/granite-generator-share.jar").toURI().toURL())

Ant.taskdef(name: "gas3", classname: "org.granite.generator.ant.AntJavaAs3Task")

Ant.path(id: "gas3.compile.classpath", compileClasspath)

target(gas3: "Gas3") {
    def domainJar = as3Config.domainJar
    def genClassPath =  domainJar?tmpPath:classesDirPath
    Ant.path(id: "gas3.generate.classpath") { path(location: genClassPath) }
    
    def grailsApp = initGrailsApp()
    def domainClasses = grailsApp.getArtefacts('Domain') as List
    if (domainClasses.size()>0) {
        domainClasses = mergeClasses(domainClasses)
        if(domainJar)  {
            Ant.mkdir(dir:tmpPath)
            Ant.unzip(dest:tmpPath,src:domainJar) {
                patternset() {
                    domainClasses.each{grailsClass->
                        include(name: grailsClass.name.replaceAll("\\.","/")+"*")
                    }                
                }
            }
        }
        File outDir = new File("${basedir}/grails-app/views/mxml")
        if(!outDir.exists()) {
            outDir.mkdirs()
        }
        Ant.gas3(outputdir: outDir, tide: "true", classpathref: "gas3.generate.classpath") {
            fileset(dir: genClassPath) {
                domainClasses.each{grailsClass->
                    include(name: grailsClass.name.replaceAll("\\.","/")+".class")
                }
            }
        }
    }
}

def mergeClasses(domainClasses) {
    def otherClassesMap = [:]
    domainClasses.each{grailsClass->
        Class idClazz = grailsClass.identifier.type
        if(!ClassUtils.isPrimitiveOrWrapper(idClazz)) {
            checkMap(otherClassesMap,idClazz)
        }
        grailsClass.persistentProperties.each{
            if(it.type&& !ClassUtils.isPrimitiveOrWrapper(it.type) &&
            it.type.isAnnotationPresent(javax.persistence.Embeddable.class) ) {
                checkMap(otherClassesMap,it.type)
            }
        }
        Class clazz = grailsClass.clazz
        while(clazz && clazz != Object.class) {
            checkMap(otherClassesMap,clazz)
            clazz = clazz.superclass
        }
    }
    return otherClassesMap.values() as List
}

private def checkMap(otherClassesMap,clazz) {
    if(!otherClassesMap.containsKey(clazz.name)) {
        otherClassesMap.put(clazz.name,clazz)
    }
}
def initGrailsApp() {
    def builder =  new WebBeanBuilder()
    beanDefinitions = builder.beans {
        resourceHolder(org.codehaus.groovy.grails.commons.spring.GrailsResourceHolder) {
            resources = GrailsPluginUtils.getArtefactResources(basedir, resolveResources)
        }
        grailsResourceLoader(org.codehaus.groovy.grails.commons.GrailsResourceLoaderFactoryBean) { grailsResourceHolder = resourceHolder }
        grailsApplication(org.codehaus.groovy.grails.commons.DefaultGrailsApplication, ref("grailsResourceLoader"))
        pluginMetaManager(DefaultPluginMetaManager) { grailsApplication = ref('grailsApplication') }
    }
    def appCtx =  beanDefinitions.createApplicationContext()
    def servletContext = new MockServletContext('web-app', new FileSystemResourceLoader())
    appCtx.servletContext = servletContext
    
    def grailsApp = appCtx.grailsApplication
    
    PluginManagerHolder.pluginManager = null
    loadPlugins()
    pluginManager = PluginManagerHolder.pluginManager
    pluginManager.application = grailsApp
    pluginManager.doArtefactConfiguration()
    grailsApp.initialise()
    def config = new org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator(grailsApp,appCtx)
    appCtx = config.configure(servletContext)
    return grailsApp
}
