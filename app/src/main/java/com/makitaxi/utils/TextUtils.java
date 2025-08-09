package com.makitaxi.utils;

import java.text.Normalizer;

public class TextUtils {
    
    /**
     * Transforms text to Latin characters by removing diacritics and converting Cyrillic to Latin
     * @param text Input text that may contain non-Latin characters
     * @return Text converted to Latin characters
     */
    public static String transformToLatin(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        // First, convert Cyrillic to Latin (Serbian specific)
        String latinized = cyrillicToLatin(text);
        
        // Then remove diacritics (accents) to get pure Latin
        String normalized = Normalizer.normalize(latinized, Normalizer.Form.NFD);
        String withoutDiacritics = normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        
        return withoutDiacritics;
    }
    
    /**
     * Converts Serbian Cyrillic characters to Latin equivalents
     * @param text Input text in Cyrillic
     * @return Text converted to Latin
     */
    private static String cyrillicToLatin(String text) {
        // Serbian Cyrillic to Latin mapping
        return text
                .replace("А", "A").replace("а", "a")
                .replace("Б", "B").replace("б", "b")
                .replace("В", "V").replace("в", "v")
                .replace("Г", "G").replace("г", "g")
                .replace("Д", "D").replace("д", "d")
                .replace("Ђ", "Dj").replace("ђ", "dj")
                .replace("Е", "E").replace("е", "e")
                .replace("Ж", "Z").replace("ж", "z")
                .replace("З", "Z").replace("з", "z")
                .replace("И", "I").replace("и", "i")
                .replace("Ј", "J").replace("ј", "j")
                .replace("К", "K").replace("к", "k")
                .replace("Л", "L").replace("л", "l")
                .replace("Љ", "Lj").replace("љ", "lj")
                .replace("М", "M").replace("м", "m")
                .replace("Н", "N").replace("н", "n")
                .replace("Њ", "Nj").replace("њ", "nj")
                .replace("О", "O").replace("о", "o")
                .replace("П", "P").replace("п", "p")
                .replace("Р", "R").replace("р", "r")
                .replace("С", "S").replace("с", "s")
                .replace("Т", "T").replace("т", "t")
                .replace("Ћ", "C").replace("ћ", "c")
                .replace("У", "U").replace("у", "u")
                .replace("Ф", "F").replace("ф", "f")
                .replace("Х", "H").replace("х", "h")
                .replace("Ц", "C").replace("ц", "c")
                .replace("Ч", "C").replace("ч", "c")
                .replace("Џ", "Dz").replace("џ", "dz")
                .replace("Ш", "S").replace("ш", "s");
    }
}
