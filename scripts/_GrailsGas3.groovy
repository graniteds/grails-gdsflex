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
import java.lang.reflect.Method
import javax.persistence.*


Ant.property(environment:"env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"

tmpPath = System.properties."java.io.tmpdir"+"/gdsflex-tmp/"
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
    def genClassPath =  tmpPath
    Ant.path(id: "gas3.generate.classpath") { path(location: genClassPath) }
    
    def grailsApp = initGrailsApp()
    def domainClasses = grailsApp.getArtefacts('Domain') as List
    if (domainClasses.size()>0) {
        domainClasses = mergeClasses(domainClasses)
        if(domainJar)  {
            Ant.mkdir(dir:tmpPath)
            Ant.unzip(dest:tmpPath,src:domainJar) {
                patternset() {
                    domainClasses.each{domainClass->
                        include(name: domainClass.name.replaceAll("\\.","/")+"*")
                    }                
                }
            }
        }
        domainClasses.each {domainClass->
            String fullName = domainClass.name.replaceAll("\\.","/")+".class"
            ClassWriter cw = new ClassWriter(true)
            ClassReader cr = new ClassReader(new FileInputStream("${classesDirPath}/${fullName}"))
            EntityAnnotationAdapter cp = new EntityAnnotationAdapter(cw,domainClass);
            cr.accept(cp,false)
            int idx = fullName.lastIndexOf("/")
            if(idx!=-1) {
                File f = new File(tmpPath+fullName[0..idx])
                if(!f.exists()) {
                    f.mkdirs()
                }
            }
            new File(tmpPath+fullName).withOutputStream{os->os.write(cw.toByteArray())}
        }
        File outDir = new File("${basedir}/grails-app/views/mxml")
        if(!outDir.exists()) {
            outDir.mkdirs()
        }
        Ant.gas3(outputdir: outDir, tide: "true", classpathref: "gas3.generate.classpath") {
            fileset(dir: genClassPath) {
                domainClasses.each{domainClass->
                    include(name: domainClass.name.replaceAll("\\.","/")+".class")
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
            it.type.isAnnotationPresent(Embeddable.class) ) {
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

public class EntityAnnotationAdapter extends ClassAdapter {
    private final static String ENTITY =  Type.getDescriptor(Entity.class)
    private final static String ID = Type.getDescriptor(Id.class)
    private final static String VERSION = Type.getDescriptor(Version.class)
    private boolean isAnnotationPresent = false
    private Class clazz
    public EntityAnnotationAdapter(ClassVisitor cv,Class clazz) {
        super(cv)
        this.clazz= clazz
    }
    public void visit(int version, int access, String name,
    String signature, String superName, String[] interfaces) {
        int v = (version & 0xFF) < Opcodes.V1_5 ? Opcodes.V1_5 : version;
        cv.visit(v, access, name, signature, superName, interfaces);
    }
    
    public AnnotationVisitor visitAnnotation(String desc,boolean visible) {
        if (visible && desc==ENTITY) {
            isAnnotationPresent = true
        }
        return cv.visitAnnotation(desc, visible)
    }
    public void visitEnd() {
        if(!isAnnotationPresent) {
            createAnnotation(cv.visitAnnotation(ENTITY, true))
            isAnnotationPresent = true
        }
        cv.visitEnd()
    }
    public MethodVisitor visitMethod(int access, String name,String desc, String signature,String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions)
        if("getId"==name) {
            Method m = clazz.getMethod(name)
            if(m.getAnnotation(Id.class)==null)
                createAnnotation(mv.visitAnnotation(ID, true))
        }else if("getVersion"==name) {
            Method m = clazz.getMethod(name)
            if(m.getAnnotation(Version.class)==null)
                createAnnotation(mv.visitAnnotation(VERSION, true))
        }
        return mv
    }
    private def createAnnotation(AnnotationVisitor av) {
        if (av != null) {
            av.visitEnd();
        }
    }
}
