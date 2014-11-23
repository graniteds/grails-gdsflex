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

package org.granite.grails.integration;

import java.lang.reflect.Type;
import java.sql.Blob;
import java.sql.Clob;

import org.granite.messaging.amf.io.convert.Converter;
import org.granite.messaging.amf.io.convert.Converters;
import org.granite.messaging.amf.io.convert.Reverter;

public class GrailsBinaryConverter extends Converter implements Reverter {

	public GrailsBinaryConverter(Converters converters) {
		super(converters);
	}

	@Override
	protected boolean internalCanConvert(Object value, Type targetType) {
		return targetType instanceof Class && (Blob.class.isAssignableFrom((Class<?>)targetType)
			|| Clob.class.isAssignableFrom((Class<?>)targetType));
	}

	@Override
	protected Object internalConvert(Object value, Type targetType) {
		return null;
	}

	//@Override
	public boolean canRevert(Object value) {
		return value instanceof Blob || value instanceof Clob;
	}

	//@Override
	public Object revert(Object value) {
		return value != null ? "" : null;
	}
}
