package com.example.game.engine

/** Etap 4 – raz na dobę: rozbudowa żyły, ceny targu, karawany, tablica kontraktów. */
object EconomyDailyPhase : TickPhase {
    override fun run(frame: TickFrame) {
        val s = frame.before
        if (!s.e4Initialized || frame.day == s.currentDay) return
        if (s.veinUpgradeDoneDay in 1..frame.day) {
            frame.veinLevel = (s.veinLevel + 1).coerceAtMost(Vein.MAX_LEVEL)
            frame.veinUpgradeDoneDay = 0
            frame.notice = frame.notice ?: "Żyła duchowa pogłębiona: ${Vein.name(frame.veinLevel)} (Qi +${((Vein.qiMultiplier(frame.veinLevel) - 1) * 100).toInt()}%)."
        }
        Market.daily(frame)
        Contracts.daily(frame)
    }
}
