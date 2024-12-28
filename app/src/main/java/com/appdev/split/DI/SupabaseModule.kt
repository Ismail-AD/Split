package com.appdev.split.DI

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = "https://gshywqrogwvqkwflgjdx.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdzaHl3cXJvZ3d2cWt3ZmxnamR4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzUyMzk4MzQsImV4cCI6MjA1MDgxNTgzNH0.1hwoefoHr17NxEGk-IV7WQ-DAZLKUReMOA5x5CHc-pA"
        ) {
            install(Auth) {
                alwaysAutoRefresh = false
                autoLoadFromStorage = false
            }
            install(Storage)
        }
    }
}