package com.dgsd.android.MangaReader.Util;

import android.content.Context;
import android.text.format.DateUtils;

public class TimeUtil {
    public static final int JULIAN_DAY_AT_RATA_EPOCH = 1721425;

    public static final int getJulianDay(int rataDay) {
        return JULIAN_DAY_AT_RATA_EPOCH + rataDay;
    }

    public static String getDateString(Context context, long millis) {
        return DateUtils.formatDateTime(context, millis,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_ALL);
    }

}
