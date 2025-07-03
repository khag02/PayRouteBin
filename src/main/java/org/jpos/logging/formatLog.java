package org.jpos.logging;

import org.jpos.iso.ISOComponent;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.util.LogEvent;

import java.util.*;

public class formatLog {
    private static final Set<Integer> EXCLUDED_FIELDS = new HashSet<>(Arrays.asList(2, 14, 35, 55));

    public static void log(ISOMsg m, LogEvent evt) {
        try {
            String directionStr = m.getDirection() == 1 ? "incoming" : m.getDirection() == 2 ? "outgoing" : "unknown";
            Map<String, String> lines = new LinkedHashMap<>();

            lines.put("Direction", directionStr);

            if (m.getPackager() != null) {
                String desc = m.getPackager().getDescription();
                String fileName = desc.replaceAll(".*\\[([^\\]]+)\\].*", "$1");
                fileName = fileName.substring(Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\')) + 1);
                lines.put("Packager", fileName);
            }
            lines.put("Header", ISOUtil.hexString(m.getHeader()));
            lines.put("MTI", m.getMTI());

            for (int i = 1; i <= m.getMaxField(); i++) {
                ISOComponent c = m.getComponent(i);
                if (c == null)
                    continue;

                if (c.getChildren().isEmpty()) {
                    String key = "Field " + i;
                    if (EXCLUDED_FIELDS.contains(i)) {
                        byte[] raw = m.getBytes(i);
                        int len = raw != null ? raw.length : 16;
                        lines.put(key, repeatChar('_', len));
                    } else {
                        lines.put(key, m.getString(i));
                    }
                } else {
                    for (Object keyObj : c.getChildren().keySet()) {
                        int j = (int) keyObj;
                        String key = "Field " + i + "." + j;
                        String value = m.getString(i + "." + j);
                        if (value != null && !value.trim().isEmpty()) {
                            lines.put(key, value);
                        }
                    }
                }
            }

            int maxKeyLength = lines.keySet().stream().mapToInt(String::length).max().orElse(0);
            evt.addMessage("================ ISO Message =================");
            for (Map.Entry<String, String> entry : lines.entrySet()) {
                evt.addMessage(String.format("%-" + maxKeyLength + "s = [%s]", entry.getKey(), entry.getValue()));
            }
            evt.addMessage("================ End of ISO Message ==========");

        } catch (ISOException e) {
            throw new RuntimeException("Error formatting ISO message", e);
        }
    }

    private static String repeatChar(char ch, int count) {
        if (count <= 0)
            return "";
        char[] arr = new char[count];
        Arrays.fill(arr, ch);
        return new String(arr);
    }
}
