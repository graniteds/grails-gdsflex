/*
  GRANITE DATA SERVICES
  Copyright (C) 2007-2008 ADEQUATE SYSTEMS SARL

  This file is part of Granite Data Services.

  Granite Data Services is free software; you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation; either version 3 of the License, or (at your
  option) any later version.

  Granite Data Services is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
  for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this library; if not, see <http://www.gnu.org/licenses/>.
*/

package org.granite.grails.gas3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.granite.generator.Listener;
import org.granite.generator.as3.JavaAs3GroovyConfiguration;
import org.granite.generator.as3.JavaAs3GroovyTransformer;
import org.granite.generator.as3.reflect.JavaFieldProperty;
import org.granite.generator.as3.reflect.JavaImport;
import org.granite.generator.as3.reflect.JavaProperty;
import org.granite.generator.as3.reflect.JavaType;
import org.granite.generator.as3.reflect.JavaType.Kind;
import org.granite.util.ClassUtil;

/**
 * @author Franck WOLFF
 */
public class GrailsAs3GroovyTransformer extends JavaAs3GroovyTransformer {

	protected final Map<Class<?>, JavaType> javaTypes = new HashMap<Class<?>, JavaType>();
    protected final Map<Class<?>, JavaImport> javaImports = new HashMap<Class<?>, JavaImport>();

	public GrailsAs3GroovyTransformer() {
		super();
	}

	public GrailsAs3GroovyTransformer(JavaAs3GroovyConfiguration config, Listener listener) {
		super(config, listener);
	}

	@Override
	public JavaType getJavaType(Class<?> clazz) {
		JavaType javaType = javaTypes.get(clazz);
		if (javaType == null && getConfig().isGenerated(clazz)) {
			URL url = ClassUtil.findResource(clazz);
			Kind kind = getKind(clazz);
			switch (kind) {
			case ENTITY:
	            javaType = new GrailsDomainClass(this, clazz, url);
	            break;
	        default:
	        	javaType = super.getJavaType(clazz);
			}
	        javaTypes.put(clazz, javaType);
		}
		return javaType;
	}
	
	@Override
	public Kind getKind(Class<?> clazz) {
        if (clazz.isEnum())
            return Kind.ENUM;
        if (clazz.isInterface())
            return Kind.INTERFACE;
        boolean hasId = false;
        boolean hasVersion = false;
        for (Method m : clazz.getMethods()) {
        	if ("getId".equals(m.getName()))
        		hasId = true;
        	else if ("getVersion".equals(m.getName()))
        		hasVersion = true;
        }
        if (hasId && hasVersion)
            return Kind.ENTITY;
        return Kind.BEAN;
	}

	@Override
	public boolean isId(JavaFieldProperty fieldProperty) {
        Field field = fieldProperty.getMember();

        return "id".equals(field.getName());
	}

	@Override
	public boolean isUid(JavaProperty property) {
    	return getConfig().getUid() == null
			? "uid".equals(property.getName())
			: getConfig().getUid().equals(property.getName());
	}

	@Override
	public boolean isVersion(JavaProperty property) {
        return "version".equals(property.getName());
	}
}
