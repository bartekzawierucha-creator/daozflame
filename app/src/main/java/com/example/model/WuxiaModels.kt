package com.example.model

enum class CultivationRealm(
    val polishName: String,
    val tier: Int,
    val powerMultiplier: Double,
    val maxQi: Int,
    val baseLifespanYears: Int
) {
    QI_CONDENSATION_1("Kondensacja Qi I", 1, 1.0, 100, 100),
    QI_CONDENSATION_5("Kondensacja Qi V", 2, 1.5, 250, 110),
    QI_CONDENSATION_9("Kondensacja Qi IX (Szczyt)", 3, 2.2, 500, 120),
    FOUNDATION_EARLY("Fundamenty (Wczesne)", 4, 3.5, 1000, 200),
    FOUNDATION_LATE("Fundamenty (Szczyt)", 5, 5.0, 2000, 250),
    CORE_FORMATION("Złoty Rdzeń (Złote Dao)", 6, 8.5, 4500, 500),
    NASCENT_SOUL("Narodziny Duszy (Yuan Ying)", 7, 15.0, 10000, 1200),
    SOUL_TRANSFORMATION("Transformacja Duszy (Hua Shen)", 8, 22.0, 18000, 2500),
    VOID_TRIBULATION("Pustka i Próba Niebios (Du Jie)", 9, 38.0, 32000, 5000),
    MAHAYANA_GREAT_VEHICLE("Wielki Pojazd Mahajana", 10, 55.0, 55000, 8000),
    HEAVENLY_IMMORTAL("Niebiański Nieśmiertelny", 11, 85.0, 100000, 99999),
    GOLDEN_IMMORTAL_OF_CHAOS("Złoty Nieśmiertelny Chaosu", 12, 160.0, 250000, 999999)
}

enum class OriginType(
    val polishName: String,
    val description: String,
    val factionAffinity: String,
    val bonusStat: String
) {
    IMPERIAL_DYNASTY("Dynastia Cesarska", "Spadkobierca arystokratów cesarstwa. Wysoka charyzma i kontakty handlowe.", "Dwór Cesarski", "+20% Złoto i Dyplomacja"),
    HUNTER_VILLAGE("Wioska Górskich Myśliwych", "Wychowany w dzikich ostępach, instynkt przetrwania i odporność.", "Wolni Kultywatorzy", "+25% Szczęście i Zbieractwo"),
    TAOIST_HERMIT("Pustelnia Świętych Gór", "Od dzieciństwa obcujący z naturą i czystym Qi niebios.", "Ortodoksyjne Sekty", "+30% Zrozumienie Dao"),
    MERCHANT_GUILD("Gildia Kupców Jedwabiu", "Bogate pochodzenie, biegłość w rzadkich minerałach i targach.", "Związki Handlowe", "+40% Zniżki u kupców"),
    FALLEN_CLAN("Upadły Starożytny Ród", "Ostatni potomek zniszczonego klanu szukający zemsty i odrodzenia.", "Pradawne Frakcje", "+35% Siła Ataku"),
    DEMONIC_EXILE("Wygnaniec Sekty Cienia", "Uciekinier z bezwzględnej sekty mroku niosący zakazaną wiedzę.", "Mroczne Klany", "+25% Obrażenia Krytyczne"),
    ANCIENT_DIVINE_REALM("Zstąpienie z Wyższego Wymiaru", "Duch zrodzony w niebiańskim pałacu poza światem śmiertelników.", "Niebiańskie Klany", "+50% Zrozumienie Niebios"),
    FORBIDDEN_CULT_SURVIVOR("Ocalały z Zakazanego Kultu", "Świadek krwawych rytuałów zniszczonej przed tysiącem lat sekty.", "Sekty Zakazane", "+35% Toksyczność Qi"),
    IMMORTAL_FAMILY_RECLUSE("Pustelnik Rodziny Nieśmiertelnych", "Wychowany w odizolowanej od świata enklawie kultywatorów.", "Klany Pustelnicze", "+30% Regeneracja Meridianów"),
    SECRET_REALM_ORPHAN("Sierota z Tajemnego Wymiaru", "Odnaleziony w pradawnej ruinie zawieszonej w czasoprzestrzeni.", "Wolne Bóstwa", "+40% Szczęście Karmiczne")
}

