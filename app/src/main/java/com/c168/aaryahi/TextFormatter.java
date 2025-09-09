package com.c168.aaryahi;

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

        Pattern boldPattern = Pattern.compile("\\*\\*(.*?)\\*\\*");
        Matcher boldMatcher = boldPattern.matcher(spannableString);

        while (boldMatcher.find()) {
            int start = boldMatcher.start();
            int end = boldMatcher.end();
            String boldText = boldMatcher.group(1);

            assert boldText != null;
            String newText = spannableString.toString().replace("**" + boldText + "**", boldText);
            spannableString = new SpannableString(newText);

            int newStart = newText.indexOf(boldText);
            int newEnd = newStart + boldText.length();

            spannableString.setSpan(new StyleSpan(Typeface.BOLD), newStart, newEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

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

            spannableBuilder.append(input.substring(lastIndex, start));

            String boldText = matcher.group(1);
            SpannableString spannable = new SpannableString(boldText);
            assert boldText != null;
            spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, boldText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            spannableBuilder.append(spannable);

            lastIndex = end;
        }

        if (lastIndex < input.length()) {
            spannableBuilder.append(input.substring(lastIndex));
        }

        return spannableBuilder;
    }}