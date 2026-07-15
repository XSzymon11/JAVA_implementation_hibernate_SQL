package pl.database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.Properties;
import java.util.function.Function;

public class DatabaseService {

    private static volatile EntityManagerFactory emf;

    private static String envOrProp(String envKey, String propKey, String def) {
        String v = System.getenv(envKey);
        if (v != null && !v.isBlank()) return v;
        v = System.getProperty(propKey);
        if (v != null && !v.isBlank()) return v;
        return def;
    }

    private static String dbHost() {
        return envOrProp("DB_HOST", "db.host", "127.0.0.1");
    }

    private static String dbPort() {
        return envOrProp("DB_PORT", "db.port", "5433");
    }

    private static String dbName() {
        return envOrProp("DB_NAME", "db.name", "karent");
    }

    private static String dbUser() {
        return envOrProp("DB_USER", "db.user", "karent");
    }

    private static String dbPass() {
        return envOrProp("DB_PASSWORD", "db.password", "karent");
    }

    private static String jdbcUrl() {
        return "jdbc:postgresql://" + dbHost() + ":" + dbPort() + "/" + dbName() + "?sslmode=disable";
    }

    public static EntityManagerFactory emf() {
        if (emf == null) {
            synchronized (DatabaseService.class) {
                if (emf == null) {
                    emf = createEmfWithRetry(Duration.ofSeconds(20));
                }
            }
        }
        return emf;
    }

    private static EntityManagerFactory createEmfWithRetry(Duration maxWait) {
        waitForDb(maxWait);

        Properties overrides = new Properties();
        overrides.put("jakarta.persistence.jdbc.url", jdbcUrl());
        overrides.put("jakarta.persistence.jdbc.user", dbUser());
        overrides.put("jakarta.persistence.jdbc.password", dbPass());

        return Persistence.createEntityManagerFactory("karentPU", overrides);
    }

    private static void waitForDb(Duration maxWait) {
        long deadline = System.nanoTime() + maxWait.toNanos();

        Properties props = new Properties();
        props.setProperty("user", dbUser());
        props.setProperty("password", dbPass());

        RuntimeException last = null;

        while (System.nanoTime() < deadline) {
            try (Connection c = DriverManager.getConnection(jdbcUrl(), props)) {
                if (!c.isValid(1)) throw new IllegalStateException("Nie udało się potwierdzić połączenia z bazą danych");
                return;
            } catch (Exception e) {
                last = new RuntimeException(e);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new RuntimeException("Nie udało się połączyć z bazą danych w ciągu " + maxWait.getSeconds() + " sekund", last);
    }

    public static <T> T tx(Function<EntityManager, T> work) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();
            T result = work.apply(em);
            tx.commit();
            return result;
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public static void close() {
        EntityManagerFactory local = emf;
        if (local != null && local.isOpen()) local.close();
    }
}
