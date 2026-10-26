package com.example.game.content

import com.example.model.BiomeType
import com.example.model.DiplomaticStatus
import com.example.model.EnvironmentalHazard
import com.example.model.GeocentricFlame
import com.example.model.MythicalBeast
import com.example.model.RivalSect
import com.example.model.SectDivineBeast
import com.example.model.SectPolicy
import com.example.model.TerritoryNode
import com.example.model.TradeOffer
import com.example.model.WorldRegion

/** Rozszerzenie świata: krainy 7–12, terytoria 11–24, sekty 11–16, bestie 9–12, płomienie 9–12. */
object WorldCatalog {

    val extraRegions: List<WorldRegion> = listOf(
        WorldRegion(
            id = "reg_7", name = "Płaskowyż Wiecznej Burzy", biome = BiomeType.THUNDER_PLATEAU,
            hazard = EnvironmentalHazard.SPIRITUAL_TEMPEST, dangerLevel = 5, explorationProgress = 0,
            mineralResource = "Kryształ Gromu", herbResource = "Mech Gromowego Kamienia",
            bossBeast = MythicalBeast("Roc Dziewięciu Piorunów", "Pan Burzowych Chmur", 2600, 2600, 150, 80, "Pióro Gromowego Roka", 750, "🦅")
        ),
        WorldRegion(
            id = "reg_8", name = "Zatopiony Pałac Smoczego Króla", biome = BiomeType.SUNKEN_PALACE,
            hazard = EnvironmentalHazard.EXTREME_FROST, dangerLevel = 5, explorationProgress = 0,
            mineralResource = "Perła Głębin", herbResource = "Grzyb Księżycowej Rosy",
            bossBeast = MythicalBeast("Kraken Bezdennych Głębin", "Strażnik Zatopionego Tronu", 2900, 2900, 160, 95, "Atrament Krakena", 820, "🦑")
        ),
        WorldRegion(
            id = "reg_9", name = "Pustkowia Kości Bogów", biome = BiomeType.BONE_WASTES,
            hazard = EnvironmentalHazard.TOXIC_MIASMA, dangerLevel = 8, explorationProgress = 0,
            mineralResource = "Kość Smoka Pradawnego", herbResource = "Korzeń Krwawej Kości",
            bossBeast = MythicalBeast("Lisz Upadłego Patriarchy", "Władca Kościanego Tronu", 6000, 6000, 300, 160, "Filakteria Upadłego Patriarchy", 2100, "💀")
        ),
        WorldRegion(
            id = "reg_10", name = "Niebiańskie Wyspy Pantao", biome = BiomeType.CELESTIAL_ISLES,
            hazard = EnvironmentalHazard.NONE, dangerLevel = 8, explorationProgress = 0,
            mineralResource = "Rdzeń Gwiazdy", herbResource = "Owoc Drzewa Pantao",
            bossBeast = MythicalBeast("Małpi Król Niebiańskiego Sadu", "Złodziej Brzoskwiń Nieśmiertelności", 6800, 6800, 330, 170, "Złota Laska Małpiego Króla", 2400, "🐒")
        ),
        WorldRegion(
            id = "reg_11", name = "Ruiny Gwiezdnego Pałacu", biome = BiomeType.STAR_RUINS,
            hazard = EnvironmentalHazard.SPIRITUAL_TEMPEST, dangerLevel = 10, explorationProgress = 0,
            mineralResource = "Szkło Pustki", herbResource = "Kwiat Gwiezdnej Mgły",
            bossBeast = MythicalBeast("Strażnik Konstelacji", "Kamienny Golem Siedmiu Gwiazd", 11000, 11000, 480, 260, "Odłamek Konstelacji", 4200, "🌌")
        ),
        WorldRegion(
            id = "reg_12", name = "Morze Pierwotnego Chaosu", biome = BiomeType.PRIMORDIAL_CHAOS,
            hazard = EnvironmentalHazard.SCORCHING_MAGMA, dangerLevel = 10, explorationProgress = 0,
            mineralResource = "Jadeit Chaosu", herbResource = "Orchidea Tysiąca Lat",
            bossBeast = MythicalBeast("Hundun, Bezkształtny Chaos", "Pierwotna Bestia Przed Stworzeniem", 15000, 15000, 600, 320, "Serce Chaosu", 6000, "🌀")
        )
    )

