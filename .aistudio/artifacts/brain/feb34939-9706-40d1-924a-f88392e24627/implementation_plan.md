# Przebudowa Interfejsu: Pergaminowe Zwoje i Cytadela 2D Manhwa AAA

Kompletna transformacja interfejsu graficznego gry ze standardu tekstowego do formatu pełnokrwistej gry 2D Manhwa / Wuxia klasy AAA. Wprowadza stylizowane kaligraficzne zwoje pergaminowe, woskowe pieczęcie cesarskie, pełnoekranową cytadelę sekty bez długich list tekstowych oraz kolekcjonerskie karty adeptów z dynamicznymi aurami Qi.

## Decyzje Użytkownika i Podsumowanie Konsultacji

> [!IMPORTANT]
> **Zatwierdzone wybory z fazy pytań:**
> - **Styl obramowań i paneli**: Styl pergaminowych zwojów z kaligraficznym obramowaniem (odcienie kości słoniowej, ciemnego nefrytu, tuszu i złota, wykończone pieczęciami z czerwonego laku).
> - **Układ Kampusu Sekty**: Pełnoekranowa cytadela 2D ze standee postaci – brak pionowych list tekstowych; klikalne pagody otwierają dedykowane komnaty (zwoje dekretów).
> - **Zarządzanie adeptami**: Kolekcjonerskie karty bohaterów Manhwa z pełnymi ilustracjami (768x1376), pulsującą aurą Qi, 8 meridianami i spektakularnym rytuałem Niebiańskiej Kary.

---

## 1. Przegląd i Wizja Wizualna (Core Concept)

- **Główny Cel**: Całkowita eliminacja wrażenia "gry tekstowej w ciemnoszarych ramkach Androida". Zamiast tego gracz wkracza w świat żywego zwoju taoistycznego z epickimi ilustracjami Manhwa, kaligraficznymi pergaminami, płynną animacją cząsteczek Qi oraz przestrzenną cytadelą.
- **Filozofia Projektowa**:
  - *Zero surowego tekstu bez oprawy*: Każdy komunikat, dialog czy statystyka prezentowane są na krawędziach stylizowanego zwoju lub w imperialnych medalionach.
  - *Full-Bleed Scenery*: Tło stanowi żywy świat 2D Góry Świętego Smoka, a elementy UI to estetyczne zwoje i nefrytowe talizmany.
  - *Kolekcjonerski Sznyt*: Uczniowie sekty prezentowani są jak w topowych grach gacha/RPG (pełne ilustracje, ramy żywiołów, gwiazdy kultywacji).

---

## 2. Architektura UI i Doświadczenie Użytkownika (UX & Visual Design)

### 2.1 Nowa Komponentowa Ramka: Pergaminowy Zwój (`ScrollParchmentFrame`)
- Zastępuje dotychczasowe `WuxiaCard`.
- **Wygląd**:
  - Tło o fakturze postarzanego jedwabiu / pergaminu (`Color(0xFF161022)` przechodzące w `Color(0xFF231834)` z kaligraficznym gradientem tuszu).
  - Podwójna obwódka: zewnętrzna złota linka (`#FFD700`) oraz wewnętrzna ramka z chińskimi narożnikami kaligraficznymi (zwoje chmur i smoków).
  - W rogach lub nagłówkach obecne czerwone pieczęcie laku (`宗`, `道`, `命`) z chińskimi ideogramami.
  - Subtelny cień i winieta pergaminowa.

### 2.2 Pełnoekranowa Cytadela Sekty (`SectCitadelScreen`)
- **Ekran bez przewijania list tekstowych**:
  - Górny i centralny obszar zajmuje interaktywna panorama góry sekty 2D (`Wuxia2dSanctuaryCanvas`) z żywymi cząsteczkami i animowanymi pagodami.
  - Na pierwszym planie stoi animowany Patriarcha Sekty (`Standee`) z dymkiem mądrości i aktualnym stanem sekty.
  - **4 Główne Złote Zwoje Szybkiego Dostępu** (Interactive Pavilion Shrines):
    1. 🏛️ **Pawilon Patriarchy** -> Otwiera zwój z dekretem naboru, budżetem i poziomem sekty.
    2. ⚗️ **Kocioł 9 Płomieni** -> Przejście do animowanego laboratorium alchemii.
    3. 🧘 **Szczyt Kultywacji** -> Przejście do masowych przełomów Qi i medytacji.
    4. 🔨 **Boska Kuźnia** -> Przejście do kowadła artefaktów.
  - Żadnych nudnych 6-poziomowych list tekstowych z opisami na pół ekranu.

