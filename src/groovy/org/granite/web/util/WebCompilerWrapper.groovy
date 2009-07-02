package org.granite.web.util

import java.io.File
import org.granite.webcompiler.WebCompiler
import org.granite.webcompiler.WebCompilerException
import org.granite.webcompiler.WebCompilerType

public class WebCompilerWrapper {

    private static WebCompiler webCompiler = WebCompiler.getInstance()
    
    
    static def init(basePath) {
    	if (!("".equals(basePath)))
    		webCompiler.init(basePath)
    	else if (new File("web-app/WEB-INF").exists())
	        webCompiler.init("web-app/WEB-INF")
    	else if (new File("WEB-INF").exists())
    		webCompiler.init("WEB-INF")
    }
     
     static def compile(sourceDir,appName,mxmlFiles=null) {
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
    
    private static def checkXmlList(appXmlList,file) {
        if(file.name.endsWith(".mxml")) {
            String content = file.text
            if(content.indexOf("</mx:Application>") != -1 ) {
                appXmlList.add([file:file,type:WebCompilerType.application])
            }else if(content.indexOf("</mx:Module>")!=-1){
                appXmlList.add([file:file,type:WebCompilerType.application])
            }
        }
    }
    
    
}
