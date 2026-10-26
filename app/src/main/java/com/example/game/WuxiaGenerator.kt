package com.example.game

import com.example.model.*
import java.util.UUID
import kotlin.random.Random

object WuxiaGenerator {

    private val maleFirstNames = listOf(
        "Chen", "Feng", "Hao", "Li", "Bo", "Tian", "Long", "Jian", "Wu", "Yuan", "Xiao", "Yan", "Fan", "Zhen", "Wulin", "Ming"
    )
    private val femaleFirstNames = listOf(
        "Ling'er", "Qing", "Wan", "Yue", "Xue", "Mei", "Ru", "Yan'er", "Ning", "Shu", "Xia", "Lan", "Huan", "Ying", "Bao"
    )
    private val familyNames = listOf(
        "Lin", "Ye", "Xiao", "Chu", "Meng", "Han", "Su", "Bai", "Tang", "Zhao", "Gu", "Shen", "Mu", "Jiang", "Duan", "Song"
    )

    private val avatarEmojis = listOf("🥋", "⚔️", "🧘", "🏮", "🐉", "🦅", "⚡", "🗡️", "🌸", "🌙")

    private val ancientClans = listOf(
        AncientClan("Ród Gu", "Mistrzowie Pasożytów Gu i Trucizn", "🦂", "Zakazane Toksyny", "+40% Obrażenia od Trucizn", "+25% Krytyk"),
        AncientClan("Ród Murong", "Władcy Iluzji i Przesunięcia Gwiazd", "🪞", "Odbicie Cięć", "+35% Szansa na Kontratak", "+30% Uniki"),
        AncientClan("Ród Xiao", "Pradawny Płomień Dziewięciu Niebios", "🔥", "Spopielenie Qi", "+45% Siła Ataku Ogniem", "+35% Atak"),
        AncientClan("Ród Ye", "Gwiezdni Wędrowcy Pustki", "🌌", "Manipulacja Przestrzenią", "+50% Szybkość Kultywacji", "+25% Dao"),
        AncientClan("Ród Tang", "Tysiąc Ukrytych Ostrzy i Igieł", "🎯", "Broń Miotana", "+40% Przebicie Pancerza", "+20% Atak"),
        AncientClan("Ród Lin", "Naczelny Ród Miecza Niebios", "🗡️", "Czysta Intencja Miecza", "+50% Obrażenia Miecza", "+40% Atak"),
        AncientClan("Ród Nangong", "Niezłomna Gwardia Cesarska", "🛡️", "Żelazna Ściana Qi", "+60% Obrona i Tarcza", "+50% Obrona"),
        AncientClan("Ród Chu", "Władcy Dusz i Mistycznych Bestii", "🦅", "Oswajanie Bestii", "+45% Bonus przeciw Bestiom", "+30% HP"),
        AncientClan("Ród Zhuge", "Mistrzowie Formacji i Taktyki Dao", "📜", "Wielkie Pieczęcie", "+40% Efektywność Alchemii", "+35% Zrozumienie"),
        AncientClan("Ród Feng", "Władcy Wichru i Piorunów", "⚡", "Błyskawiczne Pchnięcie", "+35% Prędkość Ruchu", "+30% Atak Piorunem"),
        AncientClan("Ród Bai", "Święty Ród Lodowego Tygrysa", "🐯", "Zamrażający Ryk", "+40% Spowolnienie Wroga", "+35% Kontrola"),
        AncientClan("Ród Ouyang", "Mroczni Władcy Pradawnej Krwi", "🩸", "Wyssanie Witalności", "+25% Kradzież Życia", "+25% HP"),
        AncientClan("Ród Dugu", "Pustelnicy Niezwyciężonego Miecza", "⚔️", "Samotny Szermierz", "+60% Obrażenia Pojedynczego Celu", "+45% Atak"),
        AncientClan("Ród Huangfu", "Strażnicy Złotego Panteonu", "👑", "Majestat Cesarski", "+50% Prestiż i Autorytet", "+30% Wszystkie Staty"),
        AncientClan("Ród Gongsun", "Tancerze Dwóch Ostrzy Feniksa", "🪶", "Taniec Bliźniaczych Ostrzy", "+45% Szansa na Podwójny Atak", "+35% Zwinność"),
        AncientClan("Ród Sikong", "Władcy Cieni i Nocnych Mgieł", "🌑", "Cień Niewidzialności", "+55% Uniki i Zaskoczenie", "+40% Krytyk"),
        AncientClan("Ród Zhongli", "Pradawni Mistrzowie Alchemii i Ognia", "⚗️", "Kotły Wielkiego Pieca", "+60% Jakość Wypalania Pigułek", "+40% Alchemia"),
        AncientClan("Ród Yuwen", "Kowale Meteorytów i Błyskawic", "🔨", "Młot Tytanów", "+55% Statystyki Wykutej Broni", "+35% Siła")
    )

    private val fatedTraitsPool = listOf(
        FatedTrait("ft_1", "Urodzony Bóg Wojny", "Boski", "Podwaja siłę ciosu w obliczu potężniejszych wrogów", attackBonusPercent = 45, critChanceBonus = 15),
        FatedTrait("ft_2", "Oko Zrozumienia Dao", "Boski", "Błyskawicznie pojmuje zasady niebios i formuły eliksirów", cultivationBonusPercent = 50, alchemyBonusPercent = 35),
        FatedTrait("ft_3", "Ciało ze Szpiku Gwiazd", "Legendarny", "Niezrównana wytrzymałość na ciosy i tribulacje", defenseBonusPercent = 40),
        FatedTrait("ft_4", "Pakt z Płomieniem Serca", "Legendarny", "Wrodzone mistrzostwo destylacji pigułek i pieców", alchemyBonusPercent = 50),
        FatedTrait("ft_5", "Kowalskie Dłonie Tytana", "Legendarny", "Potrafi przekuwać meteoryty z zerową stratą esencji", forgingBonusPercent = 50),
        FatedTrait("ft_6", "Błogosławieństwo Pomyślnego Losu", "Boski", "Niezwykłe szczęście przy przełomach meridianów", cultivationBonusPercent = 35, critChanceBonus = 10),
        FatedTrait("ft_7", "Zimna Krew Bez Trwogi", "Niezwykły", "Odporność na panikę i rany krytyczne", defenseBonusPercent = 25),
        FatedTrait("ft_8", "Pożeracz Esencji Bestii", "Legendarny", "Pochłania 20% siły każdej pokonanej bestii", attackBonusPercent = 30),
        FatedTrait("ft_9", "Harmonijny Przepływ Czystego Yang", "Wybitny", "Przyspiesza regenerację energii życiowej", cultivationBonusPercent = 30),
        FatedTrait("ft_10", "Zabójca Cienia i Mgły", "Wybitny", "Ciche pchnięcie prosto w serce Dantian wroga", attackBonusPercent = 35, critChanceBonus = 20),
        FatedTrait("ft_11", "Niebiańska Aura Charyzmy", "Niezwykły", "Zjednuje przychylność obcych klanów i sekt", defenseBonusPercent = 15),
        FatedTrait("ft_12", "Czyste Oko Prawdy", "Legendarny", "Ignoruje obronę żywiołów przeciwnika", attackBonusPercent = 40),
        FatedTrait("ft_13", "Dusza Wiecznego Feniksa", "Boski", "Automatyczne odrodzenie z 50% HP raz na walkę", defenseBonusPercent = 50, cultivationBonusPercent = 25),
        FatedTrait("ft_14", "Władca Pioruna Tribulacji", "Boski", "Absorbuje energię błyskawic tribulacji zwiększając CP", attackBonusPercent = 50, critChanceBonus = 20),
        FatedTrait("ft_15", "Mistyk Złotego Dzwonu", "Legendarny", "Redukuje wszelkie otrzymywane obrażenia o 30%", defenseBonusPercent = 45),
        FatedTrait("ft_16", "Serce Pradawnego Smoka", "Boski", "Wydziela aurę paraliżującą przeciwników o niższym królestwie", attackBonusPercent = 55),
        FatedTrait("ft_17", "Gwiezdny Podróżnik Pustki", "Legendarny", "Zwiększa uniki o 35% i szybkość zbierania ziół o 50%", cultivationBonusPercent = 35),
        FatedTrait("ft_18", "Dotyk Niebiańskiego Alchemika", "Legendarny", "100% szansa na wytworzenie pigułki najwyższej jakości", alchemyBonusPercent = 60)
    )

    private val innateTechniques = listOf(
        "Taniec Siedmiu Spadających Gwiazd",
        "Rozcięcie Przestrzeni Pustki",
        "Ryk Płonącego Smoka Asury",
        "Miażdżące Pchnięcie Dziewięciu Gór",
        "Iluzja Trzech Tysięcy Cieni",
        "Władza Nad Pierwotnym Gromem",
        "Klątwa Lodowego Grobowca",
        "Formacja Wirującego Ostrza Dao",
        "Piekielne Spopielenie Dantian",
        "Oczyszczające Światło Lotosu",
        "Sąd Płomiennego Feniksa",
        "Cięcie Niebiańskiego Wymiaru",
        "Święty Bastion Xuanwu",
        "Kroki Złotego Ptaka Kunpenga",
        "Dotyk Śmierci Dziewięciu Toksyn",
        "Fuzja Pięciu Piorunów Niebios",
        "Spopielenie Słonecznego Kruka",
        "Aura Wiecznego Spokoju Dao"
    )

    fun getInitialSectLeader(): SectLeaderData {
        return SectLeaderData(
            name = "Mistrz Chen Tian",
            title = "Patriarcha Dziewięciu Niebios",
            clanHeritage = "Starożytny Ród Chen z Góry Świętego Smoka",
            ancientClan = AncientClan("Ród Chen", "Pradawni Władcy Płomienia", "🐉", "Szermierka Dao", "Zwiększa siłę ataku mieczem o 25%", "+25% Atak"),
            lineage = "Krew Złotego Smoka",
            divineConstitution = "Dziewięć Boskich Meridianów",
            realm = "Narodzenie Duszy (Nascent Soul)",
            prestigeLevel = 3,
            leadershipPoints = 8,
            divineAuthority = 450,
            activeDecrees = listOf(
                SectDecree("dec_1", "Edykt Złotego Smoka", "Podnosi absorpcję Qi dla wszystkich uczniów o 35%", costPrestige = 2, isActive = true, bonusDesc = "+35% Zysk Qi Sekty", icon = "🐉"),
                SectDecree("dec_2", "Dekret Niebiańskiego Zaciągu", "Przyciąga adeptów z rodów szlacheckich o randze S i wyższej", costPrestige = 3, isActive = true, bonusDesc = "Gwarantowana ranga talentu min. S", icon = "🏮"),
                SectDecree("dec_3", "Tarcza Ochronna Patriarchy", "Zmniejsza szansę na niepowodzenie przełomu o 25%", costPrestige = 3, isActive = false, bonusDesc = "+25% Szansa na Przełom", icon = "🛡️"),
                SectDecree("dec_4", "Imperialny Pakt Handlowy", "Zwiększa zyski ze szlaków minerałów i ziół o 40%", costPrestige = 2, isActive = false, bonusDesc = "+40% Zbiory Surowców", icon = "💎")
            )
        )
    }

