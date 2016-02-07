package com.xanderfehsenfeld.swearjar;

/**
 * Created by Xander on 2/6/16.
 */
public class SpeechAnalyzer {

    /* grade the dirtiness of your diction */
    public static int analyzeSpeech(String input){
        /* count number of censored words */
        String[] words = input.split(" ");
        String currWord = "";
        int count = 0;
        for (int i = 0; i < words.length; i ++){
            currWord = words[i];
            if (currWord.contains("*")){
                count ++;
            }
        }
        return count;
    }
}
