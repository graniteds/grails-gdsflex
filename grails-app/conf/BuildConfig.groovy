grails.servlet.version = '3.0'

grails.project.repos.mavenLocal.url = '~/.m2/repository'

grails.project.repos.default = 'sonatypeoss'

grails.project.dependency.resolution = {
    inherits "global" // inherit Grails' default dependencies
    log "warn" // log level of Ivy resolver, either 'error',
               // 'warn', 'info', 'debug' or 'verbose'
	
    repositories {
		grailsCentral()
		
		mavenLocal()
        mavenCentral()
		
		mavenRepo 'https://oss.sonatype.org/content/repositories/snapshots/'		
    }
	
    dependencies {
		compile 'org.graniteds:granite-server:3.1.1-SNAPSHOT'
		compile 'org.graniteds:granite-server-beanvalidation:3.1.1-SNAPSHOT'
		compile 'org.graniteds:granite-server-datanucleus:3.1.1-SNAPSHOT'
		compile 'org.graniteds:granite-server-hibernate4:3.1.1-SNAPSHOT'
		compile 'org.graniteds:granite-server-spring:3.1.1-SNAPSHOT'
		compile 'org.graniteds:granite-server-appengine:3.1.1-SNAPSHOT'
		compile 'org.graniteds:granite-generator-share:3.1.1-SNAPSHOT'
		compile 'org.graniteds.grails:granite-grails-generator:2.0.0-SNAPSHOT'
		compile 'org.graniteds.grails:granite-grails-springsecurity:2.0.0-SNAPSHOT'
		compile 'javax.interceptor:javax.interceptor-api:1.2'
		compile 'javax.jdo:jdo2-api:2.3-eb'
		provided 'org.hibernate:hibernate-core:4.3.6.Final'
		provided 'org.springframework:spring-orm:4.0.6.RELEASE'
		provided 'org.springframework.security:spring-security-web:3.2.5.RELEASE'
		build 'org.apache.commons:commons-io:1.3.2'
    }
	
	plugins {		
		build ":release:3.0.1"
    }
}