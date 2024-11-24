package com.appdev.split.DI

import android.content.Context
import androidx.room.Room
import com.appdev.split.Room.AppDatabase
import com.appdev.split.Room.DaoClasses.ContactDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "split_app_database"
        ).build()
    }

    @Provides
    fun provideContactDao(database: AppDatabase): ContactDao {
        return database.contactDao()
    }
}