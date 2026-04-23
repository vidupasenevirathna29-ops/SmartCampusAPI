# Smart Campus API

## API Overview

Smart Campus API is a JAX-RS (Jersey 2.41) REST API deployed on Tomcat 9. It lets you manage campus **rooms** and the **sensors** inside them, and keeps a history of readings for each sensor. All data is kept in memory using `HashMap` and `ArrayList` inside a singleton `DataStore` — there's no database.

**Base URL:** `http://localhost:8080/api/v1`

The WAR is packaged as `ROOT.war` so Tomcat serves it at the context root `/`, making every endpoint available at `/api/v1/...`.

**Resource Hierarchy:**

```
GET /api/v1                                      ← Discovery root

/api/v1/rooms
  GET    /api/v1/rooms                           ← List all rooms
  POST   /api/v1/rooms                           ← Create a room (201)
  GET    /api/v1/rooms/{roomId}                  ← Get one room (200 / 404)
  PUT    /api/v1/rooms/{roomId}                  ← Update a room (200 / 404)
  DELETE /api/v1/rooms/{roomId}                  ← Delete a room (204 / 404 / 409)

/api/v1/sensors
  POST   /api/v1/sensors                         ← Register a sensor (201 / 422)
  GET    /api/v1/sensors                         ← List all; optional ?type= filter
  GET    /api/v1/sensors/{sensorId}              ← Get one sensor (200 / 404)
  PUT    /api/v1/sensors/{sensorId}              ← Update a sensor (200 / 404)
  DELETE /api/v1/sensors/{sensorId}              ← Delete a sensor (204 / 404)

  /api/v1/sensors/{sensorId}/readings            ← Sub-resource (locator pattern)
    GET  /api/v1/sensors/{sensorId}/readings     ← Fetch reading history
    POST /api/v1/sensors/{sensorId}/readings     ← Append a reading (201 / 403 / 404)
```

**HTTP Status Codes used:**

| Code | When used |
|------|-----------|
| 200 | Successful GET / PUT |
| 201 | Successful POST — includes `Location` header |
| 204 | Successful DELETE |
| 400 | Missing required fields in body |
| 403 | POST reading to MAINTENANCE / OFFLINE sensor |
| 404 | Resource does not exist |
| 409 | DELETE room that still has sensors |
| 415 | Request `Content-Type` is not `application/json` |
| 422 | POST sensor with a `roomId` that doesn't exist |
| 500 | Unexpected runtime error |

---

## Build and Launch Instructions

### Prerequisites

| Tool | Version |
|------|---------|
| Java JDK | 11 or higher |
| Apache Maven | 3.8 or higher |
| Apache Tomcat | 9.x (not 10+) |

### Step 1 — Clone the repository

```bash
git clone https://github.com/<your-username>/SmartCampusAPI.git
cd SmartCampusAPI
```

### Step 2 — Build the WAR file

```bash
mvn clean package -DskipTests
```

This compiles all Java source files and packages them into `target/ROOT.war`.

### Step 3 — Stop Tomcat (if already running)

```bash
# Windows
%CATALINA_HOME%\bin\shutdown.bat

# Linux / macOS
$CATALINA_HOME/bin/shutdown.sh
```

### Step 4 — Remove any existing ROOT deployment

```bash
# Windows
del /q %CATALINA_HOME%\webapps\ROOT.war
rmdir /s /q %CATALINA_HOME%\webapps\ROOT

# Linux / macOS
rm -rf $CATALINA_HOME/webapps/ROOT $CATALINA_HOME/webapps/ROOT.war
```

### Step 5 — Copy ROOT.war to Tomcat

```bash
# Windows
copy target\ROOT.war %CATALINA_HOME%\webapps\ROOT.war

# Linux / macOS
cp target/ROOT.war $CATALINA_HOME/webapps/ROOT.war
```

### Step 6 — Start Tomcat

```bash
# Windows
%CATALINA_HOME%\bin\startup.bat

# Linux / macOS
$CATALINA_HOME/bin/startup.sh
```

### Step 7 — Verify the server is running

Open Tomcat's log (`logs/catalina.out`) and look for:

```
INFO: Server startup in [N] ms
```

