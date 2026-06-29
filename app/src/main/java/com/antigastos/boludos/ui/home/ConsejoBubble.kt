package com.antigastos.boludos.ui.home

import androidx.compose.runtime.Composable
import com.antigastos.boludos.domain.Consejo

/**
 * Modal animado de consejos según tus gastos (premio / sugerencia / freno / emergencia).
 * Implementación: [ConsejoEventModal].
 *
 * @param onCyclePersona si no es null, se renderiza un botón "ruleta" arriba del
 *   modal que cambia la personalidad que pone la voz al consejo.
 */
@Composable
fun ConsejoBubble(
    consejo: Consejo?,
    onDismiss: () -> Unit,
    onCyclePersona: (() -> Unit)? = null,
) {
    ConsejoEventModal(
        consejo = consejo,
        onDismiss = onDismiss,
        onCyclePersona = onCyclePersona,
    )
}
