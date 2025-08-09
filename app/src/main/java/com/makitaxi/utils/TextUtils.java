package com.makitaxi.utils;

import java.text.Normalizer;

public class TextUtils {
    
    public static String transformToLatin(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        String latinized = cyrillicToLatin(text);
        
        String normalized = Normalizer.normalize(latinized, Normalizer.Form.NFD);
        String withoutDiacritics = normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        
        return withoutDiacritics;
    }
    
    private static String cyrillicToLatin(String text) {
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
