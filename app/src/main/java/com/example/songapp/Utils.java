package com.example.songapp;

import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static String mGuess;
    private static boolean guessedAlternateTitle = false;

    private static Map<String, String> mSpecialCharacters = new HashMap<String, String>() {{
        put("á", "a");
        put("é", "e");
        put("í", "i");
        put("ó", "o");
        put("ú", "u");
        put("ñ", "n");
    }};

    public static String parsePlaylistLink(String playlistLink)
    {
        String pattern = "[^/?]+(?=\\?)";

        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(playlistLink);

        if(matcher.find())
        {
            return matcher.group(0);
        }
        else
        {
            return "";
        }
    }

    public static Pair<Boolean, Boolean> parseGuess(String guess,
                                                    String currentName, String currentArtist)
    {
        Log.d("Utils:parseGuess", "guessBefore: " + guess);
        Pair<Boolean, Boolean> nameArtistResult = new Pair<>(false, false);

        mGuess = guess.toLowerCase();

        if(guess.isEmpty())
        {
            return nameArtistResult;
        }

        String lGuess = removePunctuation(guess.toLowerCase());
        String lCurrentName = removePunctuation(currentName.toLowerCase());
        String lCurrentArtist = removePunctuation(currentArtist.toLowerCase());

        Log.d("Utils:parseGuess", "guessAfter: " + lGuess);
        Log.d("Utils:parseGuess", "lCurrentName: " + lCurrentName);
        Log.d("Utils:parseGuess", "lCurrentArtist: " + lCurrentArtist);
        Log.d("Utils:parseGuess", "guessedAlternateTitle: " + guessedAlternateTitle);

        if((guessedAlternateTitle || validateGuess(lGuess, lCurrentName) || lGuess.contains(lCurrentName)) &&
                (validateGuess(lGuess, lCurrentArtist) || lGuess.contains(lCurrentArtist)))
        {
            nameArtistResult = new Pair<>(true, true);
            guessedAlternateTitle = false;
        }
        else if(guessedAlternateTitle || validateGuess(lGuess, lCurrentName) || lGuess.contains(lCurrentName))
        {
            nameArtistResult = new Pair<>(true, false);
            guessedAlternateTitle = false;
        }
        else if(validateGuess(lGuess, lCurrentArtist) || lGuess.contains(lCurrentArtist))
        {
            nameArtistResult = new Pair<>(false, true);
        }

        return nameArtistResult;
    }

    private static String removePunctuation(String s)
    {
        String stringToModify = s;
        String parenthesesRegex = "[^(?]+(?=\\))";
        String dashRegex = "(?<=\\-)\\s*(.*)";
        int numWordsMatch = 0;

        Pattern pattern = Pattern.compile(parenthesesRegex);
        Matcher matcher = pattern.matcher(stringToModify);

        if(matcher.find())
        {
            // determine if the song name has an alternate title
            String textInParentheses = matcher.group(0);
            String[] words = textInParentheses.split("\\s+");

            for(String word : words)
            {
                if(mGuess.contains(word))
                {
                    numWordsMatch++;
                }
            }

            // if the user guessed the alternate title,
            // keep the text in the parentheses
            if(numWordsMatch == words.length)
            {
                guessedAlternateTitle = true;
            }
            else
            {
                stringToModify =
                        stringToModify.replaceAll(
                                "\\(" + matcher.group(0) + "\\)", "");
            }
        }

        pattern = Pattern.compile(dashRegex);
        matcher = pattern.matcher(stringToModify);

        if(matcher.find())
        {
            stringToModify = stringToModify.replaceAll("\\-" + matcher.group(0), "");
        }

        stringToModify = stringToModify.replaceAll("\\p{Punct}", "");

        for(Map.Entry<String, String> entry : mSpecialCharacters.entrySet())
        {
            stringToModify = stringToModify.replaceAll(entry.getKey(), entry.getValue());
        }

        return stringToModify.trim();
    }

    private static boolean validateGuess(String guess, String info)
    {
        int numWordsMatch = 0;
        int length = info.split("\\s+").length;

        String[] guessArray = guess.split("\\s+");

        for(String s : guessArray)
        {
            if(info.contains(s))
            {
                numWordsMatch++;
            }
        }

        double similarity = (double) numWordsMatch / (double) length;
        if(similarity >= 0.5)
        {
            return true;
        }

        return false;
    }
}