enum class BloodlineType(
    val polishName: String,
    val description: String,
    val combatBonus: String,
    val inheritChance: Double
) {
    GOLDEN_DRAGON("Krew Złotego Smoka", "Starożytny ród władców niebios. Smoczy majestat miażdży obronę wrogów.", "+35% Przebicie Pancerza", 0.35),
    ANCIENT_PHOENIX("Krew Pradawnego Feniksa", "Spuścizna wiecznego płomienia. Regeneruje witalność w krytycznych momentach.", "+40% Regeneracja HP", 0.40),
    MOON_SHADOW_WOLF("Duch Księżycowego Wilka", "Dziedzictwo nocnych łowców. Otrzymuje gigantyczną moc pod osłoną nocy.", "+50% Atak w nocy", 0.45),
    TITANIC_ASURA("Linia Tytanicznego Asury", "Niepohamowany szał bitewny, rosnący wraz z ranami.", "+45% Obrażeń przy niskim HP", 0.30),
    PURPLE_LIGHTNING_QILIN("Krew Purpurowego Qilina", "Dziedzictwo boskiej bestii piorunów. Poraża wrogów prądem niebios.", "+50% Piorun & Paraliż", 0.30),
    IMMORTAL_LOTUS_HEART("Krew Nieśmiertelnego Lotosu", "Czyste serce kultywatora. Niepodatny na demony wewnętrzne.", "+35% Zrozumienie Dao", 0.40),
    PRIMORDIAL_CHAOS("Krew Pierwotnego Chaosu", "Zrodzony z chaosu przed narodzinami nieba i ziemi. Legendarny ród.", "+60% Wszechmoc Wszystkich Żywiołów", 0.15),
    HEAVENLY_NETHER_VIPER("Krew Żmii Otchłani", "Mroczna krew zatruwająca duszę i meridiany przeciwnika.", "+40% Przebicie Pancerza Trucizną", 0.35),
    VOID_DEVOURER("Krew Pustki Otchłani", "Pochłania energię duchową przeciwników przy każdym ciosie.", "+20% Kradzież Qi", 0.25),
    BLACK_TORTOISE_ANCESTOR("Krew Czarnego Żółwia Xuanwu", "Niezrównany pancerz nieprzeniknionej otchłani morskiej.", "+55% Redukcja Obrażeń", 0.30),
    SOLAR_CROW_DIVINE("Krew Słonecznego Kruka Niebios", "Żar pradawnego słońca wypalający meridiany przeciwników.", "+50% Obrażenia od Ognia", 0.25),
    NINE_TAILED_SPIRIT_FOX("Duch Dziewięcioogoniastego Lisa", "Mistrzostwo iluzji, hipnozy i kradzieży witalności.", "+40% Szansa na Obezwładnienie", 0.35),
    KUNPENG_VOID_WING("Skrzydła Pustki Mitycznego Kunpenga", "Władca wiatru i kosmicznego bezkresu omijający prawa materii.", "+60% Zwinność i Uniki", 0.20),
    PRIMORDIAL_DAO_ORIGIN("Pierwotne Pochodzenie Wielkiego Dao", "Najrzadsza krew jednocząca prawa wszechświata.", "+70% Do Wszystkich Statystyk", 0.08),
    PURE_MORTAL("Czyste Serce Śmiertelnika", "Brak boskiej krwi, ale niezłomna determinacja i czyste meridiany.", "+20% Szybsza Kultywacja", 0.60)
}

