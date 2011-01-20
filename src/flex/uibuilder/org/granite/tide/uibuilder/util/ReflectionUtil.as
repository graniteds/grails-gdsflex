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

package org.granite.tide.uibuilder.util
{
	import flash.utils.getQualifiedClassName;
	
    
    public class ReflectionUtil {
        
        public static function getUpperCaseEntityName(entity:Object):String {
	        var entityName:String = getEntityName(entity);
        	return entityName.substring(0, 1).toUpperCase() + entityName.substring(1);
        }
        
        public static function getEntityName(entity:Object):String {
        	var className:String = entity is String ? entity as String : getQualifiedClassName(entity);
        	var entityName:String;
        	if (className.indexOf("::") > 0) {
        		entityName = className.substring(className.lastIndexOf("::")+2);
        		entityName = entityName.substring(0, 1).toLowerCase() + entityName.substring(1);
        	}
        	else
        		entityName = className.substring(0, 1).toLowerCase() + className.substring(1);
        	return entityName;
        }
        
        public static function getQualifiedEntityName(entity:Object):String {
        	var className:String = entity is String ? entity as String : getQualifiedClassName(entity);
        	var entityName:String = getEntityName(className);
        	var qualifiedEntityName:String;
        	if (className.indexOf("::") > 0)
        		qualifiedEntityName = className.substring(0, className.lastIndexOf("::")) + "." + entityName;
        	else
        		qualifiedEntityName = entityName;
        	return qualifiedEntityName;
        }
        
    }
}
