package com.example.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Encja bazy danych Room przechowująca trwały stan kultywacji, meridianów
 * oraz energii Qi ucznia sekty.
 */
@Entity(tableName = "disciple_cultivations")
data class DiscipleCultivationEntity(
    @PrimaryKey
    val discipleId: String,
    val discipleName: String,
    val clanName: String,
    val realmName: String,
    val realmTier: Int,
    val currentQi: Int,
    val maxQi: Int,
    val realmProgress: Int, // 0 - 100%
    val meridiansUnlocked: Int = 1, // 1 do 8 cudownych meridianów
    val dantianPurity: Float = 1.0f, // Mnożnik czystości skondensowanego Qi (1.0x - 2.5x)
    val qiGatheringRate: Int = 10, // Pkt Qi absorbowane na cykl medytacji
    val heartDemonResistance: Int = 75, // Odporność na demony serca (%)
    val talentGradeCode: String = "S", // SSS, SS, S, A, B
    val fatedTraitsSummary: String = "",
    val innateTechnique: String = "",
    val isTribulationReady: Boolean = false,
    val lastCultivatedTimestamp: Long = System.currentTimeMillis()
)
