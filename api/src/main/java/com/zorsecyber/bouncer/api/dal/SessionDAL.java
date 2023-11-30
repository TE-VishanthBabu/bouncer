package com.zorsecyber.bouncer.api.dal;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zorsecyber.bouncer.api.dao.Session;

public class SessionDAL {
	private static final Logger log = LoggerFactory.getLogger(SessionDAL.class);
    private static final Object syncLockObj = new Object();

    private static final String getSessionByJwtString = 
    		"from Session where sessionId= :sessionId order by id desc";
    
    public static Session getSession(String jwt)
    {
        EntityManager entityManager = EntityManagerFactorySingleton.getInstance().getEntityManager();
        synchronized (syncLockObj) {
            try {
                Query query = entityManager.createQuery(getSessionByJwtString);
				query.setParameter("sessionId", jwt);
				query.setMaxResults(1);
				Session session = null;
				try
				{
					session = (Session) query.getSingleResult();
				}
				catch(NoResultException ex)
				{
					ex.printStackTrace();
				}
                return (Session) query.getSingleResult();
            } catch (Exception ex) {
                if (entityManager != null && entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().rollback();
                }
                return null;
            } finally {
                if (entityManager != null) {
                    entityManager.close();
                }
            }
        }
    }
    
}
