import org.granite.tide.spring.security.Identity


graniteConfig {
    
    springSecurityAuthorizationEnabled = false
    springSecurityIdentityClass = Identity.class
    
    gravityEnabled = false
    gravityServletClassName = "org.granite.gravity.tomcat.GravityTomcatServlet"
    
    dataDispatchEnabled = false
    
}

as3Config {
    domainJar = null
    extraClasses = []
    autoCompileFlex = true
}
