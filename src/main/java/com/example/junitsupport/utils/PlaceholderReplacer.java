package com.example.junitsupport.utils;

public class PlaceholderReplacer {
    public static String replace(String text, String placeholder, String value) {
        if (value == null || value.isEmpty()) {
            // fallbackで"// TODO ..."などを設定することも可能
            return text.replace(placeholder, "");
        }
        return text.replace(placeholder, value);
    }
}
