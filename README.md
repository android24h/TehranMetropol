# 🚇 Metropol — Tehran Metro Route Finder

**Metropol** is a Tehran Metro route finder that compares metro and car travel time, with live traffic for the car.

Built with **Kotlin** and **Jetpack Compose**, using **MVVM + Clean Architecture**. It is fully bilingual (English / فارسی, with full RTL support).

## ✨ Features

- **Route finding**: Dijkstra-based planner across all Tehran Metro lines and both airport branches. You can choose **Fastest** or **Fewest changes**.
- **Transfers**: which line and direction (terminal) to board, every line change, walking time inside interchanges, and the expected wait for each leg.
- **Metro map**: an OpenStreetMap-based map that shows the network in official line colors. Pick origin and destination by tapping stations, jump to the **station nearest to you**, or view your route drawn on the map with origin, transfer, and destination markers.
- **Station info**: tap a station on the map to see its English and Persian names and the lines that serve it. The route screen lists **every station on your trip** with estimated times.
- **Metro vs car travel time**: compares the metro time with live, traffic-aware driving time from the **Neshan Direction API**. The app handles offline and error states.
- **Last-train check**: warns when a trip depends on the last train, and can schedule a reminder to leave in time.
- **Women-only cars**: a diagram of where the women-only cars are on each line.
- **Ride mode ("wake me up")**: a foreground service tracks the trip from the timetable (GPS doesn't work underground). It vibrates and rings one station before each change and before your stop.
- **Favourite routes** plus a **home-screen widget** (Jetpack Glance) that shows them with travel times.
- **Persian / English** per-app language, **Light / Dark / System** theme, recent trips, an interactive tutorial, and an estimate of CO₂ saved compared with driving.
- **Works offline**: all metro data ships with the app. Only map tiles and the car comparison need the internet.

## 🛠 Tech Stack

| Area | Used |
|---|---|
| Language | Kotlin, Coroutines & Flow (`StateFlow`) |
| UI | Jetpack Compose, Material 3, Navigation Compose, AndroidX SplashScreen |
| Architecture | MVVM + Clean Architecture (domain / data / presentation), use cases |
| Dependency injection | Manual DI container (`di/AppContainer.kt`): no annotation processors |
| Persistence | Jetpack DataStore (Preferences) for settings, recent trips, and favourites |
| Widget | Jetpack Glance (`glance-appwidget`) |
| Maps | osmdroid (OpenStreetMap / CARTO tiles) |
| Networking | `HttpURLConnection` + Gson (Neshan Direction API) |
| Background | Foreground service (trip tracking), AlarmManager (last-train reminders), notifications |
| Location | Android `LocationManager` (no Google Play Services) |
| Testing | JUnit 4 unit tests for routing, last-train, ride-alert, and travel-comparison logic |

## 🏛 Architecture

```
ir.halvaei.metropol
├── domain/            Pure Kotlin, no Android imports
│   ├── model/         Station, MetroLine, MetroNetwork, Route, RouteLeg, …
│   ├── repository/    Interfaces (MetroRepository, CarDirectionsRepository, …)
│   ├── routing/       RoutePlanner (Dijkstra), TravelTimes, ServiceSchedule, StationMatcher
│   ├── lasttrain/     LastTrainCalculator
│   └── usecase/       FindRoute, EstimateCarMinutes, CheckLastTrain, TrackRideProgress, …
├── data/              Implementations
│   ├── source/        JSON asset parsers (metro_network.json, metro_operations.json)
│   ├── repository/    Metro, Operations, Settings, Favorites, Language repositories
│   ├── directions/    NeshanDirectionsRepository (car travel time)
│   ├── trip/          Trip-tracking foreground service + notifications
│   ├── reminder/      Last-train reminders
│   └── location/ connectivity/ time/
├── di/                AppContainer (manual DI)
├── widget/            Glance favourites widget
└── presentation/      Compose screens + ViewModels
    └── home/ picker/ route/ map/ ride/ tutorial/ navigation/ theme/ components/
```

## 🧮 How Route and Travel Time Are Calculated

**Metro.** The network (`assets/metro_network.json`) is a graph whose nodes are *(station, line)* pairs. `RoutePlanner` runs Dijkstra over this graph with these costs:

- **Riding** between neighbouring stations: distance ÷ line speed, plus a 30-second dwell (minimum 1.2 min per segment).
- **Boarding** a line: half of its headway. The headway is scaled by the time of day (rush hour, late evening, closed hours).
- **Changing lines**: the interchange walking time plus half the new line's headway. In *Fewest changes* mode, each transfer also gets an extra penalty.

The total is shown as travel time and arrival clock, with times for each leg and each station.

**Car.** The app sends the origin and destination station coordinates to the **Neshan Direction API** (`type=car`), which returns a traffic-aware duration. The app sums the legs' durations and rounds up to whole minutes, then shows the result next to the metro time.

> Coordinates, headways and travel times are approximations, intended for trip estimates rather than official timetables.

## ⚙️ Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/android24h/Metropol.git
   ```
2. Copy `local.properties.example` to `local.properties` in the project root.
3. Get an API key from the [Neshan developer panel](https://platform.neshan.org/) and add it:
   ```properties
   NESHAN_API_KEY=your_key_here
   ```
   The key is injected through `BuildConfig` and is never committed. Without a key, the app still works, but the car comparison won't be available.
4. Open the project in **Android Studio** (JDK 17), let Gradle sync, and run the `app` configuration.

## 📸 Screenshots

| Home | Route | Metro vs Car |
|---|---|---|
| ![Home](docs/screenshots/home.png) | ![Route](docs/screenshots/route.png) | ![Metro vs Car](docs/screenshots/comparison.png) |

| Map | Ride mode | Widget |
|---|---|---|
| ![Map](docs/screenshots/map.png) | ![Ride mode](docs/screenshots/ride.png) | ![Widget](docs/screenshots/widget.png) |

## 🕯 Dedication

This app is named **Metropol** in memory of the victims of the Metropol building collapse in Abadan.

## 👨‍💻 Author

**Alireza Halvaei Fard**

- GitHub: [android24h](https://github.com/android24h)
- LinkedIn: [alirezahalvaei](https://www.linkedin.com/in/alirezahalvaei/)

Map tiles: © OpenStreetMap contributors © CARTO

---

<div dir="rtl">

## 🇮🇷 درباره‌ی متروپل

**متروپل** اپلیکیشن مسیریابی متروی تهران است. سریع‌ترین مسیر یا مسیر با کمترین تعویض خط را پیدا می‌کند. جهت سوار شدن، ایستگاه‌های تعویض و زمان تقریبی سفر را نشان می‌دهد. زمان سفر با مترو را هم با زمان سفر با خودرو، بر اساس ترافیک لحظه‌ای (API نشان)، مقایسه می‌کند.

امکانات دیگر: نقشه‌ی مترو، یادآور آخرین قطار، نمایش واگن‌های ویژه‌ی بانوان، حالت سفر با هشدار قبل از ایستگاه تعویض و مقصد، مسیرهای محبوب و ویجت صفحه‌ی اصلی، زبان فارسی و انگلیسی و حالت تاریک. بیشتر امکانات بدون اینترنت هم کار می‌کنند.

نام این اپلیکیشن به یاد قربانیان فروریختن ساختمان متروپل آبادان انتخاب شده است.

توسعه‌دهنده: **علیرضا حلوایی‌فرد**

</div>
