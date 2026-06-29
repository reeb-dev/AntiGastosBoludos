package com.antigastos.boludos.ui.home.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.domain.RecurringPhantomSuggestion

/**
 * Sugerencia cuando el usuario repite a mano un gasto parecido en varios meses.
 */
@Composable
internal fun PhantomRecurringCard(
    suggestion: RecurringPhantomSuggestion,
    onOpenSubscriptions: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "🔁 ¿Esto es un gasto fijo?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Cargaste algo parecido a «${suggestion.suggestedName}» " +
                    "(${MoneyFormat.formatPesos(suggestion.typicalAmountPesos)} en ${suggestion.categoryName}) " +
                    "en ${suggestion.distinctMonths} meses distintos. " +
                    "Si lo pasás a Suscripciones, un día del mes se anota solo y no te olvidás.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onOpenSubscriptions,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Ir a Suscripciones") }
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Ahora no") }
        }
    }
}
