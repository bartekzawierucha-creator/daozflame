// Test zgodności z silnikiem Kotlin: node --experimental-strip-types supabase/functions/_shared/duel.parity.test.ts
import { readFileSync } from "node:fs";
import { resolve } from "./duel.ts";

const cases = JSON.parse(readFileSync(new URL("../../../app/src/test/resources/pvp/golden_duels.json", import.meta.url), "utf8"));
let bad = 0;
for (const c of cases) {
  const d = resolve(c.a, c.b, BigInt(c.seed));
  const same = d.winnerA === c.winnerA && d.bouts.every((b, i) => b.winnerA === c.bouts[i].winnerA && b.rounds === c.bouts[i].rounds);
  if (!same) { bad++; console.error("różnica dla ziarna", c.seed); }
}
console.log(`zgodne: ${cases.length - bad}/${cases.length}`);
if (bad > 0) process.exit(1);
