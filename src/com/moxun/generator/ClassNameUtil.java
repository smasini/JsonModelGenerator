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

    /**
     * Returns the non-plural form of a plural noun like: cars -> car, children -> child, people -> person, etc.
     * @param str
     * @return the non-plural form of a plural noun like: cars -> car, children -> child, people -> person, etc.
     */
    public static String fromPlural(String str)
    {
        if(str.toLowerCase().endsWith("es") && !shouldEndWithE(str))
            return str.substring(0, str.toLowerCase().lastIndexOf("es"));
        else if(str.toLowerCase().endsWith("s"))
            return str.substring(0, str.toLowerCase().lastIndexOf('s'));
        else if(str.toLowerCase().endsWith("ies"))
            return str.substring(0, str.toLowerCase().lastIndexOf("ies")) + "y";
        else if(str.toLowerCase().endsWith("children"))
            return str.substring(0, str.toLowerCase().lastIndexOf("ren"));
        else if(str.toLowerCase().endsWith("people"))
            return str.substring(0, str.toLowerCase().lastIndexOf("ople")) + "rson";
        else
            return str;
    }

    /**
     *
     * @param str
     * @return true is the singular form of a word should end with the letter "e"
     */
    private static boolean shouldEndWithE(String str)
    {
        return str.toLowerCase().endsWith("iece")
                || str.toLowerCase().endsWith("ice")
                || str.toLowerCase().endsWith("ace")
                || str.toLowerCase().endsWith("ise")
                ;
    }
}
