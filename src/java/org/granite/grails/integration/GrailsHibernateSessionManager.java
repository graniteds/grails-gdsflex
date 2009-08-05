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
import java.lang.reflect.Method;

import org.granite.tide.data.JTAPersistenceContextManager;
import org.granite.util.Reflections;
import org.hibernate.Query;
import org.hibernate.SessionFactory;

/**
 * Responsible for attaching a session with the persistence mangager
 * @author cingram
 *
 */
public class GrailsHibernateSessionManager extends JTAPersistenceContextManager {
	
	private SessionFactory sessionFactory;
	
	
	public GrailsHibernateSessionManager() {
	}

	public GrailsHibernateSessionManager(SessionFactory sf) {
        this.sessionFactory = sf;
	}
    
	
	@Override
    protected Object beginTransaction() {
        return sessionFactory.getCurrentSession().getTransaction(); // We should be in a Spring managed transaction (or another container)
    }
    
	@Override
    protected void commitTransaction(Object tx) throws Exception {
	    // Should not commit: managed by the container
    }
    
	@Override
    protected void rollbackTransaction(Object tx) {
        // Should not rollback: managed by the container
    }

	
	/**
	 * attaches the entity to the JPA context.
	 * @return the attached entity
	 */
	@Override
	public Object findEntity(Object entity, String[] fetch) {
		if (entity == null)
			return null;
		
		try {
			Method getter = (Method)Reflections.getGetterMethod(entity.getClass(), "id");
			Serializable id = (Serializable)getter.invoke(entity);
			
			if (fetch == null)
				return sessionFactory.getCurrentSession().load(entity.getClass(), id);
			
			for (String f : fetch) {
				System.out.println("Fetching " + f);
				
		        Query q = sessionFactory.getCurrentSession().createQuery("select e from " + entity.getClass().getName() + " e left join fetch e." + f + " where e = :entity");
		        q.setParameter("entity", entity);
		        entity = q.uniqueResult();
			}
			return entity;
		}
		catch (Exception e) {
			throw new RuntimeException("Could not refresh entity " + entity.getClass(), e);
		}
	}
}
