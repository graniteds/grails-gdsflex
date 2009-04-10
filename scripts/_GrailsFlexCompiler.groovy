
Ant.property(environment:"env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"

GroovyClassLoader loader = new GroovyClassLoader(rootLoader)
Class groovyClass = loader.parseClass(new File("${gdsflexPluginDir}/src/groovy/org/granite/web/util/WebCompile.groovy"))
GroovyObject groovyObject = (GroovyObject) groovyClass.newInstance()

groovyObject.init("${basedir}/web-app/WEB-INF")

target(flexCompile: "Compile the mxml to swf file") {
    groovyObject.compile("${basedir}/grails-app/views/mxml",grailsAppName)
}
