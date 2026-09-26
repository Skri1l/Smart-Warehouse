package com.skr1l.backend.logging;

public interface AppLogger {
    void info(String message);
    void warn(String message);
    void debug(String message);
    void error(String message, Throwable ex);
}
