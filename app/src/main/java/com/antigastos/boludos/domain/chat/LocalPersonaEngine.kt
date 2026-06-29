package com.antigastos.boludos.domain.chat

import com.antigastos.boludos.domain.CopyContext
import com.antigastos.boludos.domain.CopyMood
import com.antigastos.boludos.domain.PersonaCopyPacks
import kotlin.random.Random

/**
 * Motor de personalidad 100% offline: plantillas contextuales + packs por personaje.
 * Sin red, sin LLM. Usado en chat, fallback de IA y frases del Home/Ruleta.
 */
object LocalPersonaEngine {

    fun moodFor(ctx: CopyContext?): CopyMood = when {
        ctx == null || ctx.totalLucas == 0 -> CopyMood.NEUTRAL
        (ctx.pctOfMonthlyGoal ?: 0.0) >= 1.05 -> CopyMood.ALARM
        ctx.totalLucas >= 150 -> CopyMood.BURN
        ctx.daySpentLucas == 0 && ctx.expenseCount > 0 -> CopyMood.CHEER
        ctx.daySpentLucas >= 30 -> CopyMood.BURN
        else -> CopyMood.NEUTRAL
    }

    fun emptyContext() = CopyContext(
        monthLabel = "este mes",
        totalLucas = 0,
        topCategoryName = null,
        topCategorySlug = null,
        topCategoryLucas = 0,
        deltaLucasVsPrevMonth = null,
        pctOfMonthlyGoal = null,
    )

    /** Saludo al abrir un chat vacío. */
    fun greet(personaKey: String, ctx: CopyContext?, rng: Random = Random.Default): String {
        val safe = ctx ?: emptyContext()
        val mood = moodFor(ctx)
        val opener = PersonaOpeners.greetOpener(personaKey, rng)
        val line = PersonaCopyPacks.pick(personaKey, mood, safe, rng = rng)
        return "$opener $line".trim()
    }

    /** Respuesta de chat offline según intención + contexto financiero. */
    fun chatReply(
        personaKey: String,
        ctx: CopyContext?,
        userMessage: String,
        recentPersonaLines: List<String> = emptyList(),
        rng: Random = Random.Default,
    ): String {
        val safe = ctx ?: emptyContext()
        val mood = moodFor(ctx)
        val intent = ChatIntent.detect(userMessage)
        val exclude = recentPersonaLines.takeLast(5).toSet()

        val factual = buildFactualReply(intent, safe, mood, rng)
        val voiced = if (factual.isNotBlank()) {
            PersonaOpeners.voice(personaKey, factual, intent, rng)
        } else {
            ""
        }

        val flavor = PersonaCopyPacks.pick(personaKey, mood, safe, exclude, rng)

        return when {
            voiced.isNotBlank() && shouldAppendFlavor(voiced, flavor, intent) ->
                "$voiced ${flavor.take(120)}".trim()
            voiced.isNotBlank() -> voiced
            else -> flavor
        }
    }

    /** Frase corta para Home / Ruleta sin IA. */
    fun shortPhrase(
        personaKey: String,
        ctx: CopyContext,
        mood: CopyMood,
        rng: Random = Random.Default,
    ): String {
        val contextual = PersonaCopyPacks.contextualLines(ctx, mood).randomOrNull()
        val main = PersonaCopyPacks.pick(personaKey, mood, ctx, rng = rng)
        return if (contextual != null && rng.nextDouble() < 0.5) {
            "$main $contextual".trim()
        } else {
            main
        }
    }

    /** Reescribe un consejo con opener del personaje (offline). */
    fun consejoBody(
        personaKey: String,
        title: String,
        body: String,
        rng: Random = Random.Default,
    ): String {
        val opener = PersonaOpeners.greetOpener(personaKey, rng)
        return "$opener $title: $body".trim()
    }

    private fun shouldAppendFlavor(voiced: String, flavor: String, intent: ChatIntent): Boolean =
        intent == ChatIntent.DEFAULT || intent == ChatIntent.GREETING ||
            (voiced.length < 80 && flavor !in voiced)

    private fun buildFactualReply(
        intent: ChatIntent,
        ctx: CopyContext,
        mood: CopyMood,
        rng: Random,
    ): String = when (intent) {
        ChatIntent.HOW_AM_I -> howAmI(ctx, mood)
        ChatIntent.DELIVERY -> deliveryReply(ctx, mood)
        ChatIntent.META -> metaReply(ctx, mood)
        ChatIntent.ADVICE -> LocalAdviceSnippets.pick(ctx, mood, rng)
        ChatIntent.CATEGORY -> categoryReply(ctx)
        ChatIntent.FIN_MONTH -> finMonthReply(ctx, mood)
        ChatIntent.ANALYZE -> analyzeReply(ctx, mood, rng)
        ChatIntent.GREETING -> greetingReply(ctx, mood)
        ChatIntent.THANKS -> thanksReply(mood, rng)
        ChatIntent.COMPLAINT -> complaintReply(ctx, mood)
        ChatIntent.DEFAULT -> ""
    }

    private fun howAmI(ctx: CopyContext, mood: CopyMood): String {
        val base = "En ${ctx.monthLabel} llevás ${ctx.totalLucas} lucas en ${ctx.expenseCount} gastos."
        val day = if (ctx.daySpentLucas > 0) " Hoy ya van ${ctx.daySpentLucas} lucas." else " Hoy todavía no cargaste nada."
        val meta = ctx.pctOfMonthlyGoal?.let { pct ->
            when {
                pct >= 1.0 -> " Pasaste la meta (${(pct * 100).toInt()}%)."
                pct >= 0.85 -> " Vas al ${(pct * 100).toInt()}% de la meta, ojo."
                else -> " Vas al ${(pct * 100).toInt()}% de la meta."
            }
        }.orEmpty()
        val verdict = when (mood) {
            CopyMood.CHEER -> " Vas bien, seguí."
            CopyMood.BURN -> " Estás gastando fuerte."
            CopyMood.ALARM -> " Esto ya es emergencia."
            CopyMood.NEUTRAL -> " Administrable si no te relajás."
        }
        return base + day + meta + verdict
    }

