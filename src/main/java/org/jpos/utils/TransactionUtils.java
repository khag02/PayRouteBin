package org.jpos.utils;

import org.springframework.stereotype.Component;

@Component
public class TransactionUtils {
    public static String maskPAN(String pan) {
        if (pan == null || pan.length() < 10)
            return pan;
        int start = 6;
        int end = 4;

        String prefix = pan.substring(0, start);
        String suffix = pan.substring(pan.length() - end);
        String masked = "*".repeat(pan.length() - (start + end));

        return prefix + masked + suffix;
    }
}
