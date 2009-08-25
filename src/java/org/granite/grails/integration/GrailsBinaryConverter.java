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
		return value != null && 
			(value instanceof Blob || value instanceof Clob);
	}

	//@Override
	public Object revert(Object value) {
		return value != null ? "" : null;
	}

}
