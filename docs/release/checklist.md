# Wydanie 1.0 – lista kontrolna

## Kod (gotowe w repo)
- [x] CI: testy JVM, symulator, zgodność walki Kotlin ↔ Edge Function, budowa APK
- [x] Sieć Dao: schemat `supabase/schema.sql`, funkcja `supabase/functions/verify-duel`, klient `game/net/DaoNetClient.kt`
- [x] Tryb offline bez konfiguracji (`DAONET_URL` puste)
- [x] Lokalny rejestr awarii (`CrashLog.kt`)
- [x] Rama interfejsu po angielsku, glosariusz, plan lokalizacji treści

## Do zrobienia poza kodem
- [ ] Projekt Supabase:
  - włączyć anonimowe logowanie;
  - wgrać `schema.sql`;
  - `supabase functions deploy verify-duel`;
  - adres i klucz anon wpisać do `local.properties` albo zmiennych środowiska (`DAONET_URL`, `DAONET_ANON_KEY`).
- [ ] Crashlytics: projekt Firebase i `google-services.json`. Plugin `google-services` jest już w buildzie i działa w trybie WARN.
- [ ] Klucz wydania (upload key) i zmienne `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`.
- [ ] `versionCode` / `versionName` dla wydania.
- [ ] Konto dewelopera Google Play. **Przed startem sprawdzić aktualne wymagania** dla nowych kont: liczba testerów i czas testów zamkniętych często się zmieniają.
- [ ] Testy zamknięte, potem otwarte.
- [ ] Polityka prywatności opublikowana pod stałym adresem (`docs/release/privacy-policy.md` – uzupełnić dane wydawcy).
- [ ] Formularz bezpieczeństwa danych (`data-safety.md`) i klasyfikacja wiekowa (IARC).
- [ ] Strona sklepu: teksty w `store-listing.md`, grafiki z manhwy, 6–8 zrzutów, film.
- [ ] Pełne tłumaczenie treści (docs/localization.md).
- [ ] Test na ślepo tajemnic (E6): 3 osoby × 2 tygodnie.
- [ ] Kryterium wydania: odsetek sesji bez awarii > 99,5% (Crashlytics, testy otwarte).

## Monetyzacja (opcjonalnie, niezaimplementowana)
Tylko kosmetyka:
- wygląd sekty, herby, skórki płomieni;
- „Wesprzyj twórcę”.

Żadnych zakupów przyspieszających odkrycia ani postęp.
