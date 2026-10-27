package com.example.game.content

/**
 * Katalog surowców gry: zioła i rudy z poziomem rzadkości oraz krainą pochodzenia.
 * Poziom (1–6) steruje szansą zebrania przez adeptów i progiem receptur.
 */
data class ResourceDef(
    val name: String,
    val tier: Int,
    val sourceRegionId: String?,
    val description: String
)

object ResourceCatalog {

    val herbs: List<ResourceDef> = listOf(
        ResourceDef("Trawa Wiecznej Wiosny", 1, "reg_1", "Podstawowe zioło duchowe. Rośnie w bambusowych gajach."),
        ResourceDef("Fioletowy Korzeń Skorpiona", 1, "reg_3", "Trujący korzeń z bagien, baza antidotów."),
        ResourceDef("Śnieżny Lotos Oświecenia", 2, "reg_2", "Kwitnie raz na sto lat na lodowych szczytach Kunlun."),
        ResourceDef("Grzyb Księżycowej Rosy", 2, "reg_8", "Świeci w ciemności zatopionych korytarzy, chłodzi meridiany."),
        ResourceDef("Płomienny Żeń-szeń Smoka", 3, "reg_4", "Korzeń w kształcie smoka, nasiąknięty żarem lawy."),
        ResourceDef("Kwiat Niebiańskiego Zrozumienia", 3, "reg_5", "Płatki jak zwoje sutr, budzą wgląd w Dao."),
        ResourceDef("Mech Gromowego Kamienia", 3, "reg_7", "Porasta skały rażone piorunem, trzeszczy od wyładowań."),
        ResourceDef("Korzeń Krwawej Kości", 3, "reg_9", "Wyrasta z kości poległych bogów. Hartuje ciało Asury."),
        ResourceDef("Orchidea Tysiąca Lat", 4, "reg_12", "Kwiat dojrzewający przez tysiąclecie na brzegu Chaosu."),
        ResourceDef("Owoc Drzewa Pantao", 5, "reg_10", "Brzoskwinia nieśmiertelności z niebiańskiego sadu."),
        ResourceDef("Kwiat Gwiezdnej Mgły", 5, "reg_11", "Kwitnie w pyle konstelacji, pachnie gwiazdami.")
    )

    val minerals: List<ResourceDef> = listOf(
        ResourceDef("Duchowy Nefryt", 1, "reg_1", "Kamień nasycony Qi. Podstawa pancerzy i talizmanów."),
        ResourceDef("Gwiezdne Żelazo", 2, "reg_2", "Metal z meteorytów. Twardy jak wola mistrza."),
        ResourceDef("Czarny Obsydian Miazmatu", 2, "reg_3", "Szkliwo bagien wydzielające trujące opary."),
        ResourceDef("Kryształ Geocentrycznego Ognia", 3, "reg_4", "Kryształ z płomieniem uwięzionym w środku."),
        ResourceDef("Słoneczny Złoty Piasek", 3, "reg_5", "Piasek pustyni, który pamięta światło słońca."),
        ResourceDef("Kryształ Gromu", 3, "reg_7", "Zamrożony piorun. Trzaska przy dotyku."),
        ResourceDef("Perła Głębin", 3, "reg_8", "Perła z pałacu Smoczego Króla, gromadzi wodę Yin."),
        ResourceDef("Starożytna Ruda Pustki", 4, "reg_6", "Czarna ruda z pęknięciami sączącymi pustkę."),
        ResourceDef("Kość Smoka Pradawnego", 4, "reg_9", "Skamieniała kość smoka, twardsza od stali."),
        ResourceDef("Rdzeń Gwiazdy", 5, "reg_10", "Serce upadłej gwiazdy, wciąż ciepłe."),
        ResourceDef("Szkło Pustki", 5, "reg_11", "Przezroczysta materia między gwiazdami."),
        ResourceDef("Jadeit Chaosu", 6, "reg_12", "Kamień zmieniający barwę z każdym oddechem kosmosu.")
    )

    /** Zioła/rudy dostępne do zbierania: krainy startowe + krainy już zbadane (>0%). */
    fun gatherable(list: List<ResourceDef>, exploredRegionIds: Set<String>): List<ResourceDef> =
        list.filter { it.tier == 1 || it.sourceRegionId == null || it.sourceRegionId in exploredRegionIds }

    /** Losuje surowiec: rzadsze poziomy mają mniejszą wagę (tier 1 = 16, tier 6 = 1). */
    fun pickWeighted(pool: List<ResourceDef>, random: kotlin.random.Random): ResourceDef? {
        if (pool.isEmpty()) return null
        val weights = pool.map { (32 shr it.tier.coerceIn(1, 6)).coerceAtLeast(1) }
        var roll = random.nextInt(weights.sum())
        pool.forEachIndexed { i, r ->
            roll -= weights[i]
            if (roll < 0) return r
        }
        return pool.last()
    }
}
