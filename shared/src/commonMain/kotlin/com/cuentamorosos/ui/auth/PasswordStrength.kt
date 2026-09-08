package com.cuentamorosos.ui.auth

/**
 * Fortaleza de una contraseña en niveles 0..4.
 *
 * 0 = vacía. Por debajo de 8 caracteres el nivel es siempre 1 (débil), porque
 * ese es el mínimo que exige el registro: mezclar símbolos no compensa una
 * contraseña corta. A partir de 8 se suma un punto por mezclar letras y
 * números, otro por mayúsculas+minúsculas o símbolos, y otro por llegar a 12
 * caracteres.
 */
fun passwordStrength(password: String): Int {
    if (password.isEmpty()) return 0
    if (password.length < 8) return 1

    var score = 1
    val hasLetter = password.any { it.isLetter() }
    val hasDigit = password.any { it.isDigit() }
    if (hasLetter && hasDigit) score += 1

    val hasCaseMix = password.any { it.isUpperCase() } && password.any { it.isLowerCase() }
    val hasSymbol = password.any { !it.isLetterOrDigit() }
    if (hasCaseMix || hasSymbol) score += 1

    if (password.length >= 12) score += 1

    return score.coerceAtMost(4)
}

/** Etiqueta visible para cada nivel de [passwordStrength]. Vacía para 0. */
fun passwordStrengthLabel(strength: Int): String = when {
    strength <= 0 -> ""
    strength == 1 -> "Débil"
    strength == 2 -> "Aceptable"
    strength == 3 -> "Buena"
    else -> "Fuerte"
}
