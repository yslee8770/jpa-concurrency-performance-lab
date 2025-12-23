package com.example.support;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class DbErrorClassifier {

    private final DbErrorResolver resolver;

    public DbErrorClassifier(DataSource dataSource) {
        this.resolver = chooseResolver(dataSource);
    }

    public DbError classify(Throwable t) {
        return resolver.resolve(t);
    }

    private static DbErrorResolver chooseResolver(DataSource ds) {
        String product = databaseProductName(ds);

        if (product == null) return new MySqlDbErrorResolver(); // safe default

        String p = product.toLowerCase();
        if (p.contains("postgres")) return new PostgresDbErrorResolver();
        if (p.contains("mysql") || p.contains("mariadb")) return new MySqlDbErrorResolver();

        return new MySqlDbErrorResolver();
    }

    private static String databaseProductName(DataSource ds) {
        try (Connection c = ds.getConnection()) {
            return c.getMetaData().getDatabaseProductName();
        } catch (Exception e) {
            return null;
        }
    }
}
