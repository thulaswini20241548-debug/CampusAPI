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

```
smartcampus/
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
```



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

http://localhost:8080/smart-campus-api/api/v1


## Sample curl Commands

### 1. Discovery Endpoint
bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1


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


## Report: Answers to Questions

### Part 1: Service Architecture & Setup
### 1. Project & Application Configuration

Question: Explain the default lifecycle of a JAX-RS Resource class. Is a new instance created per request or is it a singleton? How does this affect in-memory data management?

Answer:
According to JAX-RS specification ,The JAX-RS framework creates a new instance of resource classes for every new HTTP requests handled by default. This is done in order to allow threadsafety, meaning that the request only has access to instances of resource class for this single request.
However, this per-request instantiation raises a challenge when comes to data management in memory. Storing data in terms of using any collection like a HashMap as instance variable within the resource class causes creation of a new hashmap for each request and consequently results in losing all stored data. Hence, Singleton design pattern was done through DataStore API so that only 1 instance will be created every time DataStore The instance is called by calling getInstance() and assigned as a final static object within the class.
Also, as one can imagine multiple requests could come at the same time so we have to ensure data structures are thread safe . Using a regular HashMap in this situation could cause issues like race conditions, invalidation of data integrity, or infinite loops due to rehashing operations.therefore rather than using HashMap , we are using ConcurrentHashMap .

### 2. The "Discovery" Endpoint

Question: Why is HATEOAS considered a hallmark of advanced RESTful design? How does it benefit client developers compared to static documentation?

Answer:
HATEOAS is a REST architectual concept that hypermedia links within a RESTful service response tells the client where to go next as part of the application state, making it possible for clients to navigate entire end-to-end workflows by discovering both what they can do and how. HATEOAS is similar to Richardson Maturity Level 3
The biggest advantage is that you make the client discoverable. What that means is the client will not have to know the URL beforehand but rather get it from the server dynamically in its response body: "_links": {"readings": "/api/v1/sensors/TEMP-001/readings"}.
Justifying the use of the discovery process with dynamic URLS over documentation:

Dynamic URLs: The server can change its URL as it wants without inconveniencing client- as a true one will use the links in web pages instead of hard-coding any link.
Decoupling: The client does not need to understand of the server's inner workings
Self-documented: The developer could navigate the API in the browser.
Guidance for workflows: the server tells the client what actions are possible at any point (for example, a room with temperature sensors cannot delete its own sensors).


### Part 2: Room Management
### 1. Room Resource Implementation

Question: What are the implications of returning only IDs versus full room objects in a list response?

Answer:
Room IDs only: This is done so that minimal data is sent via the wire (such as ["LIB-301", "LAB-101"]). However, the client will have to make another request per room to fetch GET /rooms/{id}. This is known as N+1 requests where there is one request initially, and there are other N requests needed to fetch more details.
Sending room objects fully ensures increased transmission of data but reduces latency as all needed data would come from one request to the API. This approach is recommended in most situations since, although there may be some increase in network data transmission in the JSON format, it decreases round-trips greatly.
However, the best way to achieve both reduced payload and increased efficiency is using the sparse fieldsets, where a query parameter fields can be included (example: ?fields=id,name). In this API, I have chosen to send complete objects in the list due to its greater usability to the campus facility manager.

### 2. Room Deletion & Safety Logic

Question: Is DELETE idempotent in your implementation? Justify with what happens on repeated identical requests.

Answer:
For this implementation, DELETE /api/v1/rooms/{roomId} is idempotent in server state terms, but not idempotent in HTTP response status code terms.

First DELETE request: Room exists in DataStore, sensor-check succeeds, it gets deleted and HTTP response 200 OK is returned.
Second DELETE request for the same roomId: Room does not exist anymore in DataStore — response will be 404 Not Found, JSON payload.

Idempotency with respect to server state : In terms of server state, the invocation of DELETE /api/v1/rooms/{roomId} repeatedly will give the same outcome regarding the room itself.
Different HTTP responses for repetitive calls: The HTTP response status code differs for the first request and for any following requests (200 vs 404). While it may appear unprofessional for some developers, this approach is generally accepted. Server State Idempotency: In general, invoking DELETE /api/v1/rooms/{roomId} multiple times in a row will yield the same outcome for respect to the room itself (will not affect it).
Changing HTTP responses for each call: The first request and future requests differ on the HTTP response status code (200 vs 404). An uncommon practice when compared with some developers, but from my understanding an acceptable one.
Some purists of the REST API philosophy might require 204 No Content as the response from repeated DELETE request to ensure idempotency of the HTTP response. However, in reality, 404 Not Found is returned in production systems because of semantic reasons.

### Part 3: Sensor Operations & Linking
### 1. Sensor Resource & Integrity

Question: What happens if a client sends data in text/plain or application/xml to a method annotated with @Consumes(MediaType.APPLICATION_JSON)?

Answer:
The @Consumes(MediaType.APPLICATION_JSON) annotation advises the JAX-RS implementation to consider only requests for which the request headers hold application/json as content-type.
If client sends request headers like Content-Type=text/plain or Content-Type=application/xml, content negotiation performed by JAX-RS implementation results in no resource method being matched. Thus, the response returned by JAX-RS is HTTP 415 Unsupported Media Type without even invoking the resource method. This feature is beneficial as it helps to get rid of processing requests with unsupported data format in the application layer. The client, conversely, learns about his or her mistake; the developer knows which format of data is expected to arrive. In case the @Consume annotation has not been used, in that case wrong datatype would be sent to the resource method and you will receive error message.

### 2. Filtered Retrieval & Search

Question: Why is the query parameter approach (/sensors?type=CO2) superior to a path-based approach (/sensors/type/CO2) for filtering collections?