    fun generateDisciple(
        predefinedRealm: CultivationRealm = CultivationRealm.QI_CONDENSATION_1,
        guaranteedBloodline: BloodlineType? = null
    ): Disciple {
        val clan = ancientClans.random()
        val isMale = Random.nextBoolean()
        val gender = if (isMale) "Mężczyzna" else "Kobieta"
        val firstName = if (isMale) maleFirstNames.random() else femaleFirstNames.random()
        val clanPrefix = clan.name.removePrefix("Ród ")
        val fullName = "$clanPrefix $firstName"

        val origin = OriginType.values().random()
        val bloodline = guaranteedBloodline ?: BloodlineType.values().random()
        val constitution = ConstitutionType.values().random()
        val school = MartialSchool.values().random()

        // Losowanie Rangi Talentu (SSS: 4%, SS: 12%, S: 28%, A: 38%, B: 18%)
        val roll = Random.nextInt(100)
        val talentGrade = when {
            roll < 4 -> TalentGrade.SSS
            roll < 16 -> TalentGrade.SS
            roll < 44 -> TalentGrade.S
            roll < 82 -> TalentGrade.A
            else -> TalentGrade.B
        }

        // Losowanie 1 do 2 Cech Przeznaczenia (Fated Traits)
        val traitsCount = if (talentGrade == TalentGrade.SSS || talentGrade == TalentGrade.SS) 2 else 1
        val assignedTraits = fatedTraitsPool.shuffled().take(traitsCount)

        val talentMultiplier = when (talentGrade) {
            TalentGrade.SSS -> 1.75
            TalentGrade.SS -> 1.45
            TalentGrade.S -> 1.25
            TalentGrade.A -> 1.10
            TalentGrade.B -> 1.00
        }

        val baseHp = (when (school) {
            MartialSchool.BODY_ASURA -> 380 + Random.nextInt(60)
            MartialSchool.SWORD_IMMORTAL -> 220 + Random.nextInt(40)
            MartialSchool.ELEMENTAL_TAOIST -> 180 + Random.nextInt(35)
            MartialSchool.SHADOW_VIPER -> 200 + Random.nextInt(35)
        } * talentMultiplier).toInt()

        val baseAtk = (when (school) {
            MartialSchool.SWORD_IMMORTAL -> 40 + Random.nextInt(15)
            MartialSchool.ELEMENTAL_TAOIST -> 36 + Random.nextInt(12)
            MartialSchool.SHADOW_VIPER -> 34 + Random.nextInt(12)
            MartialSchool.BODY_ASURA -> 26 + Random.nextInt(10)
        } * talentMultiplier).toInt()

        val baseDef = (when (school) {
            MartialSchool.BODY_ASURA -> 30 + Random.nextInt(12)
            MartialSchool.SWORD_IMMORTAL -> 16 + Random.nextInt(8)
            MartialSchool.ELEMENTAL_TAOIST -> 14 + Random.nextInt(6)
            MartialSchool.SHADOW_VIPER -> 15 + Random.nextInt(8)
        } * talentMultiplier).toInt()

        val comprehension = (70 + Random.nextInt(30) * talentMultiplier).toInt().coerceAtMost(100)
        val karmicLuck = (60 + Random.nextInt(40) * talentMultiplier).toInt().coerceAtMost(100)

        val finalHp = (baseHp * predefinedRealm.powerMultiplier).toInt()
        val finalAtk = (baseAtk * predefinedRealm.powerMultiplier).toInt()
        val finalDef = (baseDef * predefinedRealm.powerMultiplier).toInt()

        // Obliczanie unikalnej Siły Bojowej (Combat Power - CP)
        val calculatedCp = (finalHp / 2 + finalAtk * 14 + finalDef * 9 + comprehension * 6 + karmicLuck * 5)

        val innateTech = innateTechniques.random()

        return Disciple(
            id = UUID.randomUUID().toString(),
            name = fullName,
            gender = gender,
            avatarEmoji = avatarEmojis.random(),
            origin = origin,
            bloodline = bloodline,
            constitution = constitution,
            martialSchool = school,
            realm = predefinedRealm,
            realmProgress = Random.nextInt(10, 45),
            maxHp = finalHp,
            currentHp = finalHp,
            attack = finalAtk,
            defense = finalDef,
            comprehension = comprehension,
            karmicLuck = karmicLuck,
            daoPoints = 3,
            role = DiscipleRole.MEDITATING,
            ancientClan = clan,
            fatedTraits = assignedTraits,
            talentGrade = talentGrade,
            combatPower = calculatedCp,
            innateTechnique = innateTech
        )
    }

