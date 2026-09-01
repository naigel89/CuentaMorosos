package com.cuentamorosos.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cuentamorosos.shared.generated.resources.Res
import cuentamorosos.shared.generated.resources.logo_cuentamorosos
import org.jetbrains.compose.resources.painterResource

/**
 * La marca de la app: la "M" verde.
 *
 * Es el mismo vector que usa el launcher de Android
 * (`mipmap-anydpi-v26/ic_launcher_foreground.xml`), copiado a `composeResources`
 * para que ambos hosts pinten exactamente el mismo dibujo. Antes iOS mostraba
 * una marca provisional y Android el recurso nativo — el tipo de divergencia
 * silenciosa que este módulo existe para impedir.
 */
@Composable
fun CuentaMorososLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(Res.drawable.logo_cuentamorosos),
        contentDescription = "CuentaMorosos",
        modifier = modifier,
    )
}
