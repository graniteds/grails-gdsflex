package org.granite.grails.gas3;

import java.sql.Blob;
import java.sql.Clob;

import org.granite.generator.as3.As3Type;
import org.granite.generator.as3.DefaultAs3TypeFactory;


public class GrailsAs3TypeFactory extends DefaultAs3TypeFactory {

    public static final As3Type FILE_REFERENCE = new As3Type("flash.net", "FileReference");
    

	@Override
    public As3Type getAs3Type(Class<?> jType) {
		As3Type type = getFromCache(jType);
		
		if (type == null) {
			if (Blob.class.isAssignableFrom(jType) || Clob.class.isAssignableFrom(jType)) {
				type = FILE_REFERENCE;
			}
			
			if (type != null)
				putInCache(jType, type);
			else
				type = super.getAs3Type(jType);
		}
		
		return type;
	}
}
