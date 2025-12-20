package com.example.jpa_concurrency_performance_lab.measure;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Component;

@Component
public class HibernateStatsProbe {

    public Statistics statistics(EntityManager em) {
        SessionFactory sf = em.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics stats = sf.getStatistics();
        stats.setStatisticsEnabled(true);
        return stats;
    }

    public void clear(Statistics stats) {
        stats.clear();
    }
}
