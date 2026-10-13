-- Nombre del empleado que hizo la limpieza. Se guarda al eliminarlo para no perder quién la hizo en el historial.
ALTER TABLE cleanings ADD COLUMN assigned_to_name VARCHAR(255) NULL;
