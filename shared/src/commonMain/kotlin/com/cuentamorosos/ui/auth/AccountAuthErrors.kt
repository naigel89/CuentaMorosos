package com.cuentamorosos.ui.auth

/**
 * Validación y mapeo de errores del flujo de cambio de contraseña en Ajustes → Seguridad.
 *
 * Lógica pura y multiplataforma: el mapeo inspecciona el mensaje y el nombre de la
 * excepción (y su cadena de causas) en lugar de depender de los tipos concretos de
 * Firebase, que difieren entre el SDK nativo de Android y gitlive.
 */
object AccountAuthErrors {

    /**
     * Valida los campos del formulario de cambio de contraseña (spec profile-security
     * R-001/R-003). Devuelve un mensaje de error para mostrar al usuario, o null si
     * el cambio puede intentarse.
     */
    fun validatePasswordChange(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String,
    ): String? = when {
        currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank() ->
            "Completa todos los campos para cambiar la contraseña."
        newPassword.length < 6 ->
            "La nueva contraseña necesita al menos 6 caracteres."
        newPassword != confirmPassword ->
            "Las contraseñas nuevas no coinciden."
        newPassword == currentPassword ->
            "La nueva contraseña debe ser distinta de la actual."
        else -> null
    }

    /**
     * Traduce un fallo de reauthenticate/updatePassword a un mensaje en español
     * (spec profile-security R-002 incluida).
     */
    fun mapChangePasswordError(throwable: Throwable): String {
        val haystack = buildString {
            var current: Throwable? = throwable
            while (current != null) {
                append(current::class.simpleName ?: "")
                append(' ')
                append(current.message ?: "")
                append(' ')
                current = current.cause
            }
        }.lowercase()

        return when {
            "recent" in haystack && ("login" in haystack || "auth" in haystack) ->
                "Tu sesión expiró. Cerrá sesión y volvé a iniciarla para cambiar la contraseña."
            "wrong_password" in haystack ||
                "invalid_login_credentials" in haystack ||
                "invalid_credential" in haystack ||
                // Nombre de clase del SDK (FirebaseAuthInvalidCredentialsException)
                "invalidcredentials" in haystack ||
                // Mensaje real del backend, sin código entre corchetes
                "credential is incorrect" in haystack ||
                "password is invalid" in haystack ->
                "La contraseña actual no es correcta."
            "weak_password" in haystack || "weak password" in haystack ->
                "La nueva contraseña es demasiado débil. Prueba con una más larga."
            "network" in haystack ->
                "No hay conexión. Revisa tu red e inténtalo de nuevo."
            "too_many" in haystack || "too many" in haystack ->
                "Demasiados intentos. Espera unos minutos y vuelve a probarlo."
            else ->
                "No se pudo cambiar la contraseña. Inténtalo de nuevo."
        }
    }
}
