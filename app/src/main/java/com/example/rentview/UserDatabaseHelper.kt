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
        private const val DATABASE_VERSION = 4

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

        // Property edit request table
        const val TABLE_PROPERTY_EDITS = "property_edit_requests"
        const val COL_EDIT_ID = "edit_id"
        const val COL_EDIT_PROPERTY_ID = "property_id"
        const val COL_EDIT_TITLE = "proposed_title"
        const val COL_EDIT_LOCATION = "proposed_location"
        const val COL_EDIT_RENT = "proposed_rent"
        const val COL_EDIT_BEDROOMS = "proposed_bedrooms"
        const val COL_EDIT_DESCRIPTION = "proposed_description"
        const val COL_EDIT_STATUS = "edit_status"
        const val COL_EDIT_CREATED_AT = "created_at"

        // Photos submitted with proposed edits
        const val TABLE_PROPERTY_EDIT_PHOTOS = "property_edit_photos"
        const val COL_EDIT_PHOTO_ID = "edit_photo_id"
        const val COL_EDIT_PHOTO_URI = "photo_uri"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
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

        createPropertyEditTables(db)
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
    private fun createPropertyEditTables(db: SQLiteDatabase) {

        val createEditsTable = """
        CREATE TABLE IF NOT EXISTS $TABLE_PROPERTY_EDITS (
            $COL_EDIT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_EDIT_PROPERTY_ID INTEGER NOT NULL,
            $COL_EDIT_TITLE TEXT NOT NULL,
            $COL_EDIT_LOCATION TEXT NOT NULL,
            $COL_EDIT_RENT REAL NOT NULL,
            $COL_EDIT_BEDROOMS INTEGER NOT NULL,
            $COL_EDIT_DESCRIPTION TEXT NOT NULL,
            $COL_EDIT_STATUS TEXT NOT NULL DEFAULT '$STATUS_PENDING',
            $COL_EDIT_CREATED_AT TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY ($COL_EDIT_PROPERTY_ID)
                REFERENCES $TABLE_PROPERTIES($COL_PROPERTY_ID)
                ON DELETE CASCADE
        )
    """.trimIndent()

        db.execSQL(createEditsTable)

        val createEditPhotosTable = """
        CREATE TABLE IF NOT EXISTS $TABLE_PROPERTY_EDIT_PHOTOS (
            $COL_EDIT_PHOTO_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_EDIT_ID INTEGER NOT NULL,
            $COL_EDIT_PHOTO_URI TEXT NOT NULL,
            FOREIGN KEY ($COL_EDIT_ID)
                REFERENCES $TABLE_PROPERTY_EDITS($COL_EDIT_ID)
                ON DELETE CASCADE
        )
    """.trimIndent()

        db.execSQL(createEditPhotosTable)
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

        if (oldVersion < 4) {
            createPropertyEditTables(db)
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

    /**
     * Submit proposed changes to an existing property.
     *
     * The approved property row and its current photos are NOT changed here.
     * The proposed details and any replacement photos are stored separately
     * until an administrator approves the request.
     *
     * If photoUris is empty, the existing photos are retained when approved.
     * If photoUris contains URIs, those photos replace the old set on approval.
     *
     * Returns the new edit request ID, or -1 if validation/ownership/pending
     * request checks fail.
     */
    fun submitPropertyEdit(
        ownerId: Long,
        propertyId: Long,
        title: String,
        location: String,
        rent: Double,
        bedrooms: Int,
        description: String,
        photoUris: List<String>
    ): Long {
        if (title.isBlank() ||
            location.isBlank() ||
            rent <= 0.0 ||
            bedrooms < 0 ||
            description.isBlank()
        ) {
            return -1L
        }

        val db = writableDatabase
        var editId = -1L

        db.beginTransaction()
        try {
            // Confirm this property belongs to this owner and is approved.
            val propertyCursor = db.query(
                TABLE_PROPERTIES,
                arrayOf(COL_PROPERTY_ID),
                "$COL_PROPERTY_ID = ? AND $COL_OWNER_ID = ? AND $COL_PROPERTY_STATUS = ?",
                arrayOf(
                    propertyId.toString(),
                    ownerId.toString(),
                    STATUS_APPROVED
                ),
                null,
                null,
                null
            )

            val propertyIsEditable = propertyCursor.use { it.moveToFirst() }
            if (!propertyIsEditable) {
                return -1L
            }

            // Allow only one pending edit request per property.
            val pendingCursor = db.query(
                TABLE_PROPERTY_EDITS,
                arrayOf(COL_EDIT_ID),
                "$COL_EDIT_PROPERTY_ID = ? AND $COL_EDIT_STATUS = ?",
                arrayOf(propertyId.toString(), STATUS_PENDING),
                null,
                null,
                null
            )

            val alreadyHasPendingEdit = pendingCursor.use { it.moveToFirst() }
            if (alreadyHasPendingEdit) {
                return -1L
            }

            val values = ContentValues().apply {
                put(COL_EDIT_PROPERTY_ID, propertyId)
                put(COL_EDIT_TITLE, title.trim())
                put(COL_EDIT_LOCATION, location.trim())
                put(COL_EDIT_RENT, rent)
                put(COL_EDIT_BEDROOMS, bedrooms)
                put(COL_EDIT_DESCRIPTION, description.trim())
                put(COL_EDIT_STATUS, STATUS_PENDING)
            }

            editId = db.insert(TABLE_PROPERTY_EDITS, null, values)
            if (editId == -1L) {
                return -1L
            }

            for (photoUri in photoUris.distinct()) {
                if (photoUri.isBlank()) continue

                val photoValues = ContentValues().apply {
                    put(COL_EDIT_ID, editId)
                    put(COL_EDIT_PHOTO_URI, photoUri)
                }

                if (db.insert(TABLE_PROPERTY_EDIT_PHOTOS, null, photoValues) == -1L) {
                    return -1L
                }
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        return editId
    }

    /**
     * Get all pending property edit requests for the administrator.
     * Includes property owner details for display in the admin screen.
     */
    fun getPendingPropertyEdits(): Cursor {
        return readableDatabase.rawQuery(
            """
            SELECT
                e.$COL_EDIT_ID,
                e.$COL_EDIT_PROPERTY_ID,
                e.$COL_EDIT_TITLE,
                e.$COL_EDIT_LOCATION,
                e.$COL_EDIT_RENT,
                e.$COL_EDIT_BEDROOMS,
                e.$COL_EDIT_DESCRIPTION,
                e.$COL_EDIT_STATUS,
                e.$COL_EDIT_CREATED_AT,
                p.$COL_PROPERTY_TITLE AS current_title,
                p.$COL_PROPERTY_LOCATION AS current_location,
                p.$COL_PROPERTY_RENT AS current_rent,
                p.$COL_PROPERTY_BEDROOMS AS current_bedrooms,
                p.$COL_PROPERTY_DESCRIPTION AS current_description,
                u.$COL_NAME AS owner_name,
                u.$COL_EMAIL AS owner_email
            FROM $TABLE_PROPERTY_EDITS e
            INNER JOIN $TABLE_PROPERTIES p
                ON e.$COL_EDIT_PROPERTY_ID = p.$COL_PROPERTY_ID
            INNER JOIN $TABLE_USERS u
                ON p.$COL_OWNER_ID = u.$COL_ID
            WHERE e.$COL_EDIT_STATUS = ?
            ORDER BY e.$COL_EDIT_CREATED_AT ASC
            """.trimIndent(),
            arrayOf(STATUS_PENDING)
        )
    }

    /** Get the proposed photo URIs for one edit request. */
    fun getPropertyEditPhotos(editId: Long): Cursor {
        return readableDatabase.query(
            TABLE_PROPERTY_EDIT_PHOTOS,
            arrayOf(COL_EDIT_PHOTO_URI),
            "$COL_EDIT_ID = ?",
            arrayOf(editId.toString()),
            null,
            null,
            "$COL_EDIT_PHOTO_ID ASC"
        )
    }

    /**
     * Approve or reject an edit request.
     *
     * On approval, proposed details replace the property's current details.
     * Proposed photos replace the current photos only if the request contains
     * at least one proposed photo. On rejection, the property remains unchanged.
     */
    fun reviewPropertyEdit(editId: Long, decision: String): Boolean {
        if (decision != STATUS_APPROVED && decision != STATUS_REJECTED) {
            return false
        }

        val db = writableDatabase
        var success = false

        db.beginTransaction()
        try {
            val editCursor = db.query(
                TABLE_PROPERTY_EDITS,
                arrayOf(
                    COL_EDIT_PROPERTY_ID,
                    COL_EDIT_TITLE,
                    COL_EDIT_LOCATION,
                    COL_EDIT_RENT,
                    COL_EDIT_BEDROOMS,
                    COL_EDIT_DESCRIPTION
                ),
                "$COL_EDIT_ID = ? AND $COL_EDIT_STATUS = ?",
                arrayOf(editId.toString(), STATUS_PENDING),
                null,
                null,
                null
            )

            val edit = editCursor.use { cursor ->
                if (!cursor.moveToFirst()) {
                    null
                } else {
                    arrayOf(
                        cursor.getLong(0).toString(),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getDouble(3).toString(),
                        cursor.getInt(4).toString(),
                        cursor.getString(5)
                    )
                }
            } ?: return false

            val propertyId = edit[0].toLong()

            if (decision == STATUS_APPROVED) {
                val propertyValues = ContentValues().apply {
                    put(COL_PROPERTY_TITLE, edit[1])
                    put(COL_PROPERTY_LOCATION, edit[2])
                    put(COL_PROPERTY_RENT, edit[3].toDouble())
                    put(COL_PROPERTY_BEDROOMS, edit[4].toInt())
                    put(COL_PROPERTY_DESCRIPTION, edit[5])
                }

                val updatedRows = db.update(
                    TABLE_PROPERTIES,
                    propertyValues,
                    "$COL_PROPERTY_ID = ? AND $COL_PROPERTY_STATUS = ?",
                    arrayOf(propertyId.toString(), STATUS_APPROVED)
                )

                if (updatedRows != 1) {
                    return false
                }

                // Use proposed photos as a replacement set only when supplied.
                val proposedPhotoCursor = db.query(
                    TABLE_PROPERTY_EDIT_PHOTOS,
                    arrayOf(COL_EDIT_PHOTO_URI),
                    "$COL_EDIT_ID = ?",
                    arrayOf(editId.toString()),
                    null,
                    null,
                    "$COL_EDIT_PHOTO_ID ASC"
                )

                val proposedPhotoUris = proposedPhotoCursor.use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            add(cursor.getString(0))
                        }
                    }
                }

                if (proposedPhotoUris.isNotEmpty()) {
                    db.delete(
                        TABLE_PROPERTY_PHOTOS,
                        "$COL_PROPERTY_ID = ?",
                        arrayOf(propertyId.toString())
                    )

                    for (photoUri in proposedPhotoUris) {
                        val photoValues = ContentValues().apply {
                            put(COL_PROPERTY_ID, propertyId)
                            put(COL_PHOTO_URI, photoUri)
                        }

                        if (db.insert(TABLE_PROPERTY_PHOTOS, null, photoValues) == -1L) {
                            return false
                        }
                    }
                }
            }

            val requestValues = ContentValues().apply {
                put(COL_EDIT_STATUS, decision)
            }

            val updatedRequestRows = db.update(
                TABLE_PROPERTY_EDITS,
                requestValues,
                "$COL_EDIT_ID = ? AND $COL_EDIT_STATUS = ?",
                arrayOf(editId.toString(), STATUS_PENDING)
            )

            if (updatedRequestRows != 1) {
                return false
            }

            db.setTransactionSuccessful()
            success = true
        } finally {
            db.endTransaction()
        }

        return success
    }

    /** Convenience wrappers for the administrator review buttons. */
    fun approvePropertyEdit(editId: Long): Boolean {
        return reviewPropertyEdit(editId, STATUS_APPROVED)
    }

    fun rejectPropertyEdit(editId: Long): Boolean {
        return reviewPropertyEdit(editId, STATUS_REJECTED)
    }

    /** Find the pending edit request for a property, if one exists. */
    fun getPendingPropertyEditId(propertyId: Long): Long? {
        val cursor = readableDatabase.query(
            TABLE_PROPERTY_EDITS,
            arrayOf(COL_EDIT_ID),
            "$COL_EDIT_PROPERTY_ID = ? AND $COL_EDIT_STATUS = ?",
            arrayOf(propertyId.toString(), STATUS_PENDING),
            null,
            null,
            "$COL_EDIT_CREATED_AT ASC",
            "1"
        )

        return cursor.use {
            if (it.moveToFirst()) it.getLong(0) else null
        }
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