    fun getAllSkillNodes(): List<SkillNode> {
        val baseSkills = listOf(
            // --- KULTYWACJA & QI GONG (Oddychanie, rozwój Dantian, oczyszczanie meridianów) ---
            SkillNode(
                id = "cult_1",
                name = "Oddychanie Pierwotnego Chaosu",
                tier = 1,
                description = "Starożytna technika oddechowa harmonizująca Yin i Yang. Zwiększa absorpcję Qi o +25%.",
                costDaoPoints = 1,
                martialSchool = MartialSchool.SWORD_IMMORTAL,
                category = TechniqueCategory.CULTIVATION,
                gradeName = "Stopień Żółty",
                qiGainBonusPercent = 25,
                specialEffect = "+25% szybsza medytacja w pawilonie"
            ),
            SkillNode(
                id = "cult_2",
                name = "Metoda Złotego Płynu Dantian",
                tier = 2,
                description = "Kondensuje energię Qi w płynną esencję w dolnym Dantian. +40% Qi, +250 HP.",
                costDaoPoints = 2,
                martialSchool = MartialSchool.BODY_ASURA,
                category = TechniqueCategory.CULTIVATION,
                gradeName = "Stopień Czarny",
                hpBonus = 250,
                qiGainBonusPercent = 40,
                specialEffect = "Rozszerza rezerwuar meridianów"
            ),
            SkillNode(
                id = "cult_3",
                name = "Sutra Oczyszczenia Dziewięciu Meridianów",
                tier = 3,
                description = "Oczyszcza wszelkie skazy i zatory w meridianach duchowych. +60% Qi, +15 Obrona.",
                costDaoPoints = 3,
                martialSchool = MartialSchool.ELEMENTAL_TAOIST,
                category = TechniqueCategory.CULTIVATION,
                gradeName = "Stopień Mistyczny",
                defenseBonus = 15,
                qiGainBonusPercent = 60,
                specialEffect = "+35% szansa na Błysk Oświecenia"
            ),
            SkillNode(
                id = "cult_4",
                name = "Zwój Kosmicznej Pustki Niebios",
                tier = 4,
                description = "Najwyższy sekret kultywacji: bezpośrednie czerpanie energii z kosmosu. +100% Qi, +500 HP.",
                costDaoPoints = 4,
                martialSchool = MartialSchool.SHADOW_VIPER,
                category = TechniqueCategory.CULTIVATION,
                gradeName = "Stopień Niebiański",
                hpBonus = 500,
                qiGainBonusPercent = 100,
                specialEffect = "Przyspiesza przełomy kultywacyjne o 50%"
            ),

            // --- PORUSZANIE & LEKKOŚĆ (Kroki, uniki, teleporcja, zwinność) ---
            SkillNode(
                id = "mov_1",
                name = "Kroki Siedmiu Gwiazd Północy",
                tier = 1,
                description = "Ruch stóp naśladujący gwiazdozbiór Wielkiej Niedźwiedzicy. +15% Uniki, +10 Obrona.",
                costDaoPoints = 1,
                martialSchool = MartialSchool.SWORD_IMMORTAL,
                category = TechniqueCategory.MOVEMENT,
                gradeName = "Stopień Żółty",
                evasionBonusPercent = 15,
                defenseBonus = 10,
                specialEffect = "Zwiększa szansę na uniknięcie ciosu"
            ),
            SkillNode(
                id = "mov_2",
                name = "Taniec Cienia Wiatru",
                tier = 2,
                description = "Ciało rozpływa się w podmuchach wiatru niczym widmo. +25% Uniki, +20 Atak.",
                costDaoPoints = 2,
                martialSchool = MartialSchool.SHADOW_VIPER,
                category = TechniqueCategory.MOVEMENT,
                gradeName = "Stopień Czarny",
                evasionBonusPercent = 25,
                attackBonus = 20,
                specialEffect = "Pozwala wymanewrować silniejszych wrogów"
            ),
            SkillNode(
                id = "mov_3",
                name = "Błyskawiczny Ślad Feniksa",
                tier = 3,
                description = "Zostawia płonący powidok podczas natychmiastowego przesunięcia. +35% Uniki, +15% Kryt.",
                costDaoPoints = 3,
                martialSchool = MartialSchool.ELEMENTAL_TAOIST,
                category = TechniqueCategory.MOVEMENT,
                gradeName = "Stopień Mistyczny",
                evasionBonusPercent = 35,
                critBonusPercent = 15,
                specialEffect = "Podpala przeciwnika przy udanym uniku"
            ),
            SkillNode(
                id = "mov_4",
                name = "Krok Przenikający Pustkę Przestrzeni",
                tier = 4,
                description = "Teleportacja przez załamanie czasoprzestrzeni. +50% Uniki, +80 Atak.",
                costDaoPoints = 4,
                martialSchool = MartialSchool.BODY_ASURA,
                category = TechniqueCategory.MOVEMENT,
                gradeName = "Stopień Niebiański",
                evasionBonusPercent = 50,
                attackBonus = 80,
                specialEffect = "Niewrażliwość na pierwszą turę ataków"
            ),

            // --- FORMACJE & TABLICE DAO (Pieczęcie barierowe, rytuały sekty) ---
            SkillNode(
                id = "form_1",
                name = "Formacja Ostrzy Dziewięciu Piekieł",
                tier = 1,
                description = "Tworzy krąg latających mieczy chroniący czarownika. +25 Atak, +15 Obrona.",
                costDaoPoints = 1,
                martialSchool = MartialSchool.SWORD_IMMORTAL,
                category = TechniqueCategory.FORMATION,
                gradeName = "Stopień Żółty",
                attackBonus = 25,
                defenseBonus = 15,
                activeTechniqueName = "Krąg Dziewięciu Ostrzy"
            ),
            SkillNode(
                id = "form_2",
                name = "Bariera Żółwiego Pancerza Xuanwu",
                tier = 2,
                description = "Pieczęć manifestująca skorupę mitycznego Czarnego Żółwia. +55 Obrona, +350 HP.",
                costDaoPoints = 2,
                martialSchool = MartialSchool.BODY_ASURA,
                category = TechniqueCategory.FORMATION,
                gradeName = "Stopień Czarny",
                defenseBonus = 55,
                hpBonus = 350,
                activeTechniqueName = "Żółwia Tarcza Bastionu"
            ),
            SkillNode(
                id = "form_3",
                name = "Rezonans Ośmiu Trygramów Bagua",
                tier = 3,
                description = "Koło trygramów zniekształcające ataki żywiołów. +60 Atak, +50 Obrona.",
                costDaoPoints = 3,
                martialSchool = MartialSchool.ELEMENTAL_TAOIST,
                category = TechniqueCategory.FORMATION,
                gradeName = "Stopień Mistyczny",
                attackBonus = 60,
                defenseBonus = 50,
                activeTechniqueName = "Pieczęć Ośmiu Trygramów"
            ),
            SkillNode(
                id = "form_4",
                name = "Wielka Tablica Duszy Cienia",
                tier = 4,
                description = "Rytuał zaciemniający całe pole bitwy, kradnący energię rywali. +120 Atak, +25% Kryt.",
                costDaoPoints = 4,
                martialSchool = MartialSchool.SHADOW_VIPER,
                category = TechniqueCategory.FORMATION,
                gradeName = "Stopień Niebiański",
                attackBonus = 120,
                critBonusPercent = 25,
                activeTechniqueName = "Całun Mrocznej Pustki"
            ),

            // --- ATAK & SZTUKI WALKI ---
            SkillNode(
                id = "sw_1",
                name = "Pieśń Płynącej Stali",
                tier = 1,
                description = "Zwiększa siłę cięcia mieczem o +18. Płynne, niepowstrzymane cięcia.",
                costDaoPoints = 1,
                martialSchool = MartialSchool.SWORD_IMMORTAL,
                category = TechniqueCategory.ATTACK,
                gradeName = "Stopień Żółty",
                attackBonus = 18
            ),
            SkillNode(
                id = "sw_3",
                name = "Trzy Tysiące Wirujących Mieczy",
                tier = 2,
                description = "Tworzy wir latających ostrzy raniący wszystkich wrogów. +40 Atak.",
                costDaoPoints = 2,
                martialSchool = MartialSchool.SWORD_IMMORTAL,
                category = TechniqueCategory.ATTACK,
                gradeName = "Stopień Czarny",
                attackBonus = 40,
                activeTechniqueName = "Taniec Trzech Tysięcy Ostrzy"
            ),
            SkillNode(
                id = "ba_2",
                name = "Pięść Zgniatająca Góry Asury",
                tier = 2,
                description = "Mocarny cios wstrząsający ziemią i pękający kości wroga. +45 Atak, +30 Obrona.",
                costDaoPoints = 2,
                martialSchool = MartialSchool.BODY_ASURA,
                category = TechniqueCategory.ATTACK,
                gradeName = "Stopień Czarny",
                attackBonus = 45,
                defenseBonus = 30,
                activeTechniqueName = "Miażdżąca Pięść Tytana"
            ),
            SkillNode(
                id = "et_3",
                name = "Sąd Piorunów Boskiej Kary",
                tier = 3,
                description = "Przyzywa niebiańskie błyskawice tribulacji prosto w serce wroga. +90 Atak.",
                costDaoPoints = 3,
                martialSchool = MartialSchool.ELEMENTAL_TAOIST,
                category = TechniqueCategory.ATTACK,
                gradeName = "Stopień Mistyczny",
                attackBonus = 90,
                activeTechniqueName = "Piorun Dziewięciu Tribulacji"
            ),
            SkillNode(
                id = "sw_5",
                name = "Wniebowstąpienie Astralnego Ostrza",
                tier = 4,
                description = "Ostateczna technika miecza niebios przecinająca czas i przestrzeń. +180 Atak, +25% Kryt.",
                costDaoPoints = 4,
                martialSchool = MartialSchool.SWORD_IMMORTAL,
                category = TechniqueCategory.ATTACK,
                gradeName = "Stopień Niebiański",
                attackBonus = 180,
                critBonusPercent = 25,
                activeTechniqueName = "Gniew Boskiego Miecza"
            ),
            SkillNode(
                id = "et_4",
                name = "Pierwotna Fuzja Pięciu Żywiołów",
                tier = 4,
                description = "Kontrola nad żywiołami ognia, wody, ziemi, metalu i drewna. +160 Atak, +70 Obrona.",
                costDaoPoints = 4,
                martialSchool = MartialSchool.ELEMENTAL_TAOIST,
                category = TechniqueCategory.ATTACK,
                gradeName = "Stopień Niebiański",
                attackBonus = 160,
                defenseBonus = 70,
                activeTechniqueName = "Kosmiczny Wybuch Żywiołów"
            ),
            SkillNode(
                id = "sv_4",
                name = "Pieczęć Zniszczenia Meridianów",
                tier = 4,
                description = "Blokuje przepływ Qi wroga i zatruwa jego Dantian. +150 Atak, +30% Kryt.",
                costDaoPoints = 4,
                martialSchool = MartialSchool.SHADOW_VIPER,
                category = TechniqueCategory.ATTACK,
                gradeName = "Stopień Niebiański",
                attackBonus = 150,
                critBonusPercent = 30,
                activeTechniqueName = "Cios Dziewięciu Punktów Śmierci"
            ),

            // --- OBRONA & ZŁOTE CIAŁO ---
            SkillNode(
                id = "ba_1",
                name = "Złoty Dzwon Niezniszczalności",
                tier = 1,
                description = "Zwiększa twardość powłoki ciała. +25 Obrona, +200 HP.",
                costDaoPoints = 1,
                martialSchool = MartialSchool.BODY_ASURA,
                category = TechniqueCategory.DEFENSE,
                gradeName = "Stopień Żółty",
                defenseBonus = 25,
                hpBonus = 200
            ),
            SkillNode(
                id = "et_2",
                name = "Płaszcz Eterycznego Lodu",
                tier = 2,
                description = "Otacza maga lodowym pancerzem zamrażającym ostrza wroga. +35 Obrona, +200 HP.",
                costDaoPoints = 2,
                martialSchool = MartialSchool.ELEMENTAL_TAOIST,
                category = TechniqueCategory.DEFENSE,
                gradeName = "Stopień Czarny",
                defenseBonus = 35,
                hpBonus = 200
            ),
            SkillNode(
                id = "ba_3",
                name = "Diamentowa Skóra Wadżry",
                tier = 3,
                description = "Komórki ciała natychmiast absorbują impakt. +500 HP, +60 Obrona.",
                costDaoPoints = 3,
                martialSchool = MartialSchool.BODY_ASURA,
                category = TechniqueCategory.DEFENSE,
                gradeName = "Stopień Mistyczny",
                hpBonus = 500,
                defenseBonus = 60
            ),
            SkillNode(
                id = "ba_4",
                name = "Złote Niezniszczalne Bóstwo Tytana",
                tier = 4,
                description = "Ciało staje się twardsze od niebiańskich piorunów. +1200 HP, +130 Obrona.",
                costDaoPoints = 4,
                martialSchool = MartialSchool.BODY_ASURA,
                category = TechniqueCategory.DEFENSE,
                gradeName = "Stopień Niebiański",
                hpBonus = 1200,
                defenseBonus = 130,
                activeTechniqueName = "Aura Nieśmiertelnego Asury"
            ),

            // --- TIER 5 OSTATECZNE SZTUKI ZAKAZANE & BOSKIE NIEBIOS ---
            SkillNode(
                id = "sw_tier5",
                name = "Wielkie Wniebowstąpienie Miecza Dao",
                tier = 5,
                description = "Miecz staje się jednością z umysłem i prawami niebios. +250 Atak, +30% Kryt.",
                costDaoPoints = 5,
                martialSchool = MartialSchool.SWORD_IMMORTAL,
                category = TechniqueCategory.ATTACK,
                gradeName = "Stopień Pierwotnego Dao",
                attackBonus = 250,
                critBonusPercent = 30,
                activeTechniqueName = "Jedność Miecza z Niebiosami"
            ),
            SkillNode(
                id = "ba_tier5",
                name = "Kosmiczne Przebudzenie Asury Chaosu",
                tier = 5,
                description = "Własne ciało adepta przekracza granice śmiertelności. +3000 HP, +260 Atak.",
                costDaoPoints = 5,
                martialSchool = MartialSchool.BODY_ASURA,
                category = TechniqueCategory.ATTACK,
                gradeName = "Stopień Pierwotnego Dao",
                hpBonus = 3000,
                attackBonus = 260,
                activeTechniqueName = "Cios Tytana Pierwotnego Chaosu"
            ),
            SkillNode(
                id = "et_tier5",
                name = "Narodziny Władcy Pięciu Żywiołów Dao",
                tier = 5,
                description = "Panowanie nad kosmicznymi prawami ognia, lodu, błyskawic i ziemi. +220 Atak, +140 Obrona.",
                costDaoPoints = 5,
                martialSchool = MartialSchool.ELEMENTAL_TAOIST,
                category = TechniqueCategory.FORMATION,
                gradeName = "Stopień Pierwotnego Dao",
                attackBonus = 220,
                defenseBonus = 140,
                activeTechniqueName = "Sąd Dziewięciu Niebios Żywiołów"
            ),
            SkillNode(
                id = "sv_tier5",
                name = "Pustka Cienia Wiecznej Otchłani",
                tier = 5,
                description = "Zanurzenie w mroku poza granicami czasoprzestrzeni. +65% Uniki, +210 Atak.",
                costDaoPoints = 5,
                martialSchool = MartialSchool.SHADOW_VIPER,
                category = TechniqueCategory.MOVEMENT,
                gradeName = "Stopień Pierwotnego Dao",
                evasionBonusPercent = 65,
                attackBonus = 210,
                activeTechniqueName = "Taniec Niewidzialnego Żniwiarza"
            ),
            SkillNode(
                id = "cult_5",
                name = "Sutra Wiecznego Oddechu Pierwotnego Dao",
                tier = 5,
                description = "Kondensuje nieskończoną energię kosmiczną bezpośrednio w Dantian. +180% Zysk Qi, +1500 HP.",
                costDaoPoints = 5,
                martialSchool = MartialSchool.SWORD_IMMORTAL,
                category = TechniqueCategory.CULTIVATION,
                gradeName = "Stopień Pierwotnego Dao",
                hpBonus = 1500,
                qiGainBonusPercent = 180,
                specialEffect = "Przyspiesza awans kultywacji o 100%"
            ),
            SkillNode(
                id = "def_5_bastion",
                name = "Święty Bastion Niezłomnego Bóstwa",
                tier = 5,
                description = "Bariera absorbująca wszelkie ciosy fizyczne i magię żywiołów. +220 Obrona, +2000 HP.",
                costDaoPoints = 5,
                martialSchool = MartialSchool.BODY_ASURA,
                category = TechniqueCategory.DEFENSE,
                gradeName = "Stopień Pierwotnego Dao",
                hpBonus = 2000,
                defenseBonus = 220,
                activeTechniqueName = "Bariera Nieprzeniknionego Bastionu"
            )
        )

        return baseSkills + generateProceduralTechniques(30)
    }

