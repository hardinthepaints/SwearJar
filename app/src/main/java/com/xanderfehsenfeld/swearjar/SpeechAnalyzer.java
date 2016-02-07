package com.xanderfehsenfeld.swearjar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Xander on 2/6/16.
 *
 * Analyze string output from text output for swearwords and count them
 *
 *      depending on whether the phone censors swearwords, the bad words will either
 *      contain '*'s or not, so the function accounts for both by storing a bunch of bad words:
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
            if (currWord.contains("*") || badWordsMap.containsKey(currWord)){
                count ++;
            }
        }
        return count;
    }

    /* get the bad words */
    public static ArrayList<String> getBadWords(String input){
                /* count number of censored words */
        String[] words = input.split(" ");
        String currWord = "";

        ArrayList<String> badwords = new ArrayList<>();

        for (int i = 0; i < words.length; i ++){
            currWord = words[i];
            if (currWord.contains("*") || badWordsMap.containsKey(currWord)){
                badwords.add(currWord);
            }
        }
        return badwords;
    }


    /* mapping of bad words */
    public static Map<String, Boolean> badWordsMap = new HashMap<String, Boolean>()
    {{
            put("anal", true);
            put("anus", true);
            put("arse", true);
            put("ass", true);
            put("ballsack", true);
            put("balls", true);
            put("bastard", true);
            put("bitch", true);
            put("biatch", true);
            put("bloody", true);
            put("blowjob", true);
            put("blow", true);
            put("job", true);
            put("bollock", true);
            put("bollok", true);
            put("boner", true);
            put("boob", true);
            put("bugger", true);
            put("bum", true);
            put("butt", true);
            put("buttplug", true);
            put("clitoris", true);
            put("cock", true);
            put("coon", true);
            put("crap", true);
            put("cunt", true);
            put("damn", true);
            put("dick", true);
            put("dildo", true);
            put("dyke", true);
            put("fag", true);
            put("feck", true);
            put("fellate", true);
            put("fellatio", true);
            put("felching", true);
            put("fuck", true);
            put("f", true);
            put("u", true);
            put("c", true);
            put("k", true);
            put("fudgepacker", true);
            put("fudge", true);
            put("packer", true);
            put("flange", true);
            put("Goddamn", true);
            put("God", true);
            put("damn", true);
            put("hell", true);
            put("homo", true);
            put("jerk", true);
            put("jizz", true);
            put("knobend", true);
            put("knob", true);
            put("end", true);
            put("labia", true);
            put("lmao", true);
            put("lmfao", true);
            put("muff", true);
            put("nigger", true);
            put("nigga", true);
            put("omg", true);
            put("penis", true);
            put("piss", true);
            put("poop", true);
            put("prick", true);
            put("pube", true);
            put("pussy", true);
            put("queer", true);
            put("scrotum", true);
            put("sex", true);
            put("shit", true);
            put("shithead", true);
            put("s", true);
            put("hit", true);
            put("sh1t", true);
            put("slut", true);
            put("smegma", true);
            put("spunk", true);
            put("tit", true);
            put("tosser", true);
            put("turd", true);
            put("twat", true);
            put("vagina", true);
            put("wank", true);
            put("whore", true);
            put("wtf", true);
        }};
}
