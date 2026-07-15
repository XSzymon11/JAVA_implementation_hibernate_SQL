package pl.auth;

import pl.database.Osoba;

public final class Session {
    private static Osoba current;

    private Session() {}

    public static Osoba getCurrent() {
        return current;
    }

    public static void setCurrent(Osoba osoba) {
        current = osoba;
    }

    public static void clear() {
        current = null;
    }
}
