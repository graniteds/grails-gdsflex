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


public class FlexCompilerWrapper {

    private static Object flexCompiler
       
       
    static def init(flexSDK, basedir, pluginDir, sourceDir, modules, appName, loader = Thread.currentThread().getContextClassLoader()) {
    	if (flexCompiler == null) {
	 		Class compilerClass = loader.loadClass("FlexCompiler")
	 		java.lang.reflect.Constructor compilerCons = compilerClass.getConstructor(Object.class, Object.class, Object.class, Object.class, Object.class, Object.class)
	 		flexCompiler = compilerCons.newInstance(flexSDK, basedir, pluginDir, sourceDir, modules, appName)
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
