package com.example.jpa_concurrency_performance_lab.measure;

import jakarta.persistence.EntityManager;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.springframework.stereotype.Component;

@Component
public class PersistenceContextProbe {
    public int managedEntityCount(EntityManager em) {
        SharedSessionContractImplementor session = em.unwrap(SharedSessionContractImplementor.class);
        PersistenceContext pc = session.getPersistenceContext();
        return pc.getNumberOfManagedEntities();
    }
}
