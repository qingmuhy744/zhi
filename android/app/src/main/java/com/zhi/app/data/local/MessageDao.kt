package com.zhi.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zhi.app.data.model.PushMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages ORDER BY receivedAt DESC")
    fun getAllMessages(): Flow<List<PushMessage>>

    @Query("SELECT * FROM messages WHERE `group` = :group ORDER BY receivedAt DESC")
    fun getMessagesByGroup(group: String): Flow<List<PushMessage>>

    @Query("SELECT DISTINCT `group` FROM messages WHERE `group` != '' ORDER BY `group` ASC")
    fun getAllGroups(): Flow<List<String>>

    @Query("SELECT * FROM messages WHERE title LIKE '%' || :keyword || '%' OR body LIKE '%' || :keyword || '%' ORDER BY receivedAt DESC")
    fun searchMessages(keyword: String): Flow<List<PushMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: PushMessage): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<PushMessage>)

    @Update
    suspend fun updateMessage(message: PushMessage)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()

    @Query("DELETE FROM messages WHERE `group` = :group")
    suspend fun clearMessagesByGroup(group: String)

    @Query("DELETE FROM messages WHERE id NOT IN (SELECT id FROM messages ORDER BY receivedAt DESC LIMIT :keepCount)")
    suspend fun retainRecentMessages(keepCount: Int)

    @Query("DELETE FROM messages WHERE receivedAt < :cutoffTimestamp")
    suspend fun deleteMessagesOlderThan(cutoffTimestamp: Long)

    @Query("SELECT * FROM messages ORDER BY receivedAt ASC")
    suspend fun getAllMessagesList(): List<PushMessage>

    @Query("UPDATE messages SET isRead = 1")
    suspend fun markAllAsRead()
}
