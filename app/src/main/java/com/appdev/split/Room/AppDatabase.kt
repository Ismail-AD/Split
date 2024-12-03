package com.appdev.split.Room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.appdev.split.Model.Data.Contact
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Room.DaoClasses.ContactDao
import com.appdev.split.Utils.Converters

@Database(entities = [Friend::class], version = 6, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
}