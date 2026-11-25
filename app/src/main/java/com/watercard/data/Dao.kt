package com.watercard.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriberDao {
    @Query("SELECT * FROM subscribers ORDER BY userNo")
    fun all(): Flow<List<Subscriber>>

    @Query("SELECT * FROM subscribers WHERE userNo = :userNo LIMIT 1")
    suspend fun byUserNo(userNo: Int): Subscriber?

    @Query("SELECT * FROM subscribers WHERE name LIKE '%' || :q || '%' OR CAST(userNo AS TEXT) LIKE '%' || :q || '%' ORDER BY userNo")
    fun search(q: String): Flow<List<Subscriber>>

    @Query("SELECT MAX(userNo) FROM subscribers")
    suspend fun maxUserNo(): Int?

    @Insert
    suspend fun insert(s: Subscriber): Long

    @Update
    suspend fun update(s: Subscriber)
}

@Dao
interface ChargeDao {
    @Query("SELECT * FROM charges ORDER BY createdAt DESC LIMIT 100")
    fun recent(): Flow<List<Charge>>

    @Query("SELECT * FROM charges WHERE userNo = :userNo ORDER BY createdAt DESC")
    fun byUser(userNo: Int): Flow<List<Charge>>

    @Insert
    suspend fun insert(c: Charge): Long
}
