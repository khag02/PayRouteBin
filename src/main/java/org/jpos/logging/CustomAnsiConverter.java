package org.jpos.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class CustomAnsiConverter extends ClassicConverter {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_FAINT = "\u001B[2m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";

    @Override
    public String convert(ILoggingEvent event) {
        String option = getFirstOption();

        if (option == null) {
            return "";
        }

        switch (option.toLowerCase()) {
            case "cyan":
                return ANSI_CYAN;
            case "green":
                return ANSI_GREEN;
            case "yellow":
                return ANSI_YELLOW;
            case "red":
                return ANSI_RED;
            case "blue":
                return ANSI_BLUE;
            case "white":
                return ANSI_WHITE;
            case "faint":
                return ANSI_FAINT;
            case "reset":
                return ANSI_RESET;
            default:
                return "";
        }
    }
}
