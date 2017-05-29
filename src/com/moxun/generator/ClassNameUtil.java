package com.moxun.generator;

/**
 * Created by moxun on 16/3/7.
 */
public class ClassNameUtil {
    public static String getName(String name) {
        char[] chars = name.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (!Character.isLetter(c)) {
                chars[i] = '_';
                if (i + 1 < chars.length) {
                    chars[i + 1] = Character.toUpperCase(chars[i + 1]);
                }
            }
        }

        return String.valueOf(chars).replaceAll("_","");
    }


    public static String suffixToUppercase(String s) {
        StringBuilder sb = new StringBuilder(s);
        sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        return sb.toString();
    }

    public static String suffixToLower(String s) {
        StringBuilder sb = new StringBuilder(s);
        sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
        return sb.toString();
    }

    /**
     *
     * @param key
     * @return key with first charachter in lower case
     */
    public static String getKeyName(String key){
        if(key!=null && !key.isEmpty() && key.length() > 1){
            return suffixToLower(key);
        }
        return "";
    }
}
