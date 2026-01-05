package com.dailystuffapp.domain

enum class DayOfWeek(val value: String) {
    MONDAY("MONDAY"),
    TUESDAY("TUESDAY"),
    WEDNESDAY("WEDNESDAY"),
    THURSDAY("THURSDAY"),
    FRIDAY("FRIDAY"),
    SATURDAY("SATURDAY"),
    SUNDAY("SUNDAY");

    companion object {
        fun fromString(value: String): DayOfWeek? {
            return values().find { it.value == value }
        }

        fun fromJavaDayOfWeek(javaDayOfWeek: java.time.DayOfWeek): DayOfWeek {
            return when (javaDayOfWeek) {
                java.time.DayOfWeek.MONDAY -> MONDAY
                java.time.DayOfWeek.TUESDAY -> TUESDAY
                java.time.DayOfWeek.WEDNESDAY -> WEDNESDAY
                java.time.DayOfWeek.THURSDAY -> THURSDAY
                java.time.DayOfWeek.FRIDAY -> FRIDAY
                java.time.DayOfWeek.SATURDAY -> SATURDAY
                java.time.DayOfWeek.SUNDAY -> SUNDAY
            }
        }

        fun parseDaysString(daysString: String): List<DayOfWeek> {
            if (daysString.isBlank()) return emptyList()
            return daysString.split(",")
                .mapNotNull { fromString(it.trim()) }
        }

        fun daysToString(days: List<DayOfWeek>): String {
            return days.joinToString(",") { it.value }
        }
    }
}

