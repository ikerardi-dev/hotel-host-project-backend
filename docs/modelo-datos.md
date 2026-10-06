# Modelo de datos

Esquema en `src/main/resources/db/migration` (Flyway). SQL estándar compatible con MySQL 8 y MariaDB.

```mermaid
erDiagram
    buildings ||--o{ apartments : tiene
    apartments ||--o{ beds : tiene
    apartments ||--o{ bookings : tiene
    apartments ||--o{ cleanings : tiene
    bookings ||--o| cleanings : "origina (salida)"
    users ||--o{ cleanings : "asignada a"
    cleanings ||--o{ cleaning_checklist_items : tiene
    users ||--o{ employee_buildings : ""
    buildings ||--o{ employee_buildings : ""

    buildings {
        BIGINT id PK
        VARCHAR name UK
    }
    apartments {
        BIGINT id PK
        BIGINT building_id FK
        VARCHAR code "UK con building_id"
        INT capacity "plazas"
    }
    beds {
        BIGINT id PK
        BIGINT apartment_id FK
        VARCHAR type "DOUBLE_BED, SINGLE_BED, DOUBLE_SOFA_BED"
        INT quantity
    }
    users {
        BIGINT id PK
        VARCHAR email UK
        VARCHAR password_hash "BCrypt"
        VARCHAR first_name
        VARCHAR last_name
        VARCHAR dni UK
        VARCHAR phone
        VARCHAR role "ADMIN, CLEANING"
        BOOLEAN active "baja lógica"
        BOOLEAN must_change_password
        DATE hire_date
        VARCHAR notes
        DATETIME created_at
    }
    employee_buildings {
        BIGINT user_id PK, FK
        BIGINT building_id PK, FK
    }
    bookings {
        BIGINT id PK
        BIGINT apartment_id FK
        VARCHAR guest_name
        VARCHAR guest_phone
        VARCHAR guest_email
        VARCHAR reference
        VARCHAR channel "BOOKING, AIRBNB, DIRECT, PHONE (informativo)"
        DATETIME check_in
        DATETIME check_out
        INT guests
        VARCHAR notes
        VARCHAR status "ACTIVE, CANCELLED"
        DATETIME created_at
    }
    cleanings {
        BIGINT id PK
        BIGINT apartment_id FK
        BIGINT booking_id FK "UK, opcional"
        BIGINT assigned_to FK "opcional"
        DATE cleaning_date
        TIME start_time
        TIME end_time
        VARCHAR status "PENDING, IN_PROGRESS, DONE, CANCELLED"
        DATETIME started_at
        DATETIME finished_at
        VARCHAR incident_notes
        DATETIME created_at
    }
    cleaning_checklist_items {
        BIGINT id PK
        BIGINT cleaning_id FK
        VARCHAR label
        BOOLEAN done
        INT position
    }
```

## Reglas

- Las reservas no se borran: se cancelan (`status = CANCELLED`). Lo mismo con las limpiezas.
- Los empleados no se borran: se dan de baja (`active = false`) para conservar su historial.
- Una reserva origina como mucho una limpieza (la de su salida).
- Las plazas (`capacity`) coinciden con la suma de camas: cama doble = 2, sofá cama doble = 2, individual = 1.