    private fun terr(id: String, name: String, type: String, sectId: String, sectName: String, res: String, yield: Int, garrison: Int) =
        TerritoryNode(id, name, type, sectId, sectName, res, yield, garrison)

    val extraTerritories: List<TerritoryNode> = listOf(
        terr("terr_11", "Grota Księżycowych Grzybów", "Podziemna Grota", "sect_moon", "Sekta Księżycowej Wróżki", "Grzyb Księżycowej Rosy", 3, 700),
        terr("terr_12", "Wieża Piorunochronów", "Placówka Burzowa", "sect_thunder", "Pawilon Dziewięciu Gromów", "Kryształ Gromu", 2, 900),
        terr("terr_13", "Rafa Perłowych Muszli", "Łowisko Głębinowe", "sect_ocean", "Pałac Smoczego Króla Mórz", "Perła Głębin", 2, 950),
        terr("terr_14", "Targowisko Jedwabnego Szlaku", "Miasto Kupieckie", "sect_guild", "Związek Kupiecki Jedwabnego Szlaku", "Kamienie Ducha", 90, 800),
        terr("terr_15", "Kopalnia Smoczych Kości", "Wykopalisko", "sect_nether", "Kult Krwawych Kości Piekieł", "Kość Smoka Pradawnego", 2, 1300),
        terr("terr_16", "Gaj Brzoskwiń Nieśmiertelności", "Niebiański Sad", "sect_imperial", "Cesarski Trybunał Smoczych Niebios", "Owoc Drzewa Pantao", 1, 1600),
        terr("terr_17", "Lodowe Wrota Północy", "Twierdza Graniczna", "sect_frost", "Klan Lodowej Wierzby Północy", "Gwiezdne Minerały", 5, 1000),
        terr("terr_18", "Ostępy Tysiąca Bestii", "Puszcza Łowiecka", "sect_beast", "Plemię Tysiąca Bestii", "Zioła Alchemiczne", 7, 850),
        terr("terr_19", "Winnica Pijanego Nieśmiertelnego", "Winnica Duchowa", "sect_drunken", "Bractwo Pijanego Nieśmiertelnego", "Kamienie Ducha", 60, 650),
        terr("terr_20", "Obserwatorium Siedmiu Gwiazd", "Wieża Astrologów", "sect_astral", "Niebiański Pawilon Gwiezdnego Ducha", "Rdzeń Gwiazdy", 1, 1700),
        terr("terr_21", "Wulkaniczna Kuźnia Yan", "Kuźnia Wulkaniczna", "sect_dragon", "Zakon Płonącego Smoka", "Kryształy Płomienia", 4, 1200),
        terr("terr_22", "Klasztorny Sad Herbaciany", "Plantacja Herbaty", "sect_shaolin", "Święty Klasztor Złotej Wadżry", "Kwiat Niebiańskiego Zrozumienia", 2, 1100),
        terr("terr_23", "Brzeg Morza Chaosu", "Przyczółek Chaosu", "neutral", "Ziemia Niczyja", "Jadeit Chaosu", 1, 2400),
        terr("terr_24", "Gwiezdny Most Pustki", "Brama Między Światami", "neutral", "Ziemia Niczyja", "Szkło Pustki", 1, 2200)
    )

