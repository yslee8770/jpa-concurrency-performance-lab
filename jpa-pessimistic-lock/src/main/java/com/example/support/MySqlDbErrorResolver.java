package com.example.support;


import java.sql.SQLException;

public final class MySqlDbErrorResolver implements DbErrorResolver {

    @Override
    public DbError resolve(Throwable t) {
        Throwable root = Throwables.rootCause(t);
        if (root instanceof SQLException sql) {
            int code = sql.getErrorCode();
            // MySQL InnoDB
            if (code == 1213) return DbError.MYSQL_DEADLOCK;
            if (code == 1205) return DbError.MYSQL_LOCK_WAIT_TIMEOUT;
        }
        return DbError.OTHER;
    }
}
