package com.example.rentview

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest

class UserDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "rentview.db"
        private const val DATABASE_VERSION = 3

        // Users table
        const val TABLE_USERS = "users"
        const val COL_ID = "id"
        const val COL_NAME = "full_name"
        const val COL_EMAIL = "email"
        const val COL_PHONE = "phone"
        const val COL_PASSWORD = "password_hash"
        const val COL_ROLE = "role"
        const val COL_APPROVAL_STATUS = "approval_status"

        // Property table
        const val TABLE_PROPERTIES = "properties"
        const val COL_PROPERTY_ID = "property_id"
        const val COL_OWNER_ID = "owner_id"
        const val COL_PROPERTY_TITLE = "title"
        const val COL_PROPERTY_LOCATION = "location"
        const val COL_PROPERTY_RENT = "rent"
        const val COL_PROPERTY_BEDROOMS = "bedrooms"
        const val COL_PROPERTY_DESCRIPTION = "description"
        const val COL_PROPERTY_STATUS = "property_status"
        const val COL_CREATED_AT = "created_at"

        // Property photos table
        const val TABLE_PROPERTY_PHOTOS = "property_photos"
        const val COL_PHOTO_ID = "photo_id"
        const val COL_PHOTO_URI = "photo_uri"

        // Roles
        const val ROLE_TENANT = "Tenant"
        const val ROLE_OWNER = "Property Owner"

        // Approval statuses
        const val STATUS_PENDING = "PENDING"
        const val STATUS_APPROVED = "APPROVED"
        const val STATUS_REJECTED = "REJECTED"
    }

    override fun onCreate(db: SQLiteDatabase) {

        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME TEXT NOT NULL,
                $COL_EMAIL TEXT NOT NULL UNIQUE,
                $COL_PHONE TEXT NOT NULL,
                $COL_PASSWORD TEXT NOT NULL,
                $COL_ROLE TEXT NOT NULL,
                $COL_APPROVAL_STATUS TEXT NOT NULL DEFAULT '$STATUS_PENDING'
            )
        """.trimIndent()

        db.execSQL(createUsersTable)

        createPropertyTables(db)
    }

    private fun createPropertyTables(db: SQLiteDatabase) {

        val createPropertiesTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_PROPERTIES (
                $COL_PROPERTY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_OWNER_ID INTEGER NOT NULL,
                $COL_PROPERTY_TITLE TEXT NOT NULL,
                $COL_PROPERTY_LOCATION TEXT NOT NULL,
                $COL_PROPERTY_RENT REAL NOT NULL,
                $COL_PROPERTY_BEDROOMS INTEGER NOT NULL,
                $COL_PROPERTY_DESCRIPTION TEXT NOT NULL,
                $COL_PROPERTY_STATUS TEXT NOT NULL DEFAULT '$STATUS_PENDING',
                $COL_CREATED_AT TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY ($COL_OWNER_ID)
                    REFERENCES $TABLE_USERS($COL_ID)
            )
        """.trimIndent()

        db.execSQL(createPropertiesTable)

        val createPhotosTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_PROPERTY_PHOTOS (
                $COL_PHOTO_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PROPERTY_ID INTEGER NOT NULL,
                $COL_PHOTO_URI TEXT NOT NULL,
                FOREIGN KEY ($COL_PROPERTY_ID)
                    REFERENCES $TABLE_PROPERTIES($COL_PROPERTY_ID)
                    ON DELETE CASCADE
            )
        """.trimIndent()

        db.execSQL(createPhotosTable)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {

        if (oldVersion < 2) {

            db.execSQL(
                """
                ALTER TABLE $TABLE_USERS
                ADD COLUMN $COL_APPROVAL_STATUS TEXT NOT NULL
                DEFAULT '$STATUS_PENDING'
                """.trimIndent()
            )

            db.execSQL(
                """
                UPDATE $TABLE_USERS
                SET $COL_APPROVAL_STATUS = '$STATUS_APPROVED'
                WHERE $COL_ROLE = '$ROLE_TENANT'
                """.trimIndent()
            )
        }

        if (oldVersion < 3) {
            createPropertyTables(db)
        }
    }

    // Register a tenant or property owner.
    fun registerUser(
        name: String,
        email: String,
        phone: String,
        password: String,
        role: String
    ): Boolean {

        val approvalStatus = when (role) {
            ROLE_TENANT -> STATUS_APPROVED
            ROLE_OWNER -> STATUS_PENDING
            else -> return false
        }

        val values = ContentValues().apply {
            put(COL_NAME, name.trim())
            put(COL_EMAIL, email.trim().lowercase())
            put(COL_PHONE, phone.trim())
            put(COL_PASSWORD, hashPassword(password))
            put(COL_ROLE, role)
            put(COL_APPROVAL_STATUS, approvalStatus)
        }

        return try {
            writableDatabase.insertOrThrow(
                TABLE_USERS,
                null,
                values
            ) != -1L
        } catch (e: SQLiteConstraintException) {
            false
        }
    }

    // Check login credentials.
    fun checkLogin(email: String, password: String): Boolean {

        val cursor = readableDatabase.query(
            TABLE_USERS,
            arrayOf(COL_ID),
            "$COL_EMAIL = ? AND $COL_PASSWORD = ?",
            arrayOf(
                email.trim().lowercase(),
                hashPassword(password)
            ),
            null,
            null,
            null
        )

        return cursor.use { it.moveToFirst() }
    }

    // Retrieve account details after checking login credentials.
    fun getUserDetails(
        email: String,
        password: String
    ): Cursor? {

        return readableDatabase.query(
            TABLE_USERS,
            arrayOf(
                COL_ID,
                COL_NAME,
                COL_ROLE,
                COL_APPROVAL_STATUS
            ),
            "$COL_EMAIL = ? AND $COL_PASSWORD = ?",
            arrayOf(
                email.trim().lowercase(),
                hashPassword(password)
            ),
            null,
            null,
            null
        )
    }

    // Check whether a property owner has been approved.
    fun isOwnerApproved(userId: Long): Boolean {

        val cursor = readableDatabase.query(
            TABLE_USERS,
            arrayOf(COL_APPROVAL_STATUS),
            "$COL_ID = ? AND $COL_ROLE = ?",
            arrayOf(userId.toString(), ROLE_OWNER),
            null,
            null,
            null
        )

        return cursor.use {
            it.moveToFirst() &&
                    it.getString(0) == STATUS_APPROVED
        }
    }

    // Retrieve property owners awaiting administrator approval.
    fun getPendingOwners(): Cursor {

        return readableDatabase.query(
            TABLE_USERS,
            arrayOf(
                COL_ID,
                COL_NAME,
                COL_EMAIL,
                COL_PHONE,
                COL_APPROVAL_STATUS
            ),
            "$COL_ROLE = ? AND $COL_APPROVAL_STATUS = ?",
            arrayOf(ROLE_OWNER, STATUS_PENDING),
            null,
            null,
            "$COL_NAME ASC"
        )
    }

    // Approve or reject a property owner.
    fun updateOwnerApproval(
        userId: Long,
        status: String
    ): Boolean {

        if (status != STATUS_APPROVED &&
            status != STATUS_REJECTED
        ) {
            return false
        }

        val values = ContentValues().apply {
            put(COL_APPROVAL_STATUS, status)
        }

        val rowsUpdated = writableDatabase.update(
            TABLE_USERS,
            values,
            "$COL_ID = ? AND $COL_ROLE = ?",
            arrayOf(userId.toString(), ROLE_OWNER)
        )

        return rowsUpdated > 0
    }

    // Save a house listing submitted by an owner.
    // New listings always begin as PENDING.
    fun addProperty(
        ownerId: Long,
        title: String,
        location: String,
        rent: Double,
        bedrooms: Int,
        description: String,
        photoUris: List<String>
    ): Long {

        if (title.isBlank() ||
            location.isBlank() ||
            rent <= 0 ||
            bedrooms < 0 ||
            description.isBlank()
        ) {
            return -1L
        }

        val db = writableDatabase
        var propertyId = -1L

        db.beginTransaction()

        try {

            // Confirm that the submitting owner is approved.
            val ownerCursor = db.query(
                TABLE_USERS,
                arrayOf(COL_ID),
                "$COL_ID = ? AND $COL_ROLE = ? AND $COL_APPROVAL_STATUS = ?",
                arrayOf(
                    ownerId.toString(),
                    ROLE_OWNER,
                    STATUS_APPROVED
                ),
                null,
                null,
                null
            )

            val ownerIsApproved = ownerCursor.use {
                it.moveToFirst()
            }

            if (!ownerIsApproved) {
                return -1L
            }

            val values = ContentValues().apply {
                put(COL_OWNER_ID, ownerId)
                put(COL_PROPERTY_TITLE, title.trim())
                put(COL_PROPERTY_LOCATION, location.trim())
                put(COL_PROPERTY_RENT, rent)
                put(COL_PROPERTY_BEDROOMS, bedrooms)
                put(COL_PROPERTY_DESCRIPTION, description.trim())
                put(COL_PROPERTY_STATUS, STATUS_PENDING)
            }

            propertyId = db.insert(
                TABLE_PROPERTIES,
                null,
                values
            )

            if (propertyId == -1L) {
                return -1L
            }

            // Save the selected photo URIs.
            for (photoUri in photoUris) {

                val photoValues = ContentValues().apply {
                    put(COL_PROPERTY_ID, propertyId)
                    put(COL_PHOTO_URI, photoUri)
                }

                val photoId = db.insert(
                    TABLE_PROPERTY_PHOTOS,
                    null,
                    photoValues
                )

                if (photoId == -1L) {
                    return -1L
                }
            }

            db.setTransactionSuccessful()

        } finally {
            db.endTransaction()
        }

        return propertyId
    }

    // Retrieve houses awaiting administrator approval.
    fun getPendingProperties(): Cursor {

        return readableDatabase.rawQuery(
            """
            SELECT
                p.$COL_PROPERTY_ID,
                p.$COL_OWNER_ID,
                p.$COL_PROPERTY_TITLE,
                p.$COL_PROPERTY_LOCATION,
                p.$COL_PROPERTY_RENT,
                p.$COL_PROPERTY_BEDROOMS,
                p.$COL_PROPERTY_DESCRIPTION,
                p.$COL_PROPERTY_STATUS,
                u.$COL_NAME AS owner_name,
                u.$COL_EMAIL AS owner_email
            FROM $TABLE_PROPERTIES p
            INNER JOIN $TABLE_USERS u
                ON p.$COL_OWNER_ID = u.$COL_ID
            WHERE p.$COL_PROPERTY_STATUS = ?
            ORDER BY p.$COL_CREATED_AT ASC
            """.trimIndent(),
            arrayOf(STATUS_PENDING)
        )
    }

    // Retrieve houses that have been approved for tenants.
    fun getApprovedProperties(): Cursor {

        return readableDatabase.query(
            TABLE_PROPERTIES,
            arrayOf(
                COL_PROPERTY_ID,
                COL_OWNER_ID,
                COL_PROPERTY_TITLE,
                COL_PROPERTY_LOCATION,
                COL_PROPERTY_RENT,
                COL_PROPERTY_BEDROOMS,
                COL_PROPERTY_DESCRIPTION,
                COL_PROPERTY_STATUS
            ),
            "$COL_PROPERTY_STATUS = ?",
            arrayOf(STATUS_APPROVED),
            null,
            null,
            "$COL_CREATED_AT DESC"
        )
    }

    // Retrieve all houses submitted by a particular owner.
    fun getOwnerProperties(ownerId: Long): Cursor {

        return readableDatabase.query(
            TABLE_PROPERTIES,
            arrayOf(
                COL_PROPERTY_ID,
                COL_OWNER_ID,
                COL_PROPERTY_TITLE,
                COL_PROPERTY_LOCATION,
                COL_PROPERTY_RENT,
                COL_PROPERTY_BEDROOMS,
                COL_PROPERTY_DESCRIPTION,
                COL_PROPERTY_STATUS
            ),
            "$COL_OWNER_ID = ?",
            arrayOf(ownerId.toString()),
            null,
            null,
            "$COL_CREATED_AT DESC"
        )
    }

    // Approve or reject a house listing.
    fun updatePropertyApproval(
        propertyId: Long,
        status: String
    ): Boolean {

        if (status != STATUS_APPROVED &&
            status != STATUS_REJECTED
        ) {
            return false
        }

        val db = writableDatabase

        val values = ContentValues().apply {
            put(COL_PROPERTY_STATUS, status)
        }

        val rowsUpdated = db.update(
            TABLE_PROPERTIES,
            values,
            "$COL_PROPERTY_ID = ? AND $COL_PROPERTY_STATUS = ?",
            arrayOf(
                propertyId.toString(),
                STATUS_PENDING
            )
        )

        return rowsUpdated > 0
    }

    // Retrieve the saved photo URIs for a house.
    fun getPropertyPhotos(propertyId: Long): Cursor {

        return readableDatabase.query(
            TABLE_PROPERTY_PHOTOS,
            arrayOf(COL_PHOTO_URI),
            "$COL_PROPERTY_ID = ?",
            arrayOf(propertyId.toString()),
            null,
            null,
            "$COL_PHOTO_ID ASC"
        )
    }

    // Hash the password before storing or checking it.
    private fun hashPassword(password: String): String {

        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(password.toByteArray(Charsets.UTF_8))

        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }
}