Then run a quick smoke test:

```bash
curl http://localhost:8080/api/v1
```

Expected: `200 OK` with a JSON body containing `"name": "Smart Campus API"`.

---

## Sample curl Commands

All commands assume the server is running at `http://localhost:8080`.

### 1 — GET discovery endpoint

```bash
curl -s http://localhost:8080/api/v1
```

**Expected 200 OK:**
```json
{
  "name": "Smart Campus API",
  "version": "1.0",
  "status": "operational",
  "_links": {
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

---

### 2 — POST create a room

```bash
curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"name": "Lab A101", "capacity": 30}'
```

**Expected 201 Created:**
```json
{
  "id": "a1b2c3d4-e5f6-...",
  "name": "Lab A101",
  "capacity": 30,
  "sensorIds": []
}
```

The response includes a `Location` header: `http://localhost:8080/api/v1/rooms/a1b2c3d4-...`

---

### 3 — POST register a sensor (linked to an existing room)

Using the seeded room `LIB-301` that exists on startup:

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type": "Humidity", "roomId": "LIB-301"}'
```

**Expected 201 Created:**
```json
{
  "id": "f7a8b9c0-...",
  "type": "Humidity",
  "status": "ACTIVE",
  "currentValue": 0.0,
  "roomId": "LIB-301"
}
```

---

### 4 — GET list sensors filtered by type

```bash
curl -s "http://localhost:8080/api/v1/sensors?type=Temperature"
```

**Expected 200 OK:** An array of all sensors whose type is `Temperature` (case-insensitive match).

```json
[
  {
    "id": "TEMP-001",
    "type": "Temperature",
    "status": "ACTIVE",
    "currentValue": 22.5,
    "roomId": "LIB-301"
  },
  {
    "id": "TEMP-002",
    "type": "Temperature",
    "status": "MAINTENANCE",
    "currentValue": 0.0,
    "roomId": "LAB-202"
  }
]
```

---

### 5 — POST add a sensor reading

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 24.1}'
```

**Expected 201 Created:**
```json
{
  "id": "r1r2r3r4-...",
  "value": 24.1,
  "timestamp": 1745352761000
}
```

The parent sensor's `currentValue` is also updated to `24.1`.

---

### 6 — DELETE room with sensors → 409 Conflict

```bash
curl -s -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

**Expected 409 Conflict:**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Room 'LIB-301' cannot be deleted because it still has sensors. Remove all sensors first."
}
```

---

### 7 — POST sensor with invalid roomId → 422

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type": "CO2", "roomId": "does-not-exist"}'
```

**Expected 422 Unprocessable Entity:**
```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Linked resource 'Room' with id 'does-not-exist' was not found."
}
```

---

### 8 — POST reading to MAINTENANCE sensor → 403

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/TEMP-002/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 99.9}'
```

`TEMP-002` is seeded with status `MAINTENANCE`.

**Expected 403 Forbidden:**
```json
{
  "error": "Forbidden",
  "message": "Sensor 'TEMP-002' is currently MAINTENANCE and cannot accept readings.",
  "sensorId": "TEMP-002",
  "currentStatus": "MAINTENANCE"
}
```

---

## Report Question Answers

### Part 1 — Project Bootstrap

#### Q1.1 — JAX-RS Resource Lifecycle: Request-Scoped vs Singleton

By default, JAX-RS creates a **new instance of each resource class for every HTTP request** (request-scoped). Fields declared inside a resource class such as `RoomResource` are freshly initialised on every call and are not shared between requests. This design keeps REST services stateless: each request is fully isolated, there are no shared mutable fields, and JAX-RS can safely inject request-specific objects like `UriInfo` via `@Context`.

The consequence in this project is that all persistent state — rooms, sensors, readings — lives inside the `DataStore` singleton (`DataStore.getInstance()`), not as fields on the resource classes. The resource classes are disposable per-request shells; `DataStore` is the single source of truth that survives across the lifetime of the server. Making a resource a true singleton is possible using `@Singleton`, but that forces every instance field to be thread-safe, adding unnecessary complexity for most use cases.

---

#### Q1.2 — HATEOAS and Hypermedia Benefits

