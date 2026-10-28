import os
import json
import sqlite3
from typing import Any

import httpx
from pydantic import BaseModel
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
load_dotenv()

app = FastAPI(
    title="cloudberry API",
    description="A season & time-based music player backend",
    version="0.2.0",
)

security = HTTPBearer()
WEATHER_API_KEY = os.getenv("WEATHER_API_KEY")
WEATHER_API_URL = "https://api.weatherapi.com/v1/current.json"
SPOTIFY_CLIENT_ID = os.getenv("SPOTIFY_CLIENT_ID")
SPOTIFY_CLIENT_SECRET = os.getenv("SPOTIFY_CLIENT_SECRET")
SPOTIFY_TOKEN_URL = "https://accounts.spotify.com/api/token"
DATABASE_PATH = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "cloudberry.db",
)


def initialize_database():
    with sqlite3.connect(DATABASE_PATH) as connection:
        connection.execute("""
            CREATE TABLE IF NOT EXISTS account_rules (
                spotify_user_id TEXT PRIMARY KEY,
                rules_json TEXT NOT NULL DEFAULT '[]',
                updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
        """)
        connection.commit()


initialize_database()


async def get_spotify_user_id(
    authorization: str | None,
) -> str:
    """Validate the access token and identify its Spotify account."""

    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=401,
            detail="Spotify login required.",
        )

    access_token = authorization.removeprefix("Bearer ").strip()

    if not access_token:
        raise HTTPException(
            status_code=401,
            detail="Spotify login required.",
        )

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(
                "https://api.spotify.com/v1/me",
                headers={
                    "Authorization": f"Bearer {access_token}"
                },
            )

        if response.status_code in (401, 403):
            raise HTTPException(
                status_code=401,
                detail="Spotify session is invalid or expired.",
            )

        if response.status_code == 429:
            raise HTTPException(
                status_code=503,
                detail="Spotify is temporarily rate-limiting requests. Try again later.",
                headers={
                    "Retry-After": response.headers.get(
                        "Retry-After", "60"
                    )
                },
            )

        if response.status_code != 200:
            raise HTTPException(
                status_code=502,
                detail="Could not verify the Spotify account.",
            )

        spotify_user_id = response.json().get("id")

        if not spotify_user_id:
            raise HTTPException(
                status_code=502,
                detail="Spotify did not return an account ID.",
            )

        return spotify_user_id

    except httpx.RequestError as exc:
        raise HTTPException(
            status_code=502,
            detail="Could not connect to Spotify to verify the account.",
        ) from exc

class SpotifyCodeRequest(BaseModel):
    code: str

class SpotifyRefreshRequest(BaseModel):
    refresh_token: str

@app.get("/")
def home():
    return {
        "message": "Welcome to cloudberry!",
        "status": "running",
    }

@app.get("/api/health")
def health_check():
    return {"status": "healthy"}

# WeatherAPI condition codes treated as rain-related.
RAIN_CONDITION_CODES = {
    1180,  # Patchy light rain
    1183,  # Light rain
    1186,  # Moderate rain at times
    1189,  # Moderate rain
    1192,  # Heavy rain at times
    1195,  # Heavy rain
    1198,  # Light freezing rain
    1201,  # Moderate or heavy freezing rain
    1240,  # Light rain shower
    1243,  # Moderate or heavy rain shower
    1246,  # Torrential rain shower
    1273,  # Patchy light rain with thunder
    1276,  # Moderate or heavy rain with thunder
}


@app.get("/api/weather")
async def get_weather(latitude: float, longitude: float):
    if not WEATHER_API_KEY:
        raise HTTPException(
            status_code=500,
            detail="WeatherAPI key is not configured.",
        )

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(
                WEATHER_API_URL,
                params={
                    "key": WEATHER_API_KEY,
                    "q": f"{latitude},{longitude}",
                    "aqi": "no",
                },
            )
            response.raise_for_status()

    except httpx.HTTPStatusError as exc:
        if exc.response.status_code == 400:
            raise HTTPException(
                status_code=400,
                detail="Invalid location or weather request.",
            ) from exc

        if exc.response.status_code == 401:
            raise HTTPException(
                status_code=502,
                detail="WeatherAPI rejected the API key.",
            ) from exc

        raise HTTPException(
            status_code=502,
            detail="WeatherAPI returned an error.",
        ) from exc

    except httpx.RequestError as exc:
        raise HTTPException(
            status_code=502,
            detail="Could not connect to WeatherAPI.",
        ) from exc

    data = response.json()
    current = data["current"]
    location = data["location"]
    condition = current["condition"]

    return {
        "city": location["name"],
        "country": location["country"],
        "condition": condition["text"],
        "condition_code": condition["code"],
        "is_raining": condition["code"] in RAIN_CONDITION_CODES,
        "temperature_c": current["temp_c"],
        "precip_mm": current["precip_mm"],
        "is_day": bool(current["is_day"]),
    }