    val extraRivalSects: List<RivalSect> = listOf(
        RivalSect(
            id = "sect_thunder", name = "Pawilon Dziewięciu Gromów", leaderName = "Mistrz Lei Zhentian",
            alignment = "Neutralna / Waleczna", powerRank = 11, discipleCount = 260, relations = 5,
            status = DiplomaticStatus.NEUTRAL, specialty = "Techniki Gromu i Hartowanie Piorunem",
            activeTrade = TradeOffer("tr_th", "Wymiana: 260 Kamieni Ducha za 3x Kryształ Gromu", 260, "Kryształ Gromu"),
            policy = SectPolicy.EXPANSIONIST, cultivationBuffName = "Hartowanie Gromem",
            cultivationBuffDescription = "+20% obrażeń żywiołu błyskawicy", cultivationBuffBonusPercent = 20,
            controlledTerritoryIds = listOf("terr_12")
        ),
        RivalSect(
            id = "sect_ocean", name = "Pałac Smoczego Króla Mórz", leaderName = "Książę Ao Guang",
            alignment = "Cesarska / Morska", powerRank = 12, discipleCount = 540, relations = 25,
            status = DiplomaticStatus.FRIENDLY, specialty = "Formacje Wodne i Skarby Głębin",
            activeTrade = TradeOffer("tr_oc", "Wymiana: 300 Kamieni Ducha za 3x Perła Głębin", 300, "Perła Głębin"),
            policy = SectPolicy.MERCHANT_CONSORTIUM, cultivationBuffName = "Przypływ Głębin",
            cultivationBuffDescription = "+25% regeneracji Qi w walce", cultivationBuffBonusPercent = 25,
            controlledTerritoryIds = listOf("terr_13")
        ),
        RivalSect(
            id = "sect_moon", name = "Sekta Księżycowej Wróżki", leaderName = "Wróżka Chang Yue",
            alignment = "Ortodoksyjna / Mistyczna", powerRank = 13, discipleCount = 190, relations = 55,
            status = DiplomaticStatus.FRIENDLY, specialty = "Iluzje Księżyca i Alchemia Yin",
            activeTrade = TradeOffer("tr_mo", "Wymiana: 150 Kamieni Ducha za 4x Grzyb Księżycowej Rosy", 150, "Grzyb Księżycowej Rosy"),
            policy = SectPolicy.ISOLATIONIST, cultivationBuffName = "Blask Pełni",
            cultivationBuffDescription = "+20% szansy na przełom w pełnię", cultivationBuffBonusPercent = 20,
            controlledTerritoryIds = listOf("terr_11")
        ),
        RivalSect(
            id = "sect_beast", name = "Plemię Tysiąca Bestii", leaderName = "Wódz Hu Lang",
            alignment = "Neutralna / Dzika", powerRank = 14, discipleCount = 380, relations = -10,
            status = DiplomaticStatus.COLD_WAR, specialty = "Oswajanie Bestii i Łowy",
            activeTrade = null, policy = SectPolicy.DEMONIC_RAIDERS, cultivationBuffName = "Zew Stada",
            cultivationBuffDescription = "+30% więzi z bestiami sekty", cultivationBuffBonusPercent = 30,
            controlledTerritoryIds = listOf("terr_18")
        ),
        RivalSect(
            id = "sect_frost", name = "Klan Lodowej Wierzby Północy", leaderName = "Matrona Bing Liu",
            alignment = "Izolacjonistyczna / Surowa", powerRank = 15, discipleCount = 240, relations = 0,
            status = DiplomaticStatus.NEUTRAL, specialty = "Lodowe Ciało i Formacje Zamarzania",
            activeTrade = TradeOffer("tr_fr", "Wymiana: 220 Kamieni Ducha za 3x Śnieżny Lotos", 220, "Śnieżny Lotos Oświecenia"),
            policy = SectPolicy.ISOLATIONIST, cultivationBuffName = "Lodowa Dyscyplina",
            cultivationBuffDescription = "+20% obrony wszystkich adeptów", cultivationBuffBonusPercent = 20,
            controlledTerritoryIds = listOf("terr_17")
        ),
        RivalSect(
            id = "sect_drunken", name = "Bractwo Pijanego Nieśmiertelnego", leaderName = "Stary Pijak Jiu Xian",
            alignment = "Chaotyczna / Wesoła", powerRank = 16, discipleCount = 90, relations = 35,
            status = DiplomaticStatus.FRIENDLY, specialty = "Pięść Pijanego Mistrza i Wina Duchowe",
            activeTrade = TradeOffer("tr_dr", "Wymiana: 120 Kamieni Ducha za Owoc Drzewa Pantao", 120, "Owoc Drzewa Pantao"),
            policy = SectPolicy.MERCHANT_CONSORTIUM, cultivationBuffName = "Natchnienie Wina",
            cultivationBuffDescription = "+15% szansy na oświecenie podczas medytacji", cultivationBuffBonusPercent = 15,
            controlledTerritoryIds = listOf("terr_19")
        )
    )