enum class ConstitutionType(
    val polishName: String,
    val description: String,
    val hazardImmunity: String,
    val attributeBuff: String
) {
    PURE_YANG("Ciało Czystego Yang", "Niebiański ogień płynący w żyłach. Pełna odporność na arktyczny mróz.", "Odporność na Mróz", "+30% Atak Ogniem"),
    CELESTIAL_FROST("Konstytucja Lodowego Nefrytu", "Urodzony z lodowym sercem. Odrzuca palące gorąco i lawę.", "Odporność na Lawę", "+30% Obrona Lodem"),
    NINE_DIVINE_MERIDIANS("Dziewięć Boskich Meridianów", "Wyjątkowo pojemne kanały Qi, odporne na duchowe burze.", "Odporność na Burze Qi", "+50% Pojemność Qi"),
    MYRIAD_POISON_IMMUNE("Ciało Tysiąca Trucizn", "Krew neutralizująca wszelkie toksyny, żądła bestii i bagienny miazmat.", "Odporność na Bagna", "+25% Toksyczne Rany"),
    INDOMITABLE_BONES("Złote Niezniszczalne Kości", "Kości twarde jak gwiezdny nefryt. Niezwykła redukcja obrażeń fizycznych.", "Odporność na Wstrząsy", "+40% Obrona Fizyczna"),
    CHAOS_HEAVEN_BODY("Ciało Pierwotnego Chaosu", "Wchłania wszelkie typy energii duchowej bez barier.", "Odporność na Klątwy", "+50% Prędkość Kultywacji"),
    VAJRA_IMMORTAL_BODY("Niezniszczalne Ciało Wadżry", "Ciało odporne na cięcia mieczy i uderzenia młotów.", "Odporność na Pęknięcia", "+50% Pancerz i Witalność"),
    DIVINE_SWORD_BODY("Wrodzone Ciało Miecza Dao", "Każdy ruch emanuje niewidzialną intencją miecza.", "Odporność na Iluzje", "+60% Krytyk Miecza"),
    ASTRAL_SUN_BODY("Ciało Gwiezdnego Słońca", "Promieniuje potęgą gwiezdnego żaru i niebiańskiego ognia.", "Odporność na Wieczną Noc", "+45% Siła Ataku Ognia"),
    MYRIAD_SWORD_ORIGIN("Ciało Nieskończonych Mieczy", "Setki niewidzialnych ostrzy chroniące Dantian i krew adepta.", "Odporność na Przebicie", "+55% Siła Cięcia Miecza"),
    DEVOURING_BLACK_HOLE("Ciało Pożerającej Pustki", "Wsysa wrogie pociski i czary niczym bezdenna przepaść.", "Odporność na Klątwy Dusz", "+40% Kradzież Energii"),
    TAOIST_FIVE_ELEMENTAL_EMPEROR("Ciało Cesarza Pięciu Żywiołów", "Władza nad wszystkimi 5 elementami bez żadnych ograniczeń.", "Odporność na Wszystkie Żywioły", "+35% Zrozumienie Żywiołów"),
    ETERNAL_SACRED_BODY("Wieczne Święte Ciało Niebios", "Mityczna sylwetka niepodatna na starzenie i tribulacje niebios.", "Odporność na Pioruny Niebios", "+80% Regeneracja HP"),
    FIVE_ELEMENT_HARMONY("Harmonijna Sylwetka Pięciu Żywiołów", "Równowaga żywiołów: woda, ogień, ziemia, metal, drewno.", "Równowaga Wszystkich Biomów", "+15% Do Wszystkich Statystyk")
}

data class AncientClan(
    val name: String,
    val title: String,
    val crestEmoji: String,
    val clanSpecialty: String,
    val traitDescription: String,
    val statAffinity: String,
    val reputationTitle: String = title,
    val buffDescription: String = traitDescription
)

data class FatedTrait(
    val id: String,
    val name: String,
    val rarity: String, // "Boski", "Legendarny", "Niezwykły"
    val effectDesc: String,
    val attackBonusPercent: Int = 0,
    val defenseBonusPercent: Int = 0,
    val cultivationBonusPercent: Int = 0,
    val critChanceBonus: Int = 0,
    val alchemyBonusPercent: Int = 0,
    val forgingBonusPercent: Int = 0,
    val description: String = effectDesc
)

