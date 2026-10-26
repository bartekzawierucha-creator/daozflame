package com.example.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) dla kultywacji uczniów w bazie Room.
 */
@Dao
interface DiscipleCultivationDao {

    @Query("SELECT * FROM disciple_cultivations ORDER BY realmTier DESC, realmProgress DESC")
    fun getAllCultivationsFlow(): Flow<List<DiscipleCultivationEntity>>

    @Query("SELECT * FROM disciple_cultivations WHERE discipleId = :id LIMIT 1")
    fun getCultivationByIdFlow(id: String): Flow<DiscipleCultivationEntity?>

    @Query("SELECT * FROM disciple_cultivations WHERE discipleId = :id LIMIT 1")
    suspend fun getCultivationById(id: String): DiscipleCultivationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(cultivation: DiscipleCultivationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cultivations: List<DiscipleCultivationEntity>)

    @Update
    suspend fun update(cultivation: DiscipleCultivationEntity)

    @Query("""
        UPDATE disciple_cultivations 
        SET realmProgress = :progress, 
            currentQi = :currentQi, 
            isTribulationReady = :ready,
            lastCultivatedTimestamp = :timestamp 
        WHERE discipleId = :id
    """)
    suspend fun updateQiProgress(
        id: String, 
        progress: Int, 
        currentQi: Int, 
        ready: Boolean, 
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE disciple_cultivations
        SET realmTier = :newTier,
            realmName = :newRealmName,
            realmProgress = 0,
            currentQi = 0,
            maxQi = :newMaxQi,
            dantianPurity = :newPurity,
            isTribulationReady = 0,
            lastCultivatedTimestamp = :timestamp
        WHERE discipleId = :id
    """)
    suspend fun recordSuccessfulBreakthrough(
        id: String,
        newTier: Int,
        newRealmName: String,
        newMaxQi: Int,
        newPurity: Float,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM disciple_cultivations WHERE discipleId = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM disciple_cultivations")
    suspend fun clearAll()
}
