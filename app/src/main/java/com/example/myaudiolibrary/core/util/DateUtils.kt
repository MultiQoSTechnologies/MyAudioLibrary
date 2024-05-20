package com.example.myaudiolibrary.core.util

import android.text.format.DateUtils

object DateUtils {
    fun formatAsRelativeTimeSpan(mills: Long) =
        DateUtils.getRelativeTimeSpanString(
            mills,
            System.currentTimeMillis(),
            DateUtils.DAY_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ) as String
}