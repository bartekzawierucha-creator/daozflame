// Edge Function: weryfikacja pojedynku Sieci Dao.
// Klient wysyła { attackerId, defenderId, seed } – serwer pobiera oba cienie z bazy, sprawdza zakresy,
// odtwarza walkę z ziarna i dopiero wtedy zapisuje wynik i nowe rankingi.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";
import { resolve, valid, type ShadowTeam } from "../_shared/duel.ts";

const K = 32;
function expectedPermille(ra: number, rb: number) {
  const diff = Math.max(-800, Math.min(800, rb - ra));
  return Math.trunc(1000 / (1 + Math.pow(10, diff / 400)));
}
function newRating(r: number, opp: number, won: boolean) {
  return Math.max(100, r + Math.trunc(K * ((won ? 1000 : 0) - expectedPermille(r, opp)) / 1000));
}

Deno.serve(async (req) => {
  const { attackerId, defenderId, seed } = await req.json();
  const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
  const { data: rows, error } = await supabase.from("shadows").select("*").in("id", [attackerId, defenderId]);
  if (error || !rows || rows.length !== 2) return new Response(JSON.stringify({ error: "shadow not found" }), { status: 404 });
  const a = rows.find((r) => r.id === attackerId)!.team as ShadowTeam;
  const b = rows.find((r) => r.id === defenderId)!.team as ShadowTeam;
  if (!valid(a) || !valid(b)) return new Response(JSON.stringify({ error: "invalid stats" }), { status: 422 });
  const duel = resolve(a, b, BigInt(seed));
  const ra = newRating(a.rating, b.rating, duel.winnerA);
  const rb = newRating(b.rating, a.rating, !duel.winnerA);
  await supabase.from("duels").insert({ attacker_id: attackerId, defender_id: defenderId, seed: String(seed), attacker_won: duel.winnerA, bouts: duel.bouts });
  await supabase.from("shadows").update({ rating: ra }).eq("id", attackerId);
  await supabase.from("shadows").update({ rating: rb }).eq("id", defenderId);
  return new Response(JSON.stringify({ duel, attackerRating: ra, defenderRating: rb }), { headers: { "content-type": "application/json" } });
});
