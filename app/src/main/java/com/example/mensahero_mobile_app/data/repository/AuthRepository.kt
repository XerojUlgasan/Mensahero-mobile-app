package com.example.mensahero_mobile_app.data.repository

import android.content.Context
import com.example.mensahero_mobile_app.data.supabase.SupabaseClientProvider
import io.github.jan.supabase.gotrue.user.UserInfo

class AuthRepository(private val context: Context) {

    private val auth = SupabaseClientProvider.getAuth(context)

    suspend fun login(email: String, password: String): Result<UserInfo> {
        return try {
            auth.signInWith(io.github.jan.supabase.gotrue.providers.builtin.Email) {
                this.email = email
                this.password = password
            }
            val user = auth.currentUserOrNull()
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUserOrNull() != null
    }

    fun getCurrentUser(): UserInfo? {
        return auth.currentUserOrNull()
    }
}
