grails.project.repos.default = 'sonatypeoss'

grails.project.dependency.resolver = 'maven'
grails.project.dependency.resolution = {

	inherits 'global'
	log 'warn'

	repositories {
		grailsCentral()
		mavenLocal()
		mavenCentral()
		mavenRepo 'https://oss.sonatype.org/content/repositories/snapshots/'
	}

	dependencies {
		String granitedsVersion = '3.1.1.GA'
		String granitedsGrailsVersion = '2.0.0.GA'
		compile "org.graniteds:granite-server:$granitedsVersion"
		compile "org.graniteds:granite-server-beanvalidation:$granitedsVersion"
		compile "org.graniteds:granite-server-datanucleus:$granitedsVersion"
		compile "org.graniteds:granite-server-hibernate4:$granitedsVersion"
		compile "org.graniteds:granite-server-spring:$granitedsVersion"
		compile "org.graniteds:granite-server-appengine:$granitedsVersion"
		compile "org.graniteds:granite-generator-share:$granitedsVersion"
		compile "org.graniteds.grails:granite-grails-generator:$granitedsGrailsVersion"
		compile "org.graniteds.grails:granite-grails-springsecurity:$granitedsGrailsVersion"
		compile 'javax.interceptor:javax.interceptor-api:1.2'
		compile 'javax.jdo:jdo2-api:2.3-eb'
		provided 'org.hibernate:hibernate-core:4.3.6.Final'
		provided 'org.springframework:spring-orm:4.0.6.RELEASE'
		provided 'org.springframework.security:spring-security-web:3.2.5.RELEASE'
		build 'org.apache.commons:commons-io:1.3.2'
	}

	plugins {
		build ':release:3.0.1', ':rest-client-builder:2.0.3', {
			export = false
		}
	}
}