Answer:
Query Parameters (@QueryParam) are appropriate for filters since they add to the collection of resources. For instance, in the above example /api/v1/sensors is the collection of sensors. The inclusion of ?type=CO2 refers to a selection of one from the collections, ignoring CO2 as sub-resource/element.
Advantages:

Optionality – GET /sensors and GET /sensors?type=CO2 are two representations of the same endpoint. Using path parameters will force us to have two different resources' methods defined.
Multiple filters – Query parameters are easy to compose, e.g. ?type=CO2&status=ACTIVE. A path-based solution will become impractical soon, /sensors/type/CO2/status/ACTIVE.
Semantics correctness – As per REST convention, path segments are resource identifiers. Resource /sensors/CO2 means that CO2 is actually a resource.
Caching support – HTTP caching is well aware that query parameters mean that a certain resource varies on certain values. Therefore, /sensors?type=CO2 and /sensors?type=Temperature can be cached individually but are related.
Simplicity for client – Query strings are properly encoded by standard HTTP client libraries.


### Part 4: Deep Nesting with Sub-Resources
### 1. The Sub-Resource Locator Pattern

Question: Discuss the architectural benefits of the Sub-Resource Locator pattern for managing complexity in large APIs.

Answer:
In the Sub-Resource Locator pattern, the responsibility of handling the nested path is delegated to another resource. In the case of our API, the SensorResource will deal with the path /sensors/{id}, and it will provide SensorReadingResource for the sub-path /sensors/{id}/readings.
Architectural advantages include:

Single Responsibility Principle – This design pattern allows each class to have one specific job only. One class focuses on managing sensor identities and their registration; the other manages reading history. No classes become god classes.
Maintainability – With numerous nested classes in the case of a campus-wide API, putting all those paths in one class results in many lines of code in the file. This becomes unmaintainable.
Dynamic instantiation – Since the locator knows the value of the path parameter (sensorId), it could validate or pass any context information to the sub-resource during its construction, as was done in this design pattern's realization.
Reusability – The sub-resource in question could be used by several parent resources dynamically.
Testability – Classes can be unit-tested without running the whole application.


### Part 5: Advanced Error Handling, Exception Mapping & Logging
### 2. Dependency Validation

Question: Why is HTTP 422 Unprocessable Entity more semantically accurate than 404 Not Found when a JSON payload references a non-existent resource?

Answer:
The 404 Not Found status code means that the requested URL/resource is not found on the server. In the current example, the client sends a request POST /api/v1/sensors. It is obvious that the URL /api/v1/sensors is existing. Therefore, a response with the 404 status code is invalid since it indicates a wrong routing.
In turn, 422 Unprocessable Entity means that the server knows how to interpret the syntax of the request, recognizes the URL, but cannot process the content of the request. Even though the JSON presented above is correctly constructed, the roomId parameter refers to the non-existing room.
Thus, in brief, one has to recognize the differences between those statuses as follows:
Status CodeWhen to Use400 Bad RequestProblem with the syntax of the request (invalid or missing parameters)404 Not FoundThe URL/path does not exist at all422 Unprocessable EntityThe path/URL exists, but the content is wrong
Considering the above-mentioned, one may argue that 422 is preferable in that case, as the server will let the client know that the request is fine, while business logic forbids processing it.

### 4. The Global Safety Net

Question: From a cybersecurity standpoint, what risks are associated with exposing Java stack traces to external API consumers?

Answer:
The possibility of exposing stack traces to API consumers is considered a highly risky vulnerability, called security misconfiguration (OWASP A05). The attacker may be able to derive:

Package and class names, such as com.smartcampus.resource.SensorResource, which gives insight into the code structure inside the application.
Versions of the frameworks and libraries that were used, such as org.glassfish.jersey.server 2.41 and com.fasterxml.jackson 2.15. The attacker can refer to the corresponding CVEs (Common Vulnerabilities and Exposures) for these specific versions of those libraries and use known vulnerabilities.
Pathnames from the file system (/opt/tomcat/webapps/...), providing information on the file system structure.
Business logic and data flow - from the order of methods being called, allowing the attacker to discover potential vulnerabilities in the application's processing logic.
Potential database queries or connection strings - for lower-level errors that include SQL exceptions.

Fortunately, the GlobalExceptionMapper provided by the present API handles this issue by intercepting all kinds of exceptions which can occur under the category of Throwable, logging these only internally, i.e., on the server side (administrators only have access to it), and responding to the client with a simple 500 error message.

### 5. API Request & Response Logging Filters

Question: Why is it better to use JAX-RS filters for logging rather than inserting Logger statements into every resource method?

Answer:
Cross-cutting concerns refer to aspects that are independent of the functionality of an application. Logging, security, request rate limiting, and CORS header settings are a few examples of cross-cutting concerns. The most appropriate way to handle this issue is using JAX-RS filters (ContainerRequestFilter / ContainerResponseFilter). Because,

Single implementation point – With the LoggingFilter class, every method gets automatic logging behavior. Otherwise, we have to add Logger.info() manually to each method, resulting in many edits.
No code duplication – Doing the same thing repeatedly doesnt comply with standard, it violates the DRY (Don't Repeat Yourself) principle. For instance, when using a filter , changing the log format means just one line of code change but if we are going to use manual logging we have to do multiple edits .
Consistency – Filters ensure consistent logging. Manual logging may lead to some methods getting missed during log changes, hence missing logs in those parts of the application.
Toggle switch – It is possible to switch on or off a filter globally at once without changing any files, while it would take some time if we wanted to delete our logging manually.
Guaranteed lifecycle – ContainerRequestFilter gets fired before the method gets called, while ContainerResponseFilter gets fired after the resource method is executed – which means we'll get our response code no matter what.
