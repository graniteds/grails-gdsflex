import org.granite.tide.spring.security.Identity


graniteConfig {
    
    gravityEnabled = false
    gravityServletClassName = "org.granite.gravity.tomcat.GravityTomcatServlet"
    
    dataDispatchEnabled = false
    
}

as3Config {
    domainJar = null
    extraClasses = []
    generateServices = true
    autoCompileFlex = true
}