    private fun deliveryReply(ctx: CopyContext, mood: CopyMood): String {
        val cat = ctx.topCategoryName ?: "comida"
        return when (mood) {
            CopyMood.ALARM -> "El delivery te está comiendo el mes. $cat te clavó ${ctx.topCategoryLucas} lucas. Cociná 3 días seguidos o fundite."
            CopyMood.BURN -> "Otra vez con pedidos? ${ctx.lastExpenseLucas} lucas en el último gasto. Vianda o guiso, elegí."
            else -> "Delivery es comodidad cara. Si bajás 2 pedidos por semana, en $cat recuperás un montón."
        }
    }

    private fun metaReply(ctx: CopyContext, mood: CopyMood): String {
        val pct = ctx.pctOfMonthlyGoal
        if (pct == null || pct <= 0) {
            return "No tenés meta cargada. Poné un tope en Metas y ahí te bardeo con números."
        }
        return when {
            pct >= 1.05 -> "Pasaste la meta (${(pct * 100).toInt()}%). Cortá gustos hasta el ${ctx.daysInMonth}: solo lo necesario."
            pct >= 0.85 -> "Al ${(pct * 100).toInt()}% de la meta con ${ctx.daysInMonth - ctx.dayOfMonth} días. Modo cinturón."
            else -> "Vas al ${(pct * 100).toInt()}% de la meta. Todavía podés, pero no te duermas."
        }
    }

    private fun categoryReply(ctx: CopyContext): String {
        val name = ctx.topCategoryName ?: return "Todavía no hay categoría dominante. Cargá gastos y te digo dónde se te va."
        return "Lo que más te pesa es \"$name\" con ${ctx.topCategoryLucas} lucas. Atacá ahí primero."
    }

    private fun finMonthReply(ctx: CopyContext, mood: CopyMood): String {
        val daysLeft = (ctx.daysInMonth - ctx.dayOfMonth).coerceAtLeast(1)
        val tip = when (mood) {
            CopyMood.ALARM -> "Modo supervivencia: vianda, transporte y nada más."
            CopyMood.BURN -> "Cero delivery. Efectivo en el bolsillo para no pasarte."
            else -> "Lista de super cerrada, sin impulsos. $daysLeft días y salís."
        }
        return "Faltan $daysLeft días de ${ctx.monthLabel}. Llevás ${ctx.totalLucas} lucas. $tip"
    }

    private fun analyzeReply(ctx: CopyContext, mood: CopyMood, rng: Random): String {
        val parts = mutableListOf<String>()
        if (ctx.expenseCount >= 8) {
            parts += "Muchos gastos chicos (${ctx.expenseCount}): patrón de 'no es tanto' que suma."
        }
        ctx.topCategorySlug?.let {
            parts += "Tu vicio visible es ${ctx.topCategoryName ?: it}."
        }
        ctx.deltaLucasVsPrevMonth?.let { delta ->
            when {
                delta > 20 -> parts += "Gastás ${delta} lucas más que el mes pasado."
                delta < -10 -> parts += "Mejoraste ${-delta} lucas vs el mes anterior. Bien."
            }
        }
        if (ctx.daySpentLucas >= 25) {
            parts += "Hoy ya te fuiste a ${ctx.daySpentLucas} lucas: día caro."
        }
        if (parts.isEmpty()) {
            parts += when (mood) {
                CopyMood.CHEER -> "Pocos datos pero vas ordenado. Seguí anotando."
                CopyMood.ALARM -> "Los números gritan. Frená variables ya."
                else -> "Patrón típico: gastos parejos sin plan. Poné meta y topá categorías."
            }
        }
        parts += LocalAdviceSnippets.pick(ctx, mood, rng)
        return parts.joinToString(" ")
    }

    private fun greetingReply(ctx: CopyContext, mood: CopyMood): String = when (mood) {
        CopyMood.CHEER -> "Qué bueno verte. Hoy venís bien con la guita."
        CopyMood.ALARM -> "Llegaste justo: hay que hablar de tus ${ctx.totalLucas} lucas."
        else -> "Acá estoy. Tirame una y te digo cómo vas con ${ctx.totalLucas} lucas."
    }

    private fun thanksReply(mood: CopyMood, rng: Random): String {
        val options = when (mood) {
            CopyMood.CHEER -> listOf(
                "De nada, seguí así.",
                "Para eso estoy. Ahora no te relajes.",
            )
            else -> listOf(
                "De nada. Ahora bajá un gasto y me debés una.",
                "No me beses, mejor ahorrá.",
                "Joya. La próxima bardeo menos si cooperás.",
            )
        }
        return options.random(rng)
    }

    private fun complaintReply(ctx: CopyContext, mood: CopyMood): String = when (mood) {
        CopyMood.ALARM -> "Sí, estás fundido: ${ctx.totalLucas} lucas. Pero todavía se puede remontar con 5 días de cinturón."
        CopyMood.BURN -> "Te entiendo, pero ${ctx.totalLucas} lucas no se fueron solas. Arrancá por el delivery."
        else -> "Respirá. Mirá la categoría top y cortá ahí una semana."
    }
}
