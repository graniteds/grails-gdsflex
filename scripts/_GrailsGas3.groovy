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

tmpPath = System.properties."java.io.tmpdir"+"/gdsflex-tmp"
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

isInjectClass = false
target(gas3: "Gas3") {
    def domainJar = as3Config.domainJar
    def extraClasses = as3Config.extraClasses
    def genClassPath =  domainJar?tmpPath:classesDirPath
    Ant.path(id: "gas3.generate.classpath") { path(location: genClassPath) }
    
    def grailsApp = initGrailsApp()
    def domainClasses = grailsApp.getArtefacts('Domain') as List
    def embedDomainClasses = [:]
    Ant.mkdir(dir:tmpPath)
    if (domainClasses.size()>0) {
        domainClasses = mergeClasses(domainClasses,extraClasses,embedDomainClasses)
        if(domainJar)  {
            Ant.unzip(dest:tmpPath,src:domainJar) {
                patternset() {
                    domainClasses.each{domainClass->
                        include(name: domainClass.name.replaceAll("\\.","/")+"*")
                    }                
                }
            }
        }else {            
            def cl = new URLClassLoader([classesDir.toURI().toURL()] as URL[], rootLoader)
            domainClasses.each {domainClass->
                String fullName = domainClass.name.replaceAll("\\.","/")+".class"
                checkDir(fullName,genClassPath)
                File src = new File("${classesDirPath}/${fullName}")
                File target = new File("${genClassPath}/${fullName}")
                def newDomainClass = cl.loadClass(domainClass.name)
                if(!target.exists() ||!isEntityAnnoation(newDomainClass)
                ||target.lastModified()<src.lastModified()) {
                    genClassWithInject(src,target,newDomainClass,embedDomainClasses)
                    isInjectClass = true
                }
            }
        }
        File outDir = new File("${basedir}/grails-app/views/flex")
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

def genClassWithInject(src,target,domainClass,embedDomainClasses) {
    ClassWriter cw = new ClassWriter(true)
    ClassReader cr = new ClassReader(new FileInputStream(src))
    
    EntityAnnotationAdapter cp = null
    if(embedDomainClasses.containsKey(domainClass.name)) {
        cp = new EntityAnnotationAdapter(cw,domainClass,Embeddable.class);
    }else {
        cp = new EntityAnnotationAdapter(cw,domainClass,Entity.class,[getId:Id.class,getVersion:Version.class]);
    }
    cr.accept(cp,false)
    target.withOutputStream{os->os.write(cw.toByteArray())}
    
}
def isEntityAnnoation(cls) {
    return cls.isAnnotationPresent(Embeddable.class) ||
    cls.isAnnotationPresent(Entity.class) ||
    cls.isAnnotationPresent(MappedSuperclass.class)
}
def checkDir(fullName,tempPath) {
    int idx = fullName.lastIndexOf("/")
    if(idx!=-1) {
        File f = new File("${tempPath}/${fullName[0..idx]}")
        if(!f.exists()) {
            f.mkdirs()
        }
    }
}
def mergeClasses(domainClasses,extraClasses,embedDomainClasses) {
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
            if(it.isEmbedded() && !embedDomainClasses.containsKey(it.type.name)) {
                embedDomainClasses.put(it.type.name,it.type)
            }
        }
        Class clazz = grailsClass.clazz
        while(clazz && clazz != Object.class) {
            checkMap(otherClassesMap,clazz)
            clazz = clazz.superclass
        }
    }
    extraClasses?.each{className->
        checkMap(otherClassesMap,ClassUtils.forName(className))
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
    private final static def ENTITIES = [Type.getDescriptor(Entity.class),
    Type.getDescriptor(Embeddable.class),
    Type.getDescriptor(MappedSuperclass.class)]
    private boolean isAnnotationPresent = false
    private Class clazz
    private final String annName
    private def annMethods = [:]
    public EntityAnnotationAdapter(ClassVisitor cv,Class clazz,annClass,annMethods=[:]) {
        super(cv)
        this.clazz= clazz
        this.annName = Type.getDescriptor(annClass)
        this.annMethods = annMethods
    }
    public void visit(int version, int access, String name,
    String signature, String superName, String[] interfaces) {
        int v = (version & 0xFF) < Opcodes.V1_5 ? Opcodes.V1_5 : version;
        cv.visit(v, access, name, signature, superName, interfaces);
    }
    
    public AnnotationVisitor visitAnnotation(String desc,boolean visible) {
        if (visible && ENTITIES.contains(desc)) {
            isAnnotationPresent = true
        }
        return cv.visitAnnotation(desc, visible)
    }
    public void visitEnd() {
        if(!isAnnotationPresent) {
            createAnnotation(cv.visitAnnotation(annName, true))
            isAnnotationPresent = true
        }
        cv.visitEnd()
    }
    public MethodVisitor visitMethod(int access, String name,String desc, String signature,String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions)
        if(annMethods.containsKey(name)) {
            Method m = clazz.getMethod(name)
            Class cls = annMethods.get(name)
            if(m.getAnnotation(cls)==null)
                createAnnotation(mv.visitAnnotation(Type.getDescriptor(cls), true))
        }
        return mv
    }
    private def createAnnotation(AnnotationVisitor av) {
        if (av != null) {
            av.visitEnd();
        }
    }
}
