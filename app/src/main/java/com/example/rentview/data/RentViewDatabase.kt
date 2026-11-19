package com.example.rentview.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        PropertyEntity::class,
        PropertyPhotoEntity::class,
        ViewingEntity::class,
        PropertyEditRequestEntity::class,
        PropertyEditPhotoEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class RentViewDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun propertyDao(): PropertyDao
    abstract fun propertyPhotoDao(): PropertyPhotoDao
    abstract fun viewingDao(): ViewingDao
    abstract fun propertyEditRequestDao(): PropertyEditRequestDao
    abstract fun propertyEditPhotoDao(): PropertyEditPhotoDao

    companion object {

        @Volatile
        private var INSTANCE: RentViewDatabase? = null

        fun getInstance(context: Context): RentViewDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RentViewDatabase::class.java,
                    "rentview.db"
                )
                    .addMigrations(
                        MIGRATION_3_6,
                        MIGRATION_5_6
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}