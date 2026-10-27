package com.example.game.content

import com.example.game.engine.Empire
import com.example.game.engine.EventScripts
import com.example.game.engine.GameContent
import com.example.model.CondType
import com.example.model.Condition
import com.example.model.Effect
import com.example.model.EffectType
import com.example.model.ScriptChoice
import com.example.model.ScriptEvent

/**
 * Etap 5c: Imperium (zdarzenia planowane przez [Empire]) i wędrowni NPC – łańcuchy 3 zdarzeń.
 * Razem z pustelnikiem i kupcem ze [ScriptCatalog] daje to 12 postaci.
 */
object NpcCatalog {

    private fun cond(type: CondType, key: String = "", value: Int = 0) = Condition(type, key, value)
    private fun stones(v: Int) = Effect(EffectType.STONES, value = v)
    private fun pct(v: Int) = Effect(EffectType.STONES_PERCENT, value = v)
    private fun rep(v: Int) = Effect(EffectType.REPUTATION, value = v)
    private fun karma(v: Int) = Effect(EffectType.KARMA, value = v)
    private fun morale(v: Int) = Effect(EffectType.MORALE, value = v)
    private fun item(name: String, n: Int) = Effect(EffectType.ITEM, name, n)
    private fun flag(name: String, v: Int = 1) = Effect(EffectType.SET_FLAG, name, v)
    private fun favor(v: Int) = Effect(EffectType.ADD_FLAG, Empire.FAVOR, v)
    private fun next(id: String, days: Int) = Effect(EffectType.SCHEDULE, id, days)
    private fun rival(id: String, v: Int) = Effect(EffectType.RIVAL_RELATION, id, v)
    private fun hurt(v: Int) = Effect(EffectType.INJURE_CHAMPION, value = v)

    private fun c(text: String, outcome: String, vararg effects: Effect, needs: List<Condition> = emptyList()) =
        ScriptChoice(text, outcome, needs, effects.toList())

    private fun system(id: String, title: String, desc: String, vararg choices: ScriptChoice) =
        ScriptEvent(id, title, desc, choices = choices.toList(), random = false, chain = EventScripts.SYSTEM_CHAIN)

    private fun first(id: String, npc: String, minDay: Int, title: String, desc: String, vararg choices: ScriptChoice, extra: List<Condition> = emptyList()) =
        ScriptEvent(id, title, desc, listOf(cond(CondType.MIN_DAY, value = minDay)) + extra, choices.toList(), random = true, once = true, chain = npc)

    private fun link(id: String, npc: String, title: String, desc: String, vararg choices: ScriptChoice) =
        ScriptEvent(id, title, desc, choices = choices.toList(), random = false, chain = npc)

