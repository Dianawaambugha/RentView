package com.example.rentview.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

// USERS
@Dao
interface UserDao {

    @Insert
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getById(userId: Long): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = :role ORDER BY full_name ASC")
    suspend fun getUsersByRole(role: String): List<UserEntity>

    @Query("SELECT * FROM users WHERE role = 'Tenant' ORDER BY full_name ASC")
    suspend fun getAllTenants(): List<UserEntity>

    // Retrieve all property owners.
    @Query("""
        SELECT * FROM users
        WHERE role = 'Property Owner'
        ORDER BY full_name ASC
    """)
    suspend fun getAllOwners(): List<UserEntity>

    // Retrieve owners awaiting administrator approval.
    @Query("""
        SELECT * FROM users
        WHERE role = 'Property Owner'
        AND approval_status = 'PENDING'
        ORDER BY full_name ASC
    """)
    suspend fun getPendingOwners(): List<UserEntity>

    @Query("UPDATE users SET approval_status = :status WHERE id = :userId")
    suspend fun updateApprovalStatus(userId: Long, status: String)

    @Query("SELECT COUNT(*) FROM users WHERE role = :role")
    suspend fun countByRole(role: String): Int

    @Query("""
        SELECT COUNT(*) FROM users
        WHERE role = :role AND approval_status = :status
    """)
    suspend fun countByRoleAndStatus(role: String, status: String): Int
}


// PROPERTIES
@Dao
interface PropertyDao {

    @Insert
    suspend fun insert(property: PropertyEntity): Long

    @Update
    suspend fun update(property: PropertyEntity)

    @Query("SELECT * FROM properties WHERE property_id = :propertyId LIMIT 1")
    suspend fun getById(propertyId: Long): PropertyEntity?

    @Query("SELECT * FROM properties ORDER BY created_at DESC")
    suspend fun getAll(): List<PropertyEntity>

    @Query("SELECT * FROM properties WHERE property_status = 'APPROVED' ORDER BY created_at DESC")
    suspend fun getApprovedProperties(): List<PropertyEntity>

    @Query("SELECT * FROM properties WHERE owner_id = :ownerId ORDER BY created_at DESC")
    suspend fun getByOwner(ownerId: Long): List<PropertyEntity>

    @Query("SELECT * FROM properties WHERE property_status = :status ORDER BY created_at DESC")
    suspend fun getByStatus(status: String): List<PropertyEntity>

    @Query("UPDATE properties SET property_status = :status WHERE property_id = :propertyId")
    suspend fun updateStatus(propertyId: Long, status: String)

    @Query("DELETE FROM properties WHERE property_id = :propertyId")
    suspend fun delete(propertyId: Long)

    @Query("SELECT COUNT(*) FROM properties")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM properties WHERE property_status = :status")
    suspend fun countByStatus(status: String): Int

    @Query("SELECT COUNT(*) FROM properties WHERE owner_id = :ownerId")
    suspend fun countByOwner(ownerId: Long): Int
}


// PROPERTY PHOTOS
@Dao
interface PropertyPhotoDao {

    @Insert
    suspend fun insert(photo: PropertyPhotoEntity): Long

    @Insert
    suspend fun insertAll(photos: List<PropertyPhotoEntity>)

    @Query("SELECT * FROM property_photos WHERE property_id = :propertyId")
    suspend fun getByProperty(propertyId: Long): List<PropertyPhotoEntity>

    @Query("SELECT photo_uri FROM property_photos WHERE property_id = :propertyId")
    suspend fun getPhotoUris(propertyId: Long): List<String>

    @Query("SELECT COUNT(*) FROM property_photos WHERE property_id = :propertyId")
    suspend fun countByProperty(propertyId: Long): Int

    @Query("DELETE FROM property_photos WHERE property_id = :propertyId")
    suspend fun deleteByProperty(propertyId: Long)

    @Query("DELETE FROM property_photos WHERE photo_id = :photoId")
    suspend fun deleteById(photoId: Long)
}


// PROPERTY EDIT REQUESTS
@Dao
interface PropertyEditRequestDao {

    @Insert
    suspend fun insert(request: PropertyEditRequestEntity): Long

    @Update
    suspend fun update(request: PropertyEditRequestEntity)

    @Query("SELECT * FROM property_edit_requests WHERE edit_id = :editId LIMIT 1")
    suspend fun getById(editId: Long): PropertyEditRequestEntity?

    @Query("SELECT * FROM property_edit_requests WHERE property_id = :propertyId ORDER BY created_at DESC")
    suspend fun getByProperty(propertyId: Long): List<PropertyEditRequestEntity>

