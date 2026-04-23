# Smart Campus Sensor & Room Management API

A RESTful API built with **JAX-RS (Jersey 2.41)**, **Java 8**, and deployed on **Apache Tomcat**. This system manages campus Rooms and IoT Sensors as part of the university's Smart Campus initiative.



## Table of Contents
1. [API Overview](#api-overview)
2. [Project Structure](#project-structure)
3. [How to Build & Run](#how-to-build--run)
4. [Sample curl Commands](#sample-curl-commands)
5. [Report: Answers to Questions](#report-answers-to-questions)



## API Overview

| Resource | Base Path | Description |
|---|---|---|
| Discovery | `GET /api/v1` | API metadata and HATEOAS links |
| Rooms | `/api/v1/rooms` | Create, list, fetch, delete rooms |
| Sensors | `/api/v1/sensors` | Register, list, filter sensors |
| Readings | `/api/v1/sensors/{id}/readings` | Historical data per sensor |

All responses are in `application/json`. Data is stored in-memory using `ConcurrentHashMap`.



## Project Structure


smart-campus-api/
├── pom.xml
└── src/
    └── main/
        ├── java/com/smartcampus/
        │   ├── application/
        │   │   ├── SmartCampusApplication.java   ← JAX-RS Application config
        │   │   └── DataStore.java                ← Singleton in-memory store
        │   ├── model/
        │   │   ├── Room.java
        │   │   ├── Sensor.java
        │   │   └── SensorReading.java
        │   ├── resource/
        │   │   ├── DiscoveryResource.java         ← GET /api/v1
        │   │   ├── RoomResource.java              ← /api/v1/rooms
        │   │   ├── SensorResource.java            ← /api/v1/sensors
        │   │   └── SensorReadingResource.java     ← /api/v1/sensors/{id}/readings
        │   ├── exception/
        │   │   ├── RoomNotEmptyException.java
        │   │   ├── RoomNotEmptyExceptionMapper.java        ← 409
        │   │   ├── LinkedResourceNotFoundException.java
        │   │   ├── LinkedResourceNotFoundExceptionMapper.java ← 422
        │   │   ├── SensorUnavailableException.java
        │   │   ├── SensorUnavailableExceptionMapper.java   ← 403
        │   │   └── GlobalExceptionMapper.java              ← 500
        │   └── filter/
        │       └── LoggingFilter.java             ← Request/Response logger
        └── webapp/
            └── WEB-INF/
                └── web.xml




## How to Build & Run

### Prerequisites
- Java 8 (JDK)
- Apache Maven 3.6+
- Apache Tomcat 9.x
- NetBeans IDE 

### Step 1 — Clone / Open the project
```bash
git clone https://github.com/YOUR_USERNAME/smart-campus-api.git
cd smart-campus-api
```
Or in NetBeans: **File → Open Project** → select the `smart-campus-api` folder.

### Step 2 — Build the WAR file
```bash
mvn clean package
```
This produces `target/smart-campus-api.war`.

### Step 3 — Deploy to Tomcat

**Option A — NetBeans (recommended):**
1. In NetBeans, right-click the project → **Properties → Run**
2. Set Server to **Apache Tomcat**
3. Click the green **Run** button — NetBeans builds and deploys automatically.

**Option B — Manual:**
1. Copy `target/smart-campus-api.war` into Tomcat's `webapps/` folder.
2. Start Tomcat: `bin/startup.sh` (Linux/Mac) or `bin/startup.bat` (Windows).

### Step 4 — Test
Open your browser or Postman and navigate to:
```
http://localhost:8080/smart-campus-api/api/v1
```


## Sample curl Commands

### 1. Discovery Endpoint
bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1
```

### 2. Create a new Room
bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"HALL-205","name":"Main Hall","capacity":200}'


### 3. Get all Rooms
bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/rooms


### 4. Register a new Sensor (with valid roomId)
bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-005","type":"CO2","status":"ACTIVE","currentValue":350.0,"roomId":"HALL-205"}'


### 5. Filter sensors by type
bash
curl -X GET "http://localhost:8080/smart-campus-api/api/v1/sensors?type=CO2"


### 6. Post a sensor reading
bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/CO2-005/readings \
  -H "Content-Type: application/json" \
  -d '{"value":412.5}'


### 7. Get reading history for a sensor
bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/sensors/TEMP-001/readings
```

### 8. Delete a room that has sensors (triggers 409 Conflict)
bash
curl -X DELETE http://localhost:8080/smart-campus-api/api/v1/rooms/LIB-301


### 9. Register sensor with invalid roomId (triggers 422)
bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-999","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"FAKE-ROOM"}'


### 10. Delete a room (success — no sensors)
bash
curl -X DELETE http://localhost:8080/smart-campus-api/api/v1/rooms/HALL-205


---

## Report: Answers to Questions

---

### Part 1.1 — JAX-RS Resource Lifecycle

**Question:** Explain the default lifecycle of a JAX-RS Resource class. Is a new instance created per request or is it a singleton? How does this affect in-memory data management?

**Answer:**

By default, JAX-RS creates a **new instance of every resource class for each incoming HTTP request** (request-scoped lifecycle). This is the default behaviour defined by the JAX-RS specification. The rationale is thread-safety: because each request gets its own object, instance fields cannot be shared accidentally between concurrent threads.

However, this per-request instantiation has a critical implication for **in-memory data management**. If the data (e.g., a `HashMap` of rooms) were stored as an instance field inside the resource class, it would be re-created fresh for every request — meaning all previously stored data would be lost. To prevent this, this API uses the **Singleton design pattern** via the `DataStore` class. `DataStore.getInstance()` returns a single shared instance held as a `static final` field, which persists across all requests for the lifetime of the application.

Furthermore, because multiple requests can arrive concurrently, the shared collections must be **thread-safe**. This is why `ConcurrentHashMap` is used instead of `HashMap`. `ConcurrentHashMap` uses segment-level locking internally, allowing concurrent reads without blocking while still preventing data corruption on writes. Using a plain `HashMap` in a multi-threaded context would risk race conditions, corrupted state, or even infinite loops during rehashing.

---

### Part 1.2 — HATEOAS (Hypermedia as the Engine of Application State)

**Question:** Why is HATEOAS considered a hallmark of advanced RESTful design? How does it benefit client developers compared to static documentation?

**Answer:**

HATEOAS is the principle that API responses should include **hyperlinks** that guide the client to related actions and resources, rather than requiring the client to hard-code URLs. It is Level 3 of the Richardson Maturity Model — the highest level of REST compliance.

The key benefit is **discoverability**. A client that receives a response containing `"_links": {"readings": "/api/v1/sensors/TEMP-001/readings"}` does not need to know that URL in advance — the server communicates it dynamically. This is a significant improvement over static documentation because:

1. **URLs can change server-side** without breaking clients — clients follow links rather than hard-coded paths.
2. **Reduced coupling** — clients are not tightly bound to the API's internal structure.
3. **Self-documenting** — a developer can explore the API by following links, similar to browsing a website.
4. **Workflow guidance** — the server can communicate which actions are currently available (e.g., a room with sensors will not include a "delete" link), preventing invalid requests before they are made.



### Part 2.1 — Returning IDs vs Full Objects in Lists

**Question:** What are the implications of returning only IDs versus full room objects in a list response?

**Answer:**

**Returning only IDs** produces a very small payload (e.g., `["LIB-301", "LAB-101"]`), which minimises network bandwidth. However, the client must then make a separate `GET /rooms/{id}` request for each room to retrieve its details. This results in the **N+1 request problem** — one request for the list, then N more for the details — increasing latency significantly when the collection is large.

**Returning full objects** increases the payload size but gives the client everything it needs in a single round-trip. This is preferable in most cases because modern networks handle moderate JSON payloads efficiently, and reducing round-trips dramatically improves perceived performance.

A good compromise, used in production APIs, is **sparse fieldsets** or **projection parameters** (e.g., `?fields=id,name`) that let the client declare which fields it needs. This API returns full room objects in the list to maximise usability for campus facilities managers who need all room details at a glance.



### Part 2.2 — Idempotency of DELETE

**Question:** Is DELETE idempotent in your implementation? Justify with what happens on repeated identical requests.

**Answer:**

In this implementation, `DELETE /api/v1/rooms/{roomId}` is **partially idempotent in terms of server state but not in HTTP response code**.

- **First DELETE call:** The room is found in the `DataStore`, passes the sensor-check, is removed, and a `200 OK` is returned.
- **Second DELETE call (same room ID):** The room no longer exists in the `DataStore`. The service returns `404 Not Found` with a JSON error body.

The **server state is idempotent** — after the first successful deletion, subsequent calls do not change the server state further (the room remains absent). This satisfies the RFC 7231 definition of idempotency at the resource level.

However, the **HTTP response differs** between the first and subsequent calls (200 vs 404), which is a common and accepted design choice. Some strict REST purists argue that a repeated DELETE on a non-existent resource should return `204 No Content` to make the status code idempotent too, but returning `404` is more semantically informative and is the standard practice followed by most production APIs.



### Part 3.1 — @Consumes and Media Type Mismatches

**Question:** What happens if a client sends data in `text/plain` or `application/xml` to a method annotated with `@Consumes(MediaType.APPLICATION_JSON)`?

**Answer:**

The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells the JAX-RS runtime to only route requests to that method if the request's `Content-Type` header matches `application/json`.

If a client sends a request with `Content-Type: text/plain` or `Content-Type: application/xml`, JAX-RS performs **content negotiation** and finds no matching resource method. It automatically returns **HTTP 415 Unsupported Media Type** — without the resource method ever being invoked. This is handled entirely by the framework, not by application code.

This is a valuable safety mechanism: it prevents malformed or unexpected data formats from reaching business logic. The client is clearly informed that the format is wrong, and the developer knows exactly what format to use. Without `@Consumes`, a resource method might receive garbled data and throw an opaque internal error, making debugging much harder.



### Part 3.2 — @QueryParam vs @PathParam for Filtering

**Question:** Why is the query parameter approach (`/sensors?type=CO2`) superior to a path-based approach (`/sensors/type/CO2`) for filtering collections?

**Answer:**

**Query parameters** (`@QueryParam`) are semantically correct for filtering because they are **optional modifiers** on a collection resource. The resource `/api/v1/sensors` represents all sensors. Adding `?type=CO2` narrows the view without implying that `CO2` is a sub-resource or a separate entity. Key advantages:

1. **Optionality** — `GET /sensors` and `GET /sensors?type=CO2` share the same endpoint. Path parameters would require two separate resource methods.
2. **Multiple filters** — Query parameters compose naturally: `?type=CO2&status=ACTIVE`. A path-based approach becomes unmanageable: `/sensors/type/CO2/status/ACTIVE`.
3. **Correct semantics** — REST convention treats path segments as resource identifiers. `/sensors/CO2` falsely implies `CO2` is a specific sensor resource, not a filter criteria.
4. **Caching** — Query parameters are understood by HTTP caches as variations of the same resource, so `/sensors?type=CO2` and `/sensors?type=Temperature` can be cached separately but are clearly related.
5. **Client simplicity** — Standard HTTP libraries handle query string encoding automatically, making client code simpler.



### Part 4.1 — Sub-Resource Locator Pattern Benefits

**Question:** Discuss the architectural benefits of the Sub-Resource Locator pattern for managing complexity in large APIs.

**Answer:**

The **Sub-Resource Locator pattern** delegates responsibility for a nested path to a separate, dedicated resource class. In this API, `SensorResource` handles `/sensors/{id}` and returns a `SensorReadingResource` instance for the `/sensors/{id}/readings` sub-path.

The architectural benefits are:

1. **Separation of Concerns** — Each class has a single, well-defined responsibility. `SensorResource` manages sensor identity and registration; `SensorReadingResource` manages reading history. Neither class becomes a "god object" with hundreds of methods.

2. **Maintainability** — In a large campus API with dozens of nested resources, placing all path handlers in a single class would result in thousands of lines of code. Sub-resource classes keep files manageable and individually testable.

3. **Dynamic instantiation** — The locator receives the path parameter (`sensorId`) at runtime and can perform validation or inject context before constructing the sub-resource object, as shown in our implementation where we verify the sensor exists before constructing `SensorReadingResource`.

4. **Reusability** — `SensorReadingResource` could theoretically be reused from multiple parent resources without modification.

5. **Testability** — Each class can be unit tested independently without needing to boot the entire application.



### Part 5.2 — Why HTTP 422 is More Accurate Than 404 for Payload Reference Errors

**Question:** Why is HTTP 422 Unprocessable Entity more semantically accurate than 404 Not Found when a JSON payload references a non-existent resource?

**Answer:**

`404 Not Found` means **the requested URL/resource does not exist** on the server. If a client sends `POST /api/v1/sensors`, the URL `/api/v1/sensors` very much exists — the endpoint is valid and reachable. Returning 404 would be misleading because it implies the API route itself is broken.

`422 Unprocessable Entity` means the server **understood the request format and the URL is valid**, but the **content of the request body is semantically invalid** and cannot be processed. In this case, the JSON is well-formed and parseable, but the `roomId` field references a room that does not exist — a business-logic validation failure, not a routing failure.

The distinction is:
- `400 Bad Request` → malformed syntax (broken JSON, missing required fields)
- `404 Not Found` → the URL/route does not exist
- `422 Unprocessable Entity` → the URL exists, the JSON parses correctly, but the **business rules reject the data**

Using `422` gives the client precise, actionable feedback: "Your request was understood, but the data inside it is logically invalid." This prevents client developers from mistakenly thinking the endpoint doesn't exist, and clearly signals they need to fix the payload, not the URL.



### Part 5.4 — Cybersecurity Risks of Exposing Stack Traces

**Question:** From a cybersecurity standpoint, what risks are associated with exposing Java stack traces to external API consumers?

**Answer:**

Exposing raw Java stack traces to API consumers is a serious **information disclosure vulnerability** (mapped to OWASP A05: Security Misconfiguration). An attacker can extract:

1. **Internal package and class names** — e.g., `com.smartcampus.resource.SensorResource` reveals the internal code structure, making it easier to reason about logic and find attack vectors.

2. **Framework and library versions** — e.g., `org.glassfish.jersey.server 2.41` or `com.fasterxml.jackson 2.15` tells an attacker exactly which libraries are in use. They can then look up **known CVEs (Common Vulnerabilities and Exposures)** for those specific versions and craft targeted exploits.

3. **File system paths** — stack traces often include absolute file paths (e.g., `/opt/tomcat/webapps/...`), revealing the server's directory structure.

4. **Internal logic and data flow** — the sequence of method calls reveals how the application processes data, which can expose business logic flaws or injection points.

5. **Database queries or connection strings** (in lower-layer exceptions) — SQL exceptions sometimes include the query that failed, potentially revealing table names or data.

The `GlobalExceptionMapper` in this API addresses this by catching all `Throwable` instances, logging the full stack trace **server-side only** (where only administrators can see it), and returning a generic, safe `500 Internal Server Error` message to the client with no technical detail. This follows the **principle of least privilege** for information disclosure.



### Part 5.5 — Why Use JAX-RS Filters for Cross-Cutting Concerns

**Question:** Why is it better to use JAX-RS filters for logging rather than inserting Logger statements into every resource method?

**Answer:**

**Cross-cutting concerns** are behaviours that apply across the entire application regardless of business logic — logging, authentication, rate limiting, and CORS headers are classic examples. JAX-RS filters (`ContainerRequestFilter` / `ContainerResponseFilter`) are the correct mechanism for these because:

1. **Single point of implementation** — The `LoggingFilter` class handles logging for every single endpoint automatically. Adding `Logger.info()` to every resource method would require changes in dozens of places.

2. **No code duplication** — Repeated manual logging violates the **DRY (Don't Repeat Yourself)** principle. If the log format needs to change, a filter requires one edit; manual logging requires editing every method.

3. **Consistency** — Filters guarantee uniform log format and coverage. Manual logging is error-prone — a developer might forget to add it to a new method, creating blind spots in observability.

4. **Separation of Concerns** — Resource methods should contain only business logic. Mixing in logging code makes methods harder to read and test.

5. **Easy toggling** — A filter can be enabled or disabled globally (e.g., in production vs. development environments) from one place, whereas removing manual log statements would require touching every file.

6. **Lifecycle guarantees** — `ContainerRequestFilter` fires before the resource method executes and `ContainerResponseFilter` fires after — guaranteeing that both the incoming request and outgoing status code are always captured, even if the resource method throws an exception.
