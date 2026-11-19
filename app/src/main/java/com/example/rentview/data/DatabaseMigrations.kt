package com.example.rentview.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// ============================================================
// MIGRATION FROM DATABASE VERSION 3 TO VERSION 6
// ============================================================

val MIGRATION_3_6 = object : Migration(3, 6) {

        override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL("PRAGMA defer_foreign_keys = ON")

                // 1. Back up original data
                db.execSQL("CREATE TABLE users_data AS SELECT * FROM users")
                db.execSQL("CREATE TABLE properties_data AS SELECT * FROM properties")
                db.execSQL("CREATE TABLE property_photos_data AS SELECT * FROM property_photos")

                // 2. Drop original tables
                db.execSQL("DROP TABLE property_photos")
                db.execSQL("DROP TABLE properties")
                db.execSQL("DROP TABLE users")

                // 3. Create users table
                db.execSQL("""
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                full_name TEXT NOT NULL,
                email TEXT NOT NULL,
                phone TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                role TEXT NOT NULL,
                approval_status TEXT NOT NULL DEFAULT 'PENDING'
            )
        """)

                // 4. Create properties table
                db.execSQL("""
            CREATE TABLE properties (
                property_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                owner_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                location TEXT NOT NULL,
                rent REAL NOT NULL,
                bedrooms INTEGER NOT NULL,
                description TEXT NOT NULL,
                property_status TEXT NOT NULL DEFAULT 'PENDING',
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(owner_id)
                    REFERENCES users(id)
                    ON UPDATE NO ACTION ON DELETE NO ACTION
            )
        """)

                // 5. Create property photos table
                db.execSQL("""
            CREATE TABLE property_photos (
                photo_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                property_id INTEGER NOT NULL,
                photo_uri TEXT NOT NULL,
                FOREIGN KEY(property_id)
                    REFERENCES properties(property_id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """)

                // 6. Create viewings table
                db.execSQL("""
            CREATE TABLE viewings (
                viewing_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                property_id INTEGER NOT NULL,
                tenant_id INTEGER NOT NULL,
                viewing_date TEXT NOT NULL,
                viewing_slot TEXT NOT NULL,
                FOREIGN KEY(property_id)
                    REFERENCES properties(property_id)
                    ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(tenant_id)
                    REFERENCES users(id)
                    ON UPDATE NO ACTION ON DELETE NO ACTION
            )
        """)

                // 7. Create property edit requests table
                db.execSQL("""
            CREATE TABLE property_edit_requests (
                edit_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                property_id INTEGER NOT NULL,
                proposed_title TEXT NOT NULL,
                proposed_location TEXT NOT NULL,
                proposed_rent REAL NOT NULL,
                proposed_bedrooms INTEGER NOT NULL,
                proposed_description TEXT NOT NULL,
                edit_status TEXT NOT NULL DEFAULT 'PENDING',
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(property_id)
                    REFERENCES properties(property_id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """)

                // 8. Create property edit photos table
                db.execSQL("""
            CREATE TABLE property_edit_photos (
                edit_photo_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                edit_id INTEGER NOT NULL,
                photo_uri TEXT NOT NULL,
                FOREIGN KEY(edit_id)
                    REFERENCES property_edit_requests(edit_id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """)

                // 9. Restore original users
                db.execSQL("""
            INSERT INTO users (
                id, full_name, email, phone,
                password_hash, role, approval_status
            )
            SELECT
                id, full_name, email, phone,
                password_hash, role, approval_status
            FROM users_data
        """)

                // 10. Restore original properties
                db.execSQL("""
            INSERT INTO properties (
                property_id, owner_id, title, location,
                rent, bedrooms, description,
                property_status, created_at
            )
            SELECT
                property_id, owner_id, title, location,
                rent, bedrooms, description,
                property_status, created_at
            FROM properties_data
        """)

                // 11. Restore original property photos
                db.execSQL("""
            INSERT INTO property_photos (
                photo_id, property_id, photo_uri
            )
            SELECT
                photo_id, property_id, photo_uri
            FROM property_photos_data
        """)

                // 12. Remove temporary backup tables
                db.execSQL("DROP TABLE users_data")
                db.execSQL("DROP TABLE properties_data")
                db.execSQL("DROP TABLE property_photos_data")

                // 13. Create Room indexes
                createRoomIndexes(db)
        }
}


// ============================================================
// MIGRATION FROM DATABASE VERSION 5 TO VERSION 6
// ============================================================

