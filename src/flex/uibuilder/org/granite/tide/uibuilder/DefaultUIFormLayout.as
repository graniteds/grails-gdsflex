/**
 * Generated by Gas3 v2.0.0 (Granite Data Services).
 *
 * NOTE: this file is only generated if it does not exist. You may safely put
 * your custom code here.
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