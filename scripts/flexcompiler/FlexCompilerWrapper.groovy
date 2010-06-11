import java.io.File


public class FlexCompilerWrapper {

    private static Object flexCompiler
       
       
    static def init(flexSDK, basedir, pluginDir, sourceDir, appName, loader = Thread.currentThread().getContextClassLoader()) {
    	if (flexCompiler == null) {
	 		Class compilerClass = loader.loadClass("FlexCompiler")
	 		java.lang.reflect.Constructor compilerCons = compilerClass.getConstructor(Object.class, Object.class, Object.class, Object.class, Object.class)
	 		flexCompiler = compilerCons.newInstance(flexSDK, basedir, pluginDir, sourceDir, appName)
	 	}
    }
    
    
    static def compile(configFile) {
    	if (!flexCompiler) {
    		println "ERROR: Flex project not initialized"
    		return
    	}
	    	
    	flexCompiler.compile(configFile)
    }
    
    static def incrementalCompile(file) {
    	if (!flexCompiler) {
    		println "ERROR: Flex project not initialized"
    		return
    	}
	    	
    	flexCompiler.incrementalCompile(file)
    }
        
}