enum class TalentGrade(val code: String, val polishName: String, val colorHex: Long) {
    SSS("SSS", "Potomek Bóstw Niebios", 0xFFFFD700),
    SS("SS", "Geniusz Tysiąclecia", 0xFFFF5252),
    S("S", "Wybitny Talent Ziemi", 0xFF00E5FF),
    A("A", "Niezwykły Adept", 0xFF10B981),
    B("B", "Zwykły Śmiertelnik", 0xFF94A3B8)
}

data class SectDecree(
    val id: String,
    val name: String,
    val description: String,
    val costPrestige: Int = 1,
    val isActive: Boolean = false,
    val bonusDesc: String = "",
    val title: String = name,
    val icon: String = "📜",
    val effectDescription: String = description
)

data class SectLeaderData(
    val name: String = "Mistrz Chen Tian",
    val title: String = "Patriarcha Dziewięciu Niebios",
    val clanHeritage: String = "Starożytny Ród Chen z Góry Świętego Smoka",
    val ancientClan: AncientClan = AncientClan("Ród Chen", "Pradawni Władcy Miecza", "🐉", "Szermierka Dao", "Zwiększa siłę ataku mieczem o 25%", "+25% Atak"),
    val lineage: String = "Krew Złotego Smoka",
    val divineConstitution: String = "Dziewięć Boskich Meridianów",
    val realm: String = "Narodzenie Duszy (Nascent Soul)",
    val prestigeLevel: Int = 3,
    val leadershipPoints: Int = 8,
    val divineAuthority: Int = 450,
    val activeDecrees: List<SectDecree> = emptyList()
)

enum class MartialSchool(
    val polishName: String,
    val roleTitle: String,
    val primaryStat: String,
    val description: String
) {
    SWORD_IMMORTAL("Szkoła Miecza Niebios", "Mistrz Latających Mieczy", "Atak & Szansa Krytyczna", "Sterowanie lewitującymi mieczami, precyzyjne cięcia przecinające przestrzeń."),
    BODY_ASURA("Zakon Żelaznego Pięściarza", "Kultywator Ciała Asury", "Maksymalne Zdrowie & Pancerz", "Przekształcenie własnego ciała w boską broń, odporną na uderzenia piorunów."),
    ELEMENTAL_TAOIST("Mistrz Duchowych Żywiołów", "Władca Żywiołów Tao", "Zrozumienie & Zdolności Obszarowe", "Manipulacja burzami, płomieniami geocentrycznymi i falami mrozu."),
    SHADOW_VIPER("Cień Niewidzialnego Węża", "Skrytobójca Meridianów", "Szybkość, Uniki & Toksyny", "Ciche uderzenia w punkty akupunkturowe wroga, paraliżujące trucizny.")
}

enum class DiscipleRole(val polishName: String, val bonusDesc: String) {
    MEDITATING("Kultywacja w Pawilonie", "+100% zysk Qi"),
    HERB_GATHERING("Zbieranie Ziół", "Pozyskuje rośliny alchemiczne"),
    MINING("Wydobycie Minerałów", "Wydobywa rudy dla Kuźni"),
    COMBAT_TRAINING("Sparingi i Walka", "+1 punkt Dao co cykl"),
    DIPLOMACY("Misje Dyplomatyczne", "+Reputacja z frakcjami"),
    SECT_GUARD("Wachta Bramy Sekty", "Ochrona przed najazdami bestii")
}

