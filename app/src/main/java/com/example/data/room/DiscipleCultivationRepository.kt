package com.example.data.room

import com.example.model.Disciple
import kotlinx.coroutines.flow.Flow

/**
 * Repozytorium warstwy danych kultywacji w architekturze Clean Architecture/MVVM.
 */
class DiscipleCultivationRepository(private val dao: DiscipleCultivationDao) {

    val allCultivations: Flow<List<DiscipleCultivationEntity>> = dao.getAllCultivationsFlow()

    fun getCultivationForDisciple(discipleId: String): Flow<DiscipleCultivationEntity?> {
        return dao.getCultivationByIdFlow(discipleId)
    }

    suspend fun saveCultivation(entity: DiscipleCultivationEntity) {
        dao.insertOrUpdate(entity)
    }

    suspend fun syncFromDisciples(disciples: List<Disciple>) {
        val entities = disciples.map { disciple ->
            DiscipleCultivationEntity(
                discipleId = disciple.id,
                discipleName = disciple.name,
                clanName = disciple.ancientClan.name,
                realmName = disciple.realm.polishName,
                realmTier = disciple.realm.tier,
                currentQi = (disciple.realmProgress * disciple.realm.maxQi / 100).coerceAtLeast(0),
                maxQi = disciple.realm.maxQi,
                realmProgress = disciple.realmProgress,
                meridiansUnlocked = (1 + (disciple.realm.tier.coerceAtMost(8))),
                dantianPurity = 1.0f + (disciple.comprehension / 200f),
                qiGatheringRate = (10 + (disciple.comprehension / 8)),
                heartDemonResistance = (65 + disciple.karmicLuck / 5).coerceIn(40, 99),
                talentGradeCode = disciple.talentGrade.name,
                fatedTraitsSummary = disciple.fatedTraits.joinToString(", ") { it.name },
                innateTechnique = disciple.innateTechnique,
                isTribulationReady = disciple.realmProgress >= 100
            )
        }
        dao.insertAll(entities)
    }

    suspend fun updateQiProgress(discipleId: String, progress: Int, maxQi: Int) {
        val currentQi = (progress * maxQi / 100).coerceAtLeast(0)
        dao.updateQiProgress(
            id = discipleId,
            progress = progress,
            currentQi = currentQi,
            ready = progress >= 100
        )
    }

    suspend fun removeDisciple(discipleId: String) {
        dao.deleteById(discipleId)
    }
}
