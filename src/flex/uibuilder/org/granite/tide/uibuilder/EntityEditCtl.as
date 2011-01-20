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
	
	import flash.utils.getQualifiedClassName;
	
	import mx.containers.TabNavigator;
	import mx.controls.Alert;
	import mx.events.CloseEvent;
	import mx.validators.Validator;
	
	import org.granite.tide.events.TideFaultEvent;
	import org.granite.tide.events.TideResultEvent;
	import org.granite.tide.TideResponder;
	import org.granite.tide.spring.Context;
	import org.granite.tide.uibuilder.util.ReflectionUtil;
	import org.granite.tide.uibuilder.events.CancelEntityEvent;
	import org.granite.tide.uibuilder.events.EditEntityEvent;
	import org.granite.tide.uibuilder.events.EndEditEntityEvent;
	import org.granite.tide.uibuilder.events.ListEntityEvent;
	import org.granite.tide.uibuilder.events.RemoveEntityEvent;
	import org.granite.tide.uibuilder.events.SaveEntityEvent;
	import org.granite.tide.uibuilder.events.ShowEntityEvent;
	
	
    public class EntityEditCtl {
       
        // The Tide context is injected in the component.
        // It will be used to manage the conversation
        [Bindable] [In]
        public var context:Context;
       
       	// We get the tab navigator for this entity
       	// As the component is defined in a subcontext, entityUI will always be a reference to the view for the current entity class
       	[Bindable] [In("#{entityUI.nav}")]
       	public var nav:TabNavigator;
       	
        [Bindable]
        private var create:Boolean;
           
        
        private var _entityClass:Class;
        private var _entityName:String;
        private var _qualifiedEntityName:String;
        private var _entityLabel:String;
        private var _editForm:EntityEdit;
        
        
        private function upperCaseEntityName():String {
        	return _entityName.substring(0, 1).toUpperCase() + _entityName.substring(1);
        }
        
        /**
         * 	Set the current entity class
         *  By default will be set by injection from EntityUI
         *  
         *  @param entityClass entity class
         */
        public function set entityClass(entityClass:Class):void {
        	_entityClass = entityClass;
        	var className:String = getQualifiedClassName(entityClass);
        	_entityName = ReflectionUtil.getEntityName(className);
        	_qualifiedEntityName = ReflectionUtil.getQualifiedEntityName(className);
		}
		
		[In]
		public var tideEntityMetadataBuilder:IEntityMetadataBuilder;
		
		protected var _properties:Array;
		
		protected function buildForm(entityInstance:Object, form:Object):void {
			_entityLabel = tideEntityMetadataBuilder.getDisplayLabel(_entityClass);
			var metadata:Array = tideEntityMetadataBuilder.buildMetadata(entityInstance);
			
			var builder:IUIBuilder = context.meta_getInstance(_qualifiedEntityName + '.tideUIBuilder') as IUIBuilder;
			if (builder == null)
				builder = IUIBuilder(context.tideUIBuilder);
			
			_properties = builder.buildEditForm(getQualifiedClassName(_entityClass), metadata, form, isNaN(entityInstance.id));
		}
    	
    	
    	// Show will be called from a browser url /show/{id}
    	// In this case we cannot be sure that the entity is already in the current entity cache
    	// so we have to issue a remote find
    	[Observer]
    	public function show(event:ShowEntityEvent):void {
    		entityClass = event.entityClass;
    		
    		// context[_qualifiedEntityName + 'Controller'] is a proxy to the Grails controller for the current entity
    		// context is passed as first argument to ensure that the controller model result is mapped in the current
    		// conversational context
            context[_qualifiedEntityName + 'Controller'].find(context, {id: event.id}, 
            	new TideResponder(findResult, null, event.id));
    	}
    	
    	private function findResult(event:TideResultEvent, id:Number):void {
    		// entity instance has been mapped in the context by Tide
    		var entityInstance:Object = context[_entityName + "Instance"];
    		if (entityInstance == null)
    			Alert.show(upperCaseEntityName() + " not found with id " + id);
    		else
				dispatchEvent(new EditEntityEvent(entityInstance));
    	}
    	
    	
    	// Edit will be called from a New/Edit button or deep linking /show url
        [Observer]
        public function edit(event:EditEntityEvent):void {
        	var entityInstance:Object = event.entityInstance;
        	
            create = entityInstance is Class;
            if (create)
                entityInstance = entityInstance is Class ? new entityInstance() : new _entityClass();
            
            context[_entityName + "Instance"] = entityInstance;
            
            showEditForm();
        }
        
        private function showEditForm():void {            
            _editForm = context.entityEdit;
            
            buildForm(context[_entityName + "Instance"], _editForm);
            
            _editForm.entityClass = _entityClass;
            _editForm.label = create 
				? 'New ' + upperCaseEntityName() 
				: context[_entityName + 'Instance'][_entityLabel];
            _editForm.create = create;            
            _editForm.properties = _properties;
           	
           	// GDS-678: nav not yet initialized
 			if (nav == null) { 
    			context.entityUI.show(context[_entityName + "Instance"].id); 
      			return; 
 			}             
 			
 			// Add the form panel as a child tab in the main view
            if (!nav.getChildByName(_editForm.name))
                nav.addChild(_editForm);           
            nav.selectedChild = _editForm;
        }
       
       
       	[Observer]
        public function save(event:SaveEntityEvent):void {
        	
        	if (!validate())
        		return;
        	
        	applyInput();
           
            // The 'Save' button calls the remote controller and passes it a params map.
            // The Grails controller will get the detached entity with params.{entity}Instance.
            var params:Object = new Object();
            params[_entityName + 'Instance'] = context[_entityName + 'Instance'];
             
            // Once again we pass the context as first argument to ensure that the result is merge 
            // in the current conversation context
            if (create)
                context[_qualifiedEntityName + 'Controller'].persist(context, params, saveResult, saveFault);
            else
                context[_qualifiedEntityName + 'Controller'].merge(context, params, saveResult, saveFault);
        }
       	
        private function saveResult(event:TideResultEvent):void {
            nav.removeChild(_editForm);
            // If the entity was a new one, trigger a refresh of the list to show the new entity.
            if (create)
                dispatchEvent(new ListEntityEvent());
            // Notify that an edit conversation is finished
            dispatchEvent(new EndEditEntityEvent());
            // Merges the current conversation context in the global context
            // (update the existing entities) and destroys the current conversation context.
            context.meta_end(true);
        }
        
        private function saveFault(event:TideFaultEvent):void {
        	if (event.fault.faultCode != 'Validation.Failed')
        		Alert.show(event.fault.faultString);
        }
        
        
        protected function validate():Boolean {
        	// Validate input
        	// The UI builder should have defined one validator for each item editor
        	var validators:Array = new Array();
        	for each (var property:Object in _properties) {
        		if (property.validator)
        			validators.push(property.validator);
        	}
        	
        	var errors:Array = Validator.validateAll(validators);
        	return errors.length == 0;
        }
        
        protected function applyInput():void {
        	var entityInstance:Object = context[_entityName + "Instance"];
        	 
        	// Apply input to entity properties
        	for each (var property:EntityProperty in _properties) {
        		var value:Object = property.editorDataField ? property.component[property.editorDataField] : null;
        		if (property.parser != null)
        			value = property.parser(value);
        		entityInstance[property.property] = value;
        	}
        }
       	
       	
       	[Observer]
        public function cancel(event:CancelEntityEvent):void {
            nav.removeChild(_editForm);
            // Notify that an edit conversation is finished
            dispatchEvent(new EndEditEntityEvent());
            // The cancel button does not merge the conversation in the global context,
            // so all that has been modified in the form is discarded.
            context.meta_end(false);
        }
       	
       	
       	[Observer]
        public function remove(event:RemoveEntityEvent):void {
            Alert.show('Are you sure ?', 'Confirmation', (Alert.YES | Alert.NO),
                null, removeConfirm);
        }
       	
        private function removeConfirm(event:CloseEvent):void {
            if (event.detail == Alert.YES)
                context[_qualifiedEntityName + 'Controller'].remove(context, {id: context[_entityName + 'Instance'].id}, removeResult);
        }
       
        private function removeResult(event:TideResultEvent):void {
            nav.removeChild(_editForm);
            // Trigger a refresh of the list to remove the deleted entity
            dispatchEvent(new ListEntityEvent());
            // End the conversation context (merge=true is maybe not necessary)
            context.meta_end(true);
        }
    }
}