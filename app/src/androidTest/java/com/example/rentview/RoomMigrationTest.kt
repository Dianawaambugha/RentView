package com.example.rentview

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.rentview.data.RentViewDatabase
import com.example.rentview.data.MIGRATION_5_6

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

import java.io.File

@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {

    @Test
    fun migrationFrom5To6PreservesAllData() {

        val instrumentation =
            InstrumentationRegistry.getInstrumentation()

        val context = instrumentation.targetContext
        val testContext = instrumentation.context

        val databaseName = "rentview_room_validation.db"

        // Ensure the application database directory exists.
        val databaseFile = context.getDatabasePath(databaseName)
        databaseFile.parentFile?.mkdirs()

        // Copy the sample database from the test APK assets.
        val prepackagedDatabase =
            File(context.cacheDir, "rentview_migration_test.db")

        testContext.assets.open("rentview_migration_test.db").use { input ->
            prepackagedDatabase.writeBytes(input.readBytes())
        }

        // Start with a clean temporary database.
        context.deleteDatabase(databaseName)

        val database = Room.databaseBuilder(
            context,
            RentViewDatabase::class.java,
            databaseName
        )
            .createFromFile(prepackagedDatabase)
            .addMigrations(MIGRATION_5_6)
            .build()

        try {
            val db = database.openHelper.writableDatabase

            // Check database version.
            assertEquals(6, db.version)

            // Check all six tables.
            assertEquals(2, countRows(db, "users"))
            assertEquals(1, countRows(db, "properties"))
            assertEquals(1, countRows(db, "property_photos"))
            assertEquals(1, countRows(db, "property_edit_requests"))
            assertEquals(1, countRows(db, "property_edit_photos"))
            assertEquals(1, countRows(db, "viewings"))

            // Check original user IDs.
            assertEquals("Test Owner", getUserName(db, 1))
            assertEquals("Test Tenant", getUserName(db, 2))

            // Check foreign-key integrity.
            val foreignKeyCursor =
                db.query("PRAGMA foreign_key_check")

            try {
                assertFalse(
                    "Foreign-key errors were found",
                    foreignKeyCursor.moveToFirst()
                )
            } finally {
                foreignKeyCursor.close()
            }

        } finally {
            database.close()
            context.deleteDatabase(databaseName)
            prepackagedDatabase.delete()
        }
    }

    private fun countRows(
        db: androidx.sqlite.db.SupportSQLiteDatabase,
        table: String
    ): Int {
        val cursor = db.query("SELECT COUNT(*) FROM $table")

        return try {
            cursor.moveToFirst()
            cursor.getInt(0)
        } finally {
            cursor.close()
        }
    }

    private fun getUserName(
        db: androidx.sqlite.db.SupportSQLiteDatabase,
        userId: Int
    ): String {
        val cursor = db.query(
            "SELECT full_name FROM users WHERE id = $userId"
        )

        return try {
            assertTrue(cursor.moveToFirst())
            cursor.getString(0)
        } finally {
            cursor.close()
        }
    }
}