    private val empire = listOf(
        system("emp_tax", "Poborca Cesarski",
            "Urzędnik w jedwabnym kapeluszu rozwija zwój: sekta winna jest Imperium daninę za ostatni miesiąc.",
            c("Zapłać daninę (5% skarbca)", "Poborca odnotowuje lojalność sekty.", pct(-5), favor(5)),
            c("Wręcz łapówkę urzędnikowi", "Urzędnik chowa sakiewkę i „gubi” zwój.", stones(-300), needs = listOf(cond(CondType.MIN_STONES, value = 300))),
            c("Odmów – sekty nie płacą cesarzom", "Poborca odjeżdża z pogróżkami. Dwór to zapamięta.", favor(-20), rep(-10))),
        system("emp_order_bandits", "Rozkaz: bandyci na trakcie",
            "Gubernator prowincji rozkazuje sekcie oczyścić trakt z bandytów, którzy łupią cesarskie karawany.",
            c("Wyślij czempiona", "Bandyci rozbici, ale czempion wraca poraniony.", hurt(30), favor(15), stones(400)),
            c("Odmów", "Gubernator wpisuje sektę na listę nieposłusznych.", favor(-15))),
        system("emp_order_herbs", "Rozkaz: zioła dla armii",
            "Armia cesarska potrzebuje ziół leczniczych. Dwór żąda 10 pęków Trawy Wiecznej Wiosny.",
            c("Dostarcz 10× Trawa Wiecznej Wiosny", "Kwatermistrz płaci uczciwie i kłania się nisko.", item("Trawa Wiecznej Wiosny", -10), favor(15), stones(300),
                needs = listOf(cond(CondType.HAS_ITEM, "Trawa Wiecznej Wiosny", 10))),
            c("Odmów", "Kwatermistrz odjeżdża z pustym wozem.", favor(-15))),
        system("emp_order_escort", "Rozkaz: eskorta księżniczki",
            "Księżniczka podróżuje przez ziemie sekty. Dwór oczekuje honorowej eskorty.",
            c("Daj najlepszych uczniów do eskorty", "Księżniczka dziękuje osobiście. Uczniowie są zmęczeni, ale dumni.", favor(10), karma(3), morale(-5), rep(15)),
            c("Odmów", "Orszak przejeżdża bez eskorty. Plotki niosą się po dworze.", favor(-15))),
        system("emp_civil_1", "Iskra wojny domowej",
            "Generał Północy podniósł bunt przeciw Cesarzowi. Obie strony przysyłają posłów: sekta musi wybrać.",
            c("Stań po stronie Cesarza", "Cesarski poseł obiecuje nagrodę po zwycięstwie.", flag("civil_side", 1), next("emp_civil_2", 10)),
            c("Wesprzyj buntowników", "Generał obiecuje, że sekty nie będą płacić danin.", flag("civil_side", 2), next("emp_civil_2", 10)),
            c("Zachowaj neutralność", "Posłowie odjeżdżają niezadowoleni.", next("emp_civil_2", 10))),
        system("emp_civil_2", "Bitwa nad Żółtą Rzeką",
            "Armie starły się nad rzeką. Ranni uciekinierzy proszą sektę o schronienie.",
            c("Przyjmij rannych z obu stron", "Sala uzdrowień jest pełna. Karma sekty rośnie.", karma(10), stones(-300), next("emp_civil_3", 10)),
            c("Zamknij bramy", "Sekta przeczekuje burzę za murami.", next("emp_civil_3", 10))),
        system("emp_civil_3", "Koniec wojny domowej",
            "Kurz opadł. Zwycięzca zjeżdża do stolicy, a sekty liczą zyski i straty.",
            c("Cesarz zwycięża – odbierz nagrodę", "Cesarz nadaje sekcie tytuł Strażnika Prowincji.", favor(30), stones(2000), rep(40),
                needs = listOf(cond(CondType.FLAG_AT_LEAST, "civil_side", 1), cond(CondType.FLAG_BELOW, "civil_side", 2))),
            c("Buntownicy zwyciężają – Imperium upada", "Nowy władca znosi daniny dla sekt. Imperium w starym kształcie przestało istnieć.",
                flag(Empire.FALLEN), rep(50), needs = listOf(cond(CondType.FLAG_AT_LEAST, "civil_side", 2))),
            c("Neutralni przetrwali", "Nikt nie dziękuje, nikt nie mści się. Dwór pamięta jednak chłód sekty.", favor(-10),
                needs = listOf(cond(CondType.FLAG_BELOW, "civil_side", 1))))
    )

