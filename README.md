# GymLog

GymLog is an Android app for logging Technogym elliptical workouts. After each session, open the app, photograph the Technogym cooldown summary screen, and the app uses on-device OCR (Google ML Kit) to extract all 10 workout metrics automatically. Sessions are saved locally with a date, and a dashboard shows trend charts for every metric over time.

## Setup

**Build:**
```bash
./gradlew assembleDebug
```

**Install on device:**
```bash
./gradlew installDebug
```

> **Note:** The Gradle wrapper JAR is not included in this repository. Run `gradle wrapper --gradle-version 8.4` once to generate it, or download it from the [Gradle Distributions page](https://gradle.org/releases/).

## How to use

1. Open **GymLog** on your Android device
2. Tap the **camera FAB** (bottom right)
3. Align the Technogym cooldown screen within the guide frame
4. Tap the **shutter button** to capture
5. Review the 10 parsed metrics — fields that could not be read are highlighted in amber
6. Correct any wrong values and tap **Sessie opslaan**
7. The **dashboard** updates immediately with your new session

## Tips

- Works best in **good lighting**. Avoid glare on the screen.
- If values are wrong after capture, edit them in the review screen before saving — nothing is committed until you tap "Sessie opslaan".
- Tap any metric card on the dashboard to open a full line chart for that metric.
- Swipe left/right on the detail screen to browse between metrics.

## Tech stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material3 |
| OCR | Google ML Kit Text Recognition v2 |
| Database | Room (SQLite) |
| Charts | Vico |
| Architecture | MVVM + Repository |
| Camera | CameraX |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 |

## Supported metrics

| Metric | Dutch label on screen | Unit |
|---|---|---|
| Calorieën | Verbruikte calorieën | kcal |
| Duur | Duur van de oefening | MM:SS |
| Afstand | Afgelegde afstand | km |
| Gem. vermogen | Gemiddeld vermogen | watt |
| Gem. snelheid | Gemiddelde snelheid | spm |
| Gem. hartslag | Gemiddelde hartfrequentie | spm |
| Max. hartslag | Maximale hartfrequentie | spm |
| Kcal per uur | Calorieën per uur | kcal/h |
| Conditie (PI) | Conditie | PI |
| MOVEs | MOVEs | — |
