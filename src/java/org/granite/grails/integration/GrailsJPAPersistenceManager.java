/*
  GRANITE DATA SERVICES
  Copyright (C) 2007-2008 ADEQUATE SYSTEMS SARL

  This file is part of Granite Data Services.

  Granite Data Services is free software; you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation; either version 3 of the License, or (at your
  option) any later version.

  Granite Data Services is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
  for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this library; if not, see <http://www.gnu.org/licenses/>.
*/

package org.granite.grails.integration;

import java.io.Serializable;

import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.granite.tide.TideTransactionManager;
import org.granite.tide.data.AbstractTidePersistenceManager;
import org.hibernate.Query;
import org.hibernate.SessionFactory;

/**
 * Responsible for attaching a session with the persistence mangager
 * @author cingram
 *
 */
public class GrailsJPAPersistenceManager extends AbstractTidePersistenceManager {
	
	private SessionFactory sessionFactory;
	
	
	public GrailsJPAPersistenceManager(TideTransactionManager tm) {
		super(tm);
	}

	public GrailsJPAPersistenceManager(SessionFactory sf, TideTransactionManager tm) {
		super(tm);
        this.sessionFactory = sf;
	}
	
	/**
	 * attaches the entity to the JPA context.
	 * @return the attached entity
	 */
	@Override
	public Object findEntity(Object entity, String[] fetch) {
		GrailsDomainClass domainClass = new DefaultGrailsDomainClass(entity.getClass());
		Serializable id = (Serializable)domainClass.getPropertyValue(domainClass.getIdentifier().getName());
		
        if (id == null)
            return null;

        if (fetch == null)
        	return sessionFactory.getCurrentSession().load(entity.getClass(), id);
        
        for (String f : fetch) {
	        Query q = sessionFactory.getCurrentSession().createQuery("select e from " + entity.getClass().getName() + " e left join fetch e." + f + " where e = :entity");
	        q.setParameter("entity", entity);
	        entity = q.uniqueResult();
        }
        return entity;
	}
}
