package com.example.rentview.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "full_name")
    val fullName: String,

    val email: String,
    val phone: String,

    @ColumnInfo(name = "password_hash")
    val passwordHash: String,

    val role: String,

    @ColumnInfo(
        name = "approval_status",
        defaultValue = "'PENDING'"
    )
    val approvalStatus: String = "PENDING"
)

@Entity(
    tableName = "properties",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["owner_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["owner_id"])]
)
data class PropertyEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "property_id")
    val propertyId: Long = 0,

    @ColumnInfo(name = "owner_id")
    val ownerId: Long,

    val title: String,
    val location: String,
    val rent: Double,
    val bedrooms: Int,
    val description: String,

    @ColumnInfo(
        name = "property_status",
        defaultValue = "'PENDING'"
    )
    val propertyStatus: String = "PENDING",

    @ColumnInfo(
        name = "created_at",
        defaultValue = "CURRENT_TIMESTAMP"
    )
    val createdAt: String
)

@Entity(
    tableName = "property_photos",
    foreignKeys = [
        ForeignKey(
            entity = PropertyEntity::class,
            parentColumns = ["property_id"],
            childColumns = ["property_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["property_id"])]
)
data class PropertyPhotoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "photo_id")
    val photoId: Long = 0,

    @ColumnInfo(name = "property_id")
    val propertyId: Long,

    @ColumnInfo(name = "photo_uri")
    val photoUri: String
)

@Entity(
    tableName = "property_edit_requests",
    foreignKeys = [
        ForeignKey(
            entity = PropertyEntity::class,
            parentColumns = ["property_id"],
            childColumns = ["property_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["property_id"])]
)
data class PropertyEditRequestEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "edit_id")
    val editId: Long = 0,

    @ColumnInfo(name = "property_id")
    val propertyId: Long,

    @ColumnInfo(name = "proposed_title")
    val proposedTitle: String,

    @ColumnInfo(name = "proposed_location")
    val proposedLocation: String,

    @ColumnInfo(name = "proposed_rent")
    val proposedRent: Double,

    @ColumnInfo(name = "proposed_bedrooms")
    val proposedBedrooms: Int,

    @ColumnInfo(name = "proposed_description")
    val proposedDescription: String,

    @ColumnInfo(
        name = "edit_status",
        defaultValue = "'PENDING'"
    )
    val editStatus: String = "PENDING",

    @ColumnInfo(
        name = "created_at",
        defaultValue = "CURRENT_TIMESTAMP"
    )
    val createdAt: String
)

@Entity(
    tableName = "property_edit_photos",
    foreignKeys = [
        ForeignKey(
            entity = PropertyEditRequestEntity::class,
            parentColumns = ["edit_id"],
            childColumns = ["edit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["edit_id"])]
)
data class PropertyEditPhotoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "edit_photo_id")
    val editPhotoId: Long = 0,

    @ColumnInfo(name = "edit_id")
    val editId: Long,

    @ColumnInfo(name = "photo_uri")
    val photoUri: String
)

@Entity(
    tableName = "viewings",
    foreignKeys = [
        ForeignKey(
            entity = PropertyEntity::class,
            parentColumns = ["property_id"],
            childColumns = ["property_id"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["tenant_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(
            value = ["property_id", "viewing_date", "viewing_slot"],
            unique = true,
            name = "idx_viewing_unique_slot"
        ),
        Index(value = ["tenant_id"])
    ]
)
data class ViewingEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "viewing_id")
    val viewingId: Long = 0,

    @ColumnInfo(name = "property_id")
    val propertyId: Long,

    @ColumnInfo(name = "tenant_id")
    val tenantId: Long,

    @ColumnInfo(name = "viewing_date")
    val viewingDate: String,

    @ColumnInfo(name = "viewing_slot")
    val viewingSlot: String
)