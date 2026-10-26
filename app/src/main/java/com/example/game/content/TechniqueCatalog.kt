package com.example.game.content

import com.example.model.MartialSchool
import com.example.model.SkillNode
import com.example.model.TechniqueCategory
import com.example.model.TechniqueCategory.ATTACK
import com.example.model.TechniqueCategory.CULTIVATION
import com.example.model.TechniqueCategory.DEFENSE
import com.example.model.TechniqueCategory.FORMATION
import com.example.model.TechniqueCategory.MOVEMENT

/**
 * Autorskie drzewka technik: 14 na każdą z 4 szkół (56), poziomy 1–5.
 * Zastępują losowo generowane techniki – nazwy i efekty są stałe między uruchomieniami.
 */
object TechniqueCatalog {

    private val grades = listOf("Stopień Żółty", "Stopień Czarny", "Stopień Mistyczny", "Stopień Niebiański", "Stopień Pierwotnego Dao")

    private class B(val school: MartialSchool, val prefix: String) {
        val out = mutableListOf<SkillNode>()
        private var n = 0
        fun t(
            tier: Int, cat: TechniqueCategory, name: String, desc: String,
            atk: Int = 0, def: Int = 0, hp: Int = 0, crit: Int = 0, qi: Int = 0, eva: Int = 0,
            active: Boolean = false, effect: String = ""
        ) {
            n++
            out += SkillNode(
                id = "${prefix}_$n", name = name, tier = tier, description = desc,
                costDaoPoints = tier, martialSchool = school,
                attackBonus = atk, defenseBonus = def, critBonusPercent = crit, hpBonus = hp,
                activeTechniqueName = if (active) name else null,
                category = cat, gradeName = grades[tier - 1], qiConsumption = 10 * tier,
                qiGainBonusPercent = qi, evasionBonusPercent = eva, specialEffect = effect
            )
        }
    }

    private fun school(s: MartialSchool, p: String, f: B.() -> Unit) = B(s, p).apply(f).out