    @Query("SELECT * FROM property_edit_requests WHERE edit_status = :status ORDER BY created_at DESC")
    suspend fun getByStatus(status: String): List<PropertyEditRequestEntity>

    @Query("SELECT * FROM property_edit_requests ORDER BY created_at DESC")
    suspend fun getAll(): List<PropertyEditRequestEntity>

    @Query("UPDATE property_edit_requests SET edit_status = :status WHERE edit_id = :editId")
    suspend fun updateStatus(editId: Long, status: String)

    @Query("DELETE FROM property_edit_requests WHERE edit_id = :editId")
    suspend fun delete(editId: Long)
}


// PROPERTY EDIT PHOTOS
@Dao
interface PropertyEditPhotoDao {

    @Insert
    suspend fun insert(photo: PropertyEditPhotoEntity): Long

    @Insert
    suspend fun insertAll(photos: List<PropertyEditPhotoEntity>)

    @Query("SELECT * FROM property_edit_photos WHERE edit_id = :editId")
    suspend fun getByEditRequest(editId: Long): List<PropertyEditPhotoEntity>

    @Query("SELECT photo_uri FROM property_edit_photos WHERE edit_id = :editId")
    suspend fun getPhotoUris(editId: Long): List<String>

    @Query("DELETE FROM property_edit_photos WHERE edit_id = :editId")
    suspend fun deleteByEditRequest(editId: Long)
}


// VIEWINGS
@Dao
interface ViewingDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(viewing: ViewingEntity): Long

    @Update
    suspend fun update(viewing: ViewingEntity)

    // Get a viewing only if it belongs to the specified tenant.
    @Query("""
        SELECT * FROM viewings
        WHERE viewing_id = :viewingId
        AND tenant_id = :tenantId
        LIMIT 1
    """)
    suspend fun getById(viewingId: Long, tenantId: Long): ViewingEntity?

    @Query("""
        SELECT * FROM viewings
        WHERE tenant_id = :tenantId
        ORDER BY viewing_date ASC, viewing_slot ASC
    """)
    suspend fun getByTenant(tenantId: Long): List<ViewingEntity>

    @Query("""
        SELECT * FROM viewings
        WHERE property_id = :propertyId
        ORDER BY viewing_date ASC, viewing_slot ASC
    """)
    suspend fun getByProperty(propertyId: Long): List<ViewingEntity>

    // Exclude the current viewing when checking availability during rescheduling.
    @Query("""
        SELECT viewing_slot FROM viewings
        WHERE property_id = :propertyId
        AND viewing_date = :date
        AND viewing_id != :excludedViewingId
    """)
    suspend fun getBookedSlots(
        propertyId: Long,
        date: String,
        excludedViewingId: Long = -1L
    ): List<String>

    @Query("""
        SELECT COUNT(*) FROM viewings
        WHERE tenant_id = :tenantId
        AND viewing_date = :date
        AND viewing_slot = :slot
        AND viewing_id != :excludedViewingId
    """)
    suspend fun countTenantConflicts(
        tenantId: Long,
        date: String,
        slot: String,
        excludedViewingId: Long = -1L
    ): Int

    @Query("""
        SELECT COUNT(*) FROM viewings
        WHERE property_id = :propertyId
        AND viewing_date = :date
        AND viewing_slot = :slot
        AND viewing_id != :excludedViewingId
    """)
    suspend fun countSlotConflicts(
        propertyId: Long,
        date: String,
        slot: String,
        excludedViewingId: Long = -1L
    ): Int

    // Update only if the viewing belongs to the specified tenant.
    @Query("""
        UPDATE viewings
        SET viewing_date = :date,
            viewing_slot = :slot
        WHERE viewing_id = :viewingId
        AND tenant_id = :tenantId
    """)
    suspend fun updateForTenant(
        viewingId: Long,
        tenantId: Long,
        date: String,
        slot: String
    ): Int

    // Delete only if the viewing belongs to the specified tenant.
    @Query("""
        DELETE FROM viewings
        WHERE viewing_id = :viewingId
        AND tenant_id = :tenantId
    """)
    suspend fun deleteForTenant(viewingId: Long, tenantId: Long): Int

    @Query("DELETE FROM viewings WHERE viewing_id = :viewingId")
    suspend fun delete(viewingId: Long)

    @Query("SELECT COUNT(*) FROM viewings")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM viewings WHERE tenant_id = :tenantId")
    suspend fun countByTenant(tenantId: Long): Int
}