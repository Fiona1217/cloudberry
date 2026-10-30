# Cloudberry

Cloudberry is a personal Android application that combines **Spotify playback, customizable time-based playlist rules, and real-time weather data** to create a more dynamic music-listening experience.

By integrating the Spotify Web API, Spotify Android SDK, and WeatherAPI, Cloudberry lets users organize their music around their daily schedules and weather conditions.

## Features

- **Spotify integration** — Connect your Spotify account and access your playlists.
- **Time-based playlist rules** — Create custom time periods and assign Spotify playlists to them.
- **Season-based playlist rules** — Assign playlists to specific seasons and combine seasonal conditions with time-based rules.
- **Weather integration** — Retrieve current weather conditions using WeatherAPI.
- **Weather-based playlist rules** — Configure music rules that respond to weather conditions, including rain.
- **Playback controls** — Control Spotify playback through the app.
- **Automatic playlist switching** — Optionally switch playlists when rules are triggered, with playback behavior controlled by user settings.
- **Rule priorities and playlist ordering** — Customize which rules take precedence and how playlists are selected.

## Technology Stack

| Area | Technology |
|---|---|
| Android Development | Kotlin, Jetpack Compose |
| Android Platform | Android SDK |
| Music Integration | Spotify Web API, Spotify Android SDK |
| Backend | Python, FastAPI |
| HTTP Requests | HTTPX |
| Database | Supabase PostgreSQL |
| Weather Data | WeatherAPI |
| Hosting | Render |
| Build System | Gradle (Kotlin DSL) |
| Version Control | Git, GitHub |

## External Services

| Service | Purpose |
|---|---|
| Spotify Web API | Retrieve playlists, verify user accounts, and interact with Spotify |
| Spotify Android SDK | Authentication and App Remote playback integration |
| WeatherAPI | Retrieve current weather conditions |
| FastAPI | Backend API and integration layer |
| Render | Backend hosting |
| Supabase (PostgreSQL) | Persistent storage for user playlist rules |

## Backend API

Cloudberry uses a FastAPI backend to handle weather requests, Spotify authentication, token management, and persistent rule storage.

| Endpoint | Method | Description |
|---|---|---|
| `/` | GET | Backend status |
| `/api/health` | GET | Health check |
| `/api/weather` | GET | Retrieve current weather using geographic coordinates |
| `/api/spotify/token` | POST | Exchange an authorization code for Spotify tokens |
| `/api/spotify/refresh` | POST | Refresh Spotify access tokens |
| `/api/rules` | GET | Retrieve the authenticated user's saved rules |
| `/api/rules` | PUT | Save or update the authenticated user's rules |

The rules endpoints require a valid Spotify access token. The backend verifies the Spotify account before accessing that account's saved rules.

### Database

Cloudberry uses Supabase PostgreSQL to persist user-specific playlist rules.

The backend uses the `public.account_rules` table:

| Column | Data type | Description |
|---|---|---|
| `spotify_user_id` | TEXT (Primary Key) | Identifies the Spotify account |
| `rules_json` | TEXT | Stores the user's playlist rules as JSON |
| `updated_at` | TEXT | Records the last update timestamp |

Each Spotify account has one database record containing its saved rules. The backend uses an upsert operation to insert new records or update existing ones.

#### Database Setup

Create a Supabase project and execute the following SQL in the Supabase SQL Editor:

```sql
CREATE TABLE public.account_rules (
    spotify_user_id TEXT PRIMARY KEY,
    rules_json TEXT NOT NULL DEFAULT '[]',
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP::text
);
```

Configure the `DATABASE_URL` environment variable with your PostgreSQL connection string.

## Project Structure

