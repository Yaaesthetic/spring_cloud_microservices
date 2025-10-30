# Spring Cloud Microservices Architecture Project

A comprehensive demonstration of Spring Cloud microservices patterns, showcasing service discovery, API gateway, centralized configuration, and distributed system design principles.

## Architecture Overview

This project implements a complete microservices ecosystem using Spring Cloud components. It demonstrates how modern distributed systems are built with loosely coupled services that communicate over the network while maintaining resilience, scalability, and maintainability.

```
                                    ┌─────────────────────────────┐
                                    │    Client Applications      │
                                    │   (Web, Mobile, Desktop)    │
                                    └──────────────┬──────────────┘
                                                   │
                                                   │ HTTP Requests
                                                   │
                    ┌──────────────────────────────▼──────────────────────────────┐
                    │                      API Gateway                             │
                    │                   (Proxy Service)                            │
                    │  ┌────────────────────────────────────────────────────┐    │
                    │  │  • Single Entry Point                               │    │
                    │  │  • Request Routing                                  │    │
                    │  │  • Load Balancing (Client-Side)                     │    │
                    │  └────────────────────────────────────────────────────┘    │
                    └──────────────────────────────┬──────────────────────────────┘
                                                   │ Port 9999
                                                   │
                    ┌──────────────────────────────┴──────────────────────────────┐
                    │                                                              │
                    │           Service-to-Service Communication                   │
                    │              (Dynamic Service Discovery)                     │
                    │                                                              │
                    └────────┬────────────────────────────────────┬────────────────┘
                             │                                    │
                 ┌───────────▼──────────┐            ┌───────────▼──────────────┐
                 │  Discovery Service   │◄───────────┤   Product Service        │
                 │   (Eureka Server)    │  Register  │   (Business Service)     │
                 │                      │   & Query  │                          │
                 │  ┌────────────────┐ │            │  ┌────────────────────┐ │
                 │  │ Service        │ │            │  │ • Business Logic   │ │
                 │  │ Registry       │ │            │  │ • Data Management  │ │
                 │  │                │ │            │  │ • REST APIs        │ │
                 │  │ [product-      │ │            │  │ • H2 Database      │ │
                 │  │  service: 3    │ │            │  │ • Actuator Health  │ │
                 │  │  instances]    │ │            │  └────────────────────┘ │
                 │  │                │ │            │                          │
                 │  │ [proxy-service:│ │            │  Instance 1: Port 8081   │
                 │  │  1 instance]   │ │            │  Instance 2: Port 8082   │
                 │  │                │ │            │  Instance 3: Port 8083   │
                 │  └────────────────┘ │            │                          │
                 │                      │            └──────────────────────────┘
                 │  Port 8761           │
                 └──────────┬───────────┘
                            │
                            │ Register
                            │
                 ┌──────────▼───────────┐
                 │   Config Service     │
                 │  (Configuration      │
                 │   Management)        │
                 │                      │
                 │  ┌────────────────┐ │
                 │  │ Git Repository │ │
                 │  │ (File-based)   │ │
                 │  │                │ │
                 │  │ • application  │ │
                 │  │   .properties  │ │
                 │  │ • product-     │ │
                 │  │   service      │ │
                 │  │   .properties  │ │
                 │  │ • proxy-       │ │
                 │  │   service.yml  │ │
                 │  │ • discovery-   │ │
                 │  │   service.yaml │ │
                 │  └────────────────┘ │
                 │                      │
                 │  Port 8888           │
                 └──────────────────────┘
```

##  Core Microservices Patterns Implemented

### 1. **Service Discovery Pattern (Eureka)**

**Problem Solved**: In a microservices architecture, services need to find and communicate with each other. Hardcoding IP addresses and ports is inflexible and doesn't scale.

**Solution**: Netflix Eureka implements the Service Registry pattern.

#### How It Works:
```
Service Registration Flow:
1. Product Service starts → Registers itself with Eureka
2. Eureka stores: { service-name: "product-service", instances: [host:port] }
3. Product Service sends heartbeats every 30 seconds
4. If heartbeats stop → Eureka marks instance as DOWN

Service Discovery Flow:
1. Proxy Service needs to call Product Service
2. Queries Eureka: "Where is product-service?"
3. Eureka responds: ["localhost:8081", "localhost:8082", "localhost:8083"]
4. Proxy Service picks one instance (load balancing)
```

