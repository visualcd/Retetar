package com.ro.aminvatatro.core.time

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class WeekUtilsTest {

    @Test
    fun weekStartAndDaysSpanMondayToSunday() {
        val expectedMonday = LocalDate(2026, 3, 2) // Monday
        val expectedSunday = LocalDate(2026, 3, 8) // Sunday
        val weekDays = WeekUtils.weekDaysOf(LocalDate(2026, 3, 5)) // Thursday

        assertEquals(7, weekDays.size)
        assertEquals(expectedMonday, weekDays.first())
        assertEquals(expectedSunday, weekDays.last())
        assertEquals(DayOfWeek.MONDAY, weekDays.first().dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, weekDays.last().dayOfWeek)

        for (day in weekDays) {
            assertEquals(expectedMonday, WeekUtils.weekStartOf(day))
            assertEquals(expectedSunday, WeekUtils.weekEndOf(day))
        }
    }

    @Test
    fun navigatesToNextAndPreviousWeek() {
        val wednesday = LocalDate(2026, 3, 4)
        assertEquals(LocalDate(2026, 3, 9), WeekUtils.nextWeek(wednesday))
        assertEquals(LocalDate(2026, 2, 23), WeekUtils.previousWeek(wednesday))
    }

    @Test
    fun handlesYearTransitionOnDecember31AndJanuary1() {
        // 2024-12-31 is Tuesday -> week 2024-12-30..2025-01-05 (ISO 2025-W01)
        val dec31_2024 = LocalDate(2024, 12, 31)
        assertEquals(LocalDate(2024, 12, 30), WeekUtils.weekStartOf(dec31_2024))
        assertEquals(LocalDate(2025, 1, 5), WeekUtils.weekEndOf(dec31_2024))
        assertEquals(IsoWeekInfo(isoYear = 2025, weekNumber = 1), WeekUtils.isoWeekInfo(dec31_2024))
        assertEquals(IsoWeekInfo(isoYear = 2025, weekNumber = 1), WeekUtils.isoWeekInfo(LocalDate(2025, 1, 1)))

        // 2023-12-31 is Sunday -> week 2023-12-25..2023-12-31 (ISO 2023-W52)
        val dec31_2023 = LocalDate(2023, 12, 31)
        assertEquals(LocalDate(2023, 12, 25), WeekUtils.weekStartOf(dec31_2023))
        assertEquals(IsoWeekInfo(isoYear = 2023, weekNumber = 52), WeekUtils.isoWeekInfo(dec31_2023))
    }

    @Test
    fun handlesIsoWeek53AcrossYearBoundary() {
        // 2020 has 53 ISO weeks: 2020-12-28 (Mon) .. 2021-01-03 (Sun)
        val dec31_2020 = LocalDate(2020, 12, 31)
        assertEquals(LocalDate(2020, 12, 28), WeekUtils.weekStartOf(dec31_2020))
        assertEquals(LocalDate(2021, 1, 3), WeekUtils.weekEndOf(dec31_2020))
        assertEquals(IsoWeekInfo(isoYear = 2020, weekNumber = 53), WeekUtils.isoWeekInfo(dec31_2020))
        assertEquals(IsoWeekInfo(isoYear = 2020, weekNumber = 53), WeekUtils.isoWeekInfo(LocalDate(2021, 1, 1)))
        assertEquals(IsoWeekInfo(isoYear = 2020, weekNumber = 53), WeekUtils.isoWeekInfo(LocalDate(2021, 1, 3)))
        assertEquals(IsoWeekInfo(isoYear = 2021, weekNumber = 1), WeekUtils.isoWeekInfo(LocalDate(2021, 1, 4)))

        // 2026 also has 53 ISO weeks: 2026-12-28 (Mon) .. 2027-01-03 (Sun)
        val dec31_2026 = LocalDate(2026, 12, 31)
        assertEquals(LocalDate(2026, 12, 28), WeekUtils.weekStartOf(dec31_2026))
        assertEquals(IsoWeekInfo(isoYear = 2026, weekNumber = 53), WeekUtils.isoWeekInfo(dec31_2026))
    }

    @Test
    fun handlesLeapYearFebruary29() {
        val leapDay = LocalDate(2024, 2, 29) // Thursday
        val days = WeekUtils.weekDaysOf(leapDay)
        assertEquals(LocalDate(2024, 2, 26), days.first())
        assertEquals(LocalDate(2024, 3, 3), days.last())
        assertEquals(7, days.size)
    }
}
