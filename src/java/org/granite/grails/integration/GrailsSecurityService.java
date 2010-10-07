package org.granite.grails.integration;

import java.util.Map;

import org.granite.messaging.service.security.AbstractSecurityContext;
import org.granite.messaging.service.security.AbstractSecurityService;
import org.granite.messaging.service.security.SecurityServiceException;

/**
 * 
 * @author william
 *
 */
public class GrailsSecurityService extends AbstractSecurityService {

	@Override
	public void configure(Map<String, String> params) {
	}

	@Override
	public void login(Object credentials) throws SecurityServiceException {
	}

	@Override
	public void logout() throws SecurityServiceException {
	}

	@Override
	public Object authorize(AbstractSecurityContext context) throws Exception {
		return context.invoke();
	}
}
