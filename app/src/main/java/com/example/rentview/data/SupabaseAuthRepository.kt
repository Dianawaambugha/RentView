package com.example.rentview.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class AuthenticatedUser(
    val id: Long = -1L,
    val fullName: String,
    val email: String,
    val phone: String,
    val role: String,
    val approvalStatus: String,
    val supabaseUserId: String
)

class SupabaseAuthRepository {

    private val client = SupabaseClient.client

    suspend fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        role: String
    ) {
        require(role == "TENANT" || role == "PROPERTY_OWNER") {
            "Invalid registration role"
        }

        client.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password

            data = buildJsonObject {
                put("full_name", fullName.trim())
                put("phone", phone.trim())
                put("role", role)
            }
        }
    }

    suspend fun login(
        email: String,
        password: String
    ): AuthenticatedUser {

        try {
            client.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }

            val supabaseUserId = client.auth.currentUserOrNull()?.id
                ?: throw Exception("Unable to retrieve authenticated user.")

            val profiles = client.from("profiles")
                .select {
                    filter {
                        eq("id", supabaseUserId)
                    }
                }
                .decodeList<JsonObject>()

            val profile = profiles.firstOrNull()
                ?: throw Exception("Your user profile could not be found.")

            fun getValue(key: String): String? {
                return profile[key]?.jsonPrimitive?.contentOrNull
            }

            val fullName = getValue("full_name")
                ?: throw Exception("Your profile is missing your full name.")

            val profileEmail = getValue("email") ?: email.trim()
            val phone = getValue("phone") ?: ""

            val cloudRole = getValue("role")
                ?: throw Exception("Your account has no assigned role.")

            val approvalStatus = getValue("approval_status") ?: "PENDING"

            val localRole = when (cloudRole.uppercase()) {
                "TENANT" -> "Tenant"
                "PROPERTY_OWNER" -> "Property Owner"
                "ADMIN" -> "ADMIN"
                else -> throw Exception("Your account has an invalid role.")
            }

            return AuthenticatedUser(
                id = -1L,
                fullName = fullName,
                email = profileEmail,
                phone = phone,
                role = localRole,
                approvalStatus = approvalStatus,
                supabaseUserId = supabaseUserId
            )

        } catch (exception: Exception) {

            try {
                client.auth.signOut()
            } catch (_: Exception) {
                // Preserve the original authentication error.
            }

            throw exception
        }
    }

    suspend fun isCurrentUserAdmin(): Boolean {
        val userId = client.auth.currentUserOrNull()?.id
            ?: return false

        val profiles = client.from("profiles")
            .select {
                filter {
                    eq("id", userId)
                    eq("role", "ADMIN")
                    eq("approval_status", "APPROVED")
                }
            }
            .decodeList<JsonObject>()

        return profiles.isNotEmpty()
    }

    suspend fun logout() {
        client.auth.signOut()
    }
}