#### Key Concepts:
- **Self-Registration**: Services register themselves automatically on startup
- **Health Checks**: Continuous heartbeat mechanism (default: 30s intervals)
- **Service Registry**: Central database of all available service instances
- **Client-Side Discovery**: Clients query the registry and choose instances
- **Peer Awareness**: Eureka servers can replicate registry data (not used in standalone mode here)

#### Configuration Highlights:
```yaml
# Discovery Service acts as standalone server
eureka:
  client:
    register-with-eureka: false  # Don't register itself
    fetch-registry: false         # Don't fetch from other Eureka servers
  server:
    waitTimeInMsWhenSyncEmpty: 0  # Start immediately
```

**Benefits**:
- Dynamic scaling: Add/remove instances without configuration changes
- Fault tolerance: Failed instances automatically removed
- Load distribution: Multiple instances handle more traffic
- Zero downtime deployments: New instances register before old ones stop

---

### 2. **API Gateway Pattern (Spring Cloud Gateway)**

**Problem Solved**: Direct client-to-microservice communication creates tight coupling, exposes internal architecture, and requires clients to handle multiple endpoints, authentication, and protocols.

**Solution**: Single entry point that routes requests to appropriate services.

#### How It Works:
```
Request Flow:
1. Client → http://localhost:9999/product-service/products
2. Gateway receives request
3. Gateway extracts path: /product-service/**
4. Gateway queries Eureka: "Where is product-service?"
5. Gateway gets: [8081, 8082, 8083]
6. Gateway applies load balancing algorithm
7. Gateway forwards → http://localhost:8081/products
8. Product Service responds
9. Gateway returns response to client
```

#### Key Responsibilities:

**Routing**:
- Path-based routing: `/product-service/**` → product-service
- Predicates define matching rules (Path, Method, Header, Query params)
- Filters can transform requests/responses

**Load Balancing**:
- Client-side load balancing using Spring Cloud LoadBalancer
- Algorithms: Round-robin (default), Random, Weighted
- Integration with Eureka for dynamic instance lists

**Cross-Cutting Concerns** (Potential):
- Authentication & Authorization (Security gateway)
- Rate limiting and throttling
- Request/Response logging
- CORS handling
- Protocol translation (REST to gRPC)
- Request aggregation (Backend for Frontend pattern)