enum class TechniqueCategory(val polishName: String, val icon: String, val summary: String) {
    CULTIVATION("Kultywacja & Qi Gong", "🧘", "Metody oddychania, oczyszczania Dantian i przyspieszania wzrostu Qi"),
    MOVEMENT("Poruszanie & Lekkość", "💨", "Kroki cienia, zwinne uniki i błyskawiczne przemieszczanie"),
    FORMATION("Formacje & Tablice Dao", "☯️", "Bariery obronne sekty, pieczęcie żywiołów i rezonans aury"),
    ATTACK("Atak & Sztuki Walki", "⚔️", "Śmiertelne cięcia miecza, uderzenia pięści i wybuchy energii"),
    DEFENSE("Obrona & Złote Ciało", "🛡️", "Niezniszczalna skóra, tarcze ochronne i ochrona meridianów")
}

data class SkillNode(
    val id: String,
    val name: String,
    val tier: Int,
    val description: String,
    val costDaoPoints: Int,
    val martialSchool: MartialSchool,
    val attackBonus: Int = 0,
    val defenseBonus: Int = 0,
    val critBonusPercent: Int = 0,
    val hpBonus: Int = 0,
    val activeTechniqueName: String? = null,
    val category: TechniqueCategory = TechniqueCategory.ATTACK,
    val gradeName: String = "Stopień Żółty",
    val qiConsumption: Int = 15,
    val qiGainBonusPercent: Int = 0,
    val evasionBonusPercent: Int = 0,
    val specialEffect: String = ""
)

data class GeocentricFlame(
    val id: String,
    val name: String,
    val tierName: String,
    val bonusCraftingRate: Int,
    val combatDamageBonus: Int,
    val description: String,
    val refinementLevel: Int = 1,
    val temperatureCelsius: Int = 1800,
    val elementAffinity: String = "Czysty Yang",
    val specialEffect: String = "Zwiększa szansę na pigułki jakości Mistycznej",
    val isTamed: Boolean = true
)

data class Artifact(
    val id: String,
    val name: String,
    val rarity: String,
    val attackBonus: Int,
    val defenseBonus: Int,
    val specialEffect: String
)

data class Disciple(
    val id: String,
    val name: String,
    val gender: String,
    val avatarEmoji: String,
    val origin: OriginType,
    val bloodline: BloodlineType,
    val constitution: ConstitutionType,
    val martialSchool: MartialSchool,
    val realm: CultivationRealm,
    val realmProgress: Int, // 0 to 100
    val maxHp: Int,
    val currentHp: Int,
    val attack: Int,
    val defense: Int,
    val comprehension: Int, // Zrozumienie Dao
    val karmicLuck: Int, // Szczęście Losu
    val daoPoints: Int,
    val unlockedSkillIds: Set<String> = emptySet(),
    val role: DiscipleRole = DiscipleRole.MEDITATING,
    val equippedArtifact: Artifact? = null,
    val equippedFlame: GeocentricFlame? = null,
    val ancientClan: AncientClan = AncientClan("Ród Chen", "Pradawni Władcy Miecza", "🐉", "Szermierka Dao", "Zwiększa siłę ataku mieczem o 25%", "+25% Atak"),
    val fatedTraits: List<FatedTrait> = emptyList(),
    val talentGrade: TalentGrade = TalentGrade.S,
    val combatPower: Int = 1250,
    val innateTechnique: String = "Taniec Ostrza Dziewięciu Chmur",
    // --- Etap 2: życie ucznia (wszystko z wartościami domyślnymi dla starych zapisów) ---
    /** null = stary zapis; silnik uzupełnia deterministycznie przy wczytaniu. */
    val spiritRoot: SpiritRoot? = null,
    /** Jawne cechy charakteru (id z katalogu). */
    val traitIds: List<String> = emptyList(),
    /** Cechy ukryte – ujawniają się w zdarzeniach albo po Teście Korzenia. */
    val hiddenTraitIds: List<String> = emptyList(),
    val rank: SectRank = SectRank.OUTER,
    /** Lojalność 0–100. */
    val loyalty: Int = 70,
    val ageYears: Int = 18,
    /** Dzień, od którego uczeń spełnia wymagania awansu, a go nie dostał (dla cechy Ambitny). */
    val promotionDueSinceDay: Int? = null,
    // --- Etap 3: kultywacja (realmProgress = procent bieżącego ETAPU, realm = zgodność z UI) ---
    /** null = zapis sprzed Etapu 3; silnik uzupełnia z [realm]. */
    val majorRealm: MajorRealm? = null,
    val stage: Int = 1,
    /** Qi zgromadzone w bieżącym etapie. */
    val stageQi: Double = 0.0,
    val foundationQuality: FoundationQuality? = null,
    /** Grad złotego rdzenia 1–9 (0 = brak rdzenia). */
    val coreGrade: Int = 0,
    val soulShape: String? = null,
    val path: CultivationPath? = null,
    val methodId: String? = null,
    /** Zrozumienie Praw Dao: nazwa prawa → 0–100%. */
    val laws: Map<String, Double> = emptyMap(),
    val bloodlineAwakening: Int = 0,
    val secludedUntilDay: Int = 0,
    val toxicity: Int = 0,
    val injuredUntilDay: Int = 0,
    val heartDemon: Boolean = false
)

