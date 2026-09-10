package com.cobblemonlegacy.v3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MiniJson {
    private final String source;
    private int index;

    private MiniJson(String source) { this.source = source; }

    static Object parse(String source) {
        MiniJson parser = new MiniJson(source);
        Object value = parser.value();
        parser.space();
        if (parser.index != source.length()) throw new IllegalArgumentException("JSON contém dados extras.");
        return value;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(String source) { return (Map<String, Object>) parse(source); }

    static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        write(out, value);
        return out.toString();
    }

    private Object value() {
        space();
        if (index >= source.length()) throw error("valor esperado");
        return switch (source.charAt(index)) {
            case '{' -> objectValue();
            case '[' -> arrayValue();
            case '"' -> stringValue();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> numberValue();
        };
    }

    private Map<String, Object> objectValue() {
        index++;
        Map<String, Object> result = new LinkedHashMap<>();
        space();
        if (take('}')) return result;
        do {
            space();
            String key = stringValue();
            space();
            if (!take(':')) throw error("':' esperado");
            result.put(key, value());
            space();
        } while (take(','));
        if (!take('}')) throw error("'}' esperado");
        return result;
    }

    private List<Object> arrayValue() {
        index++;
        List<Object> result = new ArrayList<>();
        space();
        if (take(']')) return result;
        do { result.add(value()); space(); } while (take(','));
        if (!take(']')) throw error("']' esperado");
        return result;
    }

    private String stringValue() {
        if (!take('"')) throw error("texto esperado");
        StringBuilder out = new StringBuilder();
        while (index < source.length()) {
            char c = source.charAt(index++);
            if (c == '"') return out.toString();
            if (c != '\\') { out.append(c); continue; }
            if (index >= source.length()) throw error("escape incompleto");
            char escaped = source.charAt(index++);
            switch (escaped) {
                case '"', '\\', '/' -> out.append(escaped);
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    if (index + 4 > source.length()) throw error("unicode incompleto");
                    out.append((char) Integer.parseInt(source.substring(index, index + 4), 16));
                    index += 4;
                }
                default -> throw error("escape inválido");
            }
        }
        throw error("texto não terminado");
    }

    private Object numberValue() {
        int start = index;
        if (take('-')) {}
        while (index < source.length() && Character.isDigit(source.charAt(index))) index++;
        if (take('.')) while (index < source.length() && Character.isDigit(source.charAt(index))) index++;
        if (index < source.length() && (source.charAt(index) == 'e' || source.charAt(index) == 'E')) {
            index++;
            if (index < source.length() && (source.charAt(index) == '+' || source.charAt(index) == '-')) index++;
            while (index < source.length() && Character.isDigit(source.charAt(index))) index++;
        }
        if (start == index) throw error("número esperado");
        String number = source.substring(start, index);
        return number.contains(".") || number.contains("e") || number.contains("E")
                ? Double.parseDouble(number) : Long.parseLong(number);
    }

    private Object literal(String literal, Object value) {
        if (!source.startsWith(literal, index)) throw error("literal inválido");
        index += literal.length();
        return value;
    }

    private boolean take(char expected) {
        if (index < source.length() && source.charAt(index) == expected) { index++; return true; }
        return false;
    }

    private void space() { while (index < source.length() && Character.isWhitespace(source.charAt(index))) index++; }
    private IllegalArgumentException error(String detail) { return new IllegalArgumentException(detail + " na posição " + index); }

    @SuppressWarnings("unchecked")
    private static void write(StringBuilder out, Object value) {
        if (value == null) { out.append("null"); return; }
        if (value instanceof String text) {
            out.append('"');
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                switch (c) {
                    case '"' -> out.append("\\\"");
                    case '\\' -> out.append("\\\\");
                    case '\b' -> out.append("\\b");
                    case '\f' -> out.append("\\f");
                    case '\n' -> out.append("\\n");
                    case '\r' -> out.append("\\r");
                    case '\t' -> out.append("\\t");
                    default -> { if (c < 32) out.append(String.format("\\u%04x", (int) c)); else out.append(c); }
                }
            }
            out.append('"'); return;
        }
        if (value instanceof Number || value instanceof Boolean) { out.append(value); return; }
        if (value instanceof Map<?, ?> map) {
            out.append('{'); boolean first = true;
            for (Map.Entry<?, ?> item : map.entrySet()) {
                if (!first) out.append(','); first = false;
                write(out, String.valueOf(item.getKey())); out.append(':'); write(out, item.getValue());
            }
            out.append('}'); return;
        }
        if (value instanceof Iterable<?> list) {
            out.append('['); boolean first = true;
            for (Object item : list) { if (!first) out.append(','); first = false; write(out, item); }
            out.append(']'); return;
        }
        throw new IllegalArgumentException("Tipo JSON não suportado: " + value.getClass());
    }
}
