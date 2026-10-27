package com.example.game.content

import com.example.game.engine.GameContent
import com.example.model.CondType
import com.example.model.Condition
import com.example.model.Effect
import com.example.model.EffectType
import com.example.model.ScriptChoice
import com.example.model.ScriptEvent

/**
 * Etap 5: łańcuchy zdarzeń na silniku skryptów. Każde ogniwo poza pierwszym wypada tylko z zaplanowania.
 * Walidator ([com.example.game.engine.EventScripts.validate]) sprawdza cele i pętle.
 */
object ScriptCatalog {

    private fun cond(type: CondType, key: String = "", value: Int = 0) = Condition(type, key, value)
    private fun stones(v: Int) = Effect(EffectType.STONES, value = v)
    private fun rep(v: Int) = Effect(EffectType.REPUTATION, value = v)
    private fun karma(v: Int) = Effect(EffectType.KARMA, value = v)
    private fun morale(v: Int) = Effect(EffectType.MORALE, value = v)
    private fun item(name: String, n: Int) = Effect(EffectType.ITEM, name, n)
    private fun flag(name: String, v: Int = 1) = Effect(EffectType.SET_FLAG, name, v)
    private fun next(id: String, days: Int) = Effect(EffectType.SCHEDULE, id, days)
    private fun rival(id: String, v: Int) = Effect(EffectType.RIVAL_RELATION, id, v)

    private fun choice(text: String, outcome: String, vararg effects: Effect, needs: List<Condition> = emptyList()) =
        ScriptChoice(text, outcome, needs, effects.toList())