HATEOAS (Hypermedia As The Engine Of Application State) is the REST constraint that responses should include links to related or next-step resources, so clients can navigate the API dynamically instead of relying on hardcoded URLs.

In this project the discovery endpoint (`GET /api/v1`) returns a `_links` object pointing to the rooms and sensors collections. This brings four concrete benefits:

1. **Discoverability** — A new client only needs to know one URL (the root) and can discover all other endpoints from the response, reducing coupling to external documentation.
2. **Evolvability** — If the `/rooms` path changes in a future version, clients that follow links from the discovery response automatically pick up the new path without code changes on their side.
3. **Self-documenting** — The API response itself describes what actions are possible and where, acting as a live contract alongside any written documentation.
4. **Reduced hardcoding** — Clients follow `href` values from responses rather than embedding paths, making integrations more maintainable and easier to test.

---

### Part 2 — Room Management

#### Q2.1 — Returning Full Objects vs Returning Just IDs

`GET /rooms` returns **full `Room` objects** (including `name`, `capacity`, and `sensorIds`) rather than a list of IDs only.

Returning full objects costs more bandwidth per response but saves the client from making N additional requests to resolve each ID — the classic N+1 problem. A campus management dashboard typically renders the full room list with names and capacities in a single view. At the expected scale of a campus system (tens to low hundreds of rooms), one slightly larger payload is far cheaper than dozens of sequential HTTP round-trips. For very large collections or lazy-loading UIs, returning only IDs with pagination links would be the better trade-off, but for this domain returning full objects is the pragmatic choice that minimises latency.

---

#### Q2.2 — DELETE Idempotency

RFC 9110 defines an idempotent method as one where applying it multiple times produces the same server-side state as applying it once. DELETE is idempotent by specification: calling `DELETE /rooms/abc` twice leaves the server in the same state both times — room `abc` does not exist.

In this implementation the first call deletes the room and returns `204 No Content`. The second call finds nothing and returns `404 Not Found`. The HTTP response code differs, but the server state is identical: the room is absent. This satisfies idempotency per the RFC. Returning `404` on the second call is preferable to returning `204` again because it gives the client honest, accurate feedback — the resource was already gone — without misrepresenting a no-op as a successful deletion.

---

### Part 3 — Sensor Operations

#### Q3.1 — `@Consumes` Annotation and Format Mismatch Consequences

`@Consumes(MediaType.APPLICATION_JSON)` declares the acceptable `Content-Type` for incoming request bodies. When Jersey sees this annotation on a method, it acts as a gate before the method body runs:

- **Matching Content-Type** (`application/json`): Jersey deserialises the body via Jackson and passes the resulting Java object to the method.
- **Mismatching Content-Type** (`text/plain`, `application/xml`, etc.): Jersey rejects the request immediately and returns `415 Unsupported Media Type` — the method body never executes.

Declaring `@Consumes` explicitly prevents malformed or unexpected data from reaching business logic, provides a clear machine-readable contract to API consumers, eliminates the need for manual content-type checking inside every method, and fails fast with an unambiguous error rather than a mysterious `NullPointerException` further down the call stack.

---

#### Q3.2 — `@QueryParam` vs Path Segment for Filtering

Sensor type filtering uses `GET /sensors?type=CO2` rather than `GET /sensors/CO2`.

A path segment implies that the named thing is a **distinct, addressable resource** with its own identity. `GET /sensors/CO2` would suggest CO2 is a resource of its own, not a filter criterion. Sensor type is a narrowing parameter applied to the collection — not a resource — so a query parameter is semantically correct. Query parameters are designed precisely for optional, refinement-style operations on a collection.

There is also a cacheability advantage: HTTP caches often key on the base URI (`/sensors`), treating query parameters separately. The base collection can be cached independently, whereas path-based filtering merges filtering and resource identity into one URL, making caching harder to reason about.

---

### Part 4 — Sub-Resources

#### Q4.1 — Sub-Resource Locator Architectural Benefits

A sub-resource locator is a JAX-RS method annotated with `@Path` but with **no HTTP method annotation** (`@GET`, `@POST`, etc.). Instead of handling the request itself, it returns a plain Java object — the sub-resource — which Jersey then inspects for the matching HTTP method handler.

