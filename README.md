# Court Reservation System - Documentación Completa

## Tabla de Contenidos

1. [Descripción General](#descripción-general)
2. [Arquitectura del Sistema](#arquitectura-del-sistema)
3. [Tecnologías Utilizadas](#tecnologías-utilizadas)
4. [Reglas de Negocio](#reglas-de-negocio)
5. [Modelo de Datos](#modelo-de-datos)
6. [Estructura del Proyecto](#estructura-del-proyecto)
7. [API Endpoints](#api-endpoints)
8. [Instalación y Configuración](#instalación-y-configuración)
9. [Guía de Uso](#guía-de-uso)
10. [Testing](#testing)

---

## Descripción General

**Court Reservation System** es un sistema completo de gestión de reservas para canchas deportivas desarrollado con **Spring Boot 3.2.1** y **Java 21**. El sistema implementa funcionalidades avanzadas como:

- ✅ Sistema de precios dinámicos (horario pico, valle, fin de semana)
- ✅ Gestión de membresías (BASIC, PREMIUM, VIP)
- ✅ Paquetes de horas prepagadas con descuentos
- ✅ Reservas recurrentes (semanales)
- ✅ Sistema de cancelaciones con penalizaciones progresivas
- ✅ Lista de espera con notificaciones automáticas
- ✅ Validaciones exhaustivas de disponibilidad
- ✅ Jobs automáticos para mantenimiento

---

## 🏗️ Arquitectura del Sistema

### Arquitectura en Capas

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (Controllers + Exception Handlers)     │
├─────────────────────────────────────────┤
│           Service Layer                 │
│    (Business Logic + Validations)       │
├─────────────────────────────────────────┤
│         Persistence Layer               │
│    (Repositories + Entities)            │
├─────────────────────────────────────────┤
│           Database Layer                │
│       (PostgreSQL / H2)                 │
└─────────────────────────────────────────┘
```

### Componentes Principales

| Capa | Responsabilidad | Componentes |
|------|----------------|-------------|
| **Controllers** | Exponer API REST | 6 Controllers |
| **Services** | Lógica de negocio | 8 Services |
| **Repositories** | Acceso a datos | 6 Repositories |
| **DTOs** | Transferencia de datos | 19 DTOs |
| **Entities** | Modelo de dominio | 6 Entities |
| **Mappers** | Conversión DTO ↔ Entity | 6 Mappers (MapStruct) |

---

## 🛠️ Tecnologías Utilizadas

### Backend Core
- **Java 21** - Lenguaje de programación
- **Spring Boot 3.2.1** - Framework principal
- **Spring Data JPA** - ORM y persistencia
- **Hibernate 6.4.1** - Implementación JPA
- **Maven 3.11.0** - Gestión de dependencias

### Base de Datos
- **PostgreSQL** - Base de datos producción
- **H2 Database** - Base de datos desarrollo/testing

### Validaciones y Mapeo
- **Jakarta Validation (Bean Validation 3.0)** - Validaciones
- **MapStruct 1.5.5** - Mapeo objeto a objeto
- **Lombok 1.18.30** - Reducción de boilerplate

### Documentación
- **Springdoc OpenAPI 2.6.0** - Documentación API (Swagger)

### Utilidades
- **Jackson 2.15.3** - Serialización JSON
- **SLF4J + Logback** - Logging

---

## 📐 Reglas de Negocio

### RN-001 a RN-005: Restricciones de Reserva

| ID | Regla | Implementación |
|----|-------|----------------|
| **RN-001** | No permitir solapamiento de horarios en la misma cancha | `BookingRepository.existsOverlappingBooking()` |
| **RN-002** | Duración mínima 1 hora, máxima 4 horas | `ValidationService.validateDuration()` |
| **RN-003** | Horario de operación: 06:00 - 23:00 | `ValidationService.validateOperatingHours()` |
| **RN-004** | Reserva con mínimo 2 horas de anticipación | `ValidationService.validateMinimumAdvance()` |
| **RN-005** | Límite de anticipación según membresía | `ValidationService.validateMaxDaysAdvance()` |

#### Límites de Anticipación por Membresía

| Membresía | Días Máximos | Descuento |
|-----------|--------------|-----------|
| **NONE** | 7 días | 0% |
| **BASIC** | 15 días | 5% |
| **PREMIUM** | 30 días | 10% |
| **VIP** | 60 días | 15% |

---

### RN-006 a RN-010: Precios Dinámicos

#### Factor de Precio Base: 1.0x

| ID | Condición | Factor | Ejemplo (Base: $100) |
|----|-----------|--------|----------------------|
| **RN-006** | Horario Pico (18:00-22:00) | +50% (1.5x) | $150 |
| **RN-007** | Fin de Semana (Sáb-Dom) | +30% (1.3x) | $130 |
| **RN-008** | Horario Pico + Fin de Semana | +95% (1.95x) | $195 |
| **RN-009** | Horario Valle (06:00-12:00) | -20% (0.8x) | $80 |
| **RN-010** | Horario Normal (12:00-18:00, 22:00-23:00) | Base (1.0x) | $100 |

#### Ejemplo de Cálculo Completo

```
Base Price:      $100/hora
Duration:        2 horas
Time:            19:00 (Horario Pico)
Day:             Sábado (Fin de Semana)
Membership:      PREMIUM (10% descuento)

Cálculo:
1. Base Total:   $100 × 2 = $200
2. Horario Pico: $200 × 1.5 = $300
3. Fin de Semana: $300 × 1.3 = $390
4. Descuento:    $390 × 0.90 = $351
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL FINAL:     $351.00
```

---

### RN-011 a RN-014: Descuentos por Membresía

| Membresía | Descuento | Max Días Anticipación | Beneficios Adicionales |
|-----------|-----------|----------------------|------------------------|
| **NONE** | 0% | 7 días | - |
| **BASIC** | 5% | 15 días | Prioridad normal |
| **PREMIUM** | 10% | 30 días | Cancelación 24h gratis |
| **VIP** | 15% | 60 días | Cancelación 12h gratis |

---

### RN-015 a RN-019: Sistema de Paquetes

#### Paquetes Disponibles

| Paquete | Horas | Precio | Descuento | Precio/Hora | Vigencia |
|---------|-------|--------|-----------|-------------|----------|
| **BASIC** | 10h | $900 | 10% | $90/h | 30 días |
| **PREMIUM** | 20h | $1,600 | 20% | $80/h | 60 días |
| **VIP** | 40h | $2,800 | 30% | $70/h | 90 días |

#### Reglas de Uso de Paquetes

- **RN-015**: Paquetes se compran con horas prepagadas
- **RN-016**: Las horas se descuentan al hacer reserva
- **RN-017**: Paquete vence según vigencia (30/60/90 días)
- **RN-018**: Horas no usadas se pierden al vencer
- **RN-019**: Usuario puede tener múltiples paquetes activos

---

### RN-020 a RN-025: Cancelaciones y Penalizaciones

#### Tabla de Penalizaciones

| Anticipación | Penalización | Reembolso | VIP |
|--------------|--------------|-----------|-----|
| **≥ 24 horas** | 0% | 100% | 0% |
| **12-24 horas** | 30% | 70% | 0% |
| **< 12 horas** | 50% | 50% | 30% |

#### Reglas Específicas

- **RN-020**: Más de 24h = Sin penalización
- **RN-021**: 12-24h = 30% penalización
- **RN-022**: Menos de 12h = 50% penalización
- **RN-023**: Cancelaciones afectan historial del usuario
- **RN-024**: Paquetes: horas se devuelven si cancelación con ≥24h
- **RN-025**: VIP: cancelación gratis hasta 12h antes

---

### RN-026 a RN-030: Reservas Recurrentes

- **RN-026**: Crear múltiples reservas semanales automáticamente
- **RN-027**: Máximo 12 semanas consecutivas
- **RN-028**: Si una fecha está ocupada, se omite (no falla todo)
- **RN-029**: Descuento adicional de 5% en reservas recurrentes
- **RN-030**: Se pueden cancelar todas juntas o individualmente

#### Ejemplo de Reserva Recurrente

```
Usuario solicita: Todos los Martes 18:00-20:00 por 8 semanas
Resultado:
✅ Semana 1: Reservado
✅ Semana 2: Reservado
❌ Semana 3: Ocupado (se omite)
✅ Semana 4: Reservado
✅ Semana 5: Reservado
...
Total: 7 reservas creadas, 1 omitida
```

---

### RN-031 a RN-034: Lista de Espera

- **RN-031**: Usuario puede unirse a lista de espera si horario ocupado
- **RN-032**: Al cancelarse una reserva, se notifica al primero en lista (FIFO)
- **RN-033**: Usuario tiene 30 minutos para confirmar disponibilidad
- **RN-034**: Si no responde en 30min, se notifica al siguiente

---

## 💾 Modelo de Datos

### Diagrama Entidad-Relación

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│    User     │1      ∞│   Booking    │∞      1│    Court    │
│─────────────│◄────────┤──────────────├────────►│─────────────│
│ id          │         │ id           │         │ id          │
│ name        │         │ user_id      │         │ name        │
│ email       │         │ court_id     │         │ sport_type  │
│ phone       │         │ booking_date │         │ base_price  │
│ membership  │         │ start_time   │         │ indoor      │
│ active      │         │ end_time     │         │ active      │
└─────────────┘         │ status       │         └─────────────┘
        │               │ total_price  │
        │1              │ uses_package │
        │               └──────────────┘
        │                      │
        │               ┌──────▼──────────┐
        │               │  UserPackage    │
        └──────────────►│─────────────────│
                     ∞│ id              │
                        │ user_id         │
                        │ package_id      │
                        │ initial_hours   │
                        │ remaining_hours │
                        │ expiration_date │
                        └─────────────────┘
                               │1
                               │
                        ┌──────▼──────┐
                        │   Package   │
                        │─────────────│
                        │ id          │
                        │ name        │
                        │ hours       │
                        │ total_price │
                        │ discount_% │
                        │ validity    │
                        └─────────────┘
```

### Entidades Principales

#### 1. Court (Cancha)

```java
@Entity
@Table(name = "courts")
public class Court {
    @Id @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    private SportType sportType;  // FOOTBALL, BASKETBALL, TENNIS, etc.

    @Column(nullable = false)
    private BigDecimal basePricePerHour;

    private Boolean indoor = false;
    private Boolean active = true;

    @OneToMany(mappedBy = "court")
    private List<Booking> bookings;
}
```

#### 2. User (Usuario)

```java
@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    private MembershipType membershipType = MembershipType.NONE;

    private Boolean active = true;

    @OneToMany(mappedBy = "user")
    private List<Booking> bookings;
}
```

#### 3. Booking (Reserva)

```java
@Entity
@Table(name = "bookings")
public class Booking {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @Column(nullable = false)
    private LocalDate bookingDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;  // CONFIRMED, CANCELLED, COMPLETED

    // Campos de precio
    private BigDecimal basePrice;
    private BigDecimal dynamicSurcharges;
    private BigDecimal appliedDiscount;
    private BigDecimal totalPrice;

    // Campos para paquetes
    private Boolean usesPackage = false;
    private Long userPackageId;
    private BigDecimal hoursDeducted;

    // Campos para recurrencia
    private Boolean isRecurrent = false;
    private Long parentBookingId;

    // Campos de cancelación
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private BigDecimal penaltyPercentage;
    private BigDecimal penaltyAmount;
}
```

#### 4. Package (Paquete)

```java
@Entity
@Table(name = "packages")
public class Package {
    @Id @GeneratedValue
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private Integer hoursQuantity;

    @Column(nullable = false)
    private BigDecimal totalPrice;

    @Column(nullable = false)
    private Integer discountPercentage;

    @Column(nullable = false)
    private Integer validityDays;  // 30, 60, 90

    private Boolean active = true;
}
```

#### 5. UserPackage (Paquete de Usuario)

```java
@Entity
@Table(name = "user_packages")
public class UserPackage {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "package_id", nullable = false)
    private Package packageDetails;

    private Integer initialHours;
    private Integer remainingHours;

    @Column(nullable = false)
    private LocalDateTime purchaseDate;

    @Column(nullable = false)
    private LocalDateTime expirationDate;

    private Boolean active = true;
}
```

#### 6. WaitingList (Lista de Espera)

```java
@Entity
@Table(name = "waiting_list")
public class WaitingList {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    private LocalDate desiredDate;
    private LocalTime desiredStartTime;
    private LocalTime desiredEndTime;

    @Column(nullable = false)
    private LocalDateTime requestDate;

    private Boolean notified = false;
    private LocalDateTime notificationDate;
    private LocalDateTime notificationExpirationDate;
}
```

---

## 📁 Estructura del Proyecto

```
court-reservation-system/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/salva/task/court_reservation_system/
│   │   │       │
│   │   │       ├── CourtReservationSystemApplication.java
│   │   │       │
│   │   │       ├── config/
│   │   │       │   └── OpenApiConfig.java
│   │   │       │
│   │   │       ├── controller/
│   │   │       │   ├── CourtController.java
│   │   │       │   ├── UserController.java
│   │   │       │   ├── BookingController.java ⭐
│   │   │       │   ├── PackageController.java
│   │   │       │   ├── UserPackageController.java
│   │   │       │   └── WaitingListController.java
│   │   │       │
│   │   │       ├── dto/
│   │   │       │   ├── request/
│   │   │       │   │   ├── CourtRequestDTO.java
│   │   │       │   │   ├── UserRequestDTO.java
│   │   │       │   │   ├── BookingRequestDTO.java
│   │   │       │   │   ├── RecurrentBookingRequestDTO.java
│   │   │       │   │   ├── CancellationRequestDTO.java
│   │   │       │   │   ├── PackageRequestDTO.java
│   │   │       │   │   ├── PackagePurchaseRequestDTO.java
│   │   │       │   │   └── WaitingListRequestDTO.java
│   │   │       │   │
│   │   │       │   └── response/
│   │   │       │       ├── CourtResponseDTO.java
│   │   │       │       ├── UserResponseDTO.java
│   │   │       │       ├── BookingResponseDTO.java
│   │   │       │       ├── BookingDetailResponseDTO.java
│   │   │       │       ├── RecurrentBookingResponseDTO.java
│   │   │       │       ├── CancellationResponseDTO.java
│   │   │       │       ├── CourtAvailabilityResponseDTO.java
│   │   │       │       ├── PackageResponseDTO.java
│   │   │       │       ├── UserPackageResponseDTO.java
│   │   │       │       ├── WaitingListResponseDTO.java
│   │   │       │       └── ErrorResponse.java
│   │   │       │
│   │   │       ├── entity/
│   │   │       │   ├── Court.java
│   │   │       │   ├── User.java
│   │   │       │   ├── Booking.java
│   │   │       │   ├── Package.java
│   │   │       │   ├── UserPackage.java
│   │   │       │   └── WaitingList.java
│   │   │       │
│   │   │       ├── enums/
│   │   │       │   ├── SportType.java
│   │   │       │   ├── MembershipType.java
│   │   │       │   ├── BookingStatus.java
│   │   │       │   └── RecurrenceFrequency.java
│   │   │       │
│   │   │       ├── exception/
│   │   │       │   ├── GlobalExceptionHandler.java
│   │   │       │   ├── ResourceNotFoundException.java
│   │   │       │   ├── ValidationException.java
│   │   │       │   └── BusinessException.java
│   │   │       │
│   │   │       ├── mapper/
│   │   │       │   ├── CourtMapper.java
│   │   │       │   ├── UserMapper.java
│   │   │       │   ├── BookingMapper.java
│   │   │       │   ├── PackageMapper.java
│   │   │       │   ├── UserPackageMapper.java
│   │   │       │   └── WaitingListMapper.java
│   │   │       │
│   │   │       ├── repository/
│   │   │       │   ├── CourtRepository.java
│   │   │       │   ├── UserRepository.java
│   │   │       │   ├── BookingRepository.java
│   │   │       │   ├── PackageRepository.java
│   │   │       │   ├── UserPackageRepository.java
│   │   │       │   └── WaitingListRepository.java
│   │   │       │
│   │   │       └── service/
│   │   │           ├── CourtService.java
│   │   │           ├── UserService.java
│   │   │           ├── BookingService.java ⭐
│   │   │           ├── PackageService.java
│   │   │           ├── UserPackageService.java
│   │   │           ├── WaitingListService.java
│   │   │           ├── ValidationService.java
│   │   │           ├── ScheduledTaskService.java
│   │   │           └── impl/
│   │   │               ├── CourtServiceImpl.java
│   │   │               ├── UserServiceImpl.java
│   │   │               ├── BookingServiceImpl.java
│   │   │               ├── PackageServiceImpl.java
│   │   │               ├── UserPackageServiceImpl.java
│   │   │               ├── WaitingListServiceImpl.java
│   │   │               ├── ValidationServiceImpl.java
│   │   │               └── ScheduledTaskServiceImpl.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   │
│   └── test/
│       └── java/
│           └── org/salva/task/court_reservation_system/
│               ├── service/
│               ├── controller/
│               └── repository/
│
├── pom.xml
└── README.md
```

---

## 🔌 API Endpoints

### Base URL
```
Development:  http://localhost:8080
Production:   https://api.yourdomain.com
```

### Swagger UI
```
http://localhost:8080/swagger-ui.html
```

---

### 1. Courts API

#### Crear Cancha
```http
POST /api/courts
Content-Type: application/json

{
  "name": "Cancha Fútbol 5 - Principal",
  "sportType": "FOOTBALL",
  "basePricePerHour": 100.00,
  "indoor": false,
  "capacity": 10
}

Response: 201 Created
{
  "id": 1,
  "name": "Cancha Fútbol 5 - Principal",
  "sportType": "FOOTBALL",
  "basePricePerHour": 100.00,
  "indoor": false,
  "active": true
}
```

#### Listar Canchas Activas
```http
GET /api/courts

Response: 200 OK
[
  {
    "id": 1,
    "name": "Cancha Fútbol 5 - Principal",
    "sportType": "FOOTBALL",
    "basePricePerHour": 100.00
  },
  {
    "id": 2,
    "name": "Cancha Básquet Techada",
    "sportType": "BASKETBALL",
    "basePricePerHour": 120.00
  }
]
```

#### Obtener Cancha por ID
```http
GET /api/courts/{id}

Response: 200 OK
{
  "id": 1,
  "name": "Cancha Fútbol 5 - Principal",
  "sportType": "FOOTBALL",
  "basePricePerHour": 100.00,
  "indoor": false,
  "active": true
}
```

#### Buscar por Tipo de Deporte
```http
GET /api/courts/sport-type/FOOTBALL

Response: 200 OK
[...]
```

#### Buscar por Nombre
```http
GET /api/courts/search?name=futbol

Response: 200 OK
[...]
```

#### Actualizar Cancha
```http
PUT /api/courts/{id}
Content-Type: application/json

{
  "name": "Cancha Fútbol 5 - Renovada",
  "basePricePerHour": 110.00
}

Response: 200 OK
```

#### Desactivar Cancha
```http
DELETE /api/courts/{id}

Response: 204 No Content
```

---

### 2. Users API

#### Crear Usuario
```http
POST /api/users
Content-Type: application/json

{
  "name": "Juan Pérez",
  "email": "juan.perez@example.com",
  "phone": "+51987654321",
  "membershipType": "PREMIUM"
}

Response: 201 Created
{
  "id": 1,
  "name": "Juan Pérez",
  "email": "juan.perez@example.com",
  "phone": "+51987654321",
  "membershipType": "PREMIUM",
  "active": true
}
```

#### Obtener Usuario
```http
GET /api/users/{id}
GET /api/users/email/{email}

Response: 200 OK
```

#### Actualizar Membresía
```http
PATCH /api/users/{id}/membership?membershipType=VIP

Response: 200 OK
{
  "id": 1,
  "name": "Juan Pérez",
  "membershipType": "VIP"
}
```

---

### 3. Bookings API ⭐

#### Crear Reserva Simple
```http
POST /api/bookings
Content-Type: application/json

{
  "userId": 1,
  "courtId": 2,
  "bookingDate": "2026-01-25",
  "startTime": "18:00",
  "endTime": "20:00",
  "usesPackage": false
}

Response: 201 Created
{
  "id": 1,
  "userId": 1,
  "courtId": 2,
  "bookingDate": "2026-01-25",
  "startTime": "18:00",
  "endTime": "20:00",
  "status": "CONFIRMED",
  "basePrice": 200.00,
  "dynamicSurcharges": 100.00,
  "appliedDiscount": 30.00,
  "totalPrice": 270.00
}
```

#### Crear Reservas Recurrentes
```http
POST /api/bookings/recurrent
Content-Type: application/json

{
  "userId": 1,
  "courtId": 2,
  "startDate": "2026-01-25",
  "startTime": "18:00",
  "endTime": "20:00",
  "frequency": "WEEKLY",
  "numberOfWeeks": 8,
  "usesPackage": false
}

Response: 201 Created
{
  "totalRequested": 8,
  "successfulBookings": 7,
  "failedBookings": 1,
  "estimatedTotalPrice": 1890.00,
  "recurrentDiscountApplied": true,
  "bookingDetails": [
    {
      "bookingId": 1,
      "bookingDate": "2026-01-25",
      "status": "CREATED",
      "price": 270.00
    },
    ...
  ]
}
```

#### Obtener Disponibilidad de Cancha
```http
GET /api/bookings/court/{courtId}/availability?date=2026-01-25

Response: 200 OK
{
  "courtId": 2,
  "courtName": "Cancha Básquet Techada",
  "date": "2026-01-25",
  "availableSlots": [
    {
      "startTime": "06:00",
      "endTime": "07:00",
      "estimatedPrice": 96.00,
      "priceFactor": 0.8
    },
    {
      "startTime": "14:00",
      "endTime": "15:00",
      "estimatedPrice": 120.00,
      "priceFactor": 1.0
    }
  ],
  "occupiedSlots": [
    {
      "startTime": "18:00",
      "endTime": "20:00"
    }
  ]
}
```

#### Verificar Solapamiento
```http
GET /api/bookings/check-overlap?courtId=2&date=2026-01-25&startTime=18:00&endTime=20:00

Response: 200 OK
true  // o false
```

#### Cancelar Reserva
```http
PUT /api/bookings/cancel
Content-Type: application/json

{
  "bookingId": 1,
  "reason": "Cambio de planes",
  "cancelAllRecurrent": false
}

Response: 200 OK
{
  "bookingId": 1,
  "status": "CANCELLED",
  "cancelledAt": "2026-01-20T15:30:00",
  "hoursInAdvance": 116,
  "penaltyPercentage": 0.00,
  "penaltyAmount": 0.00,
  "refundAmount": 270.00,
  "message": "Cancelación exitosa sin penalización"
}
```

#### Obtener Reservas del Usuario
```http
GET /api/bookings/user/{userId}
GET /api/bookings/user/{userId}/status/CONFIRMED
GET /api/bookings/user/{userId}/future

Response: 200 OK
[...]
```

---

### 4. Packages API

#### Crear Paquete
```http
POST /api/packages
Content-Type: application/json

{
  "name": "Paquete Premium 20 Horas",
  "description": "20 horas con 20% descuento",
  "hoursQuantity": 20,
  "totalPrice": 1600.00,
  "discountPercentage": 20,
  "validityDays": 60
}

Response: 201 Created
```

#### Listar Paquetes
```http
GET /api/packages
GET /api/packages/best-discount
GET /api/packages/best-price

Response: 200 OK
[
  {
    "id": 1,
    "name": "Paquete Basic 10h",
    "hoursQuantity": 10,
    "totalPrice": 900.00,
    "discountPercentage": 10,
    "pricePerHour": 90.00,
    "validityDays": 30
  }
]
```

---

### 5. User Packages API

#### Comprar Paquete
```http
POST /api/user-packages/purchase
Content-Type: application/json

{
  "userId": 1,
  "packageId": 2
}

Response: 201 Created
{
  "id": 1,
  "userId": 1,
  "packageName": "Paquete Premium 20h",
  "initialHours": 20,
  "remainingHours": 20,
  "purchaseDate": "2026-01-17T19:00:00",
  "expirationDate": "2026-03-18T19:00:00",
  "active": true,
  "expired": false
}
```

#### Obtener Paquetes del Usuario
```http
GET /api/user-packages/user/{userId}
GET /api/user-packages/user/{userId}/active
GET /api/user-packages/user/{userId}/best-available

Response: 200 OK
[...]
```

---

### 6. Waiting List API

#### Agregar a Lista de Espera
```http
POST /api/waiting-list
Content-Type: application/json

{
  "userId": 1,
  "courtId": 2,
  "desiredDate": "2026-01-25",
  "desiredStartTime": "18:00",
  "desiredEndTime": "20:00"
}

Response: 201 Created
{
  "id": 1,
  "userId": 1,
  "courtId": 2,
  "desiredDate": "2026-01-25",
  "desiredStartTime": "18:00",
  "desiredEndTime": "20:00",
  "requestDate": "2026-01-17T19:00:00",
  "notified": false,
  "positionInQueue": 3
}
```

#### Obtener Lista de Espera
```http
GET /api/waiting-list/user/{userId}
GET /api/waiting-list/court/{courtId}/pending?date=2026-01-25&startTime=18:00&endTime=20:00

Response: 200 OK
[...]
```

#### Eliminar de Lista
```http
DELETE /api/waiting-list/{id}

Response: 204 No Content
```

---

## 🚀 Instalación y Configuración

### Requisitos Previos

- **Java 21** (OpenJDK o Oracle JDK)
- **Maven 3.8+**
- **PostgreSQL 14+** (para producción)
- **Git**

### Paso 1: Clonar el Repositorio

```bash
git clone https://github.com/yourusername/court-reservation-system.git
cd court-reservation-system
```

### Paso 2: Configurar Base de Datos

#### Opción A: H2 (Desarrollo - Sin instalación)

El proyecto ya está configurado para usar H2 en modo desarrollo. No requiere instalación adicional.

#### Opción B: PostgreSQL (Producción)

```bash
# Crear base de datos
createdb court_reservation_db

# Crear usuario
psql -c "CREATE USER court_user WITH PASSWORD 'your_password';"
psql -c "GRANT ALL PRIVILEGES ON DATABASE court_reservation_db TO court_user;"
```

Editar `application-prod.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/court_reservation_db
    username: court_user
    password: your_password
```

### Paso 3: Compilar el Proyecto

```bash
mvn clean install -DskipTests
```

### Paso 4: Ejecutar la Aplicación

#### Modo Desarrollo (H2)
```bash
mvn spring-boot:run
```

#### Modo Producción (PostgreSQL)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

O generar JAR:
```bash
mvn clean package -DskipTests
java -jar -Dspring.profiles.active=prod target/court-reservation-system-0.0.1-SNAPSHOT.jar
```

### Paso 5: Verificar la Instalación

```bash
# Health check
curl http://localhost:8080/actuator/health

# Swagger UI
open http://localhost:8080/swagger-ui.html

# H2 Console (solo desarrollo)
open http://localhost:8080/h2-console
```

---

## 📖 Guía de Uso

### Caso de Uso 1: Crear Usuario y Reservar Cancha

```bash
# 1. Crear usuario
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "María García",
    "email": "maria@example.com",
    "phone": "+51999888777",
    "membershipType": "PREMIUM"
  }'

# Respuesta: {"id": 1, ...}

# 2. Ver canchas disponibles
curl http://localhost:8080/api/courts

# 3. Ver disponibilidad de cancha específica
curl "http://localhost:8080/api/bookings/court/1/availability?date=2026-01-25"

# 4. Crear reserva
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "courtId": 1,
    "bookingDate": "2026-01-25",
    "startTime": "18:00",
    "endTime": "20:00",
    "usesPackage": false
  }'
```

### Caso de Uso 2: Comprar Paquete y Usar Horas

```bash
# 1. Ver paquetes disponibles
curl http://localhost:8080/api/packages

# 2. Comprar paquete
curl -X POST http://localhost:8080/api/user-packages/purchase \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "packageId": 2
  }'

# Respuesta: {"id": 5, "remainingHours": 20, ...}

# 3. Reservar usando paquete
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "courtId": 1,
    "bookingDate": "2026-01-26",
    "startTime": "10:00",
    "endTime": "12:00",
    "usesPackage": true,
    "userPackageId": 5
  }'

# 4. Verificar horas restantes
curl http://localhost:8080/api/user-packages/user/1/active
# Respuesta: {"remainingHours": 18, ...}
```

### Caso de Uso 3: Reservas Recurrentes

```bash
# Crear reserva todos los Lunes 19:00-21:00 por 6 semanas
curl -X POST http://localhost:8080/api/bookings/recurrent \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "courtId": 2,
    "startDate": "2026-01-27",
    "startTime": "19:00",
    "endTime": "21:00",
    "frequency": "WEEKLY",
    "numberOfWeeks": 6,
    "usesPackage": false
  }'
```

### Caso de Uso 4: Cancelación con Penalización

```bash
# Cancelar con más de 24h de anticipación (sin penalización)
curl -X PUT http://localhost:8080/api/bookings/cancel \
  -H "Content-Type: application/json" \
  -d '{
    "bookingId": 10,
    "reason": "Mal clima"
  }'

# Respuesta:
# {
#   "penaltyPercentage": 0.00,
#   "refundAmount": 270.00,
#   "message": "Cancelación exitosa sin penalización"
# }
```

### Caso de Uso 5: Lista de Espera

```bash
# 1. Intentar reservar horario ocupado
curl -X POST http://localhost:8080/api/bookings \
  ...
# Error: "Ya existe una reserva en ese horario"

# 2. Unirse a lista de espera
curl -X POST http://localhost:8080/api/waiting-list \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "courtId": 1,
    "desiredDate": "2026-01-25",
    "desiredStartTime": "18:00",
    "desiredEndTime": "20:00"
  }'

# Respuesta: {"positionInQueue": 2, ...}

# 3. Cuando otro usuario cancela, recibes notificación automática
# (Implementación de email/SMS pending)
```

---

## 🧪 Testing

### Ejecutar Tests

```bash
# Todos los tests
mvn test

# Solo tests unitarios
mvn test -Dtest=*ServiceTest

# Solo tests de integración
mvn test -Dtest=*ControllerTest

# Con cobertura
mvn clean test jacoco:report
```

### Estructura de Tests

```
src/test/java/
├── service/
│   ├── BookingServiceTest.java
│   ├── UserServiceTest.java
│   └── ValidationServiceTest.java
├── controller/
│   ├── BookingControllerTest.java
│   └── UserControllerTest.java
└── repository/
    └── BookingRepositoryTest.java
```

---

## 📊 Monitoreo y Logs

### Niveles de Log

```yaml
logging:
  level:
    root: INFO
    org.salva.task: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
```

### Logs Importantes

```
# Inicio de aplicación
2026-01-17 19:00:00.123 INFO  [...] Started CourtReservationSystemApplication in 3.456 seconds

# Creación de reserva
2026-01-17 19:05:30.456 INFO  [...] Creating booking for user: 1 on court: 2
2026-01-17 19:05:30.789 INFO  [...] Booking created successfully with id: 10

# Validación fallida
2026-01-17 19:06:00.123 ERROR [...] Validation error: Debe reservar con al menos 2 horas de anticipación

# Job programado
2026-01-17 20:00:00.000 INFO  [...] Running scheduled task: markPastBookingsAsCompleted
2026-01-17 20:00:00.234 INFO  [...] Marked 5 bookings as completed
```

---

## 🔧 Configuración Avanzada

### Variables de Entorno

```bash
# application-prod.yml
export DB_URL=jdbc:postgresql://db.example.com:5432/court_db
export DB_USERNAME=prod_user
export DB_PASSWORD=secure_password
export SERVER_PORT=8080
```

### Configuración de CORS

```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:3000", "https://yourdomain.com")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
                    .allowCredentials(true);
            }
        };
    }
}
```

---

## 📈 Performance y Optimización

### Índices de Base de Datos

```sql
-- Índices recomendados
CREATE INDEX idx_booking_date ON bookings(booking_date);
CREATE INDEX idx_booking_court_date ON bookings(court_id, booking_date);
CREATE INDEX idx_booking_user ON bookings(user_id);
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_waiting_list_court_date ON waiting_list(court_id, desired_date);
```

### Caché (Opcional)

```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("courts", "packages");
    }
}
```

---

## 🚨 Troubleshooting

### Problema: No se compila el proyecto

```bash
# Solución 1: Limpiar e instalar de nuevo
mvn clean install -U

# Solución 2: Eliminar cache de Maven
rm -rf ~/.m2/repository
mvn clean install
```

### Problema: Error de Swagger

```
Error: NoSuchMethodError: ControllerAdviceBean.<init>
```

**Solución**: Actualizar springdoc a versión 2.6.0 compatible con Spring Boot 3.2.x

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

### Problema: H2 Console no funciona

**Solución**: Verificar `application-dev.yml`

```yaml
spring:
  h2:
    console:
      enabled: true
      path: /h2-console
```

---

## 📝 Próximas Mejoras

- [ ] Implementar Spring Security con JWT
- [ ] Sistema de notificaciones por email/SMS
- [ ] Dashboard de administración
- [ ] Reportes y estadísticas
- [ ] Pasarela de pagos (Stripe/PayPal)
- [ ] Sistema de valoraciones y reviews
- [ ] App móvil (React Native)
- [ ] Calendario integrado (Google Calendar)
- [ ] Sistema de promociones y cupones
- [ ] Chat en tiempo real para soporte

---

## 👥 Contribución

### Cómo Contribuir

1. Fork el proyecto
2. Crea tu Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push al Branch (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

### Estándares de Código

- Usar Java 21 features
- Seguir convenciones de Spring Boot
- Escribir tests para nuevas funcionalidades
- Documentar métodos públicos con Javadoc
- Mantener cobertura de tests > 80%

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver archivo `LICENSE` para más detalles.

---

## 📧 Contacto

**Desarrollador**: Rodrigo Salva  
**Email**: rodrigodanielsalvasaccatoma@gmail.com 
**GitHub**: [@Rodrigo-Salva](https://github.com/Rodrigo-Salva/court-reservation-api)

---

## 🙏 Agradecimientos

- Spring Team por el excelente framework
- MapStruct team por la librería de mapeo
- Springdoc team por la integración con OpenAPI
- Comunidad de Stack Overflow

---

**Versión**: 1.0.0  
**Última actualización**: Enero 17, 2026  
**Estado**: ✅ Producción Ready

---

## 📚 Referencias

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [MapStruct Documentation](https://mapstruct.org/)
- [Springdoc OpenAPI](https://springdoc.org/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

---

**¡Gracias por usar Court Reservation System! 🎉, Alguna consulta? me podria escribir a mi correo**
