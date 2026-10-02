package com.college.library.util;

import java.util.List;
import java.util.stream.Collectors;

/** Tiny RFC-4180 CSV builder, so reports need no extra library. */
public final class CsvWriter {

    private CsvWriter() {
    }

    public static String write(List<String> header, List<List<Object>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(line(header.stream().map(h -> (Object) h).toList()));
        rows.forEach(r -> sb.append(line(r)));
        return sb.toString();
    }

    private static String line(List<Object> cells) {
        return cells.stream().map(CsvWriter::escape).collect(Collectors.joining(",")) + "\r\n";
    }

    private static String escape(Object value) {
        if (value == null) return "";
        String s = value.toString();
        // Prevent spreadsheet formula injection when the CSV is opened in Excel.
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0 && !s.matches("-?\\d+(\\.\\d+)?")) {
            s = "'" + s;
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
