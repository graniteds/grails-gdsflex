import grails.util.Environment

Ant.property(environment:"env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"

includeTargets << grailsScript("_GrailsCompile")
includeTargets << new File("${gdsflexPluginDir}/scripts/_FlexCommon.groovy")


def configureFlexCompilation() {
	rootLoader?.addURL(new File(classesDirPath).toURI().toURL())
    rootLoader?.addURL(new File("${flexSDK}/ant/lib/flexTasks.jar").toURI().toURL())
    
    Ant.taskdef(name: "mxmlc", classname: "flex.ant.MxmlcTask")
    
    Ant.property(name:"FLEX_HOME", value:"${flexSDK}")
}

target(flexCompile: "Compile the Flex file to SWF file") {
    depends(compilePlugins)
    
    configureFlexCompilation()
    
    String flexConfig = null;
    if (Environment.current == Environment.DEVELOPMENT) {
    	flexConfig = "${gdsflexPluginDir}/src/flex/flex-config-debug.xml"
        debug = true
    } 
    else {
    	flexConfig = "${gdsflexPluginDir}/src/flex/flex-config-release.xml"
    }
    
    Ant."mxmlc"(file: "${basedir}/grails-app/views/flex/${grailsAppName}.mxml",
    		output: "${basedir}/grails-app/views/swf/${grailsAppName}.swf",
    		services: "${gdsflexPluginDir}/src/flex/services-config.xml",
    		"context-root": "/${grailsAppName}") {
    	
    	"load-config"(filename: "${flexSDK}/frameworks/flex-config.xml")
    	"load-config"(filename: flexConfig)
    }
}
