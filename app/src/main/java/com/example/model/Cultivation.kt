package com.example.model

/**
 * Etap 3: 10 wielkich królestw w 3 sferach. Każde ma etapy (warstwy), koszt Qi etapu
 * i tribulację przy wejściu. [legacy] wiąże je z dawnym `CultivationRealm`, którego używa UI.
 */
enum class MajorRealm(
    val polishName: String,
    val sphere: String,
    val stages: Int,
    /** Qi pierwszego etapu; każdy kolejny ×1,6. */
    val baseQi: Double,
    /** Uderzenia pioruna przy wejściu do tego królestwa (0 = bez tribulacji). */
    val tribulationStrikes: Int,
    val lifespanYears: Int
) {
    BODY_TEMPERING("Hartowanie Ciała", "Śmiertelna", 9, 200.0, 0, 80),
    QI_CONDENSATION("Kondensacja Qi", "Śmiertelna", 9, 1_200.0, 0, 120),
    // Koszty skalibrowane symulatorem do celów tempa (gracz aktywny): Złoty Rdzeń ~1 dzień gry,
    // Narodziny Duszy ~1 tydzień, Transformacja ~3 tygodnie, Pustka ~6 tygodni, Mahajana ~3 miesiące.
    FOUNDATION("Fundamenty", "Śmiertelna", 4, 1_400_000.0, 0, 250),
    CORE_FORMATION("Złoty Rdzeń", "Śmiertelna", 4, 14_000_000.0, 3, 500),
    NASCENT_SOUL("Narodziny Duszy", "Duszy", 4, 39_000_000.0, 6, 1_200),
    SOUL_TRANSFORMATION("Transformacja Duszy", "Duszy", 4, 160_000_000.0, 6, 2_500),
    VOID_TRIBULATION("Pustka i Próba Niebios", "Duszy", 4, 640_000_000.0, 9, 5_000),
    MAHAYANA("Wielki Pojazd Mahajana", "Nieśmiertelna", 4, 2_500_000_000.0, 9, 8_000),
    HEAVENLY_IMMORTAL("Niebiański Nieśmiertelny", "Nieśmiertelna", 4, 10_000_000_000.0, 9, Int.MAX_VALUE),
    CHAOS_GOLDEN_IMMORTAL("Złoty Nieśmiertelny Chaosu", "Nieśmiertelna", 1, 60_000_000_000.0, 9, Int.MAX_VALUE);

    val next: MajorRealm? get() = entries.getOrNull(ordinal + 1)

    fun stageName(stage: Int): String = when {
        stages == 9 -> "$stage. warstwa"
        stages == 1 -> ""
        else -> listOf("Wczesny", "Średni", "Późny", "Szczyt").getOrElse(stage - 1) { "Szczyt" }
    }
}

/** Jakość fundamentu – trwały mnożnik Qi. */
enum class FoundationQuality(val polishName: String, val qiMultiplier: Double) {
    CRACKED("Popękany", 0.8),
    NORMAL("Zwykły", 1.0),
    PERFECT("Doskonały", 1.15),
    HEAVENLY("Niebiański", 1.3)
}

/** Ścieżka kultywacji – skąd uczeń bierze siłę (wybór przy wejściu w Fundamenty). */
enum class CultivationPath(val polishName: String, val description: String, val school: MartialSchool?) {
    ORTHODOX("Ortodoksyjna", "Medytacja przy żyle: +15% Qi, stabilne przełomy.", null),
    BODY("Ciała", "Siła z walki i treningu: HP ×2, tribulacja −20% obrażeń, medytacja ×0,85, życie −20%.", MartialSchool.BODY_ASURA),
    SWORD("Miecza", "Intencja miecza: +20% obrażeń, Qi z wygranych walk.", MartialSchool.SWORD_IMMORTAL),
    PILLS("Pigułek", "Pigułki działają ×2, ale toksyczność ×1,5 i −15% obrażeń.", MartialSchool.ELEMENTAL_TAOIST),
    DEMONIC("Demoniczna", "Pochłania Qi pokonanych: +3% postępu za zwycięstwo, karma −2 za walkę.", MartialSchool.SHADOW_VIPER),
    HEART("Serca", "Olśnienia ×2, odporność na demona serca, medytacja ×0,8; wymaga karmy ≥ 0.", MartialSchool.ELEMENTAL_TAOIST)
}

/** Stopień metody kultywacji: mnożnik Qi i sufit (najwyższe osiągalne królestwo). */
enum class MethodGrade(val polishName: String, val qiMultiplier: Double, val ceiling: MajorRealm) {
    YELLOW("Żółty", 1.0, MajorRealm.FOUNDATION),
    BLACK("Czarny", 1.3, MajorRealm.CORE_FORMATION),
    MYSTIC("Mistyczny", 1.6, MajorRealm.SOUL_TRANSFORMATION),
    HEAVENLY("Niebiański", 2.0, MajorRealm.MAHAYANA),
    PRIMORDIAL("Pierwotny", 2.5, MajorRealm.CHAOS_GOLDEN_IMMORTAL)
}

/** 12 Praw Dao (jawnych). */
enum class DaoLaw(val polishName: String) {
    SWORD("Prawo Miecza"), FIRE("Prawo Ognia"), WATER("Prawo Wody"), THUNDER("Prawo Gromu"),
    ICE("Prawo Lodu"), WIND("Prawo Wiatru"), EARTH("Prawo Ziemi"), LIFE("Prawo Życia"),
    DEATH("Prawo Śmierci"), TIME("Prawo Czasu"), SPACE("Prawo Przestrzeni"), KARMA("Prawo Karmy")
}
