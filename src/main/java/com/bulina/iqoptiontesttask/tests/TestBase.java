package com.bulina.iqoptiontesttask.tests;

import com.sun.javafx.binding.StringFormatter;

import java.util.logging.Logger;

/**
 * Created by user on 13.11.2016.
 */
public class TestBase {
    private Logger logger = Logger.getLogger(this.getClass().getName());

    public void logAction(Object object) {
        logger.info(object.toString());
    }

    public void logAction(String format, Object... args) {
        logger.info(String.format(format, args));
    }

    public void logPassed(String format, Object... args) {
        logger.info("Passed. " + String.format(format, args));
    }

    public void logPassed() {
        logger.info("Passed.");
    }
}