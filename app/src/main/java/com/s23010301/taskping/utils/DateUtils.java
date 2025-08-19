package com.s23010301.taskping.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateUtils {
    public static String formatDate(Calendar calendar, String pattern) {
        if (calendar == null) return "";
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(calendar.getTime());
    }


    public static String getCurrentDate(String pattern) {
        return formatDate(Calendar.getInstance(), pattern);
    }
}