    fun generateProceduralTechniques(count: Int = 30): List<SkillNode> {
        val prefixes = listOf("Czystego Yang", "Głębokiego Yin", "Pradawnego Smoka", "Karmazynowego Feniksa", "Białego Tygrysa", "Mroźnego Lotosu", "Gwiezdnej Pustki", "Pioruna Tribulacji", "Nefrytowego Wichru", "Niebiańskiego Dao")
        val roots = listOf(
            Triple("Oddech", TechniqueCategory.CULTIVATION, "Wzmacnia meridiany i przyspiesza krążenie energii Qi"),
            Triple("Kroki", TechniqueCategory.MOVEMENT, "Niezwykle zwinne manewry omijające wrogie cięcia"),
            Triple("Pieczęć", TechniqueCategory.FORMATION, "Rytuał barierowy manifestujący ochronne runy"),
            Triple("Cięcie", TechniqueCategory.ATTACK, "Śmiertelny atak przecinający linie obronne przeciwnika"),
            Triple("Pancerz", TechniqueCategory.DEFENSE, "Wzmacnia ciało adepta esencją żywiołów")
        )
        val schools = MartialSchool.values()
        val grades = listOf("Stopień Żółty", "Stopień Czarny", "Stopień Mistyczny", "Stopień Niebiański", "Stopień Pierwotnego Dao")

        return List(count) { idx ->
            val tier = (idx % 4) + 1
            val root = roots[idx % roots.size]
            val prefix = prefixes.random()
            val school = schools[idx % schools.size]
            val techName = "${root.first} $prefix"
            val grade = grades[(tier - 1).coerceIn(0, grades.size - 1)]

            val atk = if (root.second == TechniqueCategory.ATTACK) tier * 35 + Random.nextInt(10, 25) else tier * 10
            val def = if (root.second == TechniqueCategory.DEFENSE || root.second == TechniqueCategory.FORMATION) tier * 25 + Random.nextInt(10, 20) else tier * 5
            val hp = if (root.second == TechniqueCategory.DEFENSE || root.second == TechniqueCategory.CULTIVATION) tier * 200 + Random.nextInt(50, 150) else 0
            val crit = if (root.second == TechniqueCategory.ATTACK || root.second == TechniqueCategory.MOVEMENT) tier * 5 + 3 else 0
            val evasion = if (root.second == TechniqueCategory.MOVEMENT) tier * 10 + 5 else 0
            val qiGain = if (root.second == TechniqueCategory.CULTIVATION) tier * 20 + 15 else 0

            SkillNode(
                id = "proc_tech_${idx + 1}",
                name = techName,
                tier = tier,
                description = "${root.third}. Pochodzi ze starożytnych zwojów klanowych.",
                costDaoPoints = tier,
                martialSchool = school,
                attackBonus = atk,
                defenseBonus = def,
                critBonusPercent = crit,
                hpBonus = hp,
                category = root.second,
                gradeName = grade,
                qiConsumption = 10 * tier,
                qiGainBonusPercent = qiGain,
                evasionBonusPercent = evasion,
                activeTechniqueName = if (tier >= 2) techName else null
            )
        }
    }

    fun getGeocentricFlames(): List<GeocentricFlame> {
        return listOf(
            GeocentricFlame("flame_1", "Błękitny Płomień Serca Lawy", "Ziemny Płomień", 25, 40, "Płomień wydobyty z głębi wulkanu. Wspomaga kucie ostrzy i alchemię."),
            GeocentricFlame("flame_2", "Mroźny Płomień Astralnego Lotosu", "Niebiański Płomień", 35, 65, "Płonie lodem, paraliżując ciepło i oczyszczając toksyny alchemiczne."),
            GeocentricFlame("flame_3", "Złoty Płomień Słonecznej Wrony", "Starożytny Płomień", 50, 95, "Pozostałość po legendarnym słońcu. Gwarantuje perfekcyjne wypalenie pigułek."),
            GeocentricFlame("flame_4", "Purpurowy Płomień Otchłani Pustki", "Boski Płomień Dao", 70, 150, "Płomień pożerający prawa fizyki, czyniący rzemieślnika arcymistrzem."),
            GeocentricFlame("flame_5", "Mroźny Płomień Lodowej Otchłani", "Niebiański Płomień Absolutu", 40, 80, "Zamraża meridiany wrogów w walce i stabilizuje wrzące kociołki alchemiczne."),
            GeocentricFlame("flame_6", "Duchowy Płomień Tysiąca Bestii", "Płomień Bestialskiego Szału", 45, 110, "Zrodzony z dusz potężnych bestii. Drastycznie zwiększa siłę ataków adeptów."),
            GeocentricFlame("flame_7", "Cesarz Płomieni Złotego Słońca", "Najwyższy Płomień Czystego Yang", 65, 175, "Płomień czystego słońca potrafiący przetopić najtwardsze meteoryty niebios."),
            GeocentricFlame("flame_8", "Płomień Upadłego Serca Dantian", "Wewnętrzny Płomień Iluzji", 55, 130, "Płonie wprost w sercach wrogów, paraliżując ich zdolność skupienia Qi.")
        )
    }

    fun getInitialFacilities(): List<SectFacility> {
        return listOf(
            SectFacility(FacilityType.CULTIVATION_PAVILION, 1, 150, "+30% Szybkość zbierania Qi przez uczniów"),
            SectFacility(FacilityType.ALCHEMY_CHAMBER, 1, 200, "Odblokowuje podstawowe i średnie pigułki kultywacyjne"),
            SectFacility(FacilityType.DIVINE_FORGE, 1, 250, "Umożliwia kucie broni i pancerzy z rzadkich minerałów"),
            SectFacility(FacilityType.MARTIAL_ARCHIVES, 1, 180, "+1 dodatkowy punkt Dao przy awansie kultywacji"),
            SectFacility(FacilityType.MISSION_HALL, 1, 120, "Pozwala wysyłać do 3 równoczesnych ekspedycji w świat"),
            SectFacility(FacilityType.DIPLOMACY_HALL, 1, 150, "Otwiera szlaki handlowe i poselstwa do sąsiednich sekt")
        )
    }

    fun getInitialRegions(): List<WorldRegion> {
        return listOf(
            WorldRegion(
                id = "reg_1",
                name = "Bambusowy Las Szmaragdowej Mgły",
                biome = BiomeType.BAMBOO_MIST,
                hazard = EnvironmentalHazard.NONE,
                dangerLevel = 1,
                explorationProgress = 60,
                mineralResource = "Duchowy Nefryt",
                herbResource = "Trawa Wiecznej Wiosny",
                bossBeast = MythicalBeast("Zielony Wąż Qi", "Strażnik Gajów", 450, 450, 42, 20, "Skóra Duchowego Węża", 120, "🐍")
            ),
            WorldRegion(
                id = "reg_2",
                name = "Szczyt Lodowego Smoka Kunlun",
                biome = BiomeType.KUNLUN_PEAK,
                hazard = EnvironmentalHazard.EXTREME_FROST,
                dangerLevel = 3,
                explorationProgress = 25,
                mineralResource = "Gwiezdne Żelazo",
                herbResource = "Śnieżny Lotos Oświecenia",
                bossBeast = MythicalBeast("Lodowy Qilin", "Władca Zmarzliny", 1200, 1200, 85, 45, "Róg Mroźnego Qilina", 350, "🦄")
            ),
            WorldRegion(
                id = "reg_3",
                name = "Bagno Tysiąca Trucizn",
                biome = BiomeType.VENOM_SWAMP,
                hazard = EnvironmentalHazard.TOXIC_MIASMA,
                dangerLevel = 4,
                explorationProgress = 10,
                mineralResource = "Czarny Obsydian Miazmatu",
                herbResource = "Fioletowy Korzeń Skorpiona",
                bossBeast = MythicalBeast("Dziewięciogłowy Bazyliszek", "Tyran Bagien", 1900, 1900, 115, 60, "Kieł Trującego Monstrum", 500, "🦎")
            ),
            WorldRegion(
                id = "reg_4",
                name = "Piekielna Rozpadlina Lawy",
                biome = BiomeType.LAVA_CHASM,
                hazard = EnvironmentalHazard.SCORCHING_MAGMA,
                dangerLevel = 6,
                explorationProgress = 5,
                mineralResource = "Kryształ Geocentrycznego Ognia",
                herbResource = "Płomienny Żeń-szeń Smoka",
                bossBeast = MythicalBeast("Płomienny Feniks Magmy", "Ptak Wiecznego Żaru", 3200, 3200, 175, 90, "Pióro Nieśmiertelnego Ognia", 900, "🪶")
            ),
            WorldRegion(
                id = "reg_5",
                name = "Pustynia Złotych Piasków i Wirów",
                biome = BiomeType.SPIRIT_DESERT,
                hazard = EnvironmentalHazard.SPIRITUAL_TEMPEST,
                dangerLevel = 7,
                explorationProgress = 0,
                mineralResource = "Słoneczny Złoty Piasek",
                herbResource = "Kaktus Duszy Pustki",
                bossBeast = MythicalBeast("Złoty Skorpion Burz", "Strażnik Grobowca", 4500, 4500, 220, 120, "Żądło Pustynnego Władcy", 1400, "🦂")
            ),
            WorldRegion(
                id = "reg_6",
                name = "Głębia Smoczej Otchłani",
                biome = BiomeType.DRAGON_ABYSS,
                hazard = EnvironmentalHazard.SPIRITUAL_TEMPEST,
                dangerLevel = 9,
                explorationProgress = 0,
                mineralResource = "Starożytna Ruda Pustki",
                herbResource = "Kwiat Niebiańskiego Zrozumienia",
                bossBeast = MythicalBeast("Starożytny Błękitny Smok", "Władca Otchłani Pustki", 8000, 8000, 380, 210, "Boska Smocza Łuska", 3000, "🐉")
            )
        )
    }

