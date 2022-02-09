package com.warmer.kgmaker;

import me.midday.FoolNLTK;
import me.midday.lexical.LexicalAnalyzer;
import me.midday.lexical.Word;

import java.util.List;

public class TestMain {
    public static void main(String[] args) {
        String text = "在这些企业中国有企业有十个";
        LexicalAnalyzer lexicalAnalyzer = FoolNLTK.getLSTMLexicalAnalyzer();
        // Participle
        List<List<Word>> words = lexicalAnalyzer.cut(text);
        for(List<Word> ws: words){
            ws.forEach(System.out::println);
        }


    }
}
