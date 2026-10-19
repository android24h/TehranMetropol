# 🚇 Metropool – Tehran Metro Navigator | متروپل

A beautiful, bilingual (English / فارسی) Android app for finding the fastest way across the Tehran Metro.
Built with **Kotlin + Jetpack Compose**, following **MVVM + Clean Architecture**.

## ✨ Features

- **Origin & destination search**: forgiving bilingual autocomplete. It ignores spacing and Arabic/Persian letter variants, and accepts Persian digits.
- **Pick on the map**: tap the map icon beside the origin field to choose stations on an OpenStreetMap map of Tehran. You can also jump to the **station nearest to you**.
- **Fastest Route**: total travel time, arrival clock, which **direction** (terminal) to board, and every **line change**.
- **More details**: every station on the route with estimated times, plus the expected **wait for each leg** and walking time inside interchanges.
- **Show on map**: the route drawn in official line colors over a faint network layer, with labelled origin, transfer and destination markers, and an animated train gliding along the path.
- **Fastest / Fewest changes** toggle.
- **Time-aware estimates**: waits scale with rush hour, late evening, and closed hours.
- 🌟 **Ride mode** (the creative feature): a live trip companion with a progress ring, next stop, upcoming change, keep-screen-on, and optional **notifications ~1.5 min before every change and before your stop**.
- 🌱 CO₂ saved compared with driving.
- Side menu: **English / Persian** (full RTL), **Day / Night / Auto** theme, and developer contact.
- **Tutorial** (the "?" button, also shown automatically on first launch).
- Recent trips and an overview of all lines.

## 🏛 Architecture

```
com.example.tehranmetropoll
├── domain/                 Pure Kotlin – no Android imports
│   ├── model/              Station, MetroLine, MetroNetwork, Route, RouteLeg, …
│   ├── repository/         Interfaces (MetroRepository, SettingsRepository, …)
│   ├── routing/            RoutePlanner (Dijkstra over station×line), StationMatcher, ServiceSchedule
│   └── usecase/            FindRoute, SearchStations, FindNearestStation, TrackRideProgress, …
├── data/                   Implementations
│   ├── source/             JSON asset parser  (assets/metro_network.json)
│   ├── repository/         MetroRepositoryImpl, SettingsRepositoryImpl (DataStore), LanguageRepositoryImpl
│   ├── location/           LocationManager-based location (no Play Services)
│   └── reminder/           WorkManager ride alerts + notification worker
├── di/AppContainer.kt      Manual dependency injection
└── presentation/           MVVM – Compose screens + ViewModels (StateFlow UI state)
    ├── home/  picker/  route/  map/  ride/  tutorial/  navigation/  theme/  components/
```

**Routing.** The planner runs Dijkstra over *(station, line)* states. Riding between neighbours costs the running time, derived from distance and line speed, plus a dwell. Boarding costs half the line's current headway. Changing lines adds the interchange walk. Airport branches (Line 1 → IKA, Line 4 → Mehrabad) are modelled as separate services.

## 🗺 Data

`app/src/main/assets/metro_network.json` holds all 7 lines plus both airport branches: stations with English and Persian names, coordinates, line order, typical headways, and interchange walking times. It is plain JSON, so you can update it as the network grows.

> Coordinates, headways and travel times are approximations, intended for trip estimates rather than official timetables.

## ⚙️ Build

- Android Studio Ladybug or newer, JDK 17
- `./gradlew assembleDebug`
- Unit tests: `./gradlew testDebugUnitTest`

Map tiles: © OpenStreetMap contributors © CARTO (an internet connection is required to show map tiles).

## 👨‍💻 Developer

**Alireza Halvaei Fard** – علیرضا حلوایی‌فرد
📱 09360896001 · 📧 halvaei@gmail.com