    private val npcs = listOf(
        // Alchemiczka Mei
        first("npc_mei_1", "npc_mei", 8, "Wędrowna alchemiczka",
            "Kobieta z kociołkiem na plecach prosi o nocleg w zamian za lekcję warzenia.",
            c("Przyjmij ją w Sali Alchemii", "Mei pokazuje uczniom, jak czytać kolor dymu.", morale(5), next("npc_mei_2", 7)),
            c("Odeślij ją", "Mei wzrusza ramionami i odchodzi.")),
        link("npc_mei_2", "npc_mei", "Wybuch w kotle",
            "Eksperyment Mei wymknął się spod kontroli. Kocioł pękł, ale na dnie lśnią kryształy.",
            c("Zbierz kryształy", "Kryształy są ciepłe jak żywe serce.", item(GameContent.FLAME_CRYSTAL, 2), next("npc_mei_3", 10)),
            c("Każ jej zapłacić za szkody", "Mei płaci i odchodzi obrażona.", stones(200))),
        link("npc_mei_3", "npc_mei", "Pożegnanie alchemiczki",
            "Mei wyrusza na południe. Zostawia notatki i dwie pigułki „na szczęście”.",
            c("Przyjmij dar", "W notatkach jest kilka sprytnych sztuczek.", item(GameContent.BREAKTHROUGH_PILL, 2), karma(3))),
        // Pijany żebrak
        first("npc_beggar_1", "npc_beggar", 10, "Pijany żebrak",
            "Żebrak zatacza się pod bramą i żąda wina „dla starego mistrza”.",
            c("Daj mu wina i ciepłą strawę", "Żebrak chrapie do rana. O świcie śmieje się jak młodzieniec.", stones(-30), karma(4), next("npc_beggar_2", 5)),
            c("Przegoń go", "Żebrak odchodzi, nucąc coś o ślepcach.", karma(-2))),
        link("npc_beggar_2", "npc_beggar", "Lekcja pijanego mistrza",
            "Żebrak pokazuje uczniom chwiejny krok, który wymyka się każdemu ciosowi.",
            c("Ucz się z pokorą", "Uczniowie przewracają się cały dzień – i coś zaczyna im wychodzić.", morale(6), next("npc_beggar_3", 8)),
            c("Uznaj to za błazenadę", "Żebrak macha ręką i znika.")),
        link("npc_beggar_3", "npc_beggar", "Bractwo Pijanego Nieśmiertelnego",
            "Okazuje się, że żebrak to starszy Bractwa Pijanego Nieśmiertelnego. Bractwo pamięta gościnność.",
            c("Wypij z nim za przyjaźń", "Bractwo staje się przyjacielem sekty.", rival("sect_drunken", 30), rep(15))),
        // Sierota
        first("npc_orphan_1", "npc_orphan", 6, "Dziewczynka w deszczu",
            "Mała sierota stoi w deszczu przed bramą. Wokół niej krople nie spadają na ziemię.",
            c("Przyjmij ją pod opiekę", "Dziewczynka zasypia przy ogniu. Krople nad nią wiszą w powietrzu.", stones(-50), karma(5), next("npc_orphan_2", 12)),
            c("Wskaż jej drogę do wioski", "Dziewczynka odchodzi bez słowa.")),
        link("npc_orphan_2", "npc_orphan", "Dar wody",
            "Sierota przywołała źródło na środku dziedzińca. Uczniowie piją wodę pełną Qi.",
            c("Zbuduj wokół źródła kapliczkę", "Kapliczka staje się ulubionym miejscem medytacji.", morale(10), stones(-200), next("npc_orphan_3", 20)),
            c("Zakryj źródło, by nie budziło chciwości", "Źródło milknie.", karma(2))),
        link("npc_orphan_3", "npc_orphan", "Krewni z Pałacu Mórz",
            "Do bramy pukają posłańcy Pałacu Smoczego Króla – dziewczynka jest zaginioną księżniczką.",
            c("Oddaj ją rodzinie z honorami", "Pałac Mórz zostaje dłużnikiem sekty.", rival("sect_ocean", 40), stones(1500), rep(30))),
        // Bard
        first("npc_bard_1", "npc_bard", 4, "Wędrowny bard",
            "Bard z lutnią pyta o historię sekty – chce ułożyć pieśń.",
            c("Opowiedz mu o założycielu", "Bard słucha do późnej nocy.", next("npc_bard_2", 9)),
            c("Nie mamy czasu na pieśni", "Bard odchodzi do karczmy.")),
        link("npc_bard_2", "npc_bard", "Pieśń w karczmach",
            "W karczmach całej prowincji śpiewa się o twojej sekcie. Do bramy pukają kandydaci.",
            c("Zaproś barda na ucztę", "Bard pije, śpiewa i obiecuje kolejną zwrotkę.", stones(-100), rep(25), next("npc_bard_3", 15)),
            c("Wystarczy", "Pieśń żyje własnym życiem.", rep(10))),
        link("npc_bard_3", "npc_bard", "Pieśń na dworze",
            "Bard zaśpiewał pieśń przed Cesarzem. Dwór zna teraz imię sekty.",
            c("Wyślij podziękowanie na dwór", "Dwór przyjmuje list z uśmiechem.", favor(10), rep(20))),
        // Mnich
        first("npc_monk_1", "npc_monk", 15, "Milczący mnich",
            "Mnich z Klasztoru Wadżry siada pod bramą i milczy od trzech dni.",
            c("Usiądź obok niego w milczeniu", "Trzeciego dnia mnich się uśmiecha.", karma(3), next("npc_monk_2", 6)),
            c("Zapytaj, czego chce", "Mnich wstaje i odchodzi.")),
        link("npc_monk_2", "npc_monk", "Sutra serca",
            "Mnich recytuje sutrę. Uczniowie, którzy słuchają, czują spokój demona serca.",
            c("Poproś o naukę dla uczniów", "Sala medytacji jest pełna ciszy.", morale(8), karma(5), next("npc_monk_3", 12)),
            c("Podziękuj i pożegnaj", "Mnich kłania się.", karma(2))),
        link("npc_monk_3", "npc_monk", "Zaproszenie z klasztoru",
            "Klasztor Wadżry zaprasza dwóch uczniów na zimowe odosobnienie.",
            c("Przyjmij zaproszenie", "Uczniowie wracają z kwiatem zrozumienia.", item("Kwiat Niebiańskiego Zrozumienia", 3), rival("sect_shaolin", 20))),
        // Wygnany książę
        first("npc_prince_1", "npc_prince", 25, "Wygnany książę",
            "Młodzieniec w zniszczonym płaszczu z cesarskim haftem prosi o azyl.",
            c("Daj mu schronienie", "Książę obiecuje, że nie zapomni.", favor(-10), karma(4), next("npc_prince_2", 10)),
            c("Wydaj go strażom cesarskim", "Dwór nagradza lojalność.", favor(20), karma(-10), stones(500))),
        link("npc_prince_2", "npc_prince", "Siepacze z dworu",
            "Do bramy przychodzą siepacze. Żądają wydania księcia.",
            c("Odmów i chroń gościa", "Siepacze wycofują się, ale twój czempion jest ranny.", hurt(25), favor(-10), next("npc_prince_3", 15)),
            c("Wydaj go teraz", "Książę patrzy na ciebie bez słowa.", favor(10), karma(-8))),
        link("npc_prince_3", "npc_prince", "Wdzięczność księcia",
            "Książę wraca na dwór – oczyszczony z zarzutów. Pamięta, kto go ukrył.",
            c("Przyjmij jego pieczęć", "Pieczęć księcia otwiera drzwi na dworze.", favor(30), stones(1200), rep(25))),
        // Lisi duch
        first("npc_fox_1", "npc_fox", 12, "Lisica w blasku księżyca",
            "Nocą na murze siedzi biała lisica o dziewięciu ogonach i przygląda się uczniom.",
            c("Zostaw jej miseczkę mleka", "Rano miseczka jest pusta, a na murze leży srebrny włos.", karma(3), next("npc_fox_2", 5)),
            c("Przegoń ją talizmanem", "Lisica znika z chichotem.", karma(-3)),
            extra = listOf(cond(CondType.TIME_OF_DAY, "NIGHT"))),
        link("npc_fox_2", "npc_fox", "Psoty lisicy",
            "Ktoś poprzestawiał wszystkie zioła w magazynie i zjadł rybę kucharza.",
            c("Roześmiej się", "Uczniowie śmieją się razem z tobą.", morale(6), next("npc_fox_3", 10)),
            c("Rozstaw pułapki", "Pułapki łapią tylko kucharza.", morale(-3))),
        link("npc_fox_3", "npc_fox", "Dar lisiego ducha",
            "Lisica przybiera ludzką postać i zostawia zawiniątko w podzięce za gościnę.",
            c("Rozwiń zawiniątko", "W środku leżą rzadkie zioła z gór, gdzie nie chodzą ludzie.", item("Śnieżny Lotos Oświecenia", 5), item("Kwiat Niebiańskiego Zrozumienia", 2))),
        // Kowal-tułacz
        first("npc_smith_1", "npc_smith", 18, "Kowal-tułacz",
            "Barczysty kowal szuka kuźni, w której płomień „ma duszę”.",
            c("Pokaż mu Boską Kuźnię", "Kowal kiwa głową z uznaniem.", next("npc_smith_2", 6)),
            c("Kuźnia jest tylko dla uczniów", "Kowal odchodzi.")),
        link("npc_smith_2", "npc_smith", "Ostrze z gwiezdnego żelaza",
            "Kowal prosi o gwiezdne żelazo, by wykuć „ostrze, które pamięta niebo”.",
            c("Daj mu 5× Gwiezdne Żelazo", "Kowal pracuje trzy dni i trzy noce.", item("Gwiezdne Żelazo", -5), next("npc_smith_3", 8),
                needs = listOf(cond(CondType.HAS_ITEM, "Gwiezdne Żelazo", 5))),
            c("Nie mamy żelaza na zbyciu", "Kowal rusza dalej.")),
        link("npc_smith_3", "npc_smith", "Ostrze gotowe",
            "Kowal oddaje ostrze sekcie i odchodzi bez zapłaty. Na klindze lśnią gwiazdy.",
            c("Przyjmij dar", "Ostrze trafia do skarbca – i nadwyżka żelaza wraca z nim.", item("Gwiezdne Żelazo", 8), item("Starożytna Ruda Pustki", 2), rep(10))),
        // Uczony
        first("npc_scholar_1", "npc_scholar", 30, "Uczony z mokrymi zwojami",
            "Uczony suszy zwoje na słońcu. Twierdzi, że wyłowił je z Zatopionej Biblioteki.",
            c("Pomóż mu je ocalić", "Uczniowie ostrożnie rozkładają zwoje.", stones(-150), next("npc_scholar_2", 7)),
            c("Kup najlepszy zwój", "Uczony sprzedaje za grosze – zwój jest prawie nieczytelny.", stones(-100), item("Kwiat Niebiańskiego Zrozumienia", 1))),
        link("npc_scholar_2", "npc_scholar", "Odczytane runy",
            "Runy opisują metodę kultywacji zapomnianą przez wieki. Brakuje ostatniego fragmentu.",
            c("Wyślij uczniów z uczonym po resztę", "Wyprawa rusza do ruin.", stones(-200), next("npc_scholar_3", 12)),
            c("Zadowól się tym, co jest", "Fragmenty trafiają do archiwum.", karma(1))),
        link("npc_scholar_3", "npc_scholar", "Brakujący fragment",
            "Uczniowie wracają z ostatnim fragmentem. Uczony zostawia sekcie kompletny zwój.",
            c("Przyjmij zwój", "Zwój trafia do biblioteki sekty.", item("Zwój: Kanon Siedmiu Kling", 1), rep(15))),
        // Zabójczyni
        first("npc_assassin_1", "npc_assassin", 40, "Zabójczyni bez imienia",
            "Nocą w twoim gabinecie stoi kobieta w czerni. Ktoś zapłacił jej za twoją śmierć – ale ona chce rozmawiać.",
            c("Wysłuchaj jej", "Zleceniodawcą jest Sekta Cienia. Zabójczyni chce odejść z ich służby.", next("npc_assassin_2", 4)),
            c("Wezwij straż", "Zabójczyni znika przez okno.", rival("sect_shadow", -10))),
        link("npc_assassin_2", "npc_assassin", "Cena wolności",
            "Zabójczyni prosi o 800 kamieni na nowe życie. W zamian zdradzi plany Sekty Cienia.",
            c("Zapłać", "Zabójczyni opowiada o kryjówkach i ludziach Sekty Cienia.", stones(-800), next("npc_assassin_3", 6),
                needs = listOf(cond(CondType.MIN_STONES, value = 800))),
            c("Odmów", "Zabójczyni znika w ciemności.")),
        link("npc_assassin_3", "npc_assassin", "Zdradzone plany",
            "Plany Sekty Cienia są w twoich rękach. Możesz je wykorzystać albo przekazać innym sektom.",
            c("Przekaż je Wudang", "Wudang dziękuje i uderza w Cień.", rival("sect_wudang", 25), rival("sect_shadow", -20), rep(20)),
            c("Wykorzystaj je sam", "Uczniowie łupią skrytkę Cienia.", stones(1500), rival("sect_shadow", -30), karma(-5)))
    )

    val events: List<ScriptEvent> = empire + npcs
}