    val events: List<ScriptEvent> = listOf(
        // ── Łańcuch: Ranny pustelnik ─────────────────────────────────────────
        ScriptEvent(
            id = "chain_hermit_1", chain = "npc_hermit", once = true,
            title = "Ranny pustelnik u bram",
            description = "Starzec w podartej szacie osuwa się pod bramą. Z jego rany sączy się czarne Qi, a w dłoni ściska złamany miecz.",
            conditions = listOf(cond(CondType.MIN_DAY, value = 5)),
            choices = listOf(
                choice("Opatrz go w sali uzdrowień", "Starzec zasypia spokojnie. O świcie zniknął, zostawiając wyryty na murze znak.",
                    stones(-120), karma(5), flag("hermit_helped"), next("chain_hermit_2", 6)),
                choice("Przeszukaj go i wyrzuć za bramę", "W sakwie były tylko suche zioła. Starzec spojrzał na ciebie bez gniewu i odszedł.",
                    item("Trawa Wiecznej Wiosny", 3), karma(-8)),
                choice("Zamknij bramę – to nie sprawa sekty", "Brama się zamyka. Wieczorem słychać daleki krzyk.")
            )
        ),
        ScriptEvent(
            id = "chain_hermit_2", chain = "npc_hermit", random = false,
            title = "Znak na murze",
            description = "Znak zostawiony przez pustelnika rozjarzył się w pełni księżyca. W blasku widać fragment formacji miecza – i ślad pościgu.",
            choices = listOf(
                choice("Każ uczniom studiować znak", "Uczniowie medytują przy murze. Kilku z nich pojmuje ruch, którego nikt ich nie uczył.",
                    morale(8), item("Kwiat Niebiańskiego Zrozumienia", 1), next("chain_hermit_3", 8)),
                choice("Zetrzyj znak, zanim ktoś go zobaczy", "Kamień jest znów gładki. Pościg, jeśli był, nie ma czego szukać.", karma(-2))
            )
        ),
        ScriptEvent(
            id = "chain_hermit_3", chain = "npc_hermit", random = false,
            title = "Ścigający z Sekty Cienia",
            description = "Trzech zabójców w czarnych maskach żąda wydania „zdrajcy”, którego sekta ukrywała. Ich przywódca pyta, gdzie poszedł starzec.",
            choices = listOf(
                choice("Nie wiemy nic i nic nie powiemy", "Zabójcy odchodzą z groźbą na ustach. Pustelnik wraca nocą, by podziękować.",
                    rival("sect_shadow", -15), rep(20), next("chain_hermit_4", 4)),
                choice("Wskaż kierunek, w którym odszedł", "Zabójcy kłaniają się i znikają. Sekta Cienia zapamięta tę przysługę.",
                    rival("sect_shadow", 20), karma(-15), stones(300)),
                choice("Wyzwij ich przywódcę na pojedynek", "Twój najlepszy uczeń przepędza zabójców. Wieść o tym rozchodzi się po krainie.",
                    rep(45), rival("sect_shadow", -30), needs = listOf(cond(CondType.MIN_REPUTATION, value = 400)))
            )
        ),
        ScriptEvent(
            id = "chain_hermit_4", chain = "npc_hermit", random = false,
            title = "Dar pustelnika",
            description = "Starzec okazuje się ostatnim mistrzem zniszczonej Sekty Złamanego Miecza. W podzięce zostawia to, co ocalało.",
            choices = listOf(
                choice("Przyjmij dar z ukłonem", "Pustelnik odchodzi w góry. W sekcie zostaje jego zwój i jego spokój.",
                    item("Zwój: Sutra Serca Miecza", 1), karma(10), flag("hermit_done")),
                choice("Zaproponuj mu miejsce starszego w sekcie", "Starzec długo milczy, w końcu kiwa głową. Uczniowie zyskują nauczyciela.",
                    morale(15), rep(30), flag("hermit_elder"))
            )
        ),

        // ── Łańcuch: Pożyczka kupca ──────────────────────────────────────────
        ScriptEvent(
            id = "chain_loan_1", chain = "merchant_loan", once = true,
            title = "Kupiec w tarapatach",
            description = "Właściciel karawany z Jadeitowego Miasta prosi o pożyczkę: bandyci zabrali mu towar, a dłużnicy czekają.",
            conditions = listOf(cond(CondType.MIN_DAY, value = 12), cond(CondType.MIN_STONES, value = 800)),
            choices = listOf(
                choice("Pożycz 500 kamieni", "Kupiec przysięga na przodków, że wróci z odsetkami.", stones(-500), next("chain_loan_2", 15)),
                choice("Odmów uprzejmie", "Kupiec odjeżdża przygnębiony.")
            )
        ),
        ScriptEvent(
            id = "chain_loan_2", chain = "merchant_loan", random = false,
            title = "Powrót kupca",
            description = "Kupiec wraca z odbudowaną karawaną. Skrzynia ze srebrem i nowe znajomości – ale czy zwróci wszystko?",
            choices = listOf(
                choice("Przyjmij dług z odsetkami", "Kupiec oddaje 750 kamieni i obiecuje polecać sektę.", stones(750), rep(10)),
                choice("Daruj dług w zamian za stały rabat", "Gildia Kupców zapamięta hojność sekty.", rival("sect_guild", 25), karma(5), flag("guild_discount")),
                choice("Zażądaj podwójnej kwoty", "Kupiec płaci, ale po krainie krąży plotka o chciwości sekty.",
                    stones(1000), rep(-25), karma(-10), needs = listOf(cond(CondType.MIN_REPUTATION, value = 300)))
            )
        ),

        // ── Łańcuch: Szept o płomieniu ───────────────────────────────────────
        ScriptEvent(
            id = "chain_flame_1", chain = "flame_rumor", once = true,
            title = "Wędrowiec z poparzonymi dłońmi",
            description = "Wędrowiec opowiada o miejscu w górach, gdzie śnieg topnieje w środku zimy, a kamienie świecą nocą.",
            conditions = listOf(cond(CondType.MIN_DAY, value = 20)),
            choices = listOf(
                choice("Kup od niego mapę", "Mapa jest szkicowa, ale trop wydaje się prawdziwy. Trzeba poczekać na zimę.",
                    stones(-200), flag("flame_rumor"), next("chain_flame_2", 10)),
                choice("Wyśmiej jego bajki", "Wędrowiec wzrusza ramionami i odchodzi.")
            )
        ),
        ScriptEvent(
            id = "chain_flame_2", chain = "flame_rumor", random = false,
            title = "Ciepły śnieg",
            description = "Zwiadowcy wracają z gór: miejsce z mapy istnieje. Pod lodem tli się okruch płomienia, ale nie da się go wydobyć bez ofiary Qi.",
            choices = listOf(
                choice("Wyślij uczniów z kryształami", "Uczniowie wracają wyczerpani, ale z kryształem pełnym żaru.",
                    item(GameContent.FLAME_CRYSTAL, 2), morale(-5), flag("flame_rumor", 2)),
                choice("Zostaw miejsce na później", "Mapa trafia do archiwum. Może kiedyś ktoś po nią sięgnie.")
            )
        )
    )
}
