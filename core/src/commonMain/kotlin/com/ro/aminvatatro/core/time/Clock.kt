package com.ro.aminvatatro.core.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Injectable abstraction over the current time and calendar date for deterministic testing.
 */
interface Clock {
    /** Current wall-clock timestamp in milliseconds since Unix epoch (UTC). */
    fun nowEpochMillis(): Long

    /** Current calendar date in the active time zone. */
    fun today(): LocalDate
}

/**
 * Default [Clock] backed by the system clock and the time zone supplied by [timeZoneProvider].
 */
class SystemClock(
    private val timeZoneProvider: () -> TimeZone = { TimeZone.currentSystemDefault() },
) : Clock {

    override fun nowEpochMillis(): Long =
        kotlin.time.Clock.System.now().toEpochMilliseconds()

    override fun today(): LocalDate =
        kotlin.time.Clock.System.todayIn(timeZoneProvider())
}
