package org.granite.web.util

import org.granite.webcompiler.WebCompiler
import org.granite.webcompiler.WebCompilerException
import org.granite.webcompiler.WebCompilerType

public class WebCompilerWrapper {
    private static WebCompiler webCompiler = WebCompiler.getInstance()
    private static def appXmlList = []
    static def init(basePath) {
        webCompiler.init(basePath)
    }
    
    static def compile(sourceDir,appName) {
        if(appXmlList.size()==0) {
            File root = new File(sourceDir)
            root.eachFileRecurse{ file->
                if(file.name.endsWith(".mxml") && 
                file.text.indexOf("<mx:Application ") != -1 ) {
                    appXmlList.add(file)
                }
            }
        }
        appXmlList.each { file->
            try {
                String sep = File.separator=="\\"?"\\\\":File.separator
                File swfDir = new File(file.parent.replaceAll("views${sep}mxml","views${sep}swf"))
                if(!swfDir.exists()) {
                    swfDir.mkdirs()
                }
                webCompiler.compileMxmlFile(file, 
                        new File(swfDir,file.name.replaceAll("mxml\$","swf")),
                        true,WebCompilerType.application,"/${appName}")
            }catch(WebCompilerException ex) {
            }
        }
    }
}
