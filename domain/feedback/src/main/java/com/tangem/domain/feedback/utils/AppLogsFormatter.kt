package com.tangem.domain.feedback.utils

import com.tangem.domain.feedback.models.AppLogModel
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormatterBuilder
import java.util.Locale

/**
 * App logs formatter
 *
[REDACTED_AUTHOR]
 */
internal class AppLogsFormatter {

    private val dateFormatter = createDateFormatter()

    /** Format [appLogs] to [String] */
    fun format(appLogs: List<AppLogModel>): String {
        val builder = StringBuilder()

        var sum = 0
        for (i in appLogs.lastIndex downTo 0) {
            val log = appLogs[i]
            val date = dateFormatter.print(DateTime(log.timestamp))

            val formattedLog = "$date: ${log.message}\n"

            sum += formattedLog.length
            if (sum < GMAIL_MAX_FILE_SIZE) {
                builder.insert(0, formattedLog)
            } else {
                break
            }
        }

        return builder.toString()
    }

    // Example, 00.00 00:00:00.000
    private fun createDateFormatter(): DateTimeFormatter {
        return DateTimeFormatterBuilder()
            .appendDayOfMonth(2)
            .appendLiteral('.')
            .appendMonthOfYear(2)
            .appendLiteral(' ')
            .appendHourOfDay(2)
            .appendLiteral(':')
            .appendMinuteOfHour(2)
            .appendLiteral(':')
            .appendSecondOfMinute(2)
            .appendLiteral('.')
            .appendMillisOfSecond(MIN_MILLIS_DIGITS)
            .toFormatter()
            .withLocale(Locale.getDefault())
    }

    private companion object {
        const val MIN_MILLIS_DIGITS = 3
    }
}