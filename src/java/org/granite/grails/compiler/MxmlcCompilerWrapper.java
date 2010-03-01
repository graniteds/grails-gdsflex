package org.granite.grails.compiler;

import grails.util.GrailsUtil;
import grails.util.BuildSettings;
import groovy.lang.GroovyClassLoader;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Properties;

/**
 * @author agile
 *
 */
public class MxmlcCompilerWrapper {
    
	public void compile(String sourcePath, String appName, ClassLoader parentClassLoader) {
        GroovyClassLoader classLoader = new GroovyClassLoader(parentClassLoader);
        
		String grailsHome = System.getProperty("grails.home");

        BuildSettings build = new BuildSettings(new File(grailsHome));
        
        try {
	        @SuppressWarnings("deprecation")
	        ConfigSlurper slurper = new ConfigSlurper(GrailsUtil.getEnvironment());
	        ConfigObject buildConfig = slurper.parse(classLoader.loadClass("BuildConfig"));
	        
	        String flexSDK = System.getenv("FLEX_HOME");
	        
	        Properties props = buildConfig.toProperties();
	        if (props.getProperty("flex.sdk") != null && props.getProperty("flex.sdk").toString().trim().length() > 0)
	        	flexSDK = props.getProperty("flex.sdk");
	        
	        System.setProperty("FLEX_HOME", flexSDK);
	        	
	        System.out.println("Compiling with Flex SDK " + flexSDK + " base dir " + build.getBaseDir().getPath());
	        
	        classLoader.addURL(new File(flexSDK + "/lib/mxmlc.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/xmlParserAPIs.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/xercesPatch.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/xercesImpl.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/xalan.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/asc.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/afe.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/aglj32.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/rideau.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/batik-awt-util.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/batik-bridge.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/batik-css.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/batik-dom.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/batik-ext.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/batik-gvt.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/batik-parser.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/batik-script.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/batik-svg-dom.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/batik-svggen.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/batik-util.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/batik-transcoder.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/batik-xml.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/mm-velocity-1.4.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/commons-collections.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/commons-discovery.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/commons-logging.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/license.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/swfutils.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/flex-fontkit.jar").toURI().toURL());
	        classLoader.addURL(new File(flexSDK + "/lib/flex-messaging-common.jar").toURI().toURL());
	        
	        Class<?> compilerClass = null;
	        try {
	        	compilerClass = Class.forName("flex2.tools.Mxmlc", true, classLoader);
	        }
	        catch (ClassNotFoundException f) {
	        	compilerClass = Class.forName("flex2.tools.Compiler", true, classLoader);
	        }
	        Class<?> threadLocalToolkitClass = Class.forName("flex2.compiler.util.ThreadLocalToolkit", true, classLoader);
	        ClassLoader loader = Thread.currentThread().getContextClassLoader();
	        Thread.currentThread().setContextClassLoader(classLoader);
	        
	        int errorCount = 0;
	        
           	Method compilerMethod = compilerClass.getMethod("mxmlc", String[].class);
           	String[] args = {
           		"+flexlib=" + flexSDK + "/frameworks",
           		"-output=" + build.getBaseDir().getPath() + "/grails-app/views/swf/" + appName + ".swf",
            	"-services=" + build.getBaseDir().getPath() + "/web-app/WEB-INF/flex/services-config.xml",
            	"-context-root=/" + appName,
            	// "-load-config=" + flexSDK + "/frameworks/flex-config.xml",
            	"-load-config+=" + build.getBaseDir().getPath() + "/web-app/WEB-INF/flex/flex-config-debug.xml",
            	build.getBaseDir().getPath() + "/" + sourcePath + "/" + appName + ".mxml"
           	};
            compilerMethod.invoke(null, (Object)args);

            Method errorCountMethod = threadLocalToolkitClass.getMethod("errorCount");
            errorCount = (Integer)errorCountMethod.invoke(null);
            
            Thread.currentThread().setContextClassLoader(loader);
	        
	        if (errorCount > 0)
	        	System.err.println("Flex compilation failed");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