```text
cloudberry/
├── app/
│   ├── assets/
│   ├── libs/
│   └── src/
│       └── main/
│           └── java/com/example/cloudberry/
├── backend/
│   ├── main.py
│   └── requirements.txt
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Requirements

- Android Studio
- Android SDK 24 or later
- JDK compatible with the project's Gradle configuration
- Python 3
- A Spotify Developer account
- A WeatherAPI account
- - A Supabase project with PostgreSQL

## Backend Setup

1. Navigate to the backend directory:

   ```bash
   cd backend
   ```

2. Create and activate a virtual environment:

   ```bash
   python -m venv .venv
   ```

   Windows:

   ```powershell
   .venv\Scripts\Activate.ps1
   ```

3. Install dependencies:

   ```bash
   pip install -r requirements.txt
   ```

4. Create a `.env` file in the `backend` directory:

   ```env
   WEATHER_API_KEY=your_weatherapi_key
   SPOTIFY_CLIENT_ID=your_spotify_client_id
   SPOTIFY_CLIENT_SECRET=your_spotify_client_secret
   DATABASE_URL=your_postgresql_connection_string
   ```

5. Start the backend:

   ```bash
   uvicorn main:app --reload
   ```

Keep your `.env` file private. Never commit API keys, database credentials, or the Spotify Client Secret.

## Android Setup

1. Clone this repository and open it in Android Studio.
2. Allow Gradle to sync and install the required dependencies.
3. Configure your Spotify Client ID in the Android application.
4. Configure the backend URL to point to your running backend.
5. Build and run the application on an Android device.

### Spotify Configuration

Configure your application in the Spotify Developer Dashboard.

The Android application uses the package name:

```text
com.example.cloudberry
```

Configure the required redirect URIs:

```text
https://com.example.cloudberry/callback
com.example.cloudberry://callback
```

Register the appropriate signing certificate SHA-1 fingerprints for your debug and release builds.

The Spotify Client Secret must remain on the backend and must never be included in the Android application.

## Environment Variables

| Variable | Purpose |
|---|---|
| `WEATHER_API_KEY` | WeatherAPI authentication |
| `SPOTIFY_CLIENT_ID` | Spotify application identification |
| `SPOTIFY_CLIENT_SECRET` | Spotify backend authentication |
| `DATABASE_URL` | PostgreSQL database connection |

## Notes

- Spotify account eligibility and API access are subject to Spotify's current Developer Mode requirements and platform policies.
- A Spotify Premium account may be required for certain playback features.
- This project is intended for personal and educational use.
- This project is not affiliated with or endorsed by Spotify.

## App Preview

### Home Screen
Displays the home screen, including the rotating vinyl while music is playing.
<div align="center">

https://github.com/user-attachments/assets/84fe090a-950d-4b4d-8e60-ab6013c1fb7a

</div>
### Settings
Shows the app settings, including weather configuration.
<div align="center">
    <img height="400" alt="settings" src="https://github.com/user-attachments/assets/7ec6790d-2e11-4a2f-ba19-85244c3166c4" />
</div>
### Rule List
Demonstrates viewing, expanding, and collapsing saved playlist rules.
<div align="center">

https://github.com/user-attachments/assets/bd848a51-f7a9-4c72-a225-139a77c80ed6

</div>
### Playlist Sorting
Demonstrates configuring playlist order and shuffle options when creating a rule.
<div align="center">

https://github.com/user-attachments/assets/649b76df-2bfd-4798-9f49-746369b9732d

</div>
### Adding a Rule
Demonstrates creating a new playlist rule and viewing it in the rule list.
<div align="center">

https://github.com/user-attachments/assets/bf007b69-50bc-4e43-af67-558a0144fc8b

</div>
## Design Decisions

- **User-defined rules:** Users choose their own time periods, seasons, playlists, and rule priorities.
- **Optional automation:** Automatic playlist switching is disabled by default and can be enabled by the user.
- **Playback-aware switching:** When automatic switching is enabled, the app considers the current playback state before changing playlists.
- **Weather-aware behavior:** Users can configure rules that respond to current weather conditions, including rain.
- **Persistent preferences:** Playlist rules are stored by Spotify account, allowing users to retrieve their configurations.

## Limitations

- Spotify API access is subject to Spotify Developer Mode restrictions.
- Certain playback operations depend on Spotify account eligibility and Premium requirements.
- Weather-based rules require an active internet connection and access to WeatherAPI.
- The application requires Spotify authentication to access user playlists and playback functionality.

## License

No license has currently been assigned to this project's original source code. All rights are reserved unless permission is granted by the author.

Third-party libraries and SDKs remain subject to their respective licenses and terms.
