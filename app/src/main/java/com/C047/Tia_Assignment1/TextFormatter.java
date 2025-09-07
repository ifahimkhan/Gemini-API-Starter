package com.C047.Tia_Assignment1;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextFormatter {

    public static SpannableString formatText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new SpannableString("");
        }

        SpannableString spannableString = new SpannableString(text);

        // Pattern to find text enclosed in **
        Pattern boldPattern = Pattern.compile("\\*\\*(.*?)\\*\\*");
        Matcher boldMatcher = boldPattern.matcher(spannableString);

        // List to store the start and end indices of the bold text
        while (boldMatcher.find()) {
            int start = boldMatcher.start();
            int end = boldMatcher.end();
            String boldText = boldMatcher.group(1);

            // Remove ** from the original string
            assert boldText != null;
            String newText = spannableString.toString().replace("**" + boldText + "**", boldText);
            spannableString = new SpannableString(newText);

            // Find the new start and end indices of the bold text
            int newStart = newText.indexOf(boldText);
            int newEnd = newStart + boldText.length();

            // Apply bold style to the text
            spannableString.setSpan(new StyleSpan(Typeface.BOLD), newStart, newEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Update the matcher with the new string
            boldMatcher = boldPattern.matcher(spannableString);
        }

        return spannableString;
    }

    public static SpannableStringBuilder getBoldSpannableText(String input) {
        SpannableStringBuilder spannableBuilder = new SpannableStringBuilder();
        Pattern pattern = Pattern.compile("\\*\\*(.*?)\\*\\*");
        Matcher matcher = pattern.matcher(input);
        int lastIndex = 0;

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            // Append the text before the matched **bold** text
            spannableBuilder.append(input.substring(lastIndex, start));

            // Extract the bold text without the **
            String boldText = matcher.group(1);
            SpannableString spannable = new SpannableString(boldText);
            assert boldText != null;
            spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, boldText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Append the bold text
            spannableBuilder.append(spannable);

            lastIndex = end;
        }

        // Append remaining text after the last match
        if (lastIndex < input.length()) {
            spannableBuilder.append(input.substring(lastIndex));
        }

        return spannableBuilder;
    }}