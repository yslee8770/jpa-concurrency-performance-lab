package com.example.support;

public final class Throwables {
    private Throwables() {}

    public static Throwable rootCause(Throwable t) {
        if (t == null) return null;
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur;
    }
}
