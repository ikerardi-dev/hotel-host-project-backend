package com.hotelhost.backend.dto;

/**
 * Formato único de error de la API: { "error": "CODIGO", "message": "Texto para el usuario" }
 */
public record ApiError(String error, String message) {
}
