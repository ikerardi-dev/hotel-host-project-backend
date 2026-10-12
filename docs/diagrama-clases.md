# Diagrama de clases

Tres vistas del backend, de lo más concreto a lo más general:

1. [Modelo de dominio](#1-modelo-de-dominio): las entidades JPA y sus relaciones.
2. [Capas](#2-capas): qué controlador usa qué servicio y qué servicio usa qué repositorio.
3. [Núcleo de reservas y limpiezas](#3-núcleo-de-reservas-y-limpiezas): los dos servicios con más lógica y el manejo de errores.

El esquema de la base de datos (tablas y columnas) está en [modelo-datos.md](modelo-datos.md).

## 1. Modelo de dominio

Entidades de `com.hotelhost.backend.entity`. Los getters y setters se omiten.

```mermaid
classDiagram
    direction LR

    class Building {
        Long id
        String name
    }
    class Apartment {
        Long id
        String code
        Integer capacity
    }
    class Bed {
        Long id
        BedType type
        Integer quantity
    }
    class Booking {
        Long id
        String guestName
        String guestPhone
        String guestEmail
        String reference
        BookingChannel channel
        LocalDateTime checkIn
        LocalDateTime checkOut
        Integer guests
        String notes
        BookingStatus status
        LocalDateTime createdAt
    }
    class Cleaning {
        Long id
        LocalDate cleaningDate
        LocalTime startTime
        LocalTime endTime
        CleaningStatus status
        LocalDateTime startedAt
        LocalDateTime finishedAt
        String incidentNotes
        LocalDateTime createdAt
    }
    class CleaningChecklistItem {
        Long id
        String label
        boolean done
        Integer position
    }
    class User {
        Long id
        String email
        String passwordHash
        String firstName
        String lastName
        String dni
        String phone
        UserRole role
        boolean active
        boolean mustChangePassword
        LocalDate hireDate
        String notes
        LocalDateTime createdAt
    }

    class BedType {
        <<enumeration>>
        DOUBLE_BED
        DOUBLE_SOFA_BED
        SINGLE_BED
    }
    class BookingChannel {
        <<enumeration>>
        BOOKING
        AIRBNB
        DIRECT
        PHONE
    }
    class BookingStatus {
        <<enumeration>>
        ACTIVE
        CANCELLED
    }
    class CleaningStatus {
        <<enumeration>>
        PENDING
        IN_PROGRESS
        DONE
        CANCELLED
    }
    class UserRole {
        <<enumeration>>
        ADMIN
        CLEANING
    }

    Building "1" <-- "*" Apartment : building
    Apartment "1" <-- "*" Bed : apartment
    Apartment "1" <-- "*" Booking : apartment
    Apartment "1" <-- "*" Cleaning : apartment
    Booking "0..1" <-- "0..1" Cleaning : booking
    User "0..1" <-- "*" Cleaning : assignedTo
    Cleaning "1" *-- "*" CleaningChecklistItem : checklistItems
    User "*" --> "*" Building : buildings

    Bed ..> BedType
    Booking ..> BookingChannel
    Booking ..> BookingStatus
    Cleaning ..> CleaningStatus
    User ..> UserRole
```

- Un **edificio** tiene apartamentos, y cada **apartamento** tiene sus camas; la suma de plazas es `capacity`.
- Una **reserva** es una anotación manual. El canal (`BOOKING`, `AIRBNB`…) es solo informativo: no hay integración con esas plataformas.
- Al registrar una reserva se puede programar la **limpieza** de su salida (`booking`). También puede haber limpiezas sin reserva.
- La limpieza tiene un **checklist** que se crea y se borra con ella (composición, `cascade = ALL`).
- `User` representa tanto al admin como a los empleados de limpieza (`role`). La baja de un empleado es lógica (`active = false`), para conservar su historial de limpiezas.

## 2. Capas

Cada petición pasa por `controller` (fino, solo HTTP) → `service` (toda la lógica de negocio) → `repository` (Spring Data JPA). Los controladores reciben y devuelven DTOs (`record`), nunca entidades.

```mermaid
classDiagram
    direction TB

    namespace controller {
        class AuthController
        class MeController
        class EmployeeController
        class BuildingController
        class ApartmentController
        class BookingController
        class CleaningController
        class DashboardController
        class OccupancyController
    }

    namespace service {
        class AuthService
        class UserService
        class EmployeeService
        class BuildingService
        class ApartmentService
        class BookingService
        class CleaningService
        class DashboardService
        class OccupancyService
    }

    namespace repository {
        class UserRepository
        class BuildingRepository
        class ApartmentRepository
        class BookingRepository
        class CleaningRepository
    }

    AuthController --> AuthService
    MeController --> UserService
    MeController --> CleaningService
    EmployeeController --> EmployeeService
    BuildingController --> BuildingService
    ApartmentController --> ApartmentService
    ApartmentController --> BookingService
    ApartmentController --> CleaningService
    BookingController --> BookingService
    CleaningController --> CleaningService
    DashboardController --> DashboardService
    OccupancyController --> OccupancyService

    AuthService --> UserRepository
    AuthService --> JwtService
    UserService --> UserRepository
    EmployeeService --> UserRepository
    EmployeeService --> BuildingRepository
    EmployeeService --> CleaningRepository
    BuildingService --> BuildingRepository
    ApartmentService --> ApartmentRepository
    ApartmentService --> BookingRepository
    ApartmentService --> CleaningRepository
    BookingService --> BookingRepository
    BookingService --> ApartmentRepository
    BookingService --> CleaningRepository
    BookingService --> EmployeeService
    CleaningService --> CleaningRepository
    CleaningService --> BookingRepository
    CleaningService --> EmployeeService
    DashboardService --> BookingRepository
    DashboardService --> CleaningRepository
    DashboardService --> ApartmentService
    OccupancyService --> ApartmentRepository
    OccupancyService --> BookingRepository
    OccupancyService --> CleaningRepository
```

Fuera de estas capas:

- **`security`:**
  - `JwtService` firma el token, con los claims `sub`, `role` y `exp`.
  - `PasswordChangeRequiredFilter` bloquea la API a los empleados que aún tienen la contraseña temporal.
  - `CurrentUser` es el usuario de la petición, sacado del JWT.
- **`config`:**
  - `SecurityConfig` define las rutas públicas, los permisos por rol y el JWT.
  - `CorsConfig` configura CORS.
  - `AdminInitializer` crea el primer admin si no existe.
- **`exception`:** el formato único de error, que se detalla abajo.

## 3. Núcleo de reservas y limpiezas

```mermaid
classDiagram
    direction LR

    class BookingService {
        +create(CreateBookingRequest) BookingResponse
        +update(Long id, UpdateBookingRequest) BookingResponse
        +cancel(Long id) BookingResponse
        +findByApartment(Long apartmentId, LocalDate from, LocalDate to) List~BookingResponse~
        +availability(LocalDateTime checkIn, LocalDateTime checkOut, Long excludeBookingId) AvailabilityResponse
    }
    class CleaningService {
        +search(...) List~CleaningResponse~
        +findByApartment(Long apartmentId, LocalDate from, LocalDate to) List~CleaningResponse~
        +findMine(CurrentUser) List~CleaningResponse~
        +findById(Long id, CurrentUser) CleaningResponse
        +update(Long id, UpdateCleaningRequest) CleaningResponse
        +updateStatus(Long id, CleaningStatus, CurrentUser) CleaningResponse
        +updateChecklistItem(Long id, Long itemId, boolean done, CurrentUser) CleaningResponse
        +updateIncident(Long id, String notes, CurrentUser) CleaningResponse
    }
    class EmployeeService {
        +findAll() List~EmployeeResponse~
        +create(CreateEmployeeRequest) EmployeeResponse
        +updateStatus(Long id, boolean active) EmployeeStatusResponse
        +findActiveCleaner(Long id) User
    }
    class CurrentUser {
        <<record>>
        Long id
        boolean admin
    }
    class ApiException {
        HttpStatus status
        String code
        String message
    }
    class GlobalExceptionHandler {
        <<RestControllerAdvice>>
    }
    class ApiError {
        <<record>>
        String error
        String message
    }

    BookingService --> EmployeeService : valida el empleado asignado
    CleaningService --> EmployeeService : valida el empleado al reasignar
    CleaningService ..> CurrentUser : comprueba permisos
    BookingService ..> ApiException : lanza
    CleaningService ..> ApiException : lanza
    EmployeeService ..> ApiException : lanza
    GlobalExceptionHandler ..> ApiException : captura
    GlobalExceptionHandler ..> ApiError : devuelve
```

- **`BookingService`** evita las reservas solapadas dentro de una transacción. Primero bloquea la fila del apartamento (`findByIdForUpdate`, bloqueo pesimista); después comprueba el solape. Así, dos peticiones a la vez no pueden reservar las mismas fechas. Al crear la reserva puede programar la limpieza de la salida, y al cancelarla cancela también esa limpieza si está pendiente.
- **`CleaningService`** recibe el `CurrentUser` para aplicar los permisos en el backend: un empleado solo ve y modifica sus propias limpiezas, y el admin las de todos.
- **Errores:** los servicios lanzan `ApiException(status, "CODIGO", "mensaje")`. `GlobalExceptionHandler` la convierte en `{ "error": "BOOKING_OVERLAP", "message": "..." }`, el mismo formato que usará el backend en PHP.