In this project, `SensorResource` contains a locator method that returns a `SensorReadingResource` instance constructed with the `sensorId` from the path. This design brings five architectural benefits:

1. **Separation of concerns** — `SensorResource` handles sensor CRUD; `SensorReadingResource` handles reading history. Neither class is aware of the other's implementation details, keeping each class focused and small.
2. **Constructor-based context injection** — `SensorReadingResource` receives `sensorId` via its constructor as a plain Java argument, making it a normal object with no JAX-RS routing knowledge required.
3. **Lazy instantiation** — Jersey only creates `SensorReadingResource` when the `/readings` path is actually requested, avoiding unnecessary object creation for requests that don't touch readings.
4. **Testability** — `SensorReadingResource` can be unit-tested without a running JAX-RS container by simply constructing it with a test `sensorId` and calling its methods directly.
5. **Clean URL hierarchy** — The nested path `/sensors/{id}/readings` naturally and readably expresses the parent-child ownership relationship between sensors and their readings.

---

### Part 5 — Error Handling and Logging

#### Q5.2 — HTTP 422 Unprocessable Entity vs 404 Not Found

Both codes could superficially apply when a referenced resource is missing, but they carry different semantic meanings:

- **404 Not Found** means the *requested URL itself* does not exist. It is the correct code when, for example, `GET /rooms/abc` is called and room `abc` does not exist — the resource identified by the URL is absent.
- **422 Unprocessable Entity** means the request URL is valid and the body is syntactically correct JSON, but the content is semantically invalid and cannot be processed.

When `POST /api/v1/sensors` is called with a `roomId` that does not exist, the endpoint `/api/v1/sensors` exists (so 404 would be wrong), and the body is valid JSON (so 400 would be wrong). The problem is that the `roomId` value inside the body refers to a room that doesn't exist — the request is semantically unprocessable. HTTP 422 captures this precisely. Using 404 here would be ambiguous; a client could reasonably think the `/sensors` endpoint itself was not found rather than understanding that a referenced entity was missing.

---

#### Q5.4 — Security Risks of Exposing Stack Traces

Returning raw Java stack traces in HTTP error responses poses three categories of security risk:

1. **Information leakage** — Stack traces reveal internal class names, method signatures, library versions, and file paths. An attacker can use this to identify the exact framework and version (enabling targeted exploit searches), map the internal package structure, and discover vulnerable third-party dependencies.
2. **Sensitive data exposure** — Stack traces may inadvertently print variable values, query fragments, or configuration data that were on the call stack at the time of the exception, potentially exposing credentials, tokens, or business logic.
3. **Reconnaissance** — Knowing the exact line number of a crash helps an attacker refine their attack vector without needing access to the source code.

The mitigation in this project is implemented in `GlobalExceptionMapper`: it catches all `Throwable` instances, logs the full stack trace server-side to Tomcat's log (visible only to system administrators), and returns a safe, generic JSON body to the client — `"An unexpected error occurred. Please contact support."` The principle is: *log everything internally, expose nothing externally*.

---

#### Q5.5 — Why Filters Are Better Than Manual Logging

Manual logging — calling `Logger.info(...)` at the start and end of every resource method — has serious drawbacks: it duplicates the same boilerplate in every endpoint, developers forget to add it to new endpoints, different developers produce inconsistent log formats, and changing the format requires editing every resource class. Logging is also a cross-cutting concern that has nothing to do with business logic, so embedding it inside resource methods violates the single-responsibility principle.

Filter-based logging via `ApiLoggingFilter` (implementing `ContainerRequestFilter` and `ContainerResponseFilter`) solves all of these problems:

1. **Single implementation** — Registered once in `SmartCampusApplication`, the filter intercepts every request and response automatically with no changes to resource classes.
2. **Consistent format** — One place controls the log format for the entire API.
3. **Zero boilerplate in resources** — Resource classes remain focused on business logic only.
4. **Correct architectural layer** — Cross-cutting concerns belong in filters, not in business logic.
5. **Toggleable** — Removing the filter from the `Application` class disables all API logging instantly without touching any resource code.
