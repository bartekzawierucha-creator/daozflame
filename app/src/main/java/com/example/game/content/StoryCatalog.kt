package com.example.game.content

import com.example.game.engine.EventScripts
import com.example.game.engine.GameContent
import com.example.model.CondType
import com.example.model.Condition
import com.example.model.Effect
import com.example.model.EffectType
import com.example.model.ScriptChoice
import com.example.model.ScriptEvent

/**
 * Etap 7: główny wątek – „Demon pod Górą”. Cztery akty, Kult Krwawego Księżyca, Wybraniec Losu
 * i trzy zakończenia zależne od karmy. Ogniwa planuje [com.example.game.engine.Story] (łańcuch „system”),
 * więc fabuła nie utknie, gdy kluczowy uczeń zginie – Wybraniec zawsze ma następcę.
 */
object StoryCatalog {
    const val ACT = "story_act"
    const val ENDING = "story_ending"
    const val SEAL = "story_seal"

    private fun cond(type: CondType, key: String = "", value: Int = 0) = Condition(type, key, value)
    private fun stones(v: Int) = Effect(EffectType.STONES, value = v)
    private fun rep(v: Int) = Effect(EffectType.REPUTATION, value = v)
    private fun karma(v: Int) = Effect(EffectType.KARMA, value = v)
    private fun morale(v: Int) = Effect(EffectType.MORALE, value = v)
    private fun item(name: String, n: Int) = Effect(EffectType.ITEM, name, n)
    private fun flag(name: String, v: Int = 1) = Effect(EffectType.SET_FLAG, name, v)
    private fun add(name: String, v: Int = 1) = Effect(EffectType.ADD_FLAG, name, v)
    private fun next(id: String, days: Int) = Effect(EffectType.SCHEDULE, id, days)
    private fun rival(id: String, v: Int) = Effect(EffectType.RIVAL_RELATION, id, v)
    private fun hurt(v: Int) = Effect(EffectType.INJURE_CHAMPION, value = v)

    private fun c(text: String, outcome: String, vararg effects: Effect, needs: List<Condition> = emptyList()) =
        ScriptChoice(text, outcome, needs, effects.toList())

    private fun ev(id: String, title: String, desc: String, vararg choices: ScriptChoice) =
        ScriptEvent(id, title, desc, choices = choices.toList(), random = false, chain = EventScripts.SYSTEM_CHAIN)