    fun getInitialRivalSects(): List<RivalSect> {
        return listOf(
            RivalSect(
                id = "sect_wudang",
                name = "Sekta Czystego Miecza Wudang",
                leaderName = "Mistrz Zhang Sanfeng",
                alignment = "Ortodoksyjna / Sprawiedliwa",
                powerRank = 1,
                discipleCount = 420,
                relations = 45,
                status = DiplomaticStatus.FRIENDLY,
                specialty = "Niebiańska Szkoła Miecza i Etyka Tao",
                activeTrade = TradeOffer("tr_1", "Wymiana: 200 Kamieni Ducha za 5x Śnieżny Lotos", 200, "Śnieżny Lotos Oświecenia"),
                policy = SectPolicy.RIGHTEOUS_DAO,
                cultivationBuffName = "Niebiańska Intencja Miecza",
                cultivationBuffDescription = "+25% Obrażeń mieczem i szansa na Oświecenie Dao",
                cultivationBuffBonusPercent = 25,
                controlledTerritoryIds = listOf("terr_4")
            ),
            RivalSect(
                id = "sect_dragon",
                name = "Zakon Płonącego Smoka",
                leaderName = "Patriarcha Yan Tian",
                alignment = "Neutralna / Waleczna",
                powerRank = 2,
                discipleCount = 310,
                relations = 10,
                status = DiplomaticStatus.NEUTRAL,
                specialty = "Ogniste Techniki Pięściarza i Kucie",
                activeTrade = TradeOffer("tr_2", "Wymiana: 300 Kamieni Ducha za 3x Kryształ Geocentryczny", 300, "Kryształ Geocentrycznego Ognia"),
                policy = SectPolicy.EXPANSIONIST,
                cultivationBuffName = "Szał Ognistego Smoka",
                cultivationBuffDescription = "+30% Siła ataku ogniem i zyski z kuźni",
                cultivationBuffBonusPercent = 30,
                controlledTerritoryIds = listOf("terr_3")
            ),
            RivalSect(
                id = "sect_herbs",
                name = "Dolina Stu Świętych Ziół",
                leaderName = "Uzdrowicielka Shen Nong",
                alignment = "Ortodoksyjna / Pacyfistyczna",
                powerRank = 4,
                discipleCount = 180,
                relations = 65,
                status = DiplomaticStatus.ALLIANCE,
                specialty = "Zaawansowana Alchemia i Regeneracja Meridianów",
                activeTrade = TradeOffer("tr_3", "Wymiana: 150 Kamieni Ducha za 3x Pigułka Przełomu Qi", 150, "Pigułka Przełomu Qi"),
                policy = SectPolicy.ISOLATIONIST,
                cultivationBuffName = "Zrozumienie Czystości Ziół",
                cultivationBuffDescription = "+35% Zysk Qi oraz +20% skuteczność warzenia pigułek",
                cultivationBuffBonusPercent = 35,
                controlledTerritoryIds = listOf("terr_1")
            ),
            RivalSect(
                id = "sect_shadow",
                name = "Klan Krwawego Cienia",
                leaderName = "Krwawy Asura Wu",
                alignment = "Demoniczna / Bezwzględna",
                powerRank = 3,
                discipleCount = 280,
                relations = -35,
                status = DiplomaticStatus.COLD_WAR,
                specialty = "Skrytobójstwo, Zakazane Trucizny i Kradzież Ducha",
                activeTrade = null,
                policy = SectPolicy.DEMONIC_RAIDERS,
                cultivationBuffName = "Mroczna Kradzież Meridianów",
                cultivationBuffDescription = "+40% Przebicie pancerza i wyssanie Qi przeciwnika",
                cultivationBuffBonusPercent = 40,
                controlledTerritoryIds = listOf("terr_5")
            ),
            RivalSect(
                id = "sect_guild",
                name = "Związek Kupiecki Jedwabnego Szlaku",
                leaderName = "Patriarcha Złoty Jin",
                alignment = "Kupiecka",
                powerRank = 5,
                discipleCount = 220,
                relations = 30,
                status = DiplomaticStatus.NEUTRAL,
                specialty = "Handel Międzysektorowy i Rzadkie Artefakty",
                activeTrade = TradeOffer("tr_4", "Wymiana: 500 Kamieni Ducha za Starożytny Zwój Techniki", 500, "Starożytny Zwój Techniki"),
                policy = SectPolicy.MERCHANT_CONSORTIUM,
                cultivationBuffName = "Obfitość Złotego Dao",
                cultivationBuffDescription = "+50% Przychód Kamieni Ducha ze wszystkich źródeł",
                cultivationBuffBonusPercent = 20,
                controlledTerritoryIds = listOf("terr_2")
            ),
            RivalSect(
                id = "sect_shaolin",
                name = "Święty Klasztor Złotej Wadżry",
                leaderName = "Opat Bodhidharma Xuan",
                alignment = "Ortodoksyjna / Ascetyczna",
                powerRank = 6,
                discipleCount = 500,
                relations = 50,
                status = DiplomaticStatus.FRIENDLY,
                specialty = "Złote Ciało Wadżry i Odporność Duchowa",
                activeTrade = TradeOffer("tr_5", "Wymiana: 250 Kamieni Ducha za 4x Balsam Żelaznej Kości", 250, "Balsam Żelaznej Kości Asury"),
                policy = SectPolicy.RIGHTEOUS_DAO,
                cultivationBuffName = "Niezniszczalna Wadżra",
                cultivationBuffDescription = "+45% Pancerz i odporność na tribulacje",
                cultivationBuffBonusPercent = 45,
                controlledTerritoryIds = listOf("terr_6")
            ),
            RivalSect(
                id = "sect_imperial",
                name = "Cesarski Trybunał Smoczych Niebios",
                leaderName = "Książę Zhao Yulong",
                alignment = "Cesarska / Polityczna",
                powerRank = 7,
                discipleCount = 650,
                relations = 20,
                status = DiplomaticStatus.NEUTRAL,
                specialty = "Cesarski Autorytet, Zaciąg Armii i Skarbiec",
                activeTrade = TradeOffer("tr_6", "Wymiana: 400 Kamieni Ducha za 5x Gwiezdne Żelazo", 400, "Gwiezdne Żelazo"),
                policy = SectPolicy.EXPANSIONIST,
                cultivationBuffName = "Cesarski Edykt Niebios",
                cultivationBuffDescription = "+35% Zaciąg uczniów z rodów arystokratycznych",
                cultivationBuffBonusPercent = 35,
                controlledTerritoryIds = listOf("terr_7")
            ),
            RivalSect(
                id = "sect_blades",
                name = "Dolina Niewidzialnych Ostrzy",
                leaderName = "Pani Cienia Ye Hua",
                alignment = "Neutralna / Pustelnicza",
                powerRank = 8,
                discipleCount = 140,
                relations = 15,
                status = DiplomaticStatus.NEUTRAL,
                specialty = "Szybkie Szable i Kroki Pustki",
                activeTrade = TradeOffer("tr_7", "Wymiana: 350 Kamieni Ducha za 3x Kwiat Niebiański", 350, "Kwiat Niebiańskiego Zrozumienia"),
                policy = SectPolicy.ISOLATIONIST,
                cultivationBuffName = "Niewidzialne Cięcie Dao",
                cultivationBuffDescription = "+40% Przebicie pancerza i uniki",
                cultivationBuffBonusPercent = 40,
                controlledTerritoryIds = listOf("terr_8")
            ),
            RivalSect(
                id = "sect_astral",
                name = "Niebiański Pawilon Gwiezdnego Ducha",
                leaderName = "Astrolog Chenxing",
                alignment = "Ortodoksyjna / Badawcza",
                powerRank = 9,
                discipleCount = 200,
                relations = 40,
                status = DiplomaticStatus.FRIENDLY,
                specialty = "Astrologia Dao, Mapy Kosmosu i Ołtarze",
                activeTrade = TradeOffer("tr_8", "Wymiana: 600 Kamieni Ducha za 2x Ruda Pustki", 600, "Starożytna Ruda Pustki"),
                policy = SectPolicy.RIGHTEOUS_DAO,
                cultivationBuffName = "Rezonans Kosmicznych Gwiazd",
                cultivationBuffDescription = "+50% Zrozumienie Dao i szansa na Oświecenie",
                cultivationBuffBonusPercent = 50,
                controlledTerritoryIds = listOf("terr_9")
            ),
            RivalSect(
                id = "sect_nether",
                name = "Kult Krwawych Kości Piekieł",
                leaderName = "Arcydemon Gui Luo",
                alignment = "Demoniczna / Krwiożercza",
                powerRank = 10,
                discipleCount = 380,
                relations = -60,
                status = DiplomaticStatus.TOTAL_WAR,
                specialty = "Nekromancja Qi, Wyssanie Dusz i Przekleństwa",
                activeTrade = null,
                policy = SectPolicy.DEMONIC_RAIDERS,
                cultivationBuffName = "Klątwa Krwawego Długu",
                cultivationBuffDescription = "+50% Kradzież życia w walce ze śmiertelnikami",
                cultivationBuffBonusPercent = 50,
                controlledTerritoryIds = listOf("terr_10")
            )
        )
    }

