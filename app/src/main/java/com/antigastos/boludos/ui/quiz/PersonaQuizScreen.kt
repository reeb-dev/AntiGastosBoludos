package com.antigastos.boludos.ui.quiz

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.AppFeedback
import com.antigastos.boludos.data.local.entity.SettingsEntity
import com.antigastos.boludos.domain.Persona
import com.antigastos.boludos.domain.PersonaCatalog
import kotlinx.coroutines.launch

private data class Q(val text: String, val options: List<Pair<String, String>>)

/**
 * Quiz estilo BuzzFeed: pocas preguntas, resultado con personaje y texto listo para compartir.
 */
@Composable
fun PersonaQuizScreen(
    isFirstRun: Boolean = false,
    onFirstRunComplete: (personaKey: String) -> Unit = {},
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as AntiGastosApplication
    val scope = rememberCoroutineScope()
    val settings by app.settingsRepository.flow.collectAsState(initial = SettingsEntity())

    var step by remember { mutableIntStateOf(0) }
    var scores by remember { mutableStateOf(mapOf<String, Int>()) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(finished) {
        if (finished) AppFeedback.achievementUnlocked(ctx, settings.soundEnabled)
    }

    val questions = remember {
        val pool = listOf(
            Q(
                "Es viernes a la noche. ¿Qué hace la billetera?",
                listOf(
                    "Salgo y que sea lo que Dios quiera" to "cumbiero",
                    "Me quedo en casa con mate y serie" to "termo",
                    "Salgo pero con tope en efectivo" to "profe",
                    "Compro crypto y después veo" to "cunadocripto",
                ),
            ),
            Q(
                "Te llega una promo 'últimas unidades'. Vos…",
                listOf(
                    "Caigo siempre, soy humano" to "streamer",
                    "Cierro la app y respiro" to "psicologo",
                    "Le pregunto al carnicero si conviene" to "carnicero",
                    "Prometo no comprar (y compro)" to "politico",
                ),
            ),
            Q(
                "¿Cómo afrontás el fin de mes?",
                listOf(
                    "Con fe y un plan improvisado" to "predicador",
                    "Con abrazo y guiso de garbanzos" to "abuela",
                    "Con Excel en la cabeza" to "jefe",
                    "Con birra y humor" to "tachero",
                ),
            ),
            Q(
                "La meta de ahorro la ves…",
                listOf(
                    "Como un desafío personal" to "profegym",
                    "Como una sugerencia opcional" to "fisura",
                    "Como una ley divina" to "predicador",
                    "Como un meme que me persigue" to "trapero",
                ),
            ),
            Q(
                "Si te sobran 10 lucas al final del día…",
                listOf(
                    "Las guardo en el colchón digital" to "profe",
                    "Me compro algo simbólico" to "travesti",
                    "Las convierto en dólares en mi cabeza" to "cunadocripto",
                    "Anuncio que voy a ahorrar (y no ahorro)" to "politico",
                ),
            ),
            Q(
                "El delivery te llama de noche. Vos…",
                listOf(
                    "Pido sin pensarlo, vivo una vez" to "motoquero",
                    "Reviso la heladera y ceno con lo que hay" to "abuela",
                    "Me pongo dramático y rezo por mi bolsillo" to "predicador",
                    "Me hago un té y miro un partido" to "tachero",
                ),
            ),
            Q(
                "Vas al kiosco y…",
                listOf(
                    "El kiosquero ya sabe qué te llevás" to "kiosquero",
                    "Te frenás y salís sin nada" to "psicologo",
                    "Te llevás cinco cosas que no pensabas comprar" to "fisura",
                    "Negociás un descuento por cliente fiel" to "carnicero",
                ),
            ),
            Q(
                "Si te ofrecen una cuota en 12 sin interés…",
                listOf(
                    "Firmo todo, hablamos después" to "vendedorseguros",
                    "Agarro la oportunidad como negocio" to "cunadocripto",
                    "Lo pienso una semana mirando el cielo" to "vidente",
                    "Le pregunto a mi mecánico si conviene" to "mecanico",
                ),
            ),
            Q(
                "El despertador suena un lunes. Vos…",
                listOf(
                    "Ya estoy entrenando hace una hora" to "profegym",
                    "Faltó la luz en mi alma, hoy no rindo" to "streamer",
                    "Me concentro como en un final de copa" to "comentarista",
                    "Llego al laburo cargado de café y odio" to "jefe",
                ),
            ),
            Q(
                "Tu plan de fin de semana ideal es…",
                listOf(
                    "Asado con los pibes" to "tachero",
                    "Un pilates y brunch en Recoleta" to "cheta",
                    "Cumbia 420 hasta las 6" to "cumbiero",
                    "Quedarme escuchando rap y proyectando" to "trapero",
                ),
            ),
            Q(
                "Cuando algo te sale mal con la guita, vos…",
                listOf(
                    "Lo asumo bajito y aprendo, papá" to "diez",
                    "Lloro abrazando a mi viejo, drama total" to "abuela",
                    "Salgo a la cancha a recuperarla" to "comentarista",
                    "Le mando audio de WhatsApp largo a un amigo" to "travesti",
                ),
            ),
            Q(
                "Si te tocara hablar en público sobre tus gastos…",
                listOf(
                    "Tres palabras humildes y me bajo" to "diez",
                    "Sermón largo con micrófono" to "predicador",
                    "Charla TED con datos y memes" to "psicologo",
                    "Discurso con promesas vacías" to "politico",
                ),
            ),
            Q(
                "Cuando vas al supermercado…",
                listOf(
                    "Sigo la lista religiosamente" to "predicador",
                    "Me llevo lo más barato del estante" to "termo",
                    "Compro fruta y verdura porque sí" to "abuela",
                    "Me distraigo en la góndola de promos" to "fisura",
                ),
            ),
            Q(
                "Pensás en tu yo del futuro y…",
                listOf(
                    "Le tiro las cartas, lo acompaño" to "vidente",
                    "Le grito que se ponga las pilas" to "profegym",
                    "Le mando un audio de WhatsApp largo" to "travesti",
                    "Le abro un plazo fijo" to "profe",
                ),
            ),
            Q(
                "Si te roban el bondi a las 11 PM…",
                listOf(
                    "Te juzgo desde el sillón con cara de superado" to "gato",
                    "Te culpo desde el cariño y la tragedia" to "madre",
                    "Te aviso por altavoz: 'próxima estación, La Quiebra'" to "subteman",
                    "Te grito por chat como en stream ranked" to "streamer",
                ),
            ),
            Q(
                "Tu forma de vivir la guita es más…",
                listOf(
                    "Pasión de arrabal: gambeta y corazón" to "geniopotrero",
                    "Tranqui: paso a paso sin show" to "diez",
                    "Planilla y control como el jefe" to "jefe",
                    "Caigo, me levanto y salgo de joda" to "fisura",
                ),
            ),
            Q(
                "Tu relación con las suscripciones es…",
                listOf(
                    "Las uso todas, soy fan" to "trapero",
                    "Me olvidé de cuántas tengo" to "fisura",
                    "Las anoto y las reviso cada mes" to "jefe",
                    "Las cancelo apenas terminan el trial" to "profe",
                ),
            ),
        )
        pool
            .shuffled()
            .take(5)
            .map { q -> q.copy(options = q.options.shuffled()) }
    }

    fun addScore(key: String) {
        scores = scores + (key to (scores[key] ?: 0) + 1)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
      Column(
        Modifier
            .fillMaxSize()
            .widthIn(max = 520.dp)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
            "¿Qué personaje sos con la guita?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        if (isFirstRun) {
            Text(
                "Primero descubrí qué personaje te representa. Después lo afinás en Ajustes.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            "5 preguntas rápidas. Resultado para compartir en redes.",
            style = MaterialTheme.typography.bodyMedium,
        )

        if (!finished) {
            val q = questions[step]
            Text(
                "Pregunta ${step + 1}/${questions.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(q.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            q.options.forEach { (label, personaKey) ->
                OutlinedButton(
                    onClick = {
                        AppFeedback.quizOptionTap(ctx)
                        addScore(personaKey)
                        if (step + 1 >= questions.size) {
                            finished = true
                        } else {
                            step += 1
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(label) }
            }
        } else {
            val winnerKey = remember(scores) {
                if (scores.isEmpty()) {
                    PersonaCatalog.all.random().key
                } else {
                    val top = scores.values.max()
                    val tied = scores.entries.filter { it.value == top }.map { it.key }
                    tied.random()
                }
            }
            val persona: Persona = PersonaCatalog.byKey(winnerKey)
            val pct = ((scores[winnerKey] ?: 0) * 100 / questions.size).coerceIn(40, 100)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                ),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${persona.emoji} Sos ${pct}% ${persona.displayName}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(persona.tagline, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    val tweet = "Hice el quiz de Anti-gastos boludos y salí ${pct}% ${persona.displayName} ${persona.emoji}. ¿Vos qué personaje sos? 🇦🇷 #AntiGastosBoludos"
                    Button(
                        onClick = {
                            AppFeedback.shareAction(ctx)
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, tweet)
                            }
                            ctx.startActivity(Intent.createChooser(send, "Compartir resultado"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Text("  Compartir resultado")
                        }
                    }
                    if (isFirstRun) {
                        Button(
                            onClick = { onFirstRunComplete(winnerKey) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Entrar a la app") }
                    } else {
                        Button(
                            onClick = {
                                scope.launch {
                                    app.settingsRepository.update {
                                        it.copy(personaKey = winnerKey)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Usar este personaje") }
                        OutlinedButton(
                            onClick = {
                                step = 0
                                scores = emptyMap()
                                finished = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Repetir quiz") }
                    }
                }
            }
        }
      }
    }
}