### 2.3 Kolekcjonerskie Karty Adeptów Manhwa (`ManhwaHeroCardGrid`)
- Prezentacja w układzie efektownych kart kolekcjonerskich:
  - Duży, wyrazisty portret ucznia (72dp lub karta 140dp) z ramą odpowiadającą żywiołowi szkoły:
    - *Szkoła Miecza*: Lśniący Błękit Niebiański (`#00E5FF`)
    - *Asura Ciała*: Płonący Szkarłat Krwi (`#FF5252`)
    - *Taoizm Żywiołów*: Święte Złoto Błyskawic (`#FFD700`)
    - *Cień Żmii*: Mistyczny Fiolet Nefrytu (`#E040FB`)
  - Medale kręgu kultywacji (Kondensacja Qi, Złoty Rdzeń, Dusza Początkowa).
  - Estetyczne diamentowe wskaźniki Dantian Qi (zamiast pikselowych klocków).
  - Bezpośredni przycisk "⚡ PRZEŁOM" aktywujący animację gromu i audio gongu.

### 2.4 Zmodernizowany HUD i Konsola Rozkazów
- **Top HUD**: Luksusowy zwój z cesarską pieczęcią, licznikiem kamieni ducha w formie lśniącego kryształu i tarczą faz nieba.
- **Bottom Command Bar**: Zdobiona listwa pergaminowa z rzeźbionymi medalionami i haptyczną/dźwiękową odpowiedzią na dotyk.

---

## 3. Kluczowe Decyzje Techniczne i Komponenty

```
┌─────────────────────────────────────────────────────────────┐
│                    IMPERIAL SECT TOP HUD                    │
│   [宗 Pieczęć]  Nazwa Sekty   Dzień • Faza   💎 Kryształ   │
└─────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│            FULL-SCREEN 2D SECT CITADEL (SCENERY)            │
│  [Chmury Qi]   [Pagoda Główna]   [Kocioł]   [Szczyt Dao]    │
│            [Animowany Standee Patriarchy Sekty]             │
│        [4 Główne Złote Zwoje Pawilonów z Odznakami]         │
└─────────────────────────────────────────────────────────────┘
                               │
               ┌───────────────┴───────────────┐
               ▼                               ▼
┌─────────────────────────────┐ ┌─────────────────────────────┐
│    PARCHMENT SCROLL TABS    │ │     COLLECTIBLE HEROES      │
│  • Rozbudowa Pagód (Karty)  │ │  • Karty Manhwa z Aurą Qi   │
│  • Rytuały i Dekrety        │ │  • Przełom Niebiański ⚡   │
│  • Ceremonia Naboru         │ │  • 8 Meridianów & Dantian   │
└─────────────────────────────┘ └─────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│              ANCIENT SCROLL BOTTOM COMMAND BAR              │
│       [🏛️ Sekta]  [🥋 Adeptowie]  [⚗️ Kuźnia]  [🗺️ Świat]   │
└─────────────────────────────────────────────────────────────┘
```

- **Usunięcie ostatnich reliktów pixel-artu**: Wszystkie pozostałe `PixelCard` i `PixelSegmentedBar` zostaną zastąpione jednolitym systemem `ScrollParchmentFrame` i płynnymi wektorowymi wskaźnikami Qi z poświatą.
- **Bezpieczeństwo Logiki Gry**: Żadne zmiany nie naruszą stanu gry w `WuxiaGameViewModel` ani silnika syntezy dźwięku `WuxiaAudioEngine`.

---

## 4. Plan Realizacji Krok po Kroku

1. **Krok 1: Wdrożenie `ScrollParchmentFrame` & Ozdobnych Ramek Kaligraficznych**
   - Stworzenie uniwersalnego komponentu ramki zwoju pergaminowego z motywem podwójnej nici złotej, woskową pieczęcią i narożnikami obłoków pomyślności.
2. **Krok 2: Przebudowa Ekranu Kampusu Sekty (`SectSanctuaryScreen`)**
   - Zamiana pionowych list tekstowych na pełnoekranową cytadelę 2D ze standee Patriarchy, pływającymi kaplicami zwojów i dynamicznymi modalnymi pergaminami rozbudowy.
3. **Krok 3: Przebudowa Sali Adeptów (`DisciplesScreen`)**
   - Przekształcenie widoku uczniów w kolekcjonerską galerię kart bohaterów Manhwa z aurami żywiołów, czytelnymi herbami szkół i błyskawicznym przełomem Dantian.
4. **Krok 4: Weryfikacja i Testy**
   - Kompilacja projektu narzędziem `compile_applet`, uruchomienie testów jednostkowych i weryfikacja wrażeń estetycznych w emulatorze.
