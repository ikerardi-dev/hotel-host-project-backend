-- Esquema inicial. SQL estándar compatible con MySQL 8 y MariaDB 10.

CREATE TABLE buildings (
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_buildings_name UNIQUE (name)
);

CREATE TABLE apartments (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    building_id BIGINT      NOT NULL,
    code        VARCHAR(10) NOT NULL,
    capacity    INT         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_apartments_building_code UNIQUE (building_id, code),
    CONSTRAINT fk_apartments_building FOREIGN KEY (building_id) REFERENCES buildings (id),
    CONSTRAINT ck_apartments_capacity CHECK (capacity > 0)
);

-- type: DOUBLE_BED, SINGLE_BED, DOUBLE_SOFA_BED
CREATE TABLE beds (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    apartment_id BIGINT      NOT NULL,
    type         VARCHAR(20) NOT NULL,
    quantity     INT         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_beds_apartment_type UNIQUE (apartment_id, type),
    CONSTRAINT fk_beds_apartment FOREIGN KEY (apartment_id) REFERENCES apartments (id),
    CONSTRAINT ck_beds_quantity CHECK (quantity > 0)
);

-- role: ADMIN, CLEANING
CREATE TABLE users (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    email                VARCHAR(255) NOT NULL,
    password_hash        VARCHAR(100) NOT NULL,
    first_name           VARCHAR(100) NOT NULL,
    last_name            VARCHAR(150) NOT NULL,
    dni                  VARCHAR(20),
    phone                VARCHAR(30),
    role                 VARCHAR(20)  NOT NULL,
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN      NOT NULL DEFAULT TRUE,
    hire_date            DATE,
    notes                VARCHAR(500),
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_dni UNIQUE (dni)
);

-- Edificios asignados a cada empleado de limpieza
CREATE TABLE employee_buildings (
    user_id     BIGINT NOT NULL,
    building_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, building_id),
    CONSTRAINT fk_employee_buildings_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_employee_buildings_building FOREIGN KEY (building_id) REFERENCES buildings (id)
);

-- channel: BOOKING, AIRBNB, DIRECT, PHONE (solo informativo)
-- status: ACTIVE, CANCELLED
CREATE TABLE bookings (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    apartment_id BIGINT       NOT NULL,
    guest_name   VARCHAR(150) NOT NULL,
    guest_phone  VARCHAR(30),
    guest_email  VARCHAR(255),
    reference    VARCHAR(100),
    channel      VARCHAR(20)  NOT NULL,
    check_in     DATETIME     NOT NULL,
    check_out    DATETIME     NOT NULL,
    guests       INT          NOT NULL,
    notes        VARCHAR(500),
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_bookings_apartment FOREIGN KEY (apartment_id) REFERENCES apartments (id),
    CONSTRAINT ck_bookings_dates CHECK (check_out > check_in),
    CONSTRAINT ck_bookings_guests CHECK (guests > 0)
);

CREATE INDEX ix_bookings_apartment_dates ON bookings (apartment_id, check_in, check_out);

-- status: PENDING, IN_PROGRESS, DONE, CANCELLED
CREATE TABLE cleanings (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    apartment_id   BIGINT        NOT NULL,
    booking_id     BIGINT,
    assigned_to    BIGINT,
    cleaning_date  DATE          NOT NULL,
    start_time     TIME          NOT NULL,
    end_time       TIME          NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    started_at     DATETIME,
    finished_at    DATETIME,
    incident_notes VARCHAR(1000),
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_cleanings_booking UNIQUE (booking_id),
    CONSTRAINT fk_cleanings_apartment FOREIGN KEY (apartment_id) REFERENCES apartments (id),
    CONSTRAINT fk_cleanings_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_cleanings_assigned_to FOREIGN KEY (assigned_to) REFERENCES users (id),
    CONSTRAINT ck_cleanings_times CHECK (end_time > start_time)
);

CREATE INDEX ix_cleanings_date ON cleanings (cleaning_date);
CREATE INDEX ix_cleanings_assigned_date ON cleanings (assigned_to, cleaning_date);

CREATE TABLE cleaning_checklist_items (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    cleaning_id BIGINT       NOT NULL,
    label       VARCHAR(150) NOT NULL,
    done        BOOLEAN      NOT NULL DEFAULT FALSE,
    position    INT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_checklist_cleaning FOREIGN KEY (cleaning_id) REFERENCES cleanings (id) ON DELETE CASCADE
);
