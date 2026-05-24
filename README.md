# Forex Rate Proxy

A local proxy service for getting currency exchange rates. It's a service that can be consumed by other internal services to get the exchange rate between a set of currencies so they don't have to care about the specifics of third-party providers.

The core challenge: the upstream [One-Frame](https://hub.docker.com/r/paidyinc/one-frame) service allows only **1,000 requests/day** per token, but the proxy must support **10,000 requests/day**. This is solved with a background cache that pre-fetches all rates periodically so user requests never hit One-Frame directly.

---

## Running the app

### Option 1 - Docker Compose (recommended)

Starts both One-Frame and the forex proxy with a single command.

```bash
./gradlew build -x test && docker-compose up       # macOS / Linux
gradlew.bat build -x test && docker-compose up     # Windows
```

The app is ready at `http://localhost:9090`.

### Option 2 - Local (without Docker)

**Step 1** - Start One-Frame:
```bash
docker run -p 8080:8080 paidyinc/one-frame
```

**Step 2** - Run the app:

Requires Java 21. Set `JAVA_HOME` for your platform if not already pointing to Java 21 then run:
```bash
./gradlew bootRun        # macOS / Linux
gradlew.bat bootRun      # Windows
```

The app is ready at `http://localhost:9090`.

---

## API

Swagger UI is available at `http://localhost:9090/swagger-ui.html` once the app is running.

### Get exchange rate

```
GET /rates?from={currency}&to={currency}
```

**Supported currencies:** `AUD`, `CAD`, `CHF`, `EUR`, `GBP`, `NZD`, `JPY`, `SGD`, `USD`

**Example:**
```bash
curl "http://localhost:9090/rates?from=USD&to=JPY"
```

**Success (200):**
```json
{
  "from": "USD",
  "to": "JPY",
  "price": "0.71",
  "timestamp": "2026-05-23T10:00:00Z"
}
```

**Error responses:**

| Status | Code | Reason |
|--------|------|--------|
| 400 | `CURRENCY_NOT_FOUND` | Unsupported currency code |
| 400 | `SAME_CURRENCY_PAIR` | `from` and `to` are the same |
| 400 | `MISSING_PARAMETER` | `from` or `to` param absent |
| 503 | `RATE_NOT_AVAILABLE` | Cache empty or all entries expired |
| 502 | `RATE_LOOKUP_FAILED` | One-Frame upstream error |

---

## Architecture

```
User Request
     |
     |
RatesController        - validates params, maps errors to HTTP status
     |
     |
RateService            - validates currencies, checks same-pair
     |
     |
RateCache              - Caffeine in-memory cache (TTL: 5 min)
     |                   on miss, checks reverse pair and inverts price
     |
Either<DomainError, Rate>

-------------------------------------- background --------------------------

RateCacheRefresher     - @Scheduled every 2 min
     |
     |
RateLoader             - @Retryable: 4 attempts, 500ms->1s->2s backoff
     |
     |
OneFrameClient         - single HTTP call with all 36 pairs
     |
     |
One-Frame API          - GET /rates?pair=USDJPY&pair=USDEUR&...
```

User requests **never** call One-Frame directly - they only read from the cache.

---

## Design decisions

### Background cache refresh
All rates are pre-fetched in the background every 2 minutes and stored in a Caffeine cache with a 5-minute TTL. This decouples user request throughput from the upstream rate limit entirely.

With 9 currencies there are 36 unique pairs. One-Frame accepts all pairs in a single request (`?pair=USDJPY&pair=USDEUR&...`), so each refresh costs **1 API call**. At one call per 2 minutes that is ~720 calls/day, well under the 1,000/day limit.

### Reverse pair lookup with price inversion
Only 36 unique pairs are fetched (e.g. `USD->JPY`). If a user requests `JPY->USD`, the cache looks up the reverse entry and returns `1 / price`. This halves the cache storage and the upstream payload without losing any coverage.

### Retry with exponential backoff
`RateLoader` wraps `OneFrameClient` with `@Retryable` (4 attempts, 500ms --> 1s --> 2s backoff). On failure the refresher logs an error and retains the existing cache entries which remain valid until their TTL expires. With a 2-minute refresh interval and a 5-minute TTL there is a 3-minute buffer, meaning a single failed refresh does not affect users.

### Timeout on upstream calls
`OneFrameClient` configures a 3-second connect timeout and a 5-second read timeout (configurable via `application.yml`). This bounds how long a failing OneFrame request blocks the scheduler thread.

### Caffeine for TTL management
The cache uses Caffeine's `expireAfterWrite`. This removes the need for a wrapper class, handles memory cleanup of expired entries automatically.

### RestClient over Feign
`OneFrameClient` uses Spring's built-in `RestClient` rather than Feign. Feign requires an additional Spring Cloud dependency and a separate configuration class for timeouts and error handling. For a single upstream API with one endpoint, `RestClient` is simpler and sufficient. Feign would be worth considering if the service needed to integrate with multiple external APIs where declarative HTTP interfaces add consistency.

### Error handling with `Either`
Errors are modelled as `Either<DomainError, T>` throughout the domain layer. This makes failure handling explicit at compile time - callers cannot ignore an error path.

---

## Trade-offs and known limitations

### Startup race condition
The service starts and begins the first cache refresh (`initialDelay=0`) but there is a small window (typically 1–2 seconds) between the process starting and the first refresh completing. If a load balancer or Kubernetes marks the pod ready before the cache is warm, early requests return `503`.
The fix is a readiness probe that returns `DOWN` until the cache has at least one entry. Kubernetes will hold traffic until it returns `UP`. With Redis, rolling deployments avoid this entirely since the shared cache stays warm across restarts but a cold start (first deploy or cache flush) still has the same gap.

### Two consecutive refresh failures cause a ~60s gap
With TTL=5min and refresh=2min, two consecutive full failures (each exhausting all retries) leave a ~60-second window where cache entries expire before the next refresh succeeds. Mitigation options:
- Increase TTL to 10 minutes - larger buffer but rates could be up to 10 minutes stale
- Serve stale entries beyond TTL with a warning header - availability over freshness
- Fallback provider - extract a `RateProvider` interface, implement it for One-Frame and a secondary rate source and fall back in `@Recover` when all retries are exhausted. Spring Retry's `@Recover` already provides a natural hook for this pattern

### Fixed currency set
Supported currencies are defined in `application.yml` and validated against a sealed `Currency` class. Adding a new currency requires a code change and redeployment. A production system would drive currency support from a database or external config without requiring a release.

### In-memory cache does not scale horizontally
The current Caffeine cache is local to each instance. If the service is horizontally scaled behind a load balancer, each instance maintains its own cache and runs its own refresh scheduler independently - meaning N instances make N OneFrame calls per refresh cycle, multiplying the API budget usage by the number of instances.

For horizontal scaling, the cache should be replaced with a shared distributed cache such as Redis. Each instance would read from and write to the same Redis store so only one refresh is needed regardless of instance count.

Additionally, with multiple instances each running `@Scheduled` independently, the refresh scheduler fires N times per interval - each instance calling OneFrame separately. This multiplies API usage by instance count and can exhaust the 1,000/day budget quickly. The fix is to elect a single scheduler leader using a distributed lock (e.g. Redis or ShedLock library) so only one instance refreshes the cache at a time while all instances read from the shared store.

### Metrics (not implemented)
In production this service would benefit from:
- Cache hit/miss rate - to verify the caching strategy is working as expected
- OneFrame refresh success/failure count - to alert on consecutive failures before the TTL gap occurs
- Request latency - to confirm cache reads are fast
- Retry attempt count - to detect degraded upstream health early

Spring Boot Actuator with Micrometer and a Prometheus/Grafana stack would cover all of these with minimal code.

---

## Running tests

```bash
./gradlew test        # macOS / Linux
gradlew.bat test      # Windows
```

Open the test report:
```bash
open build/reports/tests/test/index.html        # macOS
```

---

## Configuration reference

| Property | Default | Description |
|----------|---------|-------------|
| `oneframe.base-url` | `http://localhost:8080` | One-Frame base URL |
| `oneframe.connect-timeout-ms` | `3000` | TCP connect timeout |
| `oneframe.read-timeout-ms` | `5000` | HTTP read timeout |
| `forex.cache-ttl-minutes` | `5` | How long a cached rate is considered fresh |
| `forex.refresh-interval-ms` | `120000` | Delay between cache refreshes (2 min) |
| `forex.supported-currencies` | `AUD,CAD,...` | Comma-separated list of supported currencies |
| `server.port` | `9090` | HTTP port |
