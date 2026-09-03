package com.classbell.app.data.local

import androidx.room.TypeConverter
import com.classbell.app.data.model.AffairCategory
import com.classbell.app.data.model.HolidayType
import com.classbell.app.data.model.OverrideKind
import com.classbell.app.data.model.ReminderChoice
import com.classbell.app.data.model.WeekPattern
import java.time.LocalDate
import java.time.LocalTime

class Converters {

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it) }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String? = value?.joinToString(",")

    @TypeConverter
    fun toIntList(value: String?): List<Int> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    @TypeConverter
    fun fromWeekPattern(value: WeekPattern): String = value.name

    @TypeConverter
    fun toWeekPattern(value: String): WeekPattern =
        runCatching { WeekPattern.valueOf(value) }.getOrDefault(WeekPattern.ALL)

    @TypeConverter
    fun fromHolidayType(value: HolidayType): String = value.name

    @TypeConverter
    fun toHolidayType(value: String): HolidayType =
        runCatching { HolidayType.valueOf(value) }.getOrDefault(HolidayType.REST)

    @TypeConverter
    fun fromOverrideKind(value: OverrideKind): String = value.name

    @TypeConverter
    fun toOverrideKind(value: String): OverrideKind =
        runCatching { OverrideKind.valueOf(value) }.getOrDefault(OverrideKind.CANCEL)

    @TypeConverter
    fun fromAffairCategory(value: AffairCategory): String = value.name

    @TypeConverter
    fun toAffairCategory(value: String): AffairCategory =
        runCatching { AffairCategory.valueOf(value) }.getOrDefault(AffairCategory.AFFAIR)

    @TypeConverter
    fun fromReminderChoice(value: ReminderChoice): String = value.name

    @TypeConverter
    fun toReminderChoice(value: String): ReminderChoice =
        runCatching { ReminderChoice.valueOf(value) }.getOrDefault(ReminderChoice.NONE)
}
