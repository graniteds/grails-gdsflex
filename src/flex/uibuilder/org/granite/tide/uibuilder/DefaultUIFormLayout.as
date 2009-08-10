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

package org.granite.tide.uibuilder {
	
	import flash.display.DisplayObject;
	import flash.display.DisplayObjectContainer;
	
	import mx.containers.FormItem;
	import mx.core.Container;
	

	[Name("tideUIFormLayout")]
    public class DefaultUIFormLayout implements IUIFormLayout {
    	
    	public function layoutEditForm(entityForm:DisplayObjectContainer, properties:Array):void {
			for each (var property:EntityProperty in properties) {
            	var formItem:FormItem = new FormItem();
            	formItem.required = property.validator && property.validator.required;
            	formItem.label = property.property.substring(0, 1).toUpperCase() + property.property.substring(1);
            	if (property.percentWidth >= 0)
            		formItem.percentWidth = property.percentWidth;
        		formItem.addChild(property.component as DisplayObject);
            	entityForm.addChild(formItem);
			}
    	}
    }
}