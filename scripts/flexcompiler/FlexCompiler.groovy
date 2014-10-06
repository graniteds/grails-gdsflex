/*
  GRANITE DATA SERVICES
  Copyright (C) 2011 GRANITE DATA SERVICES S.A.S.

  This file is part of Granite Data Services.

  Granite Data Services is free software; you can redistribute it and/or modify
  it under the terms of the GNU Library General Public License as published by
  the Free Software Foundation; either version 2 of the License, or (at your
  option) any later version.

  Granite Data Services is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE. See the GNU Library General Public License
  for more details.

  You should have received a copy of the GNU Library General Public License
  along with this library; if not, see <http://www.gnu.org/licenses/>.
*/

import java.io.File
import java.io.FilenameFilter
import java.io.IOException
import java.util.ArrayList
import java.util.List
import java.util.ResourceBundle

import org.apache.log4j.Logger

import flex2.tools.oem.Project
import flex2.tools.oem.Application
import flex2.tools.oem.Builder
import flex2.tools.oem.Configuration
import flex2.tools.oem.Library
import flex2.tools.oem.Message
import flex2.tools.oem.VirtualLocalFileSystem
import flex2.tools.oem.VirtualLocalFile
import flex2.tools.oem.PathResolver;


/**
 * Flex compiler main manager
 */
public class FlexCompiler {

    private Logger logger = Logger.getLogger(FlexCompiler.class)
    
    private VirtualLocalFileSystem fs
    private Project project
    private Application application
    
    private String flexSDK
    private String basedir
    private String pluginDir
    private String sourceDir
    private String[] modules
    private String targetDir
    private String appName

    
    public FlexCompiler(flexSDK, basedir, pluginDir, sourceDir, modules, appName) {
    	println "Initialize Flex project (source: ${sourceDir}, app: ${appName}, modules: ${modules})"
    	
    	fs = new VirtualLocalFileSystem()
    	
    	project = new Project()
    	
    	this.flexSDK = flexSDK
    	this.basedir = basedir
    	this.pluginDir = pluginDir
    	this.sourceDir = sourceDir
    	this.modules = modules
    	this.targetDir = "${basedir}/grails-app/views/swf"
    	this.appName = appName
		
		File flexSDKDescription = new File("${flexSDK}/flex-sdk-description.xml")		
		def root = new XmlParser().parseText(flexSDKDescription.getText())
		def flexVersion = root.version.text()
		def idx = flexVersion.indexOf(".")
		def idx2 = flexVersion.indexOf(".", idx+1)
		def major = Integer.parseInt(flexVersion.substring(0, idx))
		def minor = Integer.parseInt(flexVersion.substring(idx+1, idx2))
		
    	File outputDir = new File(targetDir)
    	if (!outputDir.exists())
    		outputDir.mkdirs()
    	
    	File source = new File(sourceDir)
    	def appFile = new File(source, "${appName}.mxml") 
        def componentList = [ fs.create(appFile.canonicalPath, appFile.text, source, appFile.lastModified()) ]
        /*
        source.eachFileRecurse { file ->
        	if (file.isFile() && file.canonicalPath != appFile.canonicalPath && (file.name.endsWith(".mxml") || file.name.endsWith(".as")))
        		componentList.add(fs.create(file.canonicalPath, file.text, source, file.lastModified()))
        }
        */
        
        File outputFile = new File(outputDir, appFile.name.replaceAll("\\.mxml", ".swf"))
        
        File savedDataFile = new File("${basedir}/target/flex-${appName}.dat")
        
        application = new Application(componentList.toArray() as VirtualLocalFile[])
    	Configuration configuration = application.getDefaultConfiguration()
    	configure(configuration)
    	application.setConfiguration(configuration)
        application.setOutput(outputFile)
        if (savedDataFile.exists()) {
        	println "Load existing compilation data"
        	application.load(new FileInputStream(savedDataFile))
        }
        
        println "App config: ${application.configuration}"
        
        println "Adding Flex application to project: ${appFile.name} -> ${outputFile.name}" 
        project.addBuilder(application)
        
        for (module in modules) {
        	File moduleFile = new File(source, module)
        	VirtualLocalFile moduleVLF = fs.create(moduleFile.canonicalPath, moduleFile.text, source, moduleFile.lastModified())
	        File outFile = new File(outputDir, module.substring(0, module.lastIndexOf(".")) + ".swf")
	        	        
        	Application moduleApp = new Application([ moduleVLF ] as VirtualLocalFile[])
    		Configuration config = moduleApp.getDefaultConfiguration()
    		configure(config)
    		moduleApp.setConfiguration(config)
        	moduleApp.setOutput(outFile)
        	
        	println "Adding Flex module/asset to project: ${moduleFile.name} -> ${outFile.name}"
        	project.addBuilder(moduleApp)
        }
    }

    
    protected void configure(Configuration configuration) throws FlexCompilerException {
		
		println "Plugin dir: ${pluginDir}"
    
        configuration.includeLibraries([new File("${pluginDir}/src/flex/libs/granite-client-flex.swc")] as File[])
    	configuration.addLibraryPath([new File("${pluginDir}/src/flex/libs/granite-client-flex45-advanced.swc")] as File[])
        File file = new File("${basedir}/web-app/WEB-INF/flex/libs")
        if (file.exists())
        	configuration.addLibraryPath([file] as File[])
        file = new File("${basedir}/flex_libs")
        if (file.exists())
        	configuration.addLibraryPath([file] as File[])
        
        configuration.addSourcePath([new File(sourceDir)] as File[])
        
        configuration.addActionScriptMetadata(["Name", "In", "Inject", "Out", "Produces", "Observer", "ManagedEvent", "PostConstruct", "Destroy", "Path", "Id", "Version", "Lazy"] as String[])
        
        // configuration.setServiceConfiguration(new File("${basedir}/web-app/WEB-INF/flex/services-config.xml"))
        
        configuration.setContextRoot("/${appName}")
    }
    
    
    /**
     * Compile the Flex project 
     */
    public void compile(configFile) throws IOException, FlexCompilerException {
    	long timer = System.currentTimeMillis()
    	
    	project.build(true)
        
        File savedDataFile = new File("${basedir}/target/flex-${appName}.dat")
        application.save(new FileOutputStream(savedDataFile))
        
    	println "Full compilation of Flex project in ${(System.currentTimeMillis()-timer)/1000}s"
    }
    
    public void incrementalCompile(file) {
    	long timer = System.currentTimeMillis()
    	
    	fs.update(file.canonicalPath, file.text, file.lastModified())
    	
    	project.build(true)
    	
        File savedDataFile = new File("${basedir}/target/flex-${appName}.dat")
        application.save(new FileOutputStream(savedDataFile))
        
    	println "Incremental compilation of Flex project in ${(System.currentTimeMillis()-timer)/1000}s"
    }
}