enum class ResourceCategory { MINERAL, HERB, PILL, ESSENCE }

data class ResourceItem(
    val id: String,
    val name: String,
    val category: ResourceCategory,
    val rarity: String,
    val count: Int,
    val description: String,
    val tier: Int = 1,
    val spiritualAgeYears: Int = 100,
    val elementAffinity: String = "Neutralny",
    val potency: Int = 10
)

data class AlchemyRecipe(
    val id: String,
    val pillName: String,
    val requiredHerbName: String,
    val requiredHerbCount: Int,
    val requiredSpiritStones: Int,
    val effectDescription: String,
    val breakthroughBonusPercent: Int = 0,
    val hpRestore: Int = 0,
    val tier: Int = 1,
    val tierName: String = "Stopień 1: Oczyszczenie Qi",
    val requiredAlchemyChamberLevel: Int = 1,
    val secondaryHerbName: String? = null,
    val secondaryHerbCount: Int = 0,
    val daoPointsBonus: Int = 0
)

data class ForgingRecipe(
    val id: String,
    val artifactName: String,
    val requiredMineralName: String,
    val requiredMineralCount: Int,
    val requiredSpiritStones: Int,
    val artifactResult: Artifact
)

enum class FacilityType(val polishName: String, val iconName: String) {
    CULTIVATION_PAVILION("Pawilon Kultywacji", "spa"),
    ALCHEMY_CHAMBER("Sala Alchemii", "science"),
    DIVINE_FORGE("Boska Kuźnia", "fireplace"),
    MARTIAL_ARCHIVES("Wieża Technik Walki", "menu_book"),
    MISSION_HALL("Sala Ekspedycji", "explore"),
    DIPLOMACY_HALL("Sala Dyplomacji", "handshake")
}

data class SectFacility(
    val type: FacilityType,
    val level: Int,
    val upgradeCostStones: Int,
    val effectDescription: String
)

enum class BiomeType(val polishName: String, val emoji: String) {
    BAMBOO_MIST("Bambusowy Las Mgły", "🎋"),
    KUNLUN_PEAK("Niebiański Szczyt Kunlun", "🏔️"),
    LAVA_CHASM("Rozpadlina Płynnej Lawy", "🌋"),
    VENOM_SWAMP("Bagno Tysiąca Trucizn", "🐊"),
    SPIRIT_DESERT("Pustynia Złotych Piasków", "🏜️"),
    DRAGON_ABYSS("Głębia Smoczej Otchłani", "🐉"),
    THUNDER_PLATEAU("Płaskowyż Wiecznej Burzy", "⚡"),
    SUNKEN_PALACE("Zatopiony Pałac Głębin", "🌊"),
    BONE_WASTES("Pustkowia Kości Bogów", "💀"),
    CELESTIAL_ISLES("Niebiańskie Wyspy", "☁️"),
    STAR_RUINS("Ruiny Gwiezdnego Pałacu", "✨"),
    PRIMORDIAL_CHAOS("Morze Pierwotnego Chaosu", "🌀")
}

