-- Sieć Dao (Etap 9): cienie drużyn, pojedynki, sezony, zapisy w chmurze.
-- Wyniki pojedynków zapisuje wyłącznie Edge Function verify-duel (service role), klient tylko czyta.

create table if not exists public.shadows (
  id uuid primary key default gen_random_uuid(),
  owner uuid not null references auth.users (id) on delete cascade,
  sect_name text not null check (char_length(sect_name) between 1 and 60),
  season int not null,
  era int not null default 1,
  rating int not null default 1000,
  team jsonb not null,
  updated_at timestamptz not null default now(),
  unique (owner, season)
);

-- Zakres statystyk na serwerze (ta sama reguła co DaoNet.valid / duel.ts valid).
create or replace function public.shadow_valid(team jsonb) returns boolean language sql immutable as $$
  select jsonb_array_length(team->'fighters') between 1 and 3
    and not exists (
      select 1 from jsonb_array_elements(team->'fighters') f
      where (f->>'realm')::int not between 0 and 9
         or (f->>'stage')::int not between 1 and 9
         or (f->>'attack')::bigint not between 1 and 4000 * power(3, (f->>'realm')::int)
         or (f->>'defense')::bigint not between 0 and 4000 * power(3, (f->>'realm')::int)
         or (f->>'hp')::bigint not between 1 and 40000 * power(3, (f->>'realm')::int)
    )
$$;
alter table public.shadows add constraint shadows_team_valid check (public.shadow_valid(team));

create table if not exists public.duels (
  id bigint generated always as identity primary key,
  attacker_id uuid not null references public.shadows (id) on delete cascade,
  defender_id uuid not null references public.shadows (id) on delete cascade,
  seed text not null,
  attacker_won boolean not null,
  bouts jsonb not null,
  created_at timestamptz not null default now()
);

create table if not exists public.cloud_saves (
  owner uuid primary key references auth.users (id) on delete cascade,
  payload text not null,
  game_day int not null,
  era int not null,
  top_realm text not null,
  saved_at timestamptz not null default now()
);

alter table public.shadows enable row level security;
alter table public.duels enable row level security;
alter table public.cloud_saves enable row level security;

-- Cienie: każdy czyta (ranking, rywale), właściciel wstawia i aktualizuje swoją drużynę (bez rankingu).
create policy "shadows read" on public.shadows for select using (true);
create policy "shadows insert own" on public.shadows for insert with check (auth.uid() = owner and rating = 1000);
create policy "shadows update own team" on public.shadows for update using (auth.uid() = owner)
  with check (auth.uid() = owner);
revoke update (rating) on public.shadows from authenticated, anon;

-- Pojedynki: tylko odczyt dla graczy.
create policy "duels read" on public.duels for select using (true);

-- Zapis w chmurze: tylko właściciel.
create policy "saves own" on public.cloud_saves for all using (auth.uid() = owner) with check (auth.uid() = owner);

-- Ranking sezonu.
create or replace view public.season_ranking as
  select season, id, sect_name, rating, rank() over (partition by season order by rating desc) as place
  from public.shadows;
