-- Los administradores no pasan por el cambio obligatorio de contraseña (solo los empleados con contraseña temporal).
UPDATE users SET must_change_password = FALSE WHERE role = 'ADMIN';
