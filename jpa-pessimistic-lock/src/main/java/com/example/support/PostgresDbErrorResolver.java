package com.example.support;

import org.postgresql.util.PSQLException;

public final class PostgresDbErrorResolver implements DbErrorResolver {

    @Override
    public DbError resolve(Throwable t) {
        Throwable root = Throwables.rootCause(t);
        if (root instanceof PSQLException pg) {
            String state = pg.getSQLState();

            // deadlock_detected
            if ("40P01".equals(state)) return DbError.PG_DEADLOCK;

            // Postgres에서 lock_timeout은 보통 55P03으로 떨어짐
            if ("55P03".equals(state)) return DbError.PG_LOCK_TIMEOUT;
        }
        return DbError.OTHER;
    }
}

