package com.classbell.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.classbell.app.data.model.Affair
import com.classbell.app.data.model.Course
import com.classbell.app.data.model.Holiday
import com.classbell.app.data.model.Override
import com.classbell.app.data.model.PeriodSlot
import com.classbell.app.data.model.Semester
import com.classbell.app.data.seed.SeedDataProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Semester::class,
        Course::class,
        PeriodSlot::class,
        Holiday::class,
        Affair::class,
        Override::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ClassBellDatabase : RoomDatabase() {

    abstract fun semesterDao(): SemesterDao
    abstract fun courseDao(): CourseDao
    abstract fun periodSlotDao(): PeriodSlotDao
    abstract fun holidayDao(): HolidayDao
    abstract fun affairDao(): AffairDao
    abstract fun overrideDao(): OverrideDao

    companion object {
        @Volatile
        private var INSTANCE: ClassBellDatabase? = null

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE affairs ADD COLUMN isDone INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): ClassBellDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ClassBellDatabase::class.java,
                    "classbell.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(DatabaseSeedCallback(context.applicationContext))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseSeedCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val database = getDatabase(context)
                    // Seed initial data
                    database.semesterDao().insert(SeedDataProvider.getDefaultSemester())
                    database.holidayDao().insertAll(SeedDataProvider.getDefaultHolidays())
                    database.periodSlotDao().insertAll(SeedDataProvider.getDefaultPeriodSlots())
                    database.courseDao().insertAll(SeedDataProvider.getDefaultCourses())
                }
            }
        }
    }
}
