package com.appdev.split.Room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.appdev.split.Model.Data.Contact
import com.appdev.split.Model.Data.Friend
import com.appdev.split.Room.DaoClasses.ContactDao

@Database(entities = [Friend::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
}