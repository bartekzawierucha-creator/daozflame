# Lokalizacja (Etap 9)

## Stan

- **Rama interfejsu** – przetłumaczona: nazwa gry (`res/values-en/strings.xml`), nazwy modułów, nawigacja.
  Warstwa: `ui/i18n/Lang.kt` (`Lang.t(pl, en)`, język urządzenia).
- **Treść gry** – po polsku: katalogi zdarzeń, fabuła, NPC, tajemnice, opisy przedmiotów, komunikaty silnika.
- Skaner: `python3 tools/i18n_scan.py` liczy napisy z polskimi znakami w każdym pliku. Przy pierwszym pomiarze było ich ok. 3260.

## Plan

1. **Komunikaty silnika.** Silnik zwraca dziś gotowe zdania. Trzeba je zamienić na klucze z parametrami (`GameEvent.Notice(key, args)`). Formatowanie przenieść do UI, bo silnik nie może zależeć od Androida.
2. **Treść.** Katalogi (`game/content/*`) przenieść do JSON w `assets/content/pl/` i `assets/content/en/`. Silnik dostaje treść przez `GameContent`. Walidator zdarzeń sprawdza, że obie wersje mają te same identyfikatory.
3. **UI.** Napisy z plików `ui/**` przenieść do `strings.xml`: polski w `values/`, angielski w `values-en/`. Kolejność: najpierw moduły hubu, potem panele.
4. **Przegląd układu.** Angielskie teksty są dłuższe, więc trzeba przejść każdy ekran. Szczególnie ważne są przyciski w wierszach po 4–5.
5. **Terminy** według `docs/glossary.md`. Dao i Qi zostają bez tłumaczenia.

Reguły świata (E6) są niezależne od języka. Wskazówki (`Hint.text`) generowane z szablonów muszą powstawać w obu językach z tych samych danych.
