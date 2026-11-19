package com.ro.aminvatatro.core.time

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * ISO 8601 week-year and week-of-year number (`1..53`).
 */
data class IsoWeekInfo(
    val isoYear: Int,
    val weekNumber: Int,
)

/**
 * Pure calendar utilities for Monday-to-Sunday weeks (ISO 8601).
 */
object WeekUtils {

    /** Number of consecutive days in a week. */
    const val DAYS_IN_WEEK: Int = 7

    /**
     * Returns the Monday of the week containing [date].
     */
    fun weekStartOf(date: LocalDate): LocalDate {
        val offsetFromMonday = date.dayOfWeek.ordinal
        return if (offsetFromMonday == 0) date else date.minus(offsetFromMonday, DateTimeUnit.DAY)
    }

    /**
     * Returns the Sunday (last day) of the week containing [date].
     */
    fun weekEndOf(date: LocalDate): LocalDate =
        weekStartOf(date).plus(DAYS_IN_WEEK - 1, DateTimeUnit.DAY)

    /**
     * Returns the 7 consecutive calendar dates (Monday through Sunday) for the week containing [date].
     */
    fun weekDaysOf(date: LocalDate): List<LocalDate> {
        val monday = weekStartOf(date)
        return List(DAYS_IN_WEEK) { offset -> monday.plus(offset, DateTimeUnit.DAY) }
    }

    /**
     * Returns the Monday of the week immediately following the week containing [date].
     */
    fun nextWeek(date: LocalDate): LocalDate =
        weekStartOf(date).plus(DAYS_IN_WEEK, DateTimeUnit.DAY)

    /**
     * Returns the Monday of the week immediately preceding the week containing [date].
     */
    fun previousWeek(date: LocalDate): LocalDate =
        weekStartOf(date).minus(DAYS_IN_WEEK, DateTimeUnit.DAY)

    /**
     * Computes the ISO 8601 week-numbering year and week number (`1..53`) containing [date].
     *
     * Handles year boundaries and 53-week ISO years (e.g. 2020-W53, 2026-W53).
     */
    fun isoWeekInfo(date: LocalDate): IsoWeekInfo {
        val monday = weekStartOf(date)
        val thursday = monday.plus(3, DateTimeUnit.DAY)
        val isoYear = thursday.year
        val weekOneMonday = weekStartOf(LocalDate(isoYear, 1, 4))
        val weekNumber = (weekOneMonday.daysUntil(monday) / DAYS_IN_WEEK) + 1
        return IsoWeekInfo(isoYear = isoYear, weekNumber = weekNumber)
    }
}
