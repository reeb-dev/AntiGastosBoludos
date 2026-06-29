package com.antigastos.boludos.ui.achievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.data.AchievementWithMeta
import com.antigastos.boludos.data.local.entity.ScoreEntity
import com.antigastos.boludos.data.local.entity.ScoreEventEntity
import com.antigastos.boludos.domain.ScoreCatalog

private const val PAGE_SIZE = 60

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AchievementsScreen(app: AntiGastosApplication) {
    var items: List<AchievementWithMeta> by remember { mutableStateOf(emptyList()) }
    LaunchedEffect(Unit) { items = app.achievementsRepository.all() }

    val score by app.scoreRepository.observeScore().collectAsState(initial = null)
    val events by app.scoreRepository.observeRecentEvents(20).collectAsState(initial = emptyList())

    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    var onlyUnlocked by remember { mutableStateOf(false) }
    var visibleCount by remember { mutableStateOf(PAGE_SIZE) }

    val categories = remember(items) { items.map { it.category }.distinct().sorted() }

    val filtered = remember(items, search, category, onlyUnlocked) {
        items.asSequence()
            .filter { it.category == (category ?: it.category) }
            .filter { !onlyUnlocked || it.unlockedAt != null }
            .filter {
                search.isBlank() ||
                    it.title.contains(search, ignoreCase = true) ||
                    it.description.contains(search, ignoreCase = true)
            }
            .toList()
    }

    LaunchedEffect(filtered) { visibleCount = PAGE_SIZE }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScoreHeader(score = score ?: ScoreEntity())
        }

        if (events.isNotEmpty()) {
            item {
                Text(
                    "Premios y fundazos recientes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            items(events, key = { it.id }) { ev -> ScoreEventRow(ev) }
            item { HorizontalDivider() }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Trofeos",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                val unlocked = items.count { it.unlockedAt != null }
                Text(
                    "$unlocked desbloqueados de ${items.size} totales",
                    style = MaterialTheme.typography.bodyMedium,
                )

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Buscar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = !onlyUnlocked,
                        onClick = { onlyUnlocked = false },
                        label = { Text("Todos") },
                    )
                    FilterChip(
                        selected = onlyUnlocked,
                        onClick = { onlyUnlocked = true },
                        label = { Text("Solo desbloqueados") },
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = category == null,
                        onClick = { category = null },
                        label = { Text("Todas") },
                    )
                    categories.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = if (category == c) null else c },
                            label = { Text(c) },
                        )
                    }
                }

                Text(
                    "Mostrando ${minOf(visibleCount, filtered.size)} / ${filtered.size}",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        items(filtered.take(visibleCount), key = { it.key }) { a ->
            AchievementRow(a)
        }

        if (filtered.size > visibleCount) {
            item {
                Button(
                    onClick = { visibleCount += PAGE_SIZE },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Ver más (${filtered.size - visibleCount} restantes)")
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(a: AchievementWithMeta) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .padding(12.dp)
                .alpha(if (a.unlockedAt != null) 1f else 0.45f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(a.emoji, style = MaterialTheme.typography.headlineMedium)
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    a.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(a.description, style = MaterialTheme.typography.bodySmall)
                Text(
                    a.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                if (a.unlockedAt != null) "✅" else "🔒",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun ScoreHeader(score: ScoreEntity) {
    val rank = ScoreCatalog.forPoints(score.points)
    val next = ScoreCatalog.next(rank)
    val progress = ScoreCatalog.progressToNext(score.points)
    val accent = Color(rank.colorArgb)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f)),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(rank.emoji, style = MaterialTheme.typography.displaySmall)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        rank.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                    Text(rank.tagline, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "${score.points} pts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (next != null) {
                Text(
                    "Próximo: ${next.title} (${next.minPoints} pts)",
                    style = MaterialTheme.typography.labelMedium,
                )
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = accent,
                    trackColor = accent.copy(alpha = 0.18f),
                )
            } else {
                Text("Estás en el rango más alto. Inimputable.", style = MaterialTheme.typography.labelMedium)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatChip("Mes", "${score.monthPoints} pts")
                StatChip("Mejor mes", "${score.bestMonthPoints} pts")
                StatChip("🏅", "${score.rewardsCount}")
                StatChip("💸", "${score.failsCount}")
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ScoreEventRow(ev: ScoreEventEntity) {
    val isReward = ev.kind == "REWARD"
    val color = if (isReward) Color(0xFF2E7D32) else Color(0xFFC62828)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(ev.emoji, style = MaterialTheme.typography.titleLarge)
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            ) {
                Text(ev.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(ev.day, style = MaterialTheme.typography.labelSmall)
            }
            Box(contentAlignment = Alignment.CenterEnd) {
                Text(
                    if (ev.delta >= 0) "+${ev.delta}" else "${ev.delta}",
                    color = color,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
