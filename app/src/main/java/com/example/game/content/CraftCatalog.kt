package com.example.game.content

import com.example.model.AlchemyRecipe
import com.example.model.Artifact
import com.example.model.ForgingRecipe

/** Rozszerzenie rzemiosła: receptury 19–36 i artefakty 11–30 (w tym z łupów bossów). */
object CraftCatalog {

    private const val T1 = "Stopień 1: Oczyszczenie Qi"
    private const val T2 = "Stopień 2: Ustanowienie Fundamentu"
    private const val T3 = "Stopień 3: Formowanie Złotego Rdzenia"
    private const val T4 = "Stopień 4: Narodzenie Duszy (Nascent)"
    private const val T5 = "Stopień 5: Nieśmiertelne Dao"
    private const val T6 = "Stopień 6: Pierwotny Chaos"

    private fun rec(
        id: String, name: String, tier: Int, tierName: String, chamber: Int,
        herb: String, herbCount: Int, stones: Int, effect: String,
        breakthrough: Int = 0, hp: Int = 0, dao: Int = 0,
        second: String? = null, secondCount: Int = 0
    ) = AlchemyRecipe(
        id = id, pillName = name, requiredHerbName = herb, requiredHerbCount = herbCount,
        requiredSpiritStones = stones, effectDescription = effect, breakthroughBonusPercent = breakthrough,
        hpRestore = hp, tier = tier, tierName = tierName, requiredAlchemyChamberLevel = chamber,
        secondaryHerbName = second, secondaryHerbCount = secondCount, daoPointsBonus = dao
    )

    val extraRecipes: List<AlchemyRecipe> = listOf(
        // Stopień 1
        rec("rec_x_1", "Pigułka Ciszy Umysłu", 1, T1, 1, "Trawa Wiecznej Wiosny", 2, 40,
            "Ucisza demony serca podczas medytacji. +10% postępu Qi.", breakthrough = 10),
        rec("rec_x_2", "Maść Zrastania Kości", 1, T1, 1, "Fioletowy Korzeń Skorpiona", 2, 45,
            "Zrasta złamania po wyprawie. Leczy 500 HP.", hp = 500),
        rec("rec_x_3", "Herbata Porannej Rosy", 1, T1, 1, "Trawa Wiecznej Wiosny", 3, 30,
            "Napar budzący meridiany o świcie. Leczy 200 HP i +5% postępu Qi.", breakthrough = 5, hp = 200),
        // Stopień 2
        rec("rec_x_4", "Pigułka Księżycowej Rosy", 2, T2, 2, "Grzyb Księżycowej Rosy", 3, 120,
            "Chłodzi dantian przed przełomem. +25% postępu Qi.", breakthrough = 25),
        rec("rec_x_5", "Eliksir Głębinowego Oddechu", 2, T2, 2, "Grzyb Księżycowej Rosy", 2, 110,
            "Pozwala oddychać w głębinach. Leczy 800 HP.", hp = 800, second = "Perła Głębin", secondCount = 1),
        rec("rec_x_6", "Pigułka Lodowego Spokoju", 2, T2, 2, "Śnieżny Lotos Oświecenia", 2, 130,
            "Zamraża chaos myśli. +1 punkt Dao.", dao = 1),
        // Stopień 3
        rec("rec_x_7", "Pigułka Gromowego Hartu", 3, T3, 3, "Mech Gromowego Kamienia", 3, 260,
            "Przepuszcza piorun przez meridiany. +30% postępu Qi.", breakthrough = 30, second = "Kryształ Gromu", secondCount = 1),
        rec("rec_x_8", "Balsam Krwawej Kości", 3, T3, 3, "Korzeń Krwawej Kości", 3, 280,
            "Hartuje kości Asury krwią bogów. Leczy 1500 HP.", hp = 1500),
        rec("rec_x_9", "Eliksir Piaskowego Słońca", 3, T3, 3, "Kwiat Niebiańskiego Zrozumienia", 2, 240,
            "Wlewa światło pustyni w dantian. +2 punkty Dao.", dao = 2, second = "Słoneczny Złoty Piasek", secondCount = 2),
        // Stopień 4
        rec("rec_x_10", "Pigułka Tysiącletniej Orchidei", 4, T4, 4, "Orchidea Tysiąca Lat", 2, 520,
            "Tysiąc lat wzrostu w jednej pigułce. +45% postępu Qi.", breakthrough = 45),
        rec("rec_x_11", "Eliksir Smoczej Kości", 4, T4, 4, "Korzeń Krwawej Kości", 4, 560,
            "Wzmacnia szkielet do twardości smoczej kości. Leczy 3000 HP.", hp = 3000, second = "Kość Smoka Pradawnego", secondCount = 1),
        rec("rec_x_12", "Pigułka Dziewięciu Gromów Tribulacji", 4, T4, 4, "Mech Gromowego Kamienia", 5, 600,
            "Pozwala przetrwać tribulację piorunów. +50% postępu Qi.", breakthrough = 50, second = "Kryształ Gromu", secondCount = 2),
        // Stopień 5
        rec("rec_x_13", "Brzoskwinia Nieśmiertelności", 5, T5, 5, "Owoc Drzewa Pantao", 1, 900,
            "Owoc z niebiańskiego sadu, przyrządzony w kotle. +3 punkty Dao i leczy 5000 HP.", dao = 3, hp = 5000),
        rec("rec_x_14", "Pigułka Gwiezdnej Mgły", 5, T5, 5, "Kwiat Gwiezdnej Mgły", 2, 950,
            "Mapa konstelacji w dantian. +60% postępu Qi.", breakthrough = 60, second = "Rdzeń Gwiazdy", secondCount = 1),
        rec("rec_x_15", "Eliksir Szklanej Pustki", 5, T5, 5, "Kwiat Gwiezdnej Mgły", 3, 1000,
            "Duch staje się przejrzysty jak szkło pustki. +4 punkty Dao.", dao = 4, second = "Szkło Pustki", secondCount = 1),
        // Stopień 6
        rec("rec_x_16", "Pigułka Serca Chaosu", 6, T6, 6, "Orchidea Tysiąca Lat", 4, 1800,
            "Odrobina chaosu przed stworzeniem. +80% postępu Qi.", breakthrough = 80, second = "Jadeit Chaosu", secondCount = 1),
        rec("rec_x_17", "Eliksir Dziesięciu Tysięcy Lat", 6, T6, 6, "Owoc Drzewa Pantao", 3, 2000,
            "Przywraca pełnię sił nawet umierającemu. Pełne leczenie.", hp = 99999),
        rec("rec_x_18", "Pigułka Złotego Nieśmiertelnego", 6, T6, 6, "Kwiat Gwiezdnej Mgły", 5, 2500,
            "Ostatni krok do nieśmiertelności. +100% postępu Qi i +5 punktów Dao.", breakthrough = 100, dao = 5,
            second = "Jadeit Chaosu", secondCount = 2)
    )

