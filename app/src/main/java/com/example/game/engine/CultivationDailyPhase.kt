package com.example.game.engine

/** Etap 3 – raz na dobę: toksyczność pigułek spada, kończą się odosobnienia (szansa na wgląd). */
object CultivationDailyPhase : TickPhase {
    const val TOXICITY_DECAY = 2
    const val SECLUSION_INSIGHT_CHANCE = 15

    override fun run(frame: TickFrame) {
        val s = frame.before
        if (!s.e3Initialized || frame.day == s.currentDay) return
        val rng = frame.ctx.rng("cultivation-daily")
        frame.disciples = frame.disciples.map { d0 ->
            var d = d0
            if (d.toxicity > 0) d = d.copy(toxicity = (d.toxicity - TOXICITY_DECAY).coerceAtLeast(0))
            if (d.secludedUntilDay in 1 until frame.day) {
                d = d.copy(secludedUntilDay = 0)
                if (rng.nextInt(100) < SECLUSION_INSIGHT_CHANCE) {
                    d = d.copy(
                        daoPoints = d.daoPoints + 3,
                        laws = d.laws.mapValues { (it.value + Meditation.INSIGHT_LAW_PERCENT).coerceAtMost(100.0) }
                    )
                    if (frame.notice == null) frame.notice = "${d.name} wychodzi z odosobnienia z wglądem w Dao (+3 pkt Dao)."
                } else if (frame.notice == null) {
                    frame.notice = "${d.name} kończy odosobnienie."
                }
            }
            d
        }
    }
}
