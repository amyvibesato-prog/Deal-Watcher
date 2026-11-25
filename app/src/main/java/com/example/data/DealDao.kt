package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.DealItem
import kotlinx.coroutines.flow.Flow

@Dao
interface DealDao {
    @Query("SELECT * FROM deals ORDER BY discountPercent DESC, title ASC")
    fun getAllDeals(): Flow<List<DealItem>>

    @Query("SELECT * FROM deals WHERE store = :store ORDER BY discountPercent DESC, title ASC")
    fun getDealsByStore(store: String): Flow<List<DealItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeals(deals: List<DealItem>)

    @Query("DELETE FROM deals WHERE store = :store")
    suspend fun deleteDealsByStore(store: String)

    @Query("DELETE FROM deals WHERE id IN (:ids)")
    suspend fun deleteDealsByIds(ids: List<String>)

    @Query("DELETE FROM deals")
    suspend fun clearAllDeals()

    @Query("SELECT * FROM deals")
    suspend fun getAllDealsSync(): List<DealItem>
}
