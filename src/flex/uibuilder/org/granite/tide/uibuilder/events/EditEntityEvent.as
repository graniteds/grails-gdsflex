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

package org.granite.tide.uibuilder.events
{
	import flash.events.Event;

	import org.granite.tide.events.AbstractTideEvent;
	import org.granite.tide.events.IConversationEvent;
	import org.granite.tide.uibuilder.util.ReflectionUtil;

	public class EditEntityEvent extends AbstractTideEvent implements IConversationEvent {

		private var _conversationId:String;
		public var entityInstance:Object;

		public function EditEntityEvent(entityInstance:Object):void {
			super();

			this.entityInstance = entityInstance;

			if (entityInstance is Class)
				_conversationId = "New " + ReflectionUtil.getUpperCaseEntityName(entityInstance);
			else if (entityInstance is String)
				_conversationId = "New " + entityInstance.substring(0, 1).toUpperCase() + entityInstance.substring(1);
			else
				_conversationId = ReflectionUtil.getUpperCaseEntityName(entityInstance) + "#" + entityInstance.id;
		}

		public function get conversationId():String {
			return _conversationId;
		}

		public override function clone():Event {
			return new EditEntityEvent(entityInstance);
		}
	}
}