enum class EnvironmentalHazard(val polishName: String, val counterMeasure: String) {
    TOXIC_MIASMA("Toksyczny Miazmat", "Pigułka Odporności lub Ciało Tysiąca Trucizn"),
    EXTREME_FROST("Arktyczny Mróz Qi", "Ciało Czystego Yang lub Płomień Lawy"),
    SCORCHING_MAGMA("Piekielna Spiekota", "Konstytucja Lodowego Nefrytu"),
    SPIRITUAL_TEMPEST("Burza Przepływu Qi", "Dziewięć Boskich Meridianów"),
    NONE("Brak Zagrożeń", "Bezpieczny obszar")
}

data class MythicalBeast(
    val name: String,
    val title: String,
    val hp: Int,
    val maxHp: Int,
    val attack: Int,
    val defense: Int,
    val dropItemName: String,
    val spiritStoneReward: Int,
    val emoji: String
)

data class WorldRegion(
    val id: String,
    val name: String,
    val biome: BiomeType,
    val hazard: EnvironmentalHazard,
    val dangerLevel: Int, // 1 to 10
    val explorationProgress: Int, // 0 to 100
    val mineralResource: String,
    val herbResource: String,
    val bossBeast: MythicalBeast?,
    val isConquered: Boolean = false
)

enum class DiplomaticStatus(val polishName: String, val colorCode: Long) {
    ALLIANCE("Sojusz Wieczysty", 0xFF10B981),
    FRIENDLY("Przyjazne Stosunki", 0xFF38BDF8),
    NEUTRAL("Neutralność", 0xFF94A3B8),
    COLD_WAR("Napięcie i Rywalizacja", 0xFFF59E0B),
    TOTAL_WAR("Stan Wojny", 0xFFEF4444)
}

data class TradeOffer(
    val id: String,
    val offerText: String,
    val costStones: Int,
    val rewardItem: String
)

enum class SectPolicy(val polishName: String, val philosophy: String, val buffDescription: String) {
    RIGHTEOUS_DAO("Ortodoksyjne Prawo Dao", "Dążenie do harmonii i ochrona słabszych", "+40% Zysk Reputacji i szansa na Oświecenie"),
    EXPANSIONIST("Zbrojna Ekspansja", "Agresywna dominacja złóż duchowych i szlaków", "+35% Zyski z terytoriów i siła ataku"),
    ISOLATIONIST("Izolacjonizm Duchowy", "Zamknięcie bram, głęboka medytacja w odosobnieniu", "+30% Czystość Dantian i obrona bazy"),
    DEMONIC_RAIDERS("Demoniczni Grabieżcy", "Pochłanianie esencji pokonanych rywali", "+45% Obrażeń bitewnych i kradzież Qi"),
    MERCHANT_CONSORTIUM("Kartel Kupców Niebios", "Monopolizacja rynków pigułek i minerałów", "+50% Przychód Kamieni Ducha")
}

data class SectDivineBeast(
    val id: String,
    val name: String,
    val title: String,
    val emoji: String,
    val element: String,
    val tier: Int = 1,
    val intimacy: Int = 30, // 0 - 100%
    val currentEnergy: Int = 1200,
    val maxEnergy: Int = 1200,
    val passiveSectBuff: String,
    val combatSkillName: String,
    val combatSkillDesc: String,
    val favoriteHerb: String = "Śnieżny Lotos Oświecenia"
)

data class TerritoryNode(
    val id: String,
    val name: String,
    val regionType: String,
    val controllingSectId: String, // "player" lub id rywala
    val controllingSectName: String,
    val resourceType: String,
    val yieldAmountPerCycle: Int,
    val garrisonDefensePower: Int,
    val assignedDiscipleId: String? = null,
    val isUnderAttack: Boolean = false
)

