import grails.util.Environment

import org.granite.web.util.WebCompilerWrapper

Ant.property(environment:"env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"

includeTargets << grailsScript("_GrailsCompile")
def configureFlexCompilation() {
	//rootLoader.addURL(new File(classesDirPath).toURI().toURL())
    WebCompilerWrapper.init("${basedir}/web-app/WEB-INF")
}

target(flexCompile: "Compile the flex file to swf file") {
    depends(compilePlugins)
    configureFlexCompilation()
    if(Environment.current==Environment.DEVELOPMENT) {
        Ant.copy(tofile: "${basedir}/web-app/WEB-INF/flex/flex-config.xml",
                file:"${gdsflexPluginDir}/src/flex/flex-config-debug.xml")
    } else {
        Ant.copy(tofile: "${basedir}/web-app/WEB-INF/flex/flex-config.xml",
                file:"${gdsflexPluginDir}/src/flex/flex-config-release.xml",
                overwrite: true)
    }
    WebCompilerWrapper.compile("${basedir}/grails-app/views/flex",grailsAppName,argsMap["params"])
}
