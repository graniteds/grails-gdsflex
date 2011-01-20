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
	
	/**
	 * 	Implementations build UI elements from the metadata of an entity
	 */
    public interface IUIBuilder {
    	
    	/**
    	 * 	Build an array of data grid columns
    	 * 
    	 * 	@param className		entity qualified class name
    	 *  @param metadata			metadata array (from IUIEntityMetataBuilder)
    	 * 	@param simpleOnly		don't return columns for complex properties (used for xToMany subtables)
    	 */
    	function buildListColumns(className:String, metadata:Array, simpleOnly:Boolean = false):Array;
    	
    	/**
    	 * 	Build an array of form item descriptors
    	 * 
    	 * 	@param className		entity qualified class name
    	 *  @param metadata			metadata array (from IUIEntityMetataBuilder)
    	 * 	@param form				existing Flex form object (used for item overrides)
    	 *  @param create			is this for an entity creation form
    	 */
    	function buildEditForm(className:String, metadata:Array, form:Object, create:Boolean):Array;
    }
}