    val techniques: List<SkillNode> =
        school(MartialSchool.SWORD_IMMORTAL, "tech_sw") {
            t(1, CULTIVATION, "Oddech Ostrza o Świcie", "Medytacja z mieczem na kolanach. Qi płynie wzdłuż klingi.", qi = 20, effect = "+20% Qi w porze świtu")
            t(1, ATTACK, "Cięcie Przecinające Liść", "Pierwsza forma: cięcie tak precyzyjne, że rozdziela liść na dwa cienkie płaty.", atk = 30, crit = 5, active = true)
            t(1, MOVEMENT, "Krok Lecącej Jaskółki", "Lekki krok po wierzchołkach bambusów.", eva = 10, crit = 3)
            t(1, DEFENSE, "Parowanie Spadającej Wody", "Klinga odbija ciosy jak skała odbija strumień.", def = 20, hp = 120)
            t(2, ATTACK, "Trzy Miecze Jednej Myśli", "Jedna myśl, trzy latające miecze uderzające jednocześnie.", atk = 70, crit = 8, active = true)
            t(2, FORMATION, "Formacja Siedmiu Kling", "Siedem mieczy krąży wokół adepta jak tarcza.", def = 45, atk = 20)
            t(2, CULTIVATION, "Hartowanie Intencji Miecza", "Intencja miecza przenika dantian. Zrozumienie rośnie.", qi = 35, effect = "+1 zrozumienia co przełom")
            t(3, ATTACK, "Burza Tysiąca Ostrzy", "Niebo pokrywa się latającymi mieczami.", atk = 130, crit = 12, active = true)
            t(3, MOVEMENT, "Lot na Mieczu", "Adept staje na klindze i przecina niebo.", eva = 25, effect = "+30% szybkości wypraw")
            t(3, DEFENSE, "Lustro Klingi", "Odbija technikę przeciwnika płaskim ostrzem.", def = 90, hp = 400, effect = "Odbija 15% obrażeń technik")
            t(4, ATTACK, "Cięcie Rozdzielające Niebiosa", "Jedno cięcie, po którym chmury nie łączą się przez trzy dni.", atk = 240, crit = 18, active = true)
            t(4, CULTIVATION, "Serce Miecza Bez Skazy", "Miecz i adept stają się jednym.", qi = 70, crit = 10)
            t(5, ATTACK, "Dziesięć Tysięcy Mieczy Wracają do Źródła", "Wszystkie miecze świata kłaniają się adeptowi.", atk = 420, crit = 25, active = true, effect = "Ignoruje 30% obrony")
            t(5, FORMATION, "Wielka Formacja Niebiańskiego Miecza", "Formacja chroniąca całą sektę ostrzami ze światła.", def = 220, hp = 1500, effect = "+20% obrony garnizonów")
        } +
        school(MartialSchool.BODY_ASURA, "tech_as") {
            t(1, DEFENSE, "Skóra z Brązu", "Codzienne uderzenia kamieniem hartują skórę.", def = 30, hp = 200)
            t(1, ATTACK, "Pięść Rozbijająca Głaz", "Prosty cios, który kruszy skały.", atk = 35, active = true)
            t(1, CULTIVATION, "Oddech Żelaznego Byka", "Głęboki oddech napełniający mięśnie Qi.", qi = 15, hp = 150)
            t(1, MOVEMENT, "Szarża Dzikiego Dzika", "Nieustępliwy szarż przez szeregi wroga.", atk = 15, eva = 5)
            t(2, DEFENSE, "Kości z Żelaza", "Kości twardnieją jak kute żelazo.", def = 70, hp = 450)
            t(2, ATTACK, "Sto Pięści Asury", "Grad ciosów szybszych niż wzrok.", atk = 80, crit = 6, active = true)
            t(2, CULTIVATION, "Krwawy Dantian", "Qi krąży z krwią. Rany goją się szybciej.", qi = 30, hp = 300, effect = "Leczy 5% HP co turę")
            t(3, DEFENSE, "Złote Ciało Wadżry", "Ciało lśni złotem i odbija ostrza.", def = 140, hp = 900, active = true)
            t(3, ATTACK, "Uderzenie Trzęsącej Ziemi", "Cios w ziemię wywraca szeregi wrogów.", atk = 150, effect = "Ogłusza wszystkich wrogów na 1 turę", active = true)
            t(3, FORMATION, "Krąg Gniewu Asury", "Aura gniewu przyspiesza sojuszników.", atk = 40, def = 40)
            t(4, DEFENSE, "Nieśmiertelne Ciało Tytana", "Ciało, którego nie przebije żaden miecz śmiertelnych.", def = 260, hp = 2200)
            t(4, ATTACK, "Pięść Rozdzierająca Góry", "Jeden cios, jedna góra mniej.", atk = 260, crit = 12, active = true)
            t(5, DEFENSE, "Ciało Pierwotnego Chaosu", "Ciało starsze niż światło.", def = 420, hp = 4500, effect = "Raz na walkę ignoruje śmiertelny cios")
            t(5, ATTACK, "Ostatnia Pięść Asury", "Asura pali własne życie, by zniszczyć niebo.", atk = 480, crit = 20, active = true, effect = "Kosztuje 20% HP")
        } +
        school(MartialSchool.ELEMENTAL_TAOIST, "tech_el") {
            t(1, CULTIVATION, "Pięć Oddechów Żywiołów", "Każdy oddech przyjmuje inny żywioł.", qi = 25)
            t(1, ATTACK, "Talizman Ognistej Kuli", "Papierowy talizman wybucha płomieniem.", atk = 30, active = true)
            t(1, FORMATION, "Krąg Ochronnych Runów", "Kreda, krew i słowa mocy.", def = 25, hp = 100)
            t(1, MOVEMENT, "Krok na Wietrze", "Wiatr niesie adepta jak liść.", eva = 12)
            t(2, ATTACK, "Łańcuch Błyskawic", "Piorun przeskakuje z wroga na wroga.", atk = 75, crit = 6, active = true, effect = "Trafia 3 cele")
            t(2, FORMATION, "Formacja Mgły Iluzji", "Wrogowie błądzą we mgle.", def = 40, eva = 15)
            t(2, CULTIVATION, "Rezonans Płomienia Geocentrycznego", "Dantian rezonuje z płomieniem sekty.", qi = 40, effect = "+10% skuteczności alchemii")
            t(3, ATTACK, "Deszcz Lodowych Lanc", "Z nieba spadają włócznie lodu.", atk = 140, active = true, effect = "Spowalnia wrogów")
            t(3, FORMATION, "Wielka Formacja Ośmiu Trygramów", "Bagua zamyka wroga w labiryncie.", def = 110, hp = 500)
            t(3, CULTIVATION, "Zrozumienie Pięciu Przemian", "Ogień rodzi ziemię, ziemia metal, metal wodę...", qi = 60, effect = "+1 punkt Dao co przełom")
            t(4, ATTACK, "Gniew Niebiańskiej Burzy", "Adept przywołuje burzę wielkości miasta.", atk = 250, crit = 12, active = true)
            t(4, FORMATION, "Formacja Zapieczętowania Gór", "Pieczęć zamyka całą dolinę.", def = 200, hp = 1200, effect = "+25% obrony sekty przy najazdach")
            t(5, ATTACK, "Pięć Żywiołów Zniszczenia", "Pięć żywiołów uderza naraz, rozrywając prawa natury.", atk = 450, crit = 20, active = true)
            t(5, CULTIVATION, "Jedność z Niebiańskim Dao", "Adept słyszy oddech wszechświata.", qi = 120, effect = "+30% szansy na przełom")
        } +
        school(MartialSchool.SHADOW_VIPER, "tech_vp") {
            t(1, MOVEMENT, "Krok Bezszelestnego Kota", "Stopa nie budzi nawet kurzu.", eva = 15)
            t(1, ATTACK, "Igła w Punkt Ciszy", "Trafienie w punkt akupunkturowy odbiera głos.", atk = 28, crit = 8, active = true)
            t(1, CULTIVATION, "Oddech w Cieniu", "Medytacja w ciemności, gdzie Qi Yin jest najgęstsze.", qi = 20, effect = "+20% Qi nocą")
            t(1, DEFENSE, "Odporność na Jady", "Codzienne krople jadu uodparniają ciało.", def = 15, hp = 150, effect = "Odporność na Toksyczny Miazmat")
            t(2, ATTACK, "Kieł Zielonej Żmii", "Sztylet zatruty jadem żmii.", atk = 65, crit = 12, active = true, effect = "Trucizna 3% HP co turę")
            t(2, MOVEMENT, "Sto Cieni", "Adept zostawia za sobą sto mylących cieni.", eva = 28)
            t(2, FORMATION, "Sieć Pajęczych Nici", "Niewidzialne nici krępują wroga.", def = 30, eva = 10)
            t(3, ATTACK, "Pocałunek Czarnej Wdowy", "Cios, po którym serce bije coraz wolniej.", atk = 125, crit = 20, active = true)
            t(3, MOVEMENT, "Zniknięcie w Księżycowym Cieniu", "Adept rozpływa się w mroku.", eva = 40, effect = "Pierwszy atak w walce zawsze krytyczny")
            t(3, CULTIVATION, "Dantian Stu Trucizn", "Trucizny stają się paliwem Qi.", qi = 55, hp = 300)
            t(4, ATTACK, "Tysiąc Igieł Deszczu", "Deszcz igieł z każdej kropli.", atk = 230, crit = 25, active = true)
            t(4, MOVEMENT, "Krok Przez Pustkę", "Krótki skok przez szczelinę przestrzeni.", eva = 55, crit = 10)
            t(5, ATTACK, "Ostrze Końca Nocy", "Cios zadany w chwili, gdy noc umiera.", atk = 400, crit = 35, active = true, effect = "Zabija wroga poniżej 15% HP")
            t(5, MOVEMENT, "Cień Bez Ciała", "Adept staje się cieniem. Ostrza przechodzą na wylot.", eva = 75, effect = "50% szansy na uniknięcie każdej techniki")
        }
}