    fun getInitialSectBeasts(): List<SectDivineBeast> {
        return listOf(
            SectDivineBeast(
                id = "beast_qilin",
                name = "Szmaragdowy Qilin Ziemi",
                title = "Strażnik Świętej Góry Sekty",
                emoji = "🐉",
                element = "Ziemia / Czyste Qi",
                tier = 2,
                intimacy = 45,
                currentEnergy = 1500,
                maxEnergy = 1500,
                passiveSectBuff = "+35% Zysk Qi dla wszystkich medytujących w sekcie",
                combatSkillName = "Niebiański Ryk Qilina",
                combatSkillDesc = "Paraliżuje wrogów na 2 tury i leczy 350 HP wszystkim uczniom w walce",
                favoriteHerb = "Trawa Wiecznej Wiosny"
            ),
            SectDivineBeast(
                id = "beast_phoenix",
                name = "Płomienny Karmazynowy Feniks",
                title = "Bóstwo Wiecznego Żaru",
                emoji = "🦅",
                element = "Płomień Czystego Yang",
                tier = 1,
                intimacy = 30,
                currentEnergy = 1000,
                maxEnergy = 1000,
                passiveSectBuff = "+40% Skuteczność Kociołka Alchemii i Kucia w Boskiej Kuźni",
                combatSkillName = "Spopielenie Dziewięciu Niebios",
                combatSkillDesc = "Zadaje 650 obrażeń od ognia i spala 20% pancerza wroga",
                favoriteHerb = "Kryształ Geocentrycznego Ognia"
            ),
            SectDivineBeast(
                id = "beast_tiger",
                name = "Biały Tygrys Gwiezdnego Pioruna",
                title = "Władca Bitew i Wojny",
                emoji = "🐯",
                element = "Błyskawica Niebios",
                tier = 1,
                intimacy = 25,
                currentEnergy = 1100,
                maxEnergy = 1100,
                passiveSectBuff = "+30% Siła ataku uczniów w wyprawach i najazdach",
                combatSkillName = "Cięcie Gwiezdnego Pioruna",
                combatSkillDesc = "Przebija 100% obrony przeciwnika krytycznym cięciem łapy",
                favoriteHerb = "Gwiezdne Żelazo"
            ),
            SectDivineBeast(
                id = "beast_turtle",
                name = "Czarny Żółw Xuanwu",
                title = "Pradawny Bastion Otchłani",
                emoji = "🐢",
                element = "Woda i Cień",
                tier = 1,
                intimacy = 35,
                currentEnergy = 2000,
                maxEnergy = 2000,
                passiveSectBuff = "+45% Pancerz sekty i odporność na wrogie najazdy",
                combatSkillName = "Tarcza Niezniszczalnego Pancerza",
                combatSkillDesc = "Tworzy barierę absorbującą 1200 obrażeń",
                favoriteHerb = "Śnieżny Lotos Oświecenia"
            ),
            SectDivineBeast(
                id = "beast_dragon",
                name = "Pradawny Błękitny Smok Qinglong",
                title = "Władca Wiatru i Źródło Życia",
                emoji = "🐲",
                element = "Święte Drewno i Życie",
                tier = 2,
                intimacy = 40,
                currentEnergy = 2500,
                maxEnergy = 2500,
                passiveSectBuff = "+50% Regeneracja HP wszystkich uczniów i odporność na starzenie",
                combatSkillName = "Smoczy Oddech Wieczności",
                combatSkillDesc = "Zadaje 900 obrażeń obszarowych i w pełni leczy drużynę",
                favoriteHerb = "Kwiat Niebiańskiego Zrozumienia"
            ),
            SectDivineBeast(
                id = "beast_fox",
                name = "Dziewięcioogoniasty Niebiański Lis",
                title = "Królowa Iluzji i Mistycznych Dusz",
                emoji = "🦊",
                element = "Cień i Duch",
                tier = 2,
                intimacy = 35,
                currentEnergy = 1800,
                maxEnergy = 1800,
                passiveSectBuff = "+45% Zyski z misji i uniki adeptów sekty",
                combatSkillName = "Hipnoza Dziewięciu Ogonów",
                combatSkillDesc = "Zmusza wrogów do atakowania samych siebie przez 2 tury",
                favoriteHerb = "Fioletowy Korzeń Skorpiona"
            ),
            SectDivineBeast(
                id = "beast_crow",
                name = "Trójnożny Słoneczny Kruk",
                title = "Zwiastun Pradawnego Światła",
                emoji = "☀️",
                element = "Słońce i Ogień",
                tier = 2,
                intimacy = 30,
                currentEnergy = 2200,
                maxEnergy = 2200,
                passiveSectBuff = "+60% Moc wszystkich Płomieni Geocentrycznych",
                combatSkillName = "Słoneczna Burza Światła",
                combatSkillDesc = "Spala pole bitwy oślepiając wszystkich wrogów i zadając 1100 dmg",
                favoriteHerb = "Kryształ Geocentrycznego Ognia"
            ),
            SectDivineBeast(
                id = "beast_kunpeng",
                name = "Kosmiczny Wieloryb Kunpeng",
                title = "Władca Nieskończonej Przestrzeni",
                emoji = "🐋",
                element = "Pustka i Bezkres",
                tier = 3,
                intimacy = 50,
                currentEnergy = 3500,
                maxEnergy = 3500,
                passiveSectBuff = "+75% Prędkość podróży wypraw i +50% udźwig karawan",
                combatSkillName = "Pożarcie Czasoprzestrzeni",
                combatSkillDesc = "Połyka ataki wrogów odsyłając energię z powrotem do nadawcy",
                favoriteHerb = "Starożytna Ruda Pustki"
            )
        )
    }

    fun getInitialTerritories(): List<TerritoryNode> {
        return listOf(
            TerritoryNode(
                id = "terr_1",
                name = "Dolina Stu Świętych Ziół",
                regionType = "Dolina Alchemiczna",
                controllingSectId = "player",
                controllingSectName = "Sekta Niebiańskiego Smoka",
                resourceType = "Zioła Alchemiczne",
                yieldAmountPerCycle = 4,
                garrisonDefensePower = 350
            ),
            TerritoryNode(
                id = "terr_2",
                name = "Żyła Gwiezdnego Żelaza",
                regionType = "Wzgórza Minerałów",
                controllingSectId = "player",
                controllingSectName = "Sekta Niebiańskiego Smoka",
                resourceType = "Gwiezdne Minerały",
                yieldAmountPerCycle = 3,
                garrisonDefensePower = 400
            ),
            TerritoryNode(
                id = "terr_3",
                name = "Krater Płonącej Lawy",
                regionType = "Geocentryczny Wulkan",
                controllingSectId = "sect_dragon",
                controllingSectName = "Zakon Płonącego Smoka",
                resourceType = "Kryształy Płomienia",
                yieldAmountPerCycle = 2,
                garrisonDefensePower = 650
            ),
            TerritoryNode(
                id = "terr_4",
                name = "Szczyt Wiecznego Mrozu Kunlun",
                regionType = "Święty Szczyt Dao",
                controllingSectId = "sect_wudang",
                controllingSectName = "Sekta Czystego Miecza Wudang",
                resourceType = "Kamienie Ducha",
                yieldAmountPerCycle = 45,
                garrisonDefensePower = 800
            ),
            TerritoryNode(
                id = "terr_5",
                name = "Cmentarzysko Krwawego Miazmatu",
                regionType = "Mroczne Mokradła",
                controllingSectId = "sect_shadow",
                controllingSectName = "Klan Krwawego Cienia",
                resourceType = "Toksyczne Esencje",
                yieldAmountPerCycle = 5,
                garrisonDefensePower = 700
            ),
            TerritoryNode(
                id = "terr_6",
                name = "Kopalnia Głębinowego Nefrytu",
                regionType = "Nefrytowe Groty",
                controllingSectId = "sect_shaolin",
                controllingSectName = "Święty Klasztor Złotej Wadżry",
                resourceType = "Duchowy Nefryt",
                yieldAmountPerCycle = 5,
                garrisonDefensePower = 850
            ),
            TerritoryNode(
                id = "terr_7",
                name = "Przełęcz Smoczego Gardła",
                regionType = "Cesarska Twierdza",
                controllingSectId = "sect_imperial",
                controllingSectName = "Cesarski Trybunał Smoczych Niebios",
                resourceType = "Gwiezdne Minerały",
                yieldAmountPerCycle = 4,
                garrisonDefensePower = 950
            ),
            TerritoryNode(
                id = "terr_8",
                name = "Święte Źródło Czystego Yang",
                regionType = "Wodospady Światła",
                controllingSectId = "sect_blades",
                controllingSectName = "Dolina Niewidzialnych Ostrzy",
                resourceType = "Kamienie Ducha",
                yieldAmountPerCycle = 70,
                garrisonDefensePower = 750
            ),
            TerritoryNode(
                id = "terr_9",
                name = "Ogród Niebiańskich Brzoskwiń",
                regionType = "Mistycyzm Dao",
                controllingSectId = "sect_astral",
                controllingSectName = "Niebiański Pawilon Gwiezdnego Ducha",
                resourceType = "Zioła Alchemiczne",
                yieldAmountPerCycle = 6,
                garrisonDefensePower = 900
            ),
            TerritoryNode(
                id = "terr_10",
                name = "Ołtarz Gwiezdnej Przestrzeni",
                regionType = "Szczelina Kosmosu",
                controllingSectId = "sect_nether",
                controllingSectName = "Kult Krwawych Kości Piekieł",
                resourceType = "Ruda Pustki",
                yieldAmountPerCycle = 3,
                garrisonDefensePower = 1100
            )
        )
    }