    val extraBeasts: List<SectDivineBeast> = listOf(
        SectDivineBeast(
            id = "beast_roc", name = "Roc Dziewięciu Piorunów", title = "Pan Burzowego Nieba", emoji = "🦅",
            element = "Błyskawica i Wiatr", tier = 1, intimacy = 15, currentEnergy = 1600, maxEnergy = 1600,
            passiveSectBuff = "+40% szybkości wypraw i ucieczki z przegranych walk",
            combatSkillName = "Skrzydła Dziewięciu Gromów", combatSkillDesc = "Razi wszystkich wrogów piorunem, 30% szansy na paraliż",
            favoriteHerb = "Mech Gromowego Kamienia"
        ),
        SectDivineBeast(
            id = "beast_baize", name = "Biały Baize Mądrości", title = "Znawca Wszystkich Demonów", emoji = "🦌",
            element = "Duch i Wiedza", tier = 1, intimacy = 20, currentEnergy = 1400, maxEnergy = 1400,
            passiveSectBuff = "+1 punkt Dao co 4 cykle dla adepta z najwyższym zrozumieniem",
            combatSkillName = "Księga Dziesięciu Tysięcy Demonów", combatSkillDesc = "Ujawnia słabość wroga, +40% obrażeń krytycznych na 3 tury",
            favoriteHerb = "Kwiat Niebiańskiego Zrozumienia"
        ),
        SectDivineBeast(
            id = "beast_jiaolong", name = "Smoczy Wąż Głębin Jiaolong", title = "Pan Przypływów", emoji = "🐍",
            element = "Woda i Yin", tier = 1, intimacy = 10, currentEnergy = 2100, maxEnergy = 2100,
            passiveSectBuff = "+35% zbiorów z terytoriów nadmorskich i rzecznych",
            combatSkillName = "Wir Dziewięciu Głębin", combatSkillDesc = "Topi wrogów w wirze, -35% ich obrony na 2 tury",
            favoriteHerb = "Grzyb Księżycowej Rosy"
        ),
        SectDivineBeast(
            id = "beast_pixiu", name = "Złoty Pixiu Bogactwa", title = "Strażnik Skarbca Niebios", emoji = "🦁",
            element = "Ziemia i Złoto", tier = 1, intimacy = 25, currentEnergy = 1800, maxEnergy = 1800,
            passiveSectBuff = "+30% przychodu Kamieni Ducha ze wszystkich źródeł",
            combatSkillName = "Pożarcie Skarbów", combatSkillDesc = "Kradnie wrogowi Qi i zamienia je w Kamienie Ducha",
            favoriteHerb = "Owoc Drzewa Pantao"
        )
    )

    val extraFlames: List<GeocentricFlame> = listOf(
        GeocentricFlame("flame_9", "Gromowy Płomień Niebiańskiej Kary", "Płomień Tribulacji", 60, 160,
            "Ogień zrodzony z pioruna niebiańskiej kary. Hartuje artefakty błyskawicą.",
            temperatureCelsius = 6100, elementAffinity = "Grom", specialEffect = "+20% szansy na artefakt wyższej rzadkości"),
        GeocentricFlame("flame_10", "Zielony Płomień Drzewa Życia", "Płomień Życia", 45, 70,
            "Ciepły ogień, który leczy zamiast palić. Ulubiony płomień uzdrowicieli.",
            temperatureCelsius = 1200, elementAffinity = "Drewno", specialEffect = "Pigułki lecznicze mają podwójną moc"),
        GeocentricFlame("flame_11", "Czarny Płomień Upadłego Boga", "Płomień Zagłady", 90, 220,
            "Płomień, który spalił bóstwo. Pożera nawet Qi przeciwnika.",
            temperatureCelsius = 9000, elementAffinity = "Pustka", specialEffect = "Ataki wypalają 5% maks. HP wroga co turę"),
        GeocentricFlame("flame_12", "Pierwotny Płomień Chaosu", "Płomień Przed Stworzeniem", 100, 260,
            "Pierwszy ogień wszechświata. Kto go poskromi, warzy pigułki bogów.",
            temperatureCelsius = 12000, elementAffinity = "Chaos", specialEffect = "Każdy wytop ma 10% szansy na podwójną pigułkę")
    )
}
