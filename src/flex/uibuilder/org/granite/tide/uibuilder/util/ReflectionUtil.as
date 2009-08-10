/*
  GRANITE DATA SERVICES
  Copyright (C) 2009 ADEQUATE SYSTEMS SARL

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

package org.granite.tide.uibuilder.util
{
	import flash.utils.getQualifiedClassName;
	
    
    public class ReflectionUtil {
        
        public static function getUpperCaseEntityName(entity:Object):String {
        	var className:String = getQualifiedClassName(entity);
        	var entityName:String = className.substring(className.lastIndexOf("::")+2).toLowerCase();
        	return entityName.substring(0, 1).toUpperCase() + entityName.substring(1);
        }
        
    }
}
