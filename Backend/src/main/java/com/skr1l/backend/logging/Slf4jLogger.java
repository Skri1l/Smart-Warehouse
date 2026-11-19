package com.skr1l.backend.logging;

public class Slf4jLogger implements AppLogger{

    protected final org.slf4j.Logger log;

    protected Slf4jLogger(Class<?> clazz) {
        this.log = org.slf4j.LoggerFactory.getLogger(clazz);
    }

    public void info(String message) {
        log.info(message);
    }

    public void warn(String message) {
        log.warn(message);
    }

    public void error(String message, Throwable ex) {
        log.error(message, ex);
    }

    public void debug(String message) {
        log.debug(message);
    }
}
