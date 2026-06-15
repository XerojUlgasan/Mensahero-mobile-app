package com.example.mensahero_mobile_app.data.supabase

import android.content.Context
import com.example.mensahero_mobile_app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth

object SupabaseClientProvider {
    private var client: SupabaseClient? = null

    fun getClient(context: Context): SupabaseClient {
        if (client == null) {
            client = createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            ) {
                install(Auth)
            }
        }
        return client!!
    }

    fun getAuth(context: Context): Auth {
        return getClient(context).auth
    }
}
