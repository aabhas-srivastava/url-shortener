# Snip — URL Shortener

Spring Boot URL shortener with a web UI. Paste a long link, get a short one, track clicks, and generate a QR code.

**Stack:** Java 17 · Spring Boot 3.2 · MySQL 8 · Redis 7 · Docker

## Features

- Auto Base62 codes (6 chars) or custom codes (3–10 alphanumeric)
- Optional expiry (default 30 days)
- 302 redirect, click analytics, PNG QR codes
- Rate limit: 10 creates per IP per minute
- Redis cache (10 min TTL)
- UI at `/` — Create, Analytics, History

## Run with Docker

```bash
docker compose up --build
```

Open http://localhost:8080

| Service | Port |
| --- | --- |
| App | 8080 |
| MySQL | 3307 |
| Redis | 6379 |

Set `APP_BASE_URL` (or `app.base-url`) to your public URL when you deploy.

## Run locally

Need JDK 17, Maven, MySQL (`localhost:3306`, user `root` / `secret`), and Redis (`6379`). Then:

```bash
mvn spring-boot:run
```

For Docker MySQL from the host, use port **3307**.

## API

| Method | Path | What it does |
| --- | --- | --- |
| `POST` | `/api/urls` | Create short URL |
| `GET` | `/{shortCode}` | Redirect |
| `GET` | `/api/urls/{shortCode}/analytics` | Click stats |
| `GET` | `/api/urls/{shortCode}/qr` | QR PNG |
| `DELETE` | `/api/urls/{shortCode}` | Deactivate |

**Create body**

```json
{
  "originalUrl": "https://example.com/long/path",
  "customCode": "java101",
  "expiresAt": "2026-12-31T00:00:00"
}
```

`customCode` and `expiresAt` are optional.

Errors: `400` invalid input · `404` not found · `409` code taken · `410` expired · `429` rate limited