    private fun forge(id: Int, name: String, mineral: String, count: Int, stones: Int, rarity: String, atk: Int, def: Int, effect: String) =
        ForgingRecipe(
            id = "forge_$id", artifactName = name, requiredMineralName = mineral, requiredMineralCount = count,
            requiredSpiritStones = stones, artifactResult = Artifact("art_$id", name, rarity, atk, def, effect)
        )

    val extraForging: List<ForgingRecipe> = listOf(
        forge(11, "Nefrytowy Wisior Spokoju", "Duchowy Nefryt", 3, 80, "Rzadki", 5, 25, "+10% szybszej medytacji"),
        forge(12, "Obsydianowe Sztylety Miazmatu", "Czarny Obsydian Miazmatu", 3, 140, "Rzadki", 55, 5, "Zatruwa wroga: 3% HP co turę"),
        forge(13, "Karwasze Słonecznego Piasku", "Słoneczny Złoty Piasek", 4, 180, "Niebiański", 20, 50, "+20% obrony za dnia"),
        forge(14, "Miecz Gromowego Kryształu", "Kryształ Gromu", 4, 320, "Niebiański", 95, 15, "15% szansy na porażenie piorunem"),
        forge(15, "Perłowy Diadem Głębin", "Perła Głębin", 3, 300, "Niebiański", 25, 70, "+25% regeneracji Qi w walce"),
        forge(16, "Rękawice Smoczej Kości", "Kość Smoka Pradawnego", 3, 520, "Boski", 120, 60, "Ciosy pięścią ignorują 20% obrony"),
        forge(17, "Kościana Zbroja Lisza", "Filakteria Upadłego Patriarchy", 1, 900, "Nieśmiertelny", 40, 180, "Raz na walkę wstaje z 30% HP"),
        forge(18, "Laska Rdzenia Gwiazdy", "Rdzeń Gwiazdy", 2, 800, "Nieśmiertelny", 140, 60, "+30% obrażeń technik żywiołów"),
        forge(19, "Płaszcz ze Szkła Pustki", "Szkło Pustki", 2, 850, "Nieśmiertelny", 30, 160, "+35% uników przed technikami"),
        forge(20, "Łuk Pióra Gromowego Roka", "Pióro Gromowego Roka", 1, 600, "Boski", 130, 20, "Strzały przeskakują na 2 kolejnych wrogów"),
        forge(21, "Tarcza Atramentu Krakena", "Atrament Krakena", 1, 620, "Boski", 20, 140, "Oślepia atakującego na 1 turę"),
        forge(22, "Wąż-Bicz Duchowej Skóry", "Skóra Duchowego Węża", 1, 200, "Rzadki", 60, 20, "+20% szansy na unik po trafieniu"),
        forge(23, "Rogaty Hełm Mroźnego Qilina", "Róg Mroźnego Qilina", 1, 380, "Niebiański", 15, 95, "Odporność na Arktyczny Mróz Qi"),
        forge(24, "Kieł-Sztylet Bazyliszka", "Kieł Trującego Monstrum", 1, 420, "Niebiański", 105, 10, "Trucizna: 5% HP wroga co turę"),
        forge(25, "Wachlarz Nieśmiertelnego Ognia", "Pióro Nieśmiertelnego Ognia", 1, 700, "Boski", 115, 45, "Podpala wszystkich wrogów"),
        forge(26, "Włócznia Żądła Pustyni", "Żądło Pustynnego Władcy", 1, 900, "Boski", 165, 30, "+40% obrażeń krytycznych"),
        forge(27, "Smocza Łuskowa Zbroja", "Boska Smocza Łuska", 1, 1600, "Nieśmiertelny", 60, 240, "-30% wszystkich obrażeń"),
        forge(28, "Złota Laska Wielkiego Mędrca", "Złota Laska Małpiego Króla", 1, 1800, "Wieczne Dao", 220, 120, "Uderzenie rozszczepia się na 3 klony"),
        forge(29, "Gwiazdozbiór na Dłoni", "Odłamek Konstelacji", 1, 2400, "Wieczne Dao", 240, 160, "Raz na walkę przyzywa deszcz gwiazd"),
        forge(30, "Miecz Pierwotnego Chaosu", "Serce Chaosu", 1, 3600, "Wieczne Dao", 320, 200, "Każdy cios ma 10% szansy na natychmiastowe zwycięstwo")
    )
}