    fun getAlchemyRecipes(): List<AlchemyRecipe> {
        return listOf(
            // TIER 1 - Kondensacja Qi (Wymagana Sala Alchemii Poz. 1)
            AlchemyRecipe(
                id = "rec_t1_1",
                pillName = "Pigułka Przełomu Qi",
                requiredHerbName = "Trawa Wiecznej Wiosny",
                requiredHerbCount = 3,
                requiredSpiritStones = 50,
                effectDescription = "Zwiększa szansę na udany przełom kondensacji Qi o +35%.",
                breakthroughBonusPercent = 35,
                tier = 1,
                tierName = "Stopień 1: Oczyszczenie Qi",
                requiredAlchemyChamberLevel = 1
            ),
            AlchemyRecipe(
                id = "rec_t1_2",
                pillName = "Eliksir Witalności Meridianów",
                requiredHerbName = "Trawa Wiecznej Wiosny",
                requiredHerbCount = 2,
                requiredSpiritStones = 40,
                effectDescription = "Oczyszcza podstawowe meridiany, natychmiast leczy 350 HP.",
                hpRestore = 350,
                tier = 1,
                tierName = "Stopień 1: Oczyszczenie Qi",
                requiredAlchemyChamberLevel = 1
            ),
            AlchemyRecipe(
                id = "rec_t1_3",
                pillName = "Pigułka Odporności na Miazmat",
                requiredHerbName = "Fioletowy Korzeń Skorpiona",
                requiredHerbCount = 2,
                requiredSpiritStones = 60,
                effectDescription = "Zapewnia odporność na trujące opary bagien i toksyny.",
                breakthroughBonusPercent = 15,
                tier = 1,
                tierName = "Stopień 1: Oczyszczenie Qi",
                requiredAlchemyChamberLevel = 1
            ),

            // TIER 2 - Ustanowienie Fundamentu (Wymagana Sala Alchemii Poz. 2)
            AlchemyRecipe(
                id = "rec_t2_1",
                pillName = "Pigułka Fundamentu Dziewięciu Niebios",
                requiredHerbName = "Śnieżny Lotos Oświecenia",
                requiredHerbCount = 3,
                requiredSpiritStones = 120,
                effectDescription = "Kluczowa do skrystalizowania fundamentu Dao. +45% szansy na przełom.",
                breakthroughBonusPercent = 45,
                tier = 2,
                tierName = "Stopień 2: Ustanowienie Fundamentu",
                requiredAlchemyChamberLevel = 2,
                secondaryHerbName = "Trawa Wiecznej Wiosny",
                secondaryHerbCount = 2
            ),
            AlchemyRecipe(
                id = "rec_t2_2",
                pillName = "Eliksir Oczyszczenia Meridianów",
                requiredHerbName = "Śnieżny Lotos Oświecenia",
                requiredHerbCount = 2,
                requiredSpiritStones = 100,
                effectDescription = "Głęboko leczy meridiany i przywraca 900 HP.",
                hpRestore = 900,
                tier = 2,
                tierName = "Stopień 2: Ustanowienie Fundamentu",
                requiredAlchemyChamberLevel = 2,
                secondaryHerbName = "Duchowy Nefryt",
                secondaryHerbCount = 1
            ),
            AlchemyRecipe(
                id = "rec_t2_3",
                pillName = "Balsam Żelaznej Kości Asury",
                requiredHerbName = "Gwiezdne Żelazo",
                requiredHerbCount = 2,
                requiredSpiritStones = 140,
                effectDescription = "Nasyca ciało odpornością na cięcia. +40 Obrona na stałe.",
                breakthroughBonusPercent = 20,
                tier = 2,
                tierName = "Stopień 2: Ustanowienie Fundamentu",
                requiredAlchemyChamberLevel = 2,
                secondaryHerbName = "Fioletowy Korzeń Skorpiona",
                secondaryHerbCount = 2
            ),

            // TIER 3 - Złoty Rdzeń (Wymagana Sala Alchemii Poz. 3)
            AlchemyRecipe(
                id = "rec_t3_1",
                pillName = "Boska Pigułka Złotego Rdzenia",
                requiredHerbName = "Płomienny Żeń-szeń Smoka",
                requiredHerbCount = 3,
                requiredSpiritStones = 250,
                effectDescription = "Pozwala uformować nieskazitelny Złoty Rdzeń. +55% do przełomu.",
                breakthroughBonusPercent = 55,
                tier = 3,
                tierName = "Stopień 3: Formowanie Złotego Rdzenia",
                requiredAlchemyChamberLevel = 3,
                secondaryHerbName = "Śnieżny Lotos Oświecenia",
                secondaryHerbCount = 2
            ),
            AlchemyRecipe(
                id = "rec_t3_2",
                pillName = "Nektar Przebudzenia Świadomości Boskiej",
                requiredHerbName = "Płomienny Żeń-szeń Smoka",
                requiredHerbCount = 2,
                requiredSpiritStones = 300,
                effectDescription = "Otwiera trzecie oko Dao. Daje natychmiast +2 Punkty Dao i 2000 HP.",
                hpRestore = 2000,
                daoPointsBonus = 2,
                tier = 3,
                tierName = "Stopień 3: Formowanie Złotego Rdzenia",
                requiredAlchemyChamberLevel = 3,
                secondaryHerbName = "Duchowy Nefryt",
                secondaryHerbCount = 2
            ),
            AlchemyRecipe(
                id = "rec_t3_3",
                pillName = "Pigułka Płonącego Serca Feniksa",
                requiredHerbName = "Kryształ Geocentrycznego Ognia",
                requiredHerbCount = 2,
                requiredSpiritStones = 320,
                effectDescription = "Nasyca ataki adepta piekielnym płomieniem. +55 Atak.",
                breakthroughBonusPercent = 25,
                tier = 3,
                tierName = "Stopień 3: Formowanie Złotego Rdzenia",
                requiredAlchemyChamberLevel = 3,
                secondaryHerbName = "Płomienny Żeń-szeń Smoka",
                secondaryHerbCount = 2
            ),

            // TIER 4 - Narodzenie Duszy / Nascent Soul (Wymagana Sala Alchemii Poz. 4)
            AlchemyRecipe(
                id = "rec_t4_1",
                pillName = "Pigułka Narodzin Duszy Nascent",
                requiredHerbName = "Kwiat Niebiańskiego Zrozumienia",
                requiredHerbCount = 3,
                requiredSpiritStones = 500,
                effectDescription = "Uwalnia miniaturową duszę ze Złotego Rdzenia. +65% szansy na przełom.",
                breakthroughBonusPercent = 65,
                tier = 4,
                tierName = "Stopień 4: Narodzenie Duszy (Nascent)",
                requiredAlchemyChamberLevel = 4,
                secondaryHerbName = "Kryształ Geocentrycznego Ognia",
                secondaryHerbCount = 2
            ),
            AlchemyRecipe(
                id = "rec_t4_2",
                pillName = "Eliksir Życia Feniksa Dziewięciu Niebios",
                requiredHerbName = "Kwiat Niebiańskiego Zrozumienia",
                requiredHerbCount = 3,
                requiredSpiritStones = 550,
                effectDescription = "Przywraca 4500 HP i regeneruje całkowicie spalone meridiany.",
                hpRestore = 4500,
                tier = 4,
                tierName = "Stopień 4: Narodzenie Duszy (Nascent)",
                requiredAlchemyChamberLevel = 4,
                secondaryHerbName = "Śnieżny Lotos Oświecenia",
                secondaryHerbCount = 3
            ),

            // TIER 5 - Nieśmiertelne Wniebowstąpienie (Wymagana Sala Alchemii Poz. 5)
            AlchemyRecipe(
                id = "rec_t5_1",
                pillName = "Niebiańska Pigułka Wniebowstąpienia",
                requiredHerbName = "Kwiat Niebiańskiego Zrozumienia",
                requiredHerbCount = 4,
                requiredSpiritStones = 1000,
                effectDescription = "Niszczy ziemskie kajdany. +85% szansy na wniebowstąpienie i +5 Punktów Dao.",
                breakthroughBonusPercent = 85,
                daoPointsBonus = 5,
                tier = 5,
                tierName = "Stopień 5: Nieśmiertelne Dao",
                requiredAlchemyChamberLevel = 5,
                secondaryHerbName = "Starożytna Ruda Pustki",
                secondaryHerbCount = 2
            ),
            AlchemyRecipe(
                id = "rec_t5_2",
                pillName = "Eliksir Wiecznego Dao Chaosu",
                requiredHerbName = "Starożytna Ruda Pustki",
                requiredHerbCount = 3,
                requiredSpiritStones = 1200,
                effectDescription = "Czysta esencja pierwotnego wszechświata. Przywraca 9999 HP i daje nieśmiertelność.",
                hpRestore = 9999,
                tier = 5,
                tierName = "Stopień 5: Nieśmiertelne Dao",
                requiredAlchemyChamberLevel = 5,
                secondaryHerbName = "Płomienny Żeń-szeń Smoka",
                secondaryHerbCount = 4
            ),
            AlchemyRecipe(
                id = "rec_t1_4",
                pillName = "Pigułka Klarowności Dantian",
                requiredHerbName = "Trawa Wiecznej Wiosny",
                requiredHerbCount = 2,
                requiredSpiritStones = 30,
                effectDescription = "Klaruje dolny Dantian. +20% zysk Qi dla medytujących.",
                breakthroughBonusPercent = 10,
                tier = 1,
                tierName = "Stopień 1: Oczyszczenie Qi",
                requiredAlchemyChamberLevel = 1,
                secondaryHerbName = "Duchowy Nefryt",
                secondaryHerbCount = 1
            ),
            AlchemyRecipe(
                id = "rec_t2_4",
                pillName = "Eliksir Młodości i Żywotności",
                requiredHerbName = "Fioletowy Korzeń Skorpiona",
                requiredHerbCount = 2,
                requiredSpiritStones = 110,
                effectDescription = "Regeneruje meridiany. Przywraca 650 HP i dodaje +15 Obrona.",
                hpRestore = 650,
                tier = 2,
                tierName = "Stopień 2: Ustanowienie Fundamentu",
                requiredAlchemyChamberLevel = 2,
                secondaryHerbName = "Śnieżny Lotos Oświecenia",
                secondaryHerbCount = 1
            ),
            AlchemyRecipe(
                id = "rec_t3_4",
                pillName = "Pigułka Pancerza Złotego Dzwonu",
                requiredHerbName = "Gwiezdne Żelazo",
                requiredHerbCount = 3,
                requiredSpiritStones = 280,
                effectDescription = "Nasyca krew esencją żelaza. Dodaje na stałe +60 Obrony.",
                breakthroughBonusPercent = 20,
                tier = 3,
                tierName = "Stopień 3: Formowanie Złotego Rdzenia",
                requiredAlchemyChamberLevel = 3,
                secondaryHerbName = "Płomienny Żeń-szeń Smoka",
                secondaryHerbCount = 2
            ),
            AlchemyRecipe(
                id = "rec_t4_3",
                pillName = "Pigułka Siedmiu Oświeceń Duszy",
                requiredHerbName = "Kwiat Niebiańskiego Zrozumienia",
                requiredHerbCount = 2,
                requiredSpiritStones = 480,
                effectDescription = "Otwiera percepcję kosmosu. Daje +3 Punkty Dao i +70 Atak.",
                daoPointsBonus = 3,
                tier = 4,
                tierName = "Stopień 4: Narodzenie Duszy (Nascent)",
                requiredAlchemyChamberLevel = 4,
                secondaryHerbName = "Kryształ Geocentrycznego Ognia",
                secondaryHerbCount = 2
            ),
            AlchemyRecipe(
                id = "rec_t5_3",
                pillName = "Pigułka Przekroczenia Tribulacji Pustki",
                requiredHerbName = "Starożytna Ruda Pustki",
                requiredHerbCount = 3,
                requiredSpiritStones = 1100,
                effectDescription = "Chroni przed piorunami niebiańskiej kary. +90% szansy na przełom.",
                breakthroughBonusPercent = 90,
                tier = 5,
                tierName = "Stopień 5: Nieśmiertelne Dao",
                requiredAlchemyChamberLevel = 5,
                secondaryHerbName = "Kwiat Niebiańskiego Zrozumienia",
                secondaryHerbCount = 3
            )
        )
    }

    fun generateProceduralIngredients(count: Int = 50): List<ResourceItem> {
        val ages = listOf(100, 300, 500, 800, 1000, 3000, 5000, 8000, 10000)
        val elements = listOf("Czysty Yang", "Głębokie Yin", "Płomień", "Błyskawica", "Mróz Kunlun", "Pustka Przestrzeni", "Ziemia", "Święte Światło", "Mroczna Krew")
        val herbBases = listOf("Lotos Oświecenia", "Żeń-szeń Smoka", "Korzeń Skorpiona", "Trawa Wiecznej Wiosny", "Grzyb Lingzhi", "Pąk Nefrytowej Orchidei", "Liść Purpurowej Mgły", "Ziele Przełomu Dantian", "Niebiańska Brzoskwinia Wieczności", "Złoty Pąk Słońca")
        val mineralBases = listOf("Duchowy Nefryt", "Gwiezdne Żelazo", "Kryształ Geocentrycznego Ognia", "Kruszec Czarnego Złota", "Obsydian Błyskawic", "Ruda Pustki Kosmicznej", "Meteoryt Smoczego Serca", "Kamień Duszy Przodków")
        val essenceBases = listOf("Esencja Płomiennego Feniksa", "Jądro Szmaragdowego Qilina", "Żółć Lodowego Węża", "Kieł Białego Tygrysa", "Łuska Czarnego Żółwia", "Krew Błękitnego Smoka", "Pióro Słonecznego Kruka")

        return List(count) { idx ->
            val cat = when (idx % 3) {
                0 -> ResourceCategory.HERB
                1 -> ResourceCategory.MINERAL
                else -> ResourceCategory.ESSENCE
            }
            val age = ages[idx % ages.size]
            val elem = elements[idx % elements.size]
            val tier = (idx % 5) + 1
            val baseName = when (cat) {
                ResourceCategory.HERB -> herbBases[idx % herbBases.size]
                ResourceCategory.MINERAL -> mineralBases[idx % mineralBases.size]
                ResourceCategory.ESSENCE -> essenceBases[idx % essenceBases.size]
                else -> "Składnik Dao"
            }
            val rarity = when (tier) {
                1 -> "Zwykły"
                2 -> "Rzadki"
                3 -> "Mistyczny"
                4 -> "Niebiański"
                else -> "Boski"
            }
            val name = "${age}-letni $baseName"
            ResourceItem(
                id = "proc_res_${idx + 1}",
                name = name,
                category = cat,
                rarity = rarity,
                count = Random.nextInt(1, 8),
                description = "Mistycyzm: $rarity ($elem). Wiek: $age lat. Odpowiedni dla receptur Rzędu $tier.",
                tier = tier,
                spiritualAgeYears = age,
                elementAffinity = elem,
                potency = tier * 30
            )
        }
    }

