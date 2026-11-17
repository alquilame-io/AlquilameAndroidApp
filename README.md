# Alquilame Android App

Native Android app (Kotlin, Jetpack Compose, Material 3) for running a rental
business on Alquilame. It talks to the `/api/mobile/v2` API of the `rent`
Phoenix app.

## Features

- **Sign in** with email and password, or by scanning the QR code shown in the
  web panel (Settings → Mobile app).
- **Agenda**: today, tomorrow and upcoming reservations.
- **Calendar**: month view with the reservations of each day.
- **Reservations and events**: one product for a day, or several products
  over a date range, with live stock for the chosen date. Create the contract
  from either.
- **Inventory**: search, filter by category, create and edit products, photo
  from the camera or the gallery.
- **Clients**: directory, one-tap call / WhatsApp / email, history.
- **Contracts**: status, client, extra services, discount, tax, deposit,
  payments, Stripe payment links, contract and service-order PDFs.
- **Quotes**: build, share as PDF, convert into a contract.
- **Payment links** and **reports**.
- Sections built for large screens (site builder, floor plans, settings,
  billing) open the web panel inside the app, already signed in.
- English and Spanish.

## Project layout

```
app/src/main/java/com/io/alquilame/
  data/        Api.kt (Retrofit), Models.kt, Backend.kt (session + networking)
  ui/          AppRoot.kt (navigation)
  ui/common/   shared components, pickers, formatting, intents
  ui/screens/  one file per feature
  ui/theme/    colours
```

## Build

Requires JDK 17 and the Android SDK (compile SDK 36, min SDK 24).

```bash
./gradlew assembleDebug        # debug APK
./gradlew assembleRelease      # minified release APK (unsigned)
./gradlew testDebugUnitTest lintDebug
```

### Running against a local server

Debug builds use `https://alquilame.io/` unless `local.properties` overrides it:

```properties
ALQUILAME_API_URL=http://localhost:4000/
```

Then forward the port to the device or emulator and start Phoenix:

```bash
adb reverse tcp:4000 tcp:4000
```

The API only answers on the platform host, so use `localhost` (not `10.0.2.2`).
