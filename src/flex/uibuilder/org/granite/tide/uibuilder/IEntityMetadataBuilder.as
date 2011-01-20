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
	 * 	Implementations build a metadata array for an entity class
	 */
    public interface IEntityMetadataBuilder {
    	
    	/**
    	 * 	Build metadata array for the specified entity
    	 *  The result is a array of objects sorted in display order
    	 *  Each element contains at least :
    	 * 		name		: property name
    	 * 		type		: property class
    	 * 		kind		: property kind (String)
    	 *      association : association type (oneToOne, oneToMany, manyToMany)
    	 * 
    	 * 	Implementations can add other properties to be used by the UI builder
    	 * 
    	 * 	@param entity
    	 *  @return metadata array
    	 */ 
    	function buildMetadata(entity:Object):Array;
    	
    	/**
    	 * 	Returns the property used as a label for the entity class
    	 * 
    	 * 	@param entityClass
    	 *  @return label property
    	 */ 
    	function getDisplayLabel(entityClass:Class):String;
    }
}