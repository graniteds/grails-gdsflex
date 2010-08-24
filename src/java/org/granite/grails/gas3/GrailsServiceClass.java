package org.granite.grails.gas3;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.granite.generator.as3.reflect.JavaMethod;
import org.granite.generator.as3.reflect.JavaRemoteDestination;
import org.granite.generator.as3.reflect.JavaTypeFactory;
import org.granite.generator.as3.reflect.JavaMethod.MethodType;
import org.granite.messaging.service.annotations.IgnoredMethod;


public class GrailsServiceClass extends JavaRemoteDestination {


    public GrailsServiceClass(JavaTypeFactory provider, Class<?> type, URL url) {
    	super(provider, type, url);
    }
    
    @Override
	protected List<JavaMethod> initMethods() {
		List<JavaMethod> methodMap = new ArrayList<JavaMethod>();
		
		// Get all methods for interfaces: normally, even if it is possible in Java
		// to override a method into a inherited interface, there is no meaning
		// to do so (we just ignore potential compilation issues with generated AS3
		// classes for this case since it is always possible to remove the method
		// re-declaration in the child interface).
		Method[] methods = type.getMethods();
		
		for (Method method : methods) {
			if (Modifier.isPublic(method.getModifiers()) &&
				!Modifier.isStatic(method.getModifiers()) &&
				!method.isAnnotationPresent(IgnoredMethod.class)) {
				
				if (method.getName().startsWith("super$1$") || method.getName().startsWith("this$dist$"))
					continue;
				if (method.getName().equals("getProperty") || method.getName().equals("setProperty") || method.getName().equals("invokeMethod"))
					continue;
				if (method.getName().equals("getMetaClass") || method.getName().equals("setMetaClass"))
					continue;
				
				for (Class<?> clazz : method.getParameterTypes()) {
					if (clazz.isMemberClass() && !clazz.isEnum()) {
						throw new UnsupportedOperationException(
							"Inner classes are not supported (except enums): " + clazz
						);
					}
					addToImports(provider.getJavaImport(clazz));
				}

				methodMap.add(new JavaMethod(method, MethodType.OTHER, this.provider));
			}
		}

		return methodMap;
	}
}
