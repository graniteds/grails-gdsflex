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
