package net.xach.testmod;

public class SafeLogger {
    private static final String MOD_ID = "testmod";

    public static void info(String message) {
        System.out.println("[" + MOD_ID + "] INFO: " + message);
    }

    public static void warning(String message) {
        System.out.println("[" + MOD_ID + "] WARNING: " + message);
    }

    public static void error(String message) {
        System.err.println("[" + MOD_ID + "] ERROR: " + message);
    }
}