    fun getForgingRecipes(): List<ForgingRecipe> {
        return listOf(
            ForgingRecipe(
                id = "forge_1",
                artifactName = "Latający Miecz Szmaragdowego Wiatru",
                requiredMineralName = "Duchowy Nefryt",
                requiredMineralCount = 4,
                requiredSpiritStones = 120,
                artifactResult = Artifact("art_1", "Latający Miecz Szmaragdowego Wiatru", "Rzadki", 45, 10, "+15% szybkości ataku")
            ),
            ForgingRecipe(
                id = "forge_2",
                artifactName = "Pancerz z Gwiezdnego Żelaza",
                requiredMineralName = "Gwiezdne Żelazo",
                requiredMineralCount = 4,
                requiredSpiritStones = 180,
                artifactResult = Artifact("art_2", "Pancerz z Gwiezdnego Żelaza", "Niebiański", 15, 65, "-20% otrzymywanych obrażeń fizycznych")
            ),
            ForgingRecipe(
                id = "forge_3",
                artifactName = "Pierścień Piekielnego Płomienia",
                requiredMineralName = "Kryształ Geocentrycznego Ognia",
                requiredMineralCount = 3,
                requiredSpiritStones = 250,
                artifactResult = Artifact("art_3", "Pierścień Piekielnego Płomienia", "Boski", 75, 25, "Podpala przeciwników w każdej turze")
            ),
            ForgingRecipe(
                id = "forge_4",
                artifactName = "Dzwon Tłumienia Duszy Pustki",
                requiredMineralName = "Starożytna Ruda Pustki",
                requiredMineralCount = 3,
                requiredSpiritStones = 400,
                artifactResult = Artifact("art_4", "Dzwon Tłumienia Duszy Pustki", "Nieśmiertelny", 110, 80, "Oszałamia wrogów przy ciosie krytycznym")
            ),
            ForgingRecipe(
                id = "forge_5",
                artifactName = "Włócznia Przebijająca Smocze Niebiosa",
                requiredMineralName = "Gwiezdne Żelazo",
                requiredMineralCount = 5,
                requiredSpiritStones = 320,
                artifactResult = Artifact("art_5", "Włócznia Przebijająca Smocze Niebiosa", "Niebiański", 90, 20, "+30% szansa na krytyczne pchnięcie")
            ),
            ForgingRecipe(
                id = "forge_6",
                artifactName = "Szata Niebiańskiego Pióra Feniksa",
                requiredMineralName = "Duchowy Nefryt",
                requiredMineralCount = 5,
                requiredSpiritStones = 350,
                artifactResult = Artifact("art_6", "Szata Niebiańskiego Pióra Feniksa", "Boski", 30, 95, "Leczy 300 HP na początku każdej tury walki")
            ),
            ForgingRecipe(
                id = "forge_7",
                artifactName = "Zwierciadło Ośmiu Trygramów Bagua",
                requiredMineralName = "Duchowy Nefryt",
                requiredMineralCount = 6,
                requiredSpiritStones = 420,
                artifactResult = Artifact("art_7", "Zwierciadło Ośmiu Trygramów Bagua", "Boski", 60, 85, "Odbija 25% czarów żywiołów przeciwnika")
            ),
            ForgingRecipe(
                id = "forge_8",
                artifactName = "Kocioł Dziewięciu Smoków Pierwotnego Chaosu",
                requiredMineralName = "Kryształ Geocentrycznego Ognia",
                requiredMineralCount = 5,
                requiredSpiritStones = 550,
                artifactResult = Artifact("art_8", "Kocioł Dziewięciu Smoków Pierwotnego Chaosu", "Nieśmiertelny", 130, 90, "Podwaja szansę na pigułki rzędu Boskiego")
            ),
            ForgingRecipe(
                id = "forge_9",
                artifactName = "Wachlarz Eterycznych Piór Kunpenga",
                requiredMineralName = "Starożytna Ruda Pustki",
                requiredMineralCount = 4,
                requiredSpiritStones = 600,
                artifactResult = Artifact("art_9", "Wachlarz Eterycznych Piór Kunpenga", "Nieśmiertelny", 100, 70, "+45% do uników w walce")
            ),
            ForgingRecipe(
                id = "forge_10",
                artifactName = "Korona Cesarza Gwiazd Pustki",
                requiredMineralName = "Starożytna Ruda Pustki",
                requiredMineralCount = 5,
                requiredSpiritStones = 800,
                artifactResult = Artifact("art_10", "Korona Cesarza Gwiazd Pustki", "Wieczne Dao", 160, 130, "Całkowita niewrażliwość na obrażenia krytyczne")
            )
        )
    }

    fun getRandomEvents(): List<RandomEvent> {
        return listOf(
            RandomEvent(
                id = "ev_1",
                title = "Odkrycie Starożytnego Grobowca Dao",
                description = "W pobliżu granic sekty z ziemi wyłoniła się kamienna pagoda pokryta pradawnymi runami. Wyczuwalne są potężne wibracje energii duchowej.",
                choices = listOf(
                    RandomEventChoice("Wyślij najsilniejszych adeptów do zbadania wnętrza", "Uczniowie przełamali barierę grobowca i odnaleźli zapomniane skarby!", stoneChange = 350, repChange = 15, rewardItem = "Gwiezdne Żelazo"),
                    RandomEventChoice("Zabezpiecz wejście i odpraw rytuał ochronny", "Sekta zyskała szacunek okolicznych pustelników za roztropność.", stoneChange = 100, repChange = 25),
                    RandomEventChoice("Zaoferuj wejście sąsiednim sektom za opłatą", "Sojusznicy zapłacili sowicie za dostęp do starożytnej wiedzy.", stoneChange = 500, repChange = -10)
                )
            ),
            RandomEvent(
                id = "ev_2",
                title = "Wędrowny Uzdrowiciel z Gór Kunlun",
                description = "Do bram sekty przybył sędziwy mędrzec w poszarpanym płaszczu z koszem pełnym rzadkich ziół. Szuka schronienia na kilka nocy.",
                choices = listOf(
                    RandomEventChoice("Przyjmij mędrca z wszelkimi honorami w głównej sali", "Mędrzec w podzięce podarował sekcie drogocenne zioła i pobłogosławił kotły alchemiczne!", stoneChange = 0, repChange = 30, rewardItem = "Śnieżny Lotos Oświecenia"),
                    RandomEventChoice("Wymień się z nim zapasami minerałów", "Udało się dobić uczciwego targu na rzadkie komponenty.", stoneChange = -50, repChange = 5, rewardItem = "Trawa Wiecznej Wiosny"),
                    RandomEventChoice("Odpraw go ze względów bezpieczeństwa", "Mędrzec odszedł w milczeniu, znikając w porannej mgle.", stoneChange = 0, repChange = -5)
                )
            ),
            RandomEvent(
                id = "ev_3",
                title = "Nocny Deszcz Duchowych Meteorytów",
                description = "Niebo rozświetliły spadające gwiazdy nasycone esencją czystego niebiańskiego metalu. Fragmenty spadły w dolinie sekty.",
                choices = listOf(
                    RandomEventChoice("Zarządź nocną zbiórkę odłamków przez wszystkich uczniów", "Zebrano wyjątkowe minerały z kosmicznego pyłu!", stoneChange = 150, repChange = 10, rewardItem = "Kryształ Geocentrycznego Ognia"),
                    RandomEventChoice("Wykorzystaj kosmiczną energię do wspólnej medytacji", "Wszyscy uczniowie zrobili ogromny postęp w zrozumieniu praw wszechświata!", stoneChange = 0, repChange = 20)
                )
            ),
            RandomEvent(
                id = "ev_4",
                title = "Wygnany Geniusz Szuka Schronienia",
                description = "U stóp góry znaleziono ciężko rannego młodzieńca z rzadką Konstytucją Czystego Yang. Ucieka przed zemstą bezwzględnego klanu.",
                choices = listOf(
                    RandomEventChoice("Udziel mu azylu i włącz do sekty jako ucznia wewnętrznego", "Zyskałeś nieprzeciętnego geniusza, lecz wrogi klan rzuca groźby!", stoneChange = -100, repChange = 15),
                    RandomEventChoice("Opatrz jego rany, lecz odeślij poza granice sekty", "Młodzieniec podziękował za ratunek i odszedł własną ścieżką.", stoneChange = 0, repChange = 5)
                )
            )
        )
    }

    fun getMultiplayerRankings(): List<MultiplayerSectRanking> {
        return listOf(
            MultiplayerSectRanking(1, "Sekta Nieskończonego Dao", "Patriarcha Chen Bei", 2850000, "EU-Central (Frankfurt)", 14),
            MultiplayerSectRanking(2, "Zakon Złotego Smoka Kunlun", "Arcymistrz Li Feng", 2420000, "EU-West (London)", 18),
            MultiplayerSectRanking(3, "Twoja Sekta (Lider)", "Ty (Mistrz Sekty)", 1950000, "Lokalny Węzeł Realm", 8),
            MultiplayerSectRanking(4, "Świątynia Lodowego Feniksa", "Władczyni Bai Yue", 1820000, "EU-North (Stockholm)", 22),
            MultiplayerSectRanking(5, "Klan Cienia Północy", "Mistrz Wu Xie", 1640000, "EU-East (Warszawa)", 12),
            MultiplayerSectRanking(6, "Dolina Pięciu Żywiołów", "Mędrzec Zhang", 1310000, "Asia-East (Tokyo)", 85)
        )
    }

    fun getInitialWorldBoss(): WorldBossRaid {
        return WorldBossRaid(
            name = "Władca Próżni i Otchłani",
            title = "Starożytne Kosmiczne Bóstwo Zagłady",
            currentHp = 1420000L,
            maxHp = 2000000L,
            playerContributionDamage = 45200L,
            participatingSectsCount = 142,
            hoursRemaining = 16
        )
    }
}
