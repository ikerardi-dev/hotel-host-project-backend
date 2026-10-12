# Gestión de Apartamentos · Backend

API REST de una herramienta **interna** para organizar el servicio de limpieza de **19 apartamentos turísticos** repartidos en 3 edificios (Legazpi, Casa Baratura y La Refierta).

El encargado anota las entradas y salidas de huéspedes, y la app programa y controla las limpiezas entre una estancia y la siguiente. El personal de limpieza ve sus tareas del día desde el móvil y las va completando.

> Frontend (Vue 3): [hotel-host-project-frontend](https://github.com/ikerardi-dev/hotel-host-project-frontend)

---

## Índice

- [Funcionalidades](#funcionalidades)
- [Tecnologías](#tecnologías)
- [Arquitectura](#arquitectura)
- [Puesta en marcha en local](#puesta-en-marcha-en-local)
- [Variables de entorno](#variables-de-entorno)
- [API](#api)
- [Seguridad](#seguridad)
- [Base de datos](#base-de-datos)
- [Tests](#tests)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Próximos pasos](#próximos-pasos)

---

## Funcionalidades

Dos roles con vistas distintas. **No hay registro público**: solo el administrador da de alta al personal.

**Administrador**
- **Panel "Hoy":** entradas, salidas, limpiezas y ocupación del día, con una línea de tiempo ordenada por hora.
- **Apartamentos:** estado calculado de cada uno (ocupado, pendiente de limpieza, limpio y listo, libre) y calendario mensual con sus reservas y limpiezas.
- **Reservas:** registrar, editar y cancelar. Se comprueba la capacidad y que no se solapen, y al guardar se programa automáticamente la limpieza de salida.
- **Planning de ocupación:** semana por edificio, con las reservas como barras y las limpiezas marcadas.
- **Control de limpiezas:** todas las limpiezas con filtros (fechas, estado, edificio y empleado), historial y reasignación de empleado o franja horaria.
- **Empleados:** alta con contraseña temporal, baja lógica (conserva el historial) y reactivación.

**Empleado de limpieza**
- **Mis limpiezas:** tareas de hoy y próximos días, más las atrasadas.
- **Detalle de la tarea:** camas a preparar, checklist de 6 tareas, incidencias, y empezar o finalizar.
- **Primer acceso:** obliga a cambiar la contraseña temporal.

**Todos los usuarios:** perfil editable y cambio de contraseña.

> Las reservas son una **anotación manual** para saber cuándo hay que limpiar. No hay conexión con Booking, Airbnb ni otras plataformas: el canal es solo informativo.

---

## Tecnologías

| | |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Validation, Security) |
| Autenticación | JWT firmado con HS256 (Spring Security OAuth2 Resource Server) |
| Base de datos | MySQL 8.4 (compatible con MariaDB) |
| Migraciones | Flyway (SQL puro) |
| Documentación | springdoc-openapi (Swagger UI) |
| Tests | JUnit 6, Spring Boot Test, MockMvc |
| Build | Maven (con Maven Wrapper) |
| Entorno local | Docker Compose |

Sin Lombok: los DTO son `record` y las entidades tienen getters y setters normales.

---

## Arquitectura

Arquitectura en capas, con toda la lógica de negocio en los servicios:

```
controller  →  service  →  repository  →  MySQL
   (HTTP)      (reglas)      (JPA)
     ↑ dto (entrada y salida, records)
```

- **controller:** recibe la petición, valida el cuerpo (`@Valid`) y delega en el servicio.
- **service:** reglas de negocio y transacciones (solapamientos, estados de limpieza, permisos sobre limpiezas…).
- **repository:** acceso a datos con Spring Data JPA.
- **Errores:** los servicios lanzan `ApiException(estado, CODIGO, mensaje)` y `GlobalExceptionHandler` responde siempre con el mismo formato:

```json
{ "error": "BOOKING_OVERLAP", "message": "Las fechas se solapan con otra reserva de este apartamento" }
```

El contrato de la API (rutas, JSON en camelCase, fechas ISO 8601 y formato de error) está pensado para poder reimplementar el backend en PHP/Laravel sin cambiar el frontend.

---

## Puesta en marcha en local

### Requisitos

- Java 21
- Docker Desktop (para MySQL)
- No hace falta instalar Maven: se usa el wrapper `./mvnw`

### Pasos

```bash
# 1. Clonar el repositorio
git clone https://github.com/ikerardi-dev/hotel-host-project-backend.git
cd hotel-host-project-backend

# 2. Crear el .env a partir de la plantilla y rellenar los valores
cp .env.example .env

# 3. Arrancar MySQL en Docker (puerto 3308)
docker compose up -d

# 4. Arrancar la API (puerto 8081)
./mvnw spring-boot:run
```

Al arrancar:
- **Flyway** crea las tablas y carga los 3 edificios y los 19 apartamentos con sus camas.
- Si no existe ningún administrador, se crea uno con `ADMIN_EMAIL` y `ADMIN_PASSWORD` del `.env`.

| | URL |
|---|---|
| API | http://localhost:8081/api |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| OpenAPI (JSON) | http://localhost:8081/v3/api-docs |

**Comandos útiles de Docker**

```bash
docker compose stop      # parar MySQL sin perder datos
docker compose down -v   # borrar el contenedor Y los datos (base de datos desde cero)
```

---

## Variables de entorno

Se leen del archivo `.env` en local o de las variables del servidor en producción. **El `.env` no se sube al repositorio**; la plantilla es `.env.example`.

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DB_HOST` | Host de MySQL | `localhost` |
| `DB_PORT` | Puerto de MySQL | `3308` |
| `DB_NAME` | Nombre de la base de datos | `hotel_host` |
| `DB_USER` / `DB_PASSWORD` | Usuario de la aplicación | — |
| `DB_ROOT_PASSWORD` | Contraseña de root (solo la usa Docker) | — |
| `SERVER_PORT` | Puerto de la API | `8081` |
| `CORS_ALLOWED_ORIGINS` | Orígenes del frontend permitidos, separados por comas | `http://localhost:5173` |
| `JWT_SECRET` | Clave para firmar los tokens (mínimo 32 caracteres) | — |
| `JWT_EXPIRATION_HOURS` | Duración de la sesión | `12` |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Administrador inicial (solo se crea si no hay ninguno) | — |

---

## API

Todas las rutas empiezan por `/api` y, salvo el login, necesitan la cabecera `Authorization: Bearer <token>`. La documentación completa e interactiva está en **Swagger UI**.

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| POST | `/auth/login` | Público | Inicia sesión y devuelve el token |
| GET | `/me` | Todos | Datos del usuario |
| PUT | `/me` | Todos | Editar nombre, apellidos y teléfono |
| PUT | `/me/password` | Todos | Cambiar contraseña (en el primer acceso no pide la actual) |
| GET | `/me/cleanings` | Limpieza | Mis limpiezas de hoy en adelante y las atrasadas |
| GET | `/dashboard/today` | Admin | Cifras y línea de tiempo del día |
| GET | `/buildings` | Todos | Edificios con su número de apartamentos |
| GET | `/apartments` | Admin | Apartamentos con estado y huésped actual o próximo |
| GET | `/apartments/{id}` | Admin | Un apartamento |
| GET | `/apartments/{id}/bookings?from&to` | Admin | Reservas del calendario |
| GET | `/apartments/{id}/cleanings?from&to` | Admin | Limpiezas del calendario |
| GET | `/apartments/availability?checkIn&checkOut` | Admin | Apartamentos ocupados en unas fechas |
| GET | `/occupancy?buildingId&from&to` | Admin | Planning de ocupación de un edificio |
| POST | `/bookings` | Admin | Registrar reserva (y su limpieza de salida) |
| PUT | `/bookings/{id}` | Admin | Editar reserva |
| PATCH | `/bookings/{id}/cancel` | Admin | Cancelar reserva y su limpieza pendiente |
| GET | `/cleanings?from&to&status&buildingId&employeeId&unassigned` | Admin | Control de limpiezas |
| GET | `/cleanings/{id}` | Admin o empleado asignado | Detalle de una limpieza |
| PATCH | `/cleanings/{id}` | Admin | Reasignar empleado y franja (solo pendientes) |
| PATCH | `/cleanings/{id}/status` | Admin o empleado asignado | Empezar o finalizar |
| PATCH | `/cleanings/{id}/checklist/{itemId}` | Admin o empleado asignado | Marcar una tarea de la checklist |
| PATCH | `/cleanings/{id}/incident` | Admin o empleado asignado | Guardar la incidencia |
| GET | `/employees` | Admin | Empleados de limpieza |
| POST | `/employees` | Admin | Alta de empleado con contraseña temporal |
| PATCH | `/employees/{id}/status` | Admin | Dar de baja o reactivar |

---

## Seguridad

- **Sin sesión en el servidor:** cada petición lleva un **JWT** con `sub` (id del usuario), `role` y `exp`.
- **Contraseñas cifradas con BCrypt.** Nunca se devuelven en las respuestas.
- **Permisos por rol** en `SecurityConfig` y, además, comprobaciones en los servicios: un empleado solo puede ver y modificar **sus** limpiezas.
- **El personal de limpieza no recibe datos personales de los huéspedes**, solo las horas de entrada y salida.
- **Contraseña temporal:** mientras el empleado no la cambie, cualquier otra petición devuelve `403 PASSWORD_CHANGE_REQUIRED`.
- **Baja lógica:** un empleado dado de baja no puede iniciar sesión, y sus limpiezas pendientes se quedan sin asignar.
- **CORS** limitado a los orígenes de `CORS_ALLOWED_ORIGINS`.
- **Secretos** solo en variables de entorno.

---

## Base de datos

Esquema gestionado con **Flyway** en `src/main/resources/db/migration`:

| Migración | Contenido |
|---|---|
| `V1__init.sql` | Tablas, claves foráneas, restricciones e índices |
| `V2__seed_buildings_apartments.sql` | 3 edificios, 19 apartamentos y sus camas |
| `V3__admins_without_password_change.sql` | Los administradores no pasan por el cambio obligatorio de contraseña |

Diagrama entidad-relación en [docs/modelo-datos.md](docs/modelo-datos.md) (GitHub lo dibuja automáticamente).

**Reglas importantes:**
- Las reservas y las limpiezas **no se borran**, se cancelan. Los empleados se dan de baja.
- **No se permiten reservas solapadas** en un mismo apartamento. Se comprueba en una transacción que **bloquea el apartamento** (`SELECT … FOR UPDATE`), así que dos guardados simultáneos no pueden crear duplicados. La entrada sí puede coincidir con el día de salida anterior.
- Una migración ya aplicada **no se edita nunca**: los cambios van en una nueva (`V4__…`).

---

## Tests

**107 tests** de dos tipos:

| Tipo | Cuántos | Qué prueban | Cómo |
|---|---|---|---|
| **Unitarios** (`src/test/.../service`) | 29 | La lógica de negocio de cada servicio, aislada | JUnit + **Mockito**: los repositorios son *mocks*, sin Spring ni base de datos. Tardan milisegundos |
| **Integración y capa web** (`controller`, `config`, `exception`) | 78 | Cada endpoint de principio a fin: HTTP, seguridad, validación, servicio y MySQL | `@SpringBootTest` + MockMvc contra MySQL real. Cada test **deshace sus datos al terminar** (`@Transactional`) |

```bash
docker compose up -d     # los tests de integración necesitan MySQL
./mvnw test              # todos
./mvnw test -Dtest=*ServiceTest   # solo los unitarios (no necesitan MySQL)
```

Cubren, entre otros:
- login, cuenta dada de baja, roles y contraseña temporal;
- fechas, capacidad y solapamiento de reservas (incluidos los casos límite), y la limpieza automática;
- el flujo de estados de las limpiezas, la checklist y los permisos por empleado;
- editar y cancelar reservas (la limpieza pendiente se mueve o se cancela con ellas);
- formato único de errores y CORS.

---

## Estructura del proyecto

```
src/main/java/com/hotelhost/backend
├── config/        Seguridad, CORS y creación del admin inicial
├── controller/    Endpoints REST
├── dto/           Datos de entrada y salida (records)
├── entity/        Entidades JPA y enums
├── exception/     ApiException y manejador global de errores
├── repository/    Repositorios Spring Data JPA
├── security/      Generación del JWT, filtro de contraseña temporal y usuario actual
└── service/       Lógica de negocio
src/main/resources
├── application.yaml
└── db/migration/  Migraciones Flyway
```

---

## Próximos pasos

- Despliegue online (backend y MySQL en Railway; frontend en el hosting del hotel).
- Editar empleados y restablecer su contraseña desde el panel.
- Fotos en las incidencias, vista mensual del planning y bloqueo de apartamentos por avería.
- Migración del backend a **PHP (Laravel)**, manteniendo el mismo contrato de API, para alojarlo en el hosting del hotel.
