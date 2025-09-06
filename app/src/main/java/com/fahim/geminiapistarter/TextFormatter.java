package com.fahim.geminiapistarter;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextFormatter {
    public static SpannableStringBuilder getBoldSpannableText(String input) {
        SpannableStringBuilder spannableBuilder = new SpannableStringBuilder();
        Pattern pattern = Pattern.compile("\\*\\*(.*?)\\*\\*");
        Matcher matcher = pattern.matcher(input);
        int lastIndex = 0;

        while (matcher.find()) {
            spannableBuilder.append(input, lastIndex, matcher.start());

            String boldText = matcher.group(1);
            if (boldText != null) {
                SpannableString spannable = new SpannableString(boldText);
                spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, boldText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableBuilder.append(spannable);
            }

            lastIndex = matcher.end();
        }

        if (lastIndex < input.length()) {
            spannableBuilder.append(input.substring(lastIndex));
        }

        return spannableBuilder;
    }
}