    val events: List<ScriptEvent> = listOf(
        // ── Akt I: Drżenie góry ──────────────────────────────────────────────
        ev("story_1_1", "Akt I · Drżenie góry",
            "W nocy góra sekty zadrżała. W najgłębszej grocie pękła pieczęć założyciela, a z pęknięcia sączy się czerwone światło.",
            c("Zbadaj grotę osobiście", "W grocie czuć oddech czegoś ogromnego. Pieczęć trzyma – na razie.", add(SEAL, 1), next("story_1_2", 5)),
            c("Wyślij uczniów na straż", "Uczniowie czuwają przy pęknięciu. Nikt nie śpi spokojnie.", morale(-3), next("story_1_2", 5))),
        ev("story_1_2", "Akt I · Szept spod ziemi",
            "Wybraniec Losu budzi się z krzykiem. Śnił, że ktoś pod górą wypowiada jego imię – i że ten głos zna imię założyciela.",
            c("Wysłuchaj snu do końca", "Głos mówi o „Pieczęci Trzech Kluczy”. Wybraniec pamięta każde słowo.", flag("story_three_keys"), next("story_1_3", 6)),
            c("Każ mu medytować i zapomnieć", "Wybraniec milczy, ale sny wracają.", karma(-2), next("story_1_3", 6))),
        ev("story_1_3", "Akt I · Posłaniec Krwawego Księżyca",
            "Do bramy przychodzi kapłan w czerwonej masce. Kult Krwawego Księżyca proponuje „pomoc” w podtrzymaniu pieczęci – za opłatą.",
            c("Odpraw go z niczym", "Kapłan uśmiecha się pod maską. „Wrócimy, gdy księżyc będzie pełny.”", karma(5), flag("story_cult_enemy"), next("story_2_1", 1)),
            c("Przyjmij pomoc Kultu", "Kultyści rysują krwawe znaki wokół groty. Drżenie ustaje.", stones(800), karma(-15), flag("story_cult_ally"), next("story_2_1", 1))),

        // ── Akt II: Trzy klucze ──────────────────────────────────────────────
        ev("story_2_1", "Akt II · Pierwszy klucz",
            "Archiwum mówi, że pierwszy klucz pieczęci spoczywa w Grobowcu Generała Miecza. Strzegą go duchy dawnych uczniów sekty.",
            c("Wyślij Wybrańca", "Wybraniec wraca z kluczem z jadeitu – i z raną, która długo się goi.", flag(ACT, 2), hurt(30), add(SEAL, 1), next("story_2_2", 8)),
            c("Zapłać łowcom skarbów", "Łowcy przynoszą klucz. Duchy przodków patrzą z wyrzutem.", flag(ACT, 2), stones(-1000), karma(-5), add(SEAL, 1), next("story_2_2", 8),
                needs = listOf(cond(CondType.MIN_STONES, value = 1000)))),
        ev("story_2_2", "Akt II · Drugi klucz",
            "Drugi klucz przechowuje Sekta Wudang. Mistrz Zhang odda go tylko sekcie, której ufa.",
            c("Poproś z pokorą", "Mistrz Zhang przygląda się długo i oddaje klucz.", rival("sect_wudang", 10), add(SEAL, 1), next("story_2_3", 8)),
            c("Wykradnij klucz", "Klucz jest twój. Wudang już nigdy nie spojrzy na sektę tak samo.", rival("sect_wudang", -40), karma(-15), add(SEAL, 1), next("story_2_3", 8))),
        ev("story_2_3", "Akt II · Trzeci klucz",
            "Trzeci klucz ma Kult Krwawego Księżyca. Kapłan proponuje wymianę: klucz za jednego ucznia „do służby”.",
            c("Zaatakuj świątynię Kultu", "Bitwa jest krwawa, ale klucz wraca do sekty.", hurt(40), karma(10), rep(40), add(SEAL, 1), next("story_3_1", 10)),
            c("Oddaj ucznia Kultowi", "Uczeń odchodzi bez słowa. W sekcie panuje cisza.", morale(-20), karma(-25), add(SEAL, 1), next("story_3_1", 10)),
            c("Przechytrz kapłana fałszywym uczniem", "Iluzja wystarcza na tyle długo, by zabrać klucz.", karma(-3), add(SEAL, 1), next("story_3_1", 10),
                needs = listOf(cond(CondType.MIN_REPUTATION, value = 500)))),

        // ── Akt III: Krwawy księżyc ──────────────────────────────────────────
        ev("story_3_1", "Akt III · Zaćmienie",
            "Księżyc zabarwił się na czerwono. Kult zbiera armię u podnóża góry – chcą zerwać pieczęć i wypuścić demona.",
            c("Zwołaj sojuszników", "Sekty, które ci ufają, przysyłają wojowników.", flag(ACT, 3), rep(30), next("story_3_2", 5)),
            c("Umocnij formację i czekaj", "Wielka Formacja jaśnieje. Uczniowie stają na murach.", flag(ACT, 3), morale(5), next("story_3_2", 5))),
        ev("story_3_2", "Akt III · Bitwa pod górą",
            "Kult uderza. Wybraniec stoi w pierwszym szeregu, a demon pod górą śmieje się coraz głośniej.",
            c("Walcz do ostatka", "Kult zostaje odparty. Ranni zapełniają salę uzdrowień.", hurt(50), karma(5), rep(60), next("story_3_3", 7)),
            c("Złóż ofiarę z krwi, by wzmocnić uczniów", "Uczniowie walczą jak demony. Zwycięstwo smakuje żelazem.", karma(-20), rep(40), next("story_3_3", 7))),
        ev("story_3_3", "Akt III · Prawda o założycielu",
            "W ruinach świątyni Kultu leży dziennik założyciela. Demon pod górą był jego bratem – zapieczętowanym za to, że chciał przekroczyć Niebo.",
            c("Ukryj prawdę przed uczniami", "Sekret zostaje z tobą. Ciężar rośnie.", karma(-5), next("story_4_1", 15)),
            c("Wyznaj prawdę całej sekcie", "Uczniowie są wstrząśnięci, ale lojalni.", morale(-5), karma(8), next("story_4_1", 15))),

        // ── Akt IV: Pod górą ─────────────────────────────────────────────────
        ev("story_4_1", "Akt IV · Zejście",
            "Trzy klucze świecą. Wybraniec Losu schodzi z tobą pod górę, do miejsca, gdzie demon czeka od tysiąca lat.",
            c("Idź dalej", "Na dnie groty stoi postać o twarzy założyciela. Czeka na twoją decyzję.", flag(ACT, 4), next("story_4_2", 1))),
        ev("story_4_2", "Akt IV · Wybór",
            "Demon mówi: „Mogę dać twojej sekcie moc, o jakiej nie śniłeś. Albo możesz spróbować mnie zniszczyć – i zapłacić cenę.”",
            c("Zapieczętuj go na wieki światłem Dao", "Wybraniec składa swoją kultywację w ofierze. Góra milknie na zawsze.", flag(ACT, 5),
                flag(ENDING, 1), rep(200), karma(20), item(GameContent.BREAKTHROUGH_PILL, 10), needs = listOf(cond(CondType.MIN_KARMA, value = 50))),
            c("Zawrzyj pakt i przyjmij jego moc", "Demon wnika w serce sekty. Od dziś to ty jesteś postrachem kontynentu.", flag(ACT, 5),
                flag(ENDING, 2), rep(100), stones(20_000), needs = listOf(cond(CondType.MAX_KARMA, value = -50))),
            c("Odnów pieczęć założyciela i odejdź", "Pieczęć znów trzyma. Ty wiesz, co jest pod górą – i to wystarczy.", flag(ACT, 5),
                flag(ENDING, 3), rep(120), stones(5_000), needs = listOf(cond(CondType.MIN_KARMA, value = -49), cond(CondType.MAX_KARMA, value = 49))),
            c("Zapieczętuj go mimo skazy na sercu", "Pieczęć jest słabsza, niż chciałeś – ale trzyma.", flag(ACT, 5),
                flag(ENDING, 3), rep(80), needs = listOf(cond(CondType.MIN_KARMA, value = 50), cond(CondType.FLAG_AT_LEAST, "story_cult_ally", 1)))),

        // ── Wybraniec Losu ───────────────────────────────────────────────────
        ev("story_chosen", "Wybraniec Losu",
            "Niebo zaznaczyło jednego z uczniów: znamię w kształcie góry pojawiło się na jego dłoni. Los sekty splata się z jego losem.",
            c("Otocz go opieką", "Wybraniec czuje na sobie wzrok całej sekty.", morale(5))),
        ev("story_new_chosen", "Znamię przechodzi dalej",
            "Wybraniec Losu odszedł, ale znamię nie zgasło – pojawiło się na dłoni innego ucznia. Opowieść toczy się dalej.",
            c("Przyjmij wolę Nieba", "Nowy Wybraniec bierze na siebie ciężar poprzednika.", morale(-5)))
    )
}