@app.post("/api/spotify/token")
async def exchange_spotify_code(request: SpotifyCodeRequest):
    if not SPOTIFY_CLIENT_ID or not SPOTIFY_CLIENT_SECRET:
        raise HTTPException(
            status_code=500,
            detail="Spotify credentials are not configured.",
        )

    try:
        async with httpx.AsyncClient(timeout=15.0) as client:
            response = await client.post(
                SPOTIFY_TOKEN_URL,
                data={
                    "grant_type": "authorization_code",
                    "code": request.code,
                    "redirect_uri": "com.example.cloudberry://callback",
                },
                auth=(SPOTIFY_CLIENT_ID, SPOTIFY_CLIENT_SECRET),
            )

        if response.status_code != 200:
            raise HTTPException(
                status_code=502,
                detail="Spotify rejected the authorization-code exchange.",
            )

        return response.json()

    except httpx.RequestError as exc:
        raise HTTPException(
            status_code=502,
            detail="Could not connect to Spotify's token service.",
        ) from exc


@app.post("/api/spotify/refresh")
async def refresh_spotify_token(request: SpotifyRefreshRequest):
    if not SPOTIFY_CLIENT_ID or not SPOTIFY_CLIENT_SECRET:
        raise HTTPException(
            status_code=500,
            detail="Spotify credentials are not configured.",
        )

    try:
        async with httpx.AsyncClient(timeout=15.0) as client:
            response = await client.post(
                SPOTIFY_TOKEN_URL,
                data={
                    "grant_type": "refresh_token",
                    "refresh_token": request.refresh_token,
                },
                auth=(SPOTIFY_CLIENT_ID, SPOTIFY_CLIENT_SECRET),
            )

        if response.status_code != 200:
            # Don't expose Spotify's raw response or credentials.
            raise HTTPException(
                status_code=502,
                detail="Spotify rejected the token refresh request.",
            )

        return response.json()

    except httpx.RequestError as exc:
        raise HTTPException(
            status_code=502,
            detail="Could not connect to Spotify's token service.",
        ) from exc

@app.get("/api/rules")
async def get_rules(
    credentials: HTTPAuthorizationCredentials = Depends(security),
):
    authorization = f"Bearer {credentials.credentials}"
    spotify_user_id = await get_spotify_user_id(authorization)

    with sqlite3.connect(DATABASE_PATH) as connection:
        row = connection.execute(
            """
            SELECT rules_json
            FROM account_rules
            WHERE spotify_user_id = ?
            """,
            (spotify_user_id,),
        ).fetchone()

    if row is None:
        return {"rules": []}

    return {"rules": json.loads(row[0])}


@app.put("/api/rules")
async def save_rules(
    payload: dict[str, Any],
    credentials: HTTPAuthorizationCredentials = Depends(security),
):
    authorization = f"Bearer {credentials.credentials}"
    spotify_user_id = await get_spotify_user_id(authorization)
    rules = payload.get("rules")

    if not isinstance(rules, list):
        raise HTTPException(
            status_code=400,
            detail="The rules field must be a list.",
        )

    if len(rules) > 200:
        raise HTTPException(
            status_code=400,
            detail="Too many rules.",
        )

    if not all(isinstance(rule, dict) for rule in rules):
        raise HTTPException(
            status_code=400,
            detail="Every rule must be an object.",
        )

    rules_json = json.dumps(rules)

    with sqlite3.connect(DATABASE_PATH) as connection:
        connection.execute(
            """
            INSERT INTO account_rules (
                spotify_user_id,
                rules_json,
                updated_at
            )
            VALUES (?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(spotify_user_id)
            DO UPDATE SET
                rules_json = excluded.rules_json,
                updated_at = CURRENT_TIMESTAMP
            """,
            (spotify_user_id, rules_json),
        )
        connection.commit()

    return {
        "status": "saved",
        "rule_count": len(rules),
    }