package com.example.support;

public enum DbError {
    MYSQL_DEADLOCK,          // 1213
    MYSQL_LOCK_WAIT_TIMEOUT, // 1205
    OTHER,
    PG_DEADLOCK,        // SQLSTATE 40P01
    PG_LOCK_TIMEOUT     // canceling statement due to lock timeout
}