data class RivalSect(
    val id: String,
    val name: String,
    val leaderName: String,
    val alignment: String, // Praworządna, Zrównoważona, Demoniczna
    val powerRank: Int,
    val discipleCount: Int,
    val relations: Int, // -100 to +100
    val status: DiplomaticStatus,
    val specialty: String,
    val activeTrade: TradeOffer? = null,
    val policy: SectPolicy = SectPolicy.RIGHTEOUS_DAO,
    val cultivationBuffName: String = "Harmonia Qi",
    val cultivationBuffDescription: String = "+15% Zysk Qi przy przymierzu",
    val cultivationBuffBonusPercent: Int = 15,
    val controlledTerritoryIds: List<String> = emptyList()
)

enum class TimeOfDay(val polishName: String, val symbol: String, val statModifierDesc: String) {
    DAWN("Świt", "🌅", "Harmonia Yin i Yang: +10% do regeneracji Qi"),
    DAY("Dzień (Czysty Yang)", "☀️", "Energia Yang: +25% siły ataku ogniem i kuźni"),
    DUSK("Zmierzch", "🌇", "Równowaga Zmierzchu: +15% zysku z alchemii"),
    NIGHT("Noc (Głębokie Yin)", "🌙", "Energia Yin: +30% uniki i techniki skrytobójców"),
    FULL_MOON("Pełnia Księżyca Dao", "🌕", "Rezonans Kosmiczny: +45% szansy na przełom kultywacyjny!")
}

data class RandomEventChoice(
    val text: String,
    val outcomeDescription: String,
    val stoneChange: Int = 0,
    val repChange: Int = 0,
    val rewardItem: String? = null
)

data class RandomEvent(
    val id: String,
    val title: String,
    val description: String,
    val choices: List<RandomEventChoice>,
    /** Zdarzenie wypada tylko w tej porze dnia (null = zawsze). */
    val requiredTimeOfDay: TimeOfDay? = null,
    /** Minimalna reputacja sekty, by zdarzenie mogło wypaść. */
    val minReputation: Int = 0,
    /** Najwcześniejszy dzień gry, w którym zdarzenie może wypaść. */
    val minDay: Int = 0
) {
    fun isEligible(day: Int, reputation: Int, time: TimeOfDay): Boolean =
        day >= minDay && reputation >= minReputation && (requiredTimeOfDay == null || requiredTimeOfDay == time)
}

data class CombatLogEntry(
    val round: Int,
    val text: String,
    val isCrit: Boolean = false,
    val isHeal: Boolean = false
)

data class FloatingDamage(
    val id: Long,
    val text: String,
    val isCrit: Boolean = false,
    val isHeal: Boolean = false,
    val isDisciple: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class CombatState(
    val active: Boolean = false,
    val discipleName: String = "",
    val discipleSchool: MartialSchool = MartialSchool.SWORD_IMMORTAL,
    val discipleHp: Int = 0,
    val discipleMaxHp: Int = 100,
    val discipleQi: Int = 100,
    val discipleMaxQi: Int = 100,
    val enemyName: String = "",
    val enemyHp: Int = 0,
    val enemyMaxHp: Int = 100,
    val logs: List<CombatLogEntry> = emptyList(),
    val isVictory: Boolean? = null,
    val rewardText: String = "",
    val currentEffect: String = "", // "SLASH", "CRIT", "PROJECTILE", "ULTIMATE", "BEAST_ATTACK", "HEAL"
    val effectCounter: Int = 0,
    val floatingDamages: List<FloatingDamage> = emptyList()
)

data class MultiplayerSectRanking(
    val rank: Int,
    val sectName: String,
    val grandmasterName: String,
    val totalPower: Long,
    val realmRegion: String,
    val pingMs: Int
)

data class WorldBossRaid(
    val name: String,
    val title: String,
    val currentHp: Long,
    val maxHp: Long,
    val playerContributionDamage: Long,
    val participatingSectsCount: Int,
    val hoursRemaining: Int
)
