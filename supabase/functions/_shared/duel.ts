// Port silnika pojedynków Sieci Dao (app/.../game/engine/DaoNet.kt) – musi dawać IDENTYCZNE wyniki.
// Arytmetyka całkowita + SplitMix64 na BigInt. Zmiana tutaj = zmiana w DaoNet.kt (i odwrotnie).

export interface ShadowFighter { name: string; realm: number; stage: number; attack: number; defense: number; hp: number }
export interface ShadowTeam { id: string; sectName: string; rating: number; era: number; fighters: ShadowFighter[] }
export interface Bout { a: number; b: number; winnerA: boolean; rounds: number }
export interface Duel { winnerA: boolean; bouts: Bout[]; seed: string }

const GOLDEN_GAMMA = BigInt.asIntN(64, 0x9E3779B97F4A7C15n);
const MIX_1 = BigInt.asIntN(64, 0xBF58476D1CE4E5B9n);
const MIX_2 = BigInt.asIntN(64, 0x94D049BB133111EBn);
const CRIT_CHANCE = 28n;
const MAX_ROUNDS = 30;
const TEAM_SIZE = 3;
const REALMS = 10;

const u64 = (x: bigint) => BigInt.asUintN(64, x);
const i64 = (x: bigint) => BigInt.asIntN(64, x);
const ushr = (x: bigint, n: bigint) => u64(x) >> n;

function mix(value: bigint): bigint {
  let z = i64(value);
  z = i64((z ^ i64(ushr(z, 30n))) * MIX_1);
  z = i64((z ^ i64(ushr(z, 27n))) * MIX_2);
  return i64(z ^ i64(ushr(z, 31n)));
}

export class GameRng {
  private state: bigint;
  constructor(seed: bigint) { this.state = i64(seed); }
  nextRaw(): bigint { this.state = i64(this.state + GOLDEN_GAMMA); return mix(this.state); }
}

export function roll(rng: GameRng, n: bigint): bigint { return ushr(rng.nextRaw(), 1n) % n; }

function maxBig(a: bigint, b: bigint) { return a > b ? a : b; }

export function bout(a: ShadowFighter, b: ShadowFighter, rng: GameRng): [boolean, number] {
  let ha = BigInt(a.hp), hb = BigInt(b.hp);
  let round = 0;
  while (ha > 0n && hb > 0n && round < MAX_ROUNDS) {
    round++;
    const critA = roll(rng, 100n) < CRIT_CHANCE;
    let dmgA = maxBig(20n, BigInt(a.attack) - BigInt(b.defense) * 2n / 5n);
    if (critA) dmgA = dmgA * 19n / 10n;
    hb -= dmgA;
    if (hb <= 0n) break;
    const critB = roll(rng, 100n) < CRIT_CHANCE;
    let dmgB = maxBig(20n, BigInt(b.attack) - BigInt(a.defense) * 2n / 5n);
    if (critB) dmgB = dmgB * 19n / 10n;
    ha -= dmgB;
  }
  const winA = hb <= 0n ? true : ha <= 0n ? false : ha * BigInt(b.hp) >= hb * BigInt(a.hp);
  return [winA, round];
}

export function resolve(a: ShadowTeam, b: ShadowTeam, seed: bigint): Duel {
  const rng = new GameRng(seed);
  const n = Math.min(a.fighters.length, b.fighters.length);
  const bouts: Bout[] = [];
  for (let i = 0; i < n; i++) {
    const [w, r] = bout(a.fighters[i], b.fighters[i], rng);
    bouts.push({ a: i, b: i, winnerA: w, rounds: r });
  }
  const winsA = bouts.filter((x) => x.winnerA).length;
  return { winnerA: winsA * 2 > n || (winsA * 2 === n && a.fighters.length > b.fighters.length), bouts, seed: seed.toString() };
}

export function maxStat(realm: number): number {
  let v = 4000;
  for (let i = 0; i < Math.max(0, Math.min(realm, REALMS)); i++) v *= 3;
  return v;
}

export function valid(t: ShadowTeam): boolean {
  return t.fighters.length >= 1 && t.fighters.length <= TEAM_SIZE && t.fighters.every((f) =>
    f.realm >= 0 && f.realm < REALMS && f.stage >= 1 && f.stage <= 9 &&
    f.attack >= 1 && f.attack <= maxStat(f.realm) && f.defense >= 0 && f.defense <= maxStat(f.realm) &&
    f.hp >= 1 && f.hp <= maxStat(f.realm) * 10);
}