val MIGRATION_5_6 = object : Migration(5, 6) {

        override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL("PRAGMA defer_foreign_keys = ON")

                // 1. Back up all existing tables
                db.execSQL("CREATE TABLE users_data AS SELECT * FROM users")
                db.execSQL("CREATE TABLE properties_data AS SELECT * FROM properties")
                db.execSQL("CREATE TABLE property_photos_data AS SELECT * FROM property_photos")
                db.execSQL("CREATE TABLE viewings_data AS SELECT * FROM viewings")
                db.execSQL("CREATE TABLE property_edit_requests_data AS SELECT * FROM property_edit_requests")
                db.execSQL("CREATE TABLE property_edit_photos_data AS SELECT * FROM property_edit_photos")

                // 2. Drop tables in child-to-parent order
                db.execSQL("DROP TABLE property_edit_photos")
                db.execSQL("DROP TABLE property_edit_requests")
                db.execSQL("DROP TABLE viewings")
                db.execSQL("DROP TABLE property_photos")
                db.execSQL("DROP TABLE properties")
                db.execSQL("DROP TABLE users")

                // 3. Recreate users table
                db.execSQL("""
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                full_name TEXT NOT NULL,
                email TEXT NOT NULL,
                phone TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                role TEXT NOT NULL,
                approval_status TEXT NOT NULL DEFAULT 'PENDING'
            )
        """)

                // 4. Recreate properties table
                db.execSQL("""
            CREATE TABLE properties (
                property_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                owner_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                location TEXT NOT NULL,
                rent REAL NOT NULL,
                bedrooms INTEGER NOT NULL,
                description TEXT NOT NULL,
                property_status TEXT NOT NULL DEFAULT 'PENDING',
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(owner_id)
                    REFERENCES users(id)
                    ON UPDATE NO ACTION ON DELETE NO ACTION
            )
        """)

                // 5. Recreate property photos table
                db.execSQL("""
            CREATE TABLE property_photos (
                photo_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                property_id INTEGER NOT NULL,
                photo_uri TEXT NOT NULL,
                FOREIGN KEY(property_id)
                    REFERENCES properties(property_id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """)

                // 6. Recreate viewings table
                db.execSQL("""
            CREATE TABLE viewings (
                viewing_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                property_id INTEGER NOT NULL,
                tenant_id INTEGER NOT NULL,
                viewing_date TEXT NOT NULL,
                viewing_slot TEXT NOT NULL,
                FOREIGN KEY(property_id)
                    REFERENCES properties(property_id)
                    ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(tenant_id)
                    REFERENCES users(id)
                    ON UPDATE NO ACTION ON DELETE NO ACTION
            )
        """)

                // 7. Recreate property edit requests table
                db.execSQL("""
            CREATE TABLE property_edit_requests (
                edit_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                property_id INTEGER NOT NULL,
                proposed_title TEXT NOT NULL,
                proposed_location TEXT NOT NULL,
                proposed_rent REAL NOT NULL,
                proposed_bedrooms INTEGER NOT NULL,
                proposed_description TEXT NOT NULL,
                edit_status TEXT NOT NULL DEFAULT 'PENDING',
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(property_id)
                    REFERENCES properties(property_id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """)

                // 8. Recreate property edit photos table
                db.execSQL("""
            CREATE TABLE property_edit_photos (
                edit_photo_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                edit_id INTEGER NOT NULL,
                photo_uri TEXT NOT NULL,
                FOREIGN KEY(edit_id)
                    REFERENCES property_edit_requests(edit_id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """)

                // 9. Restore users
                db.execSQL("""
            INSERT INTO users (
                id, full_name, email, phone,
                password_hash, role, approval_status
            )
            SELECT
                id, full_name, email, phone,
                password_hash, role, approval_status
            FROM users_data
        """)

                // 10. Restore properties
                db.execSQL("""
            INSERT INTO properties (
                property_id, owner_id, title, location,
                rent, bedrooms, description,
                property_status, created_at
            )
            SELECT
                property_id, owner_id, title, location,
                rent, bedrooms, description,
                property_status, created_at
            FROM properties_data
        """)

                // 11. Restore property photos
                db.execSQL("""
            INSERT INTO property_photos (
                photo_id, property_id, photo_uri
            )
            SELECT
                photo_id, property_id, photo_uri
            FROM property_photos_data
        """)

                // 12. Restore viewings
                db.execSQL("""
            INSERT INTO viewings (
                viewing_id, property_id, tenant_id,
                viewing_date, viewing_slot
            )
            SELECT
                viewing_id, property_id, tenant_id,
                viewing_date, viewing_slot
            FROM viewings_data
        """)

                // 13. Restore property edit requests
                db.execSQL("""
            INSERT INTO property_edit_requests (
                edit_id, property_id, proposed_title,
                proposed_location, proposed_rent,
                proposed_bedrooms, proposed_description,
                edit_status, created_at
            )
            SELECT
                edit_id, property_id, proposed_title,
                proposed_location, proposed_rent,
                proposed_bedrooms, proposed_description,
                edit_status, created_at
            FROM property_edit_requests_data
        """)

                // 14. Restore property edit photos
                db.execSQL("""
            INSERT INTO property_edit_photos (
                edit_photo_id, edit_id, photo_uri
            )
            SELECT
                edit_photo_id, edit_id, photo_uri
            FROM property_edit_photos_data
        """)

                // 15. Remove temporary backup tables
                db.execSQL("DROP TABLE users_data")
                db.execSQL("DROP TABLE properties_data")
                db.execSQL("DROP TABLE property_photos_data")
                db.execSQL("DROP TABLE viewings_data")
                db.execSQL("DROP TABLE property_edit_requests_data")
                db.execSQL("DROP TABLE property_edit_photos_data")

                // 16. Recreate Room indexes
                createRoomIndexes(db)
        }
}


// ============================================================
// SHARED ROOM INDEX CREATION
// ============================================================

private fun createRoomIndexes(db: SupportSQLiteDatabase) {

        db.execSQL("""
        CREATE UNIQUE INDEX index_users_email
        ON users(email)
    """)

        db.execSQL("""
        CREATE INDEX index_properties_owner_id
        ON properties(owner_id)
    """)

        db.execSQL("""
        CREATE INDEX index_property_photos_property_id
        ON property_photos(property_id)
    """)

        db.execSQL("""
        CREATE UNIQUE INDEX idx_viewing_unique_slot
        ON viewings(property_id, viewing_date, viewing_slot)
    """)

        db.execSQL("""
        CREATE INDEX index_viewings_tenant_id
        ON viewings(tenant_id)
    """)

        db.execSQL("""
        CREATE INDEX index_property_edit_requests_property_id
        ON property_edit_requests(property_id)
    """)

        db.execSQL("""
        CREATE INDEX index_property_edit_photos_edit_id
        ON property_edit_photos(edit_id)
    """)
}