import grails.util.Environment

import org.granite.webcompiler.WebCompiler
import org.granite.webcompiler.WebCompilerType

Ant.property(environment:"env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"

includeTargets << grailsScript("_GrailsCompile")
def configureFlexCompilation() {
	rootLoader.addURL(new File(classesDirPath).toURI().toURL())
    new File("${gdsflexPluginDir}/scripts/lib/compile").listFiles().each {
    	rootLoader?.addURL(it.toURI().toURL())
    }
	WebCompiler webCompiler = WebCompiler.getInstance()
    webCompiler.init("${basedir}/web-app/WEB-INF")
	return webCompiler
}

def compile(webCompiler,sourceDir,appName,mxmlFiles) {
    def appXmlList = []
    if(!mxmlFiles) {
        File root = new File(sourceDir)
        root.eachFileRecurse{ file->
            checkXmlList(appXmlList,file)
        }
    }else {
        mxmlFiles.each { 
        	def fileName = it
			if(!fileName.endsWith(".mxml")) {
				fileName += ".mxml"
			}
        	checkXmlList(appXmlList,new File(sourceDir+"/"+fileName)) 
			}
    }
    appXmlList.each { appXml->
        def file = appXml.file
        try {
            String sep = File.separator=="\\"?"\\\\":File.separator
            File swfDir = new File(file.parent.replaceAll("views${sep}flex","views${sep}swf"))
            if(!swfDir.exists()) {
                swfDir.mkdirs()
            }
            println "compiling file " + file.name
            webCompiler.compileMxmlFile(file, 
                    new File(swfDir,file.name.replaceAll("mxml\$","swf")),
                    true,appXml.type,appName)
        }catch(Exception ex) {
            println "error during compilation " + ex.getMessage()
        }
        println file.name + " compilation ended at: " + new Date()
    }
}

private def checkXmlList(appXmlList,file) {
    if(file.name.endsWith(".mxml")) {
        String content = file.text
        if(content.indexOf("</mx:Application>") != -1 ) {
            appXmlList.add([file:file,type:WebCompilerType.application])
        }else if(content.indexOf("</mx:Module>")!=-1){
            appXmlList.add([file:file,type:WebCompilerType.application])
        }
    }
}

target(flexCompile: "Compile the flex file to swf file") {
    depends(compilePlugins)
    WebCompiler webCompiler = configureFlexCompilation()
    if(Environment.current==Environment.DEVELOPMENT) {
        Ant.copy(tofile: "${basedir}/web-app/WEB-INF/flex/flex-config.xml",
                file:"${gdsflexPluginDir}/src/flex/flex-config-debug.xml")
    } else {
        Ant.copy(tofile: "${basedir}/web-app/WEB-INF/flex/flex-config.xml",
                file:"${gdsflexPluginDir}/src/flex/flex-config-release.xml",
                overwrite: true)
    }
    compile(webCompiler,"${basedir}/grails-app/views/flex",grailsAppName,argsMap["params"])
}
