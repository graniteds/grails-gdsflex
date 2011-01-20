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

package org.granite.config

import grails.util.GrailsUtil
/**
 * @author agile
 *
 */
public class GraniteConfigUtil{
    
    public static ConfigObject getUserConfig(parentClassLoader = Thread.currentThread().contextClassLoader) {
        GroovyClassLoader classLoader = new GroovyClassLoader(parentClassLoader)
        
        def slurper = new ConfigSlurper(GrailsUtil.environment)
        ConfigObject userConfig
        try {
            userConfig = slurper.parse(classLoader.loadClass('GraniteDSConfig'))
        }catch (e) {
        }
        
        ConfigObject config
        ConfigObject defaultConfig = slurper.parse(classLoader.loadClass('DefaultGraniteDSConfig'))
        if (userConfig) {
            config = defaultConfig.merge(userConfig)
        }
        else {
            config = defaultConfig
        }
        
        return config
        
    }
    
    public static ConfigObject getBuildConfig(parentClassLoader = Thread.currentThread().contextClassLoader) {
        GroovyClassLoader classLoader = new GroovyClassLoader(parentClassLoader)
        
        def slurper = new ConfigSlurper(GrailsUtil.environment)
        ConfigObject buildConfig
        try {
            buildConfig = slurper.parse(classLoader.loadClass('BuildConfig'))
        }catch (e) {
        }
        
        return buildConfig
        
    }
    
}
