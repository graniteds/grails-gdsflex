package org.granite.grails.integration;

import java.lang.reflect.Type;

import org.granite.messaging.amf.io.convert.Converter;
import org.granite.messaging.amf.io.convert.Converters;
import org.granite.messaging.amf.io.convert.Reverter;


public class GrailsGStringConverter extends Converter implements Reverter {
	
	public GrailsGStringConverter(Converters converters) {
		super(converters);
	}

	@Override
	protected boolean internalCanConvert(Object value, Type targetType) {
		return false;
	}

	@Override
	protected Object internalConvert(Object value, Type targetType) {
		return null;
	}

	//@Override
	public boolean canRevert(Object value) {
		return value != null && value instanceof groovy.lang.GString;
	}

	//@Override
	public Object revert(Object value) {
		return value != null ? value.toString() : null;
	}

}