#### Configuration:
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: product-service              # Route identifier
          uri: lb://product-service/       # lb = Load Balanced from Eureka
          predicates:
            - Path=/product-service/**     # Match requests
```

**The `lb://` prefix** tells Spring Cloud Gateway to:
1. Look up "product-service" in Eureka
2. Get all available instances
3. Use LoadBalancer to pick one
4. Forward the request

**Benefits**:
- **Simplified client code**: One endpoint instead of many
- **Security**: Internal services not exposed directly
- **Flexibility**: Change backend services without affecting clients
- **Monitoring**: Centralized logging and metrics collection
- **Resilience**: Circuit breakers, retries, timeouts at gateway level

---

### 3. **Externalized Configuration Pattern (Spring Cloud Config)**

**Problem Solved**: Managing configuration across multiple environments (dev, test, prod) and multiple services becomes unmanageable with property files in each service.

**Solution**: Centralized configuration server with version-controlled config repository.

#### How It Works:
```
Configuration Bootstrap Flow:
1. Product Service starts
2. Reads application.properties → spring.config.import=optional:configserver:
3. Contacts Config Server at http://localhost:8888
4. Requests: "Give me configuration for product-service with profile default"
5. Config Server reads from Git repository
6. Returns product-service.properties content
7. Product Service merges this with local configuration
8. Service starts with complete configuration
```

#### Architecture Layers:

**Git Repository** (Configuration Storage):
```
myConfig/
├── application.properties        # Global configuration (all services)
├── product-service.properties    # Product service specific
├── proxy-service.yml             # Proxy service specific
└── discovery-service.yaml        # Discovery service specific
```

**Config Server** (Configuration Provider):
- Serves configuration over HTTP
- Supports multiple backends: Git, SVN, file system, Vault
- URL patterns:
  - `/{application}/{profile}` → Returns configuration
  - `/{application}/{profile}/{label}` → Returns configuration from specific branch

**Config Clients** (Services):
- Bootstrap phase connects to Config Server before application starts
- Can refresh configuration at runtime (with Spring Cloud Bus)
- Optional import: Service still starts if Config Server unavailable

#### Key Features:

**Environment-Specific Configuration**:
```
product-service.properties        # Default
product-service-dev.properties    # Development
product-service-prod.properties   # Production
```

**Encryption/Decryption**:
- Config Server can encrypt sensitive data (passwords, tokens)
- `{cipher}ENCRYPTED_VALUE` → Decrypted before sending to services

**Configuration Hierarchy**:
```
Priority (highest to lowest):
1. Service-specific profile config (product-service-prod.properties)
2. Service-specific config (product-service.properties)
3. Global profile config (application-prod.properties)
4. Global config (application.properties)
5. Local application.properties in service
```

**Dynamic Refresh** (Not implemented here, but available):
```
POST /actuator/refresh → Reloads configuration without restart
With Spring Cloud Bus → Broadcast refresh to all instances
```

#### Configuration:
```properties
# Config Server
spring.cloud.config.server.git.uri=file:///path/to/myConfig
spring.cloud.config.server.git.default-label=master

# Config Client
spring.config.import=optional:configserver:  # Bootstrap config discovery
```

**Benefits**:
- **Version Control**: Configuration changes tracked in Git
- **Environment Management**: Easy promotion from dev to prod
- **Security**: Centralized secret management with encryption
- **Consistency**: All services use same configuration source
- **Audit Trail**: Who changed what and when
- **Dynamic Updates**: Change configuration without redeployment

---

## Communication Patterns

### Synchronous Communication (REST over HTTP)

This project uses REST APIs for service-to-service communication:

```
Client → API Gateway → Product Service
         (HTTP)        (HTTP)
```

**Characteristics**:
- Request-Response pattern
- Client waits for response
- Direct coupling between services
- Suitable for: CRUD operations, queries

**Alternative Patterns** (Not implemented, but commonly used):
- **Asynchronous Messaging**: RabbitMQ, Kafka for event-driven architecture
- **gRPC**: Binary protocol for high-performance inter-service communication
- **GraphQL**: Flexible query language for APIs

---

##  Service Communication Flow

### Example: Fetching Products through API Gateway

```
┌─────────┐                           ┌──────────────┐
│ Client  │                           │   Eureka     │
└────┬────┘                           │   Server     │
     │                                └──────┬───────┘
     │ 1. GET /product-service/products      │
     │                                       │
     ▼                                       │
┌─────────────┐                             │
│  API        │ 2. Lookup "product-service" │
│  Gateway    │────────────────────────────►│
│  (Port      │                             │
│  9999)      │◄────────────────────────────│
└─────┬───────┘  3. Returns [8081, 8082,   
      │              8083]                   
      │                                      
      │ 4. Load balance → Pick 8082          
      │                                      
      │ 5. Forward: GET /products            
      │                                      
      ▼                                      
┌──────────────────┐                        
│ Product Service  │                        
│ Instance 2       │                        
│ (Port 8082)      │                        
│                  │                        
│ 6. Query H2 DB   │                        
│ 7. Return JSON   │                        
└─────┬────────────┘                        
      │                                      
      │ 8. Response                          
      ▼                                      
┌─────────────┐                             
│  API        │                             
│  Gateway    │                             
└─────┬───────┘                             
      │                                      
      │ 9. Return to client                 
      ▼                                      
┌─────────┐                                 
│ Client  │                                 
└─────────┘                                 
```

---

## Key Microservices Principles Demonstrated

### 1. **Single Responsibility Principle**
Each service has one clear purpose:
- **Discovery Service**: Service registry only
- **Config Service**: Configuration management only
- **Product Service**: Product domain logic only
- **Proxy Service**: Routing and gateway functions only

### 2. **Decentralized Data Management**
- Product Service has its own H2 database
- No shared database between services
- Each service owns its data (Database per Service pattern)

### 3. **Infrastructure Automation**
- Services auto-register with Eureka
- Configuration auto-loaded from Config Server
- No manual service registration needed

### 4. **Design for Failure**
- `spring.config.import=optional:configserver:` → Service starts even if Config Server down
- Eureka heartbeats detect failed instances
- Multiple Product Service instances provide redundancy

### 5. **Scalability**
- Horizontal scaling: Run multiple Product Service instances
- Load balancing distributes traffic automatically
- Add capacity without code changes

---

## Service Startup Sequence & Dependencies

Understanding the correct startup order is crucial in microservices:

```
1. Discovery Service (Port 8761)
   ↓ Why first? Other services need to register
   │
   ├─ No dependencies
   └─ Must be healthy before others start
   
2. Config Service (Port 8888)
   ↓ Registers with Eureka
   │
   ├─ Depends on: Discovery Service
   └─ Provides configuration to other services
   
3. Product Service (Port 8081+)
   ↓ Registers with Eureka, loads config
   │
   ├─ Depends on: Discovery Service (registration)
   ├─ Optional: Config Service (configuration)
   └─ Can start multiple instances in parallel
   
4. Proxy Service (Port 9999)
   ↓ Needs to discover Product Service
   │
   ├─ Depends on: Discovery Service (lookup)
   ├─ Optional: Config Service (routes config)
   └─ Should start after business services are registered
```

**Why This Order Matters**:
- Starting Proxy before Product Service → Routes to nowhere, 503 errors
- Starting without Discovery → Services can't find each other
- Starting without Config → Uses default/local configuration only

**Grace Period**: After startup, wait 30-60 seconds for:
- Service registration to complete
- Eureka cache to refresh
- Health checks to pass

---

## Getting Started

### Prerequisites
- **Java 17+**: Modern Java features and performance
- **Maven 3.6+**: Dependency management and build tool
- **Git**: For Config Server repository
- **IDE**: IntelliJ IDEA recommended (Spring Boot support)

### Startup Sequence

```bash
# 1. Start Discovery Service
cd discovery-service
mvn spring-boot:run
# Wait for: "Started DiscoveryServiceApplication"
# Verify: http://localhost:8761

# 2. Start Config Service
cd config-service
mvn spring-boot:run
# Verify: http://localhost:8888/product-service/default

# 3. Start Product Service (multiple instances)
cd product-service
mvn spring-boot:run  # Instance 1 on 8081
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082  # Instance 2
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8083  # Instance 3

# 4. Start Proxy Service
cd proxy-service
mvn spring-boot:run
```

### Running Multiple Product Service Instances

**Purpose**: Demonstrate load balancing and high availability

**Method 1: Command Line**
```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
```

**Method 2: IntelliJ IDEA Run Configuration**
```
1. Run → Edit Configurations
2. Select ProductServiceApplication
3. Program arguments: --server.port=8082
4. Click "Copy Configuration" icon
5. Rename to "ProductServiceApplication (8082)"
6. Repeat for port 8083
```

**Verification**:
- Check Eureka Dashboard: http://localhost:8761
- Should see 3 instances of PRODUCT-SERVICE
- Each request through API Gateway hits different instances (round-robin)

---
### Spring Cloud Components
- Spring Cloud Netflix Eureka (Discovery)
- Spring Cloud Config (Configuration)
- Spring Cloud Gateway (API Gateway)
- Spring Cloud LoadBalancer (Load Balancing)

### Distributed Systems Concepts
- ✅ Service Registration and Deregistration
- ✅ Heartbeat mechanism
- ✅ Dynamic service location
- ✅ Horizontal scaling
- ✅ Fault tolerance basics

---

## 📚 Further Learning Resources

### Spring Cloud Documentation
- [Spring Cloud Netflix](https://spring.io/projects/spring-cloud-netflix)
- [Spring Cloud Config](https://spring.io/projects/spring-cloud-config)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
