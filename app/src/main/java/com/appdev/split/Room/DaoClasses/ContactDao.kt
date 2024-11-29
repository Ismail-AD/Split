package com.appdev.split.Room.DaoClasses

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.appdev.split.Model.Data.Contact
import com.appdev.split.Model.Data.Friend
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Friend)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<Friend>)

    @Update
    suspend fun updateContact(contact: Friend)

    @Update
    suspend fun updateContacts(contacts: List<Friend>)

    @Delete
    suspend fun deleteContact(contact: Friend)

    @Query("SELECT * FROM friends ORDER BY name ASC")
    fun getAllContacts(): Flow<List<Friend>>

    @Query("SELECT * FROM friends WHERE contact = :contact")
    suspend fun getContactById(contact:String): Friend?

    @Query("SELECT * FROM friends WHERE name = :name")
    suspend fun getContactByName(name: String): Friend?
}
