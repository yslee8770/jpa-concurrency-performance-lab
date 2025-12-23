package com.example.support;

public interface DbErrorResolver {
    DbError resolve(Throwable t);
}
