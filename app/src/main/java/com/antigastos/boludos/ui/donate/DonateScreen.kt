package com.antigastos.boludos.ui.donate

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.domain.DonationBankConfig
import com.antigastos.boludos.domain.DonationCatalog
import com.antigastos.boludos.domain.DonationTier
import com.antigastos.boludos.domain.MoneyFormat

private val MercadoPagoBlue = Color(0xFF009EE3)
private val MercadoPagoDark = Color(0xFF007EB5)

@Composable
fun DonateScreen(
    viewModel: DonateViewModel = viewModel(
        factory = DonateViewModel.factory(LocalContext.current.applicationContext as AntiGastosApplication),
    ),
    showDeveloperEarnings: Boolean = false,
) {
    val ui by viewModel.uiState.collectAsState()
    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var selectedTier by remember { mutableStateOf<DonationTier?>(DonationCatalog.tiers.firstOrNull()) }
    var showOtherMethods by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshMercadoPagoInstalled(ctx)
    }

    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CafeHero()

            if (!ui.donationConfigured) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text(
                        "Las donaciones no están disponibles en esta versión.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else {
                Text(
                    "Elegí tu cafecito",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                )

                DonationCatalog.tiers.forEach { tier ->
                    CafeTierCard(
                        tier = tier,
                        selected = selectedTier?.key == tier.key,
                        onClick = { selectedTier = tier },
                    )
                }

                Card(
                    onClick = { selectedTier = null },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedTier == null) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ),
                    border = if (selectedTier == null) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        null
                    },
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("💝", fontSize = 32.sp)
                        Column {
                            Text("Otro monto", fontWeight = FontWeight.Bold)
                            Text(
                                "Abrís Mercado Pago y ponés lo que quieras.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (selectedTier != null) {
                            viewModel.openMercadoPago(ctx, selectedTier)
                        } else {
                            viewModel.openMercadoPagoFreeAmount(ctx)
                        }
                    },
                    enabled = ui.donationConfigured,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MercadoPagoBlue,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        if (selectedTier != null) {
                            "☕ Transferir ${MoneyFormat.formatPesos(selectedTier!!.amountPesos)} con Mercado Pago"
                        } else {
                            "☕ Abrir Mercado Pago"
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (!ui.mercadoPagoInstalled) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            "Si no tenés la app, te llevamos a instalarla o al navegador.",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }

                TextButton(onClick = { viewModel.thankYou(selectedTier) }) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("  Ya transferí — gracias")
                }
            }

            OutlinedButton(
                onClick = { showOtherMethods = !showOtherMethods },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (showOtherMethods) "Ocultar otros medios" else "CBU / copiar datos")
            }

            if (showOtherMethods) {
                OtherMethodsCard(
                    onCopyAlias = { viewModel.copyAlias(ctx) },
                    onCopyCbu = { viewModel.copyCbu(ctx) },
                )
            }

            Text(
                "100% opcional · la publicidad sigue igual · no verificamos transferencias.",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (showDeveloperEarnings) {
                MonetizationEarningsCard(ui.monetization)
            }

            Spacer(Modifier.height(32.dp))
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(12.dp))
    }
}

@Composable
private fun CafeHero() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("☕🧉", fontSize = 48.sp)
            Text(
                "Doname un cafecito",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Si Anti-gastos boludos te ayudó a cuidar la guita, " +
                    "podés invitarme un cafecito por Mercado Pago. Un toque y listo.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CafeTierCard(
    tier: DonationTier,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        label = "tierBg",
    )
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 1.dp),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(tier.emoji, fontSize = 36.sp)
            Column(Modifier.weight(1f)) {
                Text(tier.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(tier.blurb, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                MoneyFormat.formatPesos(tier.amountPesos),
                fontWeight = FontWeight.Bold,
                color = MercadoPagoDark,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
private fun OtherMethodsCard(
    onCopyAlias: () -> Unit,
    onCopyCbu: () -> Unit,
) {
    val ctx = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Otros medios", fontWeight = FontWeight.SemiBold)
            if (DonationBankConfig.alias(ctx).isNotBlank()) {
                Text("Alias: ${DonationBankConfig.alias(ctx)}")
            }
            if (DonationBankConfig.holderName(ctx).isNotBlank()) {
                Text("Titular: ${DonationBankConfig.holderName(ctx)}")
            }
            if (DonationBankConfig.cbu(ctx).length == 22) {
                Text("CBU: ${DonationBankConfig.formatCbuDisplay(ctx)}")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (DonationBankConfig.alias(ctx).isNotBlank()) {
                    OutlinedButton(onClick = onCopyAlias, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" Alias")
                    }
                }
                if (DonationBankConfig.cbu(ctx).length == 22) {
                    OutlinedButton(onClick = onCopyCbu, modifier = Modifier.weight(1f)) {
                        Text("Copiar CBU")
                    }
                }
            }
        }
    }
}

@Composable
fun MonetizationEarningsCard(snapshot: com.antigastos.boludos.domain.MonetizationSnapshot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("📊 Estimación de ganancias (este dispositivo)", fontWeight = FontWeight.Bold)
            Text("Banners: ${snapshot.bannerImpressions} impresiones", style = MaterialTheme.typography.bodySmall)
            Text("Nativos: ${snapshot.nativeImpressions} impresiones", style = MaterialTheme.typography.bodySmall)
            Text("Videos completados: ${snapshot.rewardedCompleted}", style = MaterialTheme.typography.bodySmall)
            Text(
                "Ads estimados: ${MoneyFormat.formatPesos(snapshot.estimatedAdsPesos)}",
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Total estimado: ${MoneyFormat.formatPesos(snapshot.estimatedTotalPesos)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(snapshot.activeAdUsersEstimate, style = MaterialTheme.typography.labelSmall)
        }
    }
}
