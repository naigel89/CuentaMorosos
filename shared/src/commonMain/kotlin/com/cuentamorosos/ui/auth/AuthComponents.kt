package com.cuentamorosos.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuentamorosos.ui.NeoFintechColorSet
import com.cuentamorosos.ui.NeoFintechShapes

/**
 * Piezas compartidas por las pantallas de acceso (login, registro).
 *
 * Estas pantallas viven ANTES de [com.cuentamorosos.ui.CuentaMorososTheme]
 * (no hay preferencias de usuario que leer todavía), así que reciben el
 * [NeoFintechColorSet] resuelto por la propia pantalla a partir del tema del
 * sistema, en lugar de leer `LocalNeoFintechColors`.
 */

/** Colores de campo de texto del sistema: borde `outlineVariant`, foco verde. */
@Composable
internal fun authFieldColors(colors: NeoFintechColorSet): TextFieldColors =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.primaryContainer,
        unfocusedBorderColor = colors.outlineVariant,
        focusedLabelColor = colors.primaryContainer,
        unfocusedLabelColor = colors.onSurfaceVariant,
        cursorColor = colors.primaryContainer,
        focusedTextColor = colors.onSurface,
        unfocusedTextColor = colors.onSurface,
        errorBorderColor = colors.error,
        errorLabelColor = colors.error,
        errorSupportingTextColor = colors.error,
        focusedSupportingTextColor = colors.onSurfaceVariant,
        unfocusedSupportingTextColor = colors.onSurfaceVariant,
        focusedTrailingIconColor = colors.onSurfaceVariant,
        unfocusedTrailingIconColor = colors.onSurfaceVariant,
        errorTrailingIconColor = colors.onSurfaceVariant,
    )

/**
 * Error de servidor con el patrón de banner de estado del resto de la app
 * (fondo tintado + punto de color), en lugar de una línea de texto rojo suelta.
 */
@Composable
internal fun AuthErrorBanner(
    message: String,
    colors: NeoFintechColorSet,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.error.copy(alpha = 0.08f), NeoFintechShapes.lg)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(colors.error),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = colors.error,
        )
    }
}

/**
 * CTA principal de acceso: el botón de marca (`buttonContainer`/`onButton`),
 * 56 dp de alto y radio [NeoFintechShapes.lg]. Con [loading] sustituye el texto
 * por el spinner — las pruebas de UI dependen de que el literal desaparezca.
 */
@Composable
internal fun AuthPrimaryButton(
    text: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    colors: NeoFintechColorSet,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = NeoFintechShapes.lg,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.buttonContainer,
            contentColor = colors.onButton,
            disabledContainerColor = colors.surfaceContainerHigh,
            disabledContentColor = colors.onSurfaceVariant,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
