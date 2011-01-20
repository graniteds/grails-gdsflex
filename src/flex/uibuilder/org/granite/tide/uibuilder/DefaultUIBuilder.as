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

package org.granite.tide.uibuilder {
	
	import flash.utils.ByteArray;
	import flash.utils.describeType;
	import flash.utils.getQualifiedClassName;
	import flash.utils.getQualifiedSuperclassName;
	import flash.utils.getDefinitionByName;
	import flash.net.FileReference;
	
	import mx.collections.ArrayCollection;
	import mx.controls.*;
	import mx.controls.dataGridClasses.DataGridColumn;
	import mx.core.ClassFactory;
	import mx.core.IUIComponent;
	import mx.formatters.Formatter;
	import mx.formatters.DateFormatter;
	import mx.formatters.NumberFormatter;
	import mx.utils.ObjectUtil;
	import mx.utils.StringUtil;
	import mx.validators.*;
	
	import org.granite.tide.spring.Context;
	import org.granite.tide.uibuilder.editors.*;
	
	
	[Name("tideUIBuilder")]
    public class DefaultUIBuilder implements IUIBuilder {
    	
    	[In]
    	public var context:Context;
    	
    	private var _labels:Object = new Object();
    	private var _formatters:Object = new Object();
    	
    	
    	public function buildListColumns(className:String, metadata:Array, simpleOnly:Boolean = false):Array {
			var columns:Array = new Array();
			
			if (!simpleOnly) {
				var column:DataGridColumn = new DataGridColumn("id");
				column.headerText = "Id";
				columns.push(column);
			}
			
			for each (var property:Object in metadata) {
				if (property.display == false)
					continue;
				
				_labels[className + "." + property.name] = property.label;
				
				if (property.kind == 'simple') {
					column = new DataGridColumn(property.name);
					column.headerText = property.name.substring(0, 1).toUpperCase() + property.name.substring(1);
					if (property.type == Number) {
						if (property.format) {
							var nfmt:NumberFormatter = new NumberFormatter();
							nfmt.precision = new Number(nfmt);
							_formatters[className + "." + property.name] = nfmt;
							column.labelFunction = format;
						}
					}
					if (property.type == Date) {
						if (property.format) {
							var dfmt:DateFormatter = new DateFormatter();
							dfmt.formatString = property.format;
							_formatters[className + "." + property.name] = dfmt;
							column.labelFunction = format;
						}
					}
					columns.push(column);
            	}
            	else if (property.kind == 'manyToOne' && !simpleOnly) {
					column = new DataGridColumn(property.name);
					column.headerText = property.name.substring(0, 1).toUpperCase() + property.name.substring(1);
					column.labelFunction = elementLabel;
					var cf:ClassFactory = new ClassFactory(EntityLinkButton);
					cf.properties = { context: context };
					column.itemRenderer = cf;
					columns.push(column);
            	}
			}
			
			return columns;
    	}
    	
    	protected function format(item:Object, column:DataGridColumn):String {
    		if (item == null || item[column.dataField] == null)
    			return "";
    		var className:String = getQualifiedClassName(item);
    		var formatter:Formatter = _formatters[className + "." + column.dataField];
    		while (!formatter && className != 'Object') {
    			var clazz:Class = getDefinitionByName(className) as Class;
    			className = getQualifiedSuperclassName(clazz);
    			formatter = _formatters[className + "." + column.dataField];
    		}
    		if (formatter)
    			return formatter.format(item[column.dataField]);
    		return item[column.dataField].toString();
    	}
		
		protected function elementLabel(item:Object, column:DataGridColumn):String {
			var className:String = getQualifiedClassName(item);
			var label:String = _labels[className + "." + column.dataField];
			while (!label && className != 'Object') {
    			var clazz:Class = getDefinitionByName(className) as Class;
    			className = getQualifiedSuperclassName(clazz);
    			label = _labels[className + "." + column.dataField];
			}
			if (label)
				return item[column.dataField][label];
			return "";
		}

    	
    	public function buildEditForm(className:String, metadata:Array, form:Object, create:Boolean):Array {
			var properties:Array = new Array();
			
			var desc:XML = describeType(form);
			
			for each (var property:Object in metadata) {
				var info:Object = ObjectUtil.getClassInfo(form);
				var found:Boolean = false;
				for each (var propName:String in info.properties) {
					if (form[propName] is EntityProperty && EntityProperty(form[propName]).property == property.name) {
						properties.push(form[propName]);
						found = true;
						break;
					}
				}
				
				if (!found) {
					if (property.display == false)
						continue;
					
					if (create && property.inCreate == false)
						continue;
					
					if (!create && property.inEdit == false)
						continue;
				
	            	var componentDescriptor:EntityProperty = buildEditFormItem(property, create);
	            	
	            	if (componentDescriptor)
	            		properties.push(componentDescriptor);
	   			}
			}
			
			return properties;
    	}
    	
    	protected function buildEditFormItem(property:Object, create:Boolean):EntityProperty {
        	var component:Object = null;
        	var editorDataField:String = null;
        	var entityField:String = null;
        	var validator:Validator = null;
        	var percentWidth:int = -1;
        	
        	if (property.inList) {
        		component = new ComboBox();
        		var list:Array = StringUtil.trimArrayElements(property.inList.substring(1, property.inList.length-1), ",").split(",");
        		component.dataProvider = new ArrayCollection(list);
        		editorDataField = "selectedItem";
        	}
        	else if (property.type == Number) {
        		component = new TextInput();
        		editorDataField = "text";
        	}
        	else if (property.type == String) {
        		if (property.widget == 'textArea' || (property.maxSize && property.maxSize > 255)) {
        			component = new TextArea();
        			editorDataField = "text";
        			percentWidth = 100;
        			component.percentWidth = 100;
        			component.height = 100;
        		}
        		else {
        			component = new TextInput();
        			editorDataField = "text";
        			if (property.password == true)
        				component.displayAsPassword = true;
        		}
        		if (property.email) {
        			validator = new EmailValidator();
        			if (property.required)
        				validator.required = true;
        			else
        				validator.required = false;
        		}
        		else if (property.minSize > 0 || property.maxSize) {
        			validator = new StringValidator();
        			if (property.required)
        				validator.required = true;
        			StringValidator(validator).minLength = property.minSize;
        			StringValidator(validator).maxLength = property.maxSize;
        		}
        	}
        	else if (property.type == Boolean) {
        		component = new CheckBox();
        		editorDataField = "selected";
        	}
        	else if (property.type == Date) {
        		component = new DateField();
        		if (property.format)
        			DateField(component).formatString = property.format;
        		editorDataField = "selectedDate";
        	}
        	else if (property.type == FileReference && !create) {
        		if (property.widget == "image") {
        			component = new ImageEditor();
        			component.propertyName = property.name;
        			editorDataField = "fileRef";
        			entityField = "entity";
        		}
        		else {
        			component = new BinaryEditor();
        			component.propertyName = property.name;
        			editorDataField = "fileRef";
        			entityField = "entity";
        		}
        	}
        	else if (property.type == ByteArray && !create) {
        		if (property.widget == "image") {
        			component = new ImageEditor();
        			component.propertyName = property.name;
        			editorDataField = "byteArray";
        			entityField = "entity";
        		}
        		else {
        			component = new BinaryEditor();
        			component.propertyName = property.name;
        			editorDataField = "byteArray";
        			entityField = "entity";
        		}
        	}
        	else if (property.kind == 'manyToOne') {
        		component = new ManyToOneEditor();
        		component.elementClass = property.type;
        		editorDataField = "property";
        		entityField = "entity";
        	}
        	else if (property.kind == 'oneToMany') {
        		component = new OneToManyEditor();
        		component.elementClass = property.type;
        		editorDataField = "collection";
        		entityField = "entity";
        		if (property.editable == false)
        			component.editable = false;
    			percentWidth = 100;
        	}
        	else if (property.kind == 'manyToMany') {
        		component = new ManyToManyEditor();
        		component.elementClass = property.type;
        		editorDataField = "collection";
        		entityField = "entity";
        		if (property.editable == false)
        			component.editable = false;
    			percentWidth = 100;
        	}
        	
        	if (component == null)
        		return null;
        	
        	var entityProperty:EntityProperty = new EntityProperty();
    		entityProperty.property = property.name; 
    		entityProperty.component = component as IUIComponent;
    		entityProperty.percentWidth = percentWidth;
    		entityProperty.validator = validator
    		entityProperty.editorDataField = editorDataField; 
    		entityProperty.entityField = entityField;
    		entityProperty.bound = false;
    		return entityProperty;
     	}
    }
}