package com.antigastos.boludos.domain

import com.antigastos.boludos.domain.model.HomeUiModel
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

/**
 * Generador determinístico de >1000 trofeos "random" para que el usuario
 * tenga siempre algo que cazar.
 *
 * Diseño:
 *  - Las "claves" se generan con prefijo + parámetros (ej: `streak_45`, `cat_comida_lvl_3`).
 *    Eso garantiza que entre versiones la misma clave referencie el mismo logro.
 *  - Cada trofeo tiene un `predicate` que se evalúa contra `EvalContext`.
 *  - El `EvalContext` es solo el `HomeUiModel` enriquecido con un par de campos extra
 *    (cantidad por categoría, hora de carga, etc.) calculados al evaluar.
 *  - Para el "Trofeo del día", `DailyTrophyEngine` toma esta lista y elige uno
 *    deterministicamente por fecha.
 */
object RandomTrophyCatalog {

    /**
     * Estado adicional que necesitan los predicados. El `ExpenseRepository`
     * lo arma cuando llama a [evaluate].
     */
    data class EvalContext(
        val home: HomeUiModel,
        val today: LocalDate,
        /** Conteo de gastos del mes por slug de categoría. */
        val countByCategorySlug: Map<String, Long>,
        /** Suma total del mes por slug de categoría (en pesos). */
        val sumByCategorySlug: Map<String, Long>,
        /** Hora del último gasto cargado (0..23). */
        val lastExpenseHour: Int?,
        /** Total acumulado histórico de gastos cargados (todos los meses). */
        val lifetimeExpenseCount: Long,
        /** Total histórico en pesos. */
        val lifetimeSpendPesos: Long,
        /** Si hoy es feriado (para logros temáticos). */
        val holiday: HolidayCalendar.Holiday?,
    )

    private data class GeneratedTrophy(
        val key: String,
        val title: String,
        val description: String,
        val emoji: String,
        val category: String,
        val predicate: (EvalContext) -> Boolean,
    )

    /** Cargado una vez en memoria. La lista pasa los 1000 con margen. */
    private val generated: List<GeneratedTrophy> by lazy { build() }

    fun all(): List<AchievementMeta> = generated.map {
        AchievementMeta(it.key, it.title, it.description, it.emoji)
    }

    /** Devuelve los keys que se desbloquean evaluando `ctx`. */
    fun evaluate(ctx: EvalContext): List<String> {
        val unlocked = mutableListOf<String>()
        generated.forEach { tr ->
            if (tr.predicate(ctx)) unlocked += tr.key
        }
        return unlocked
    }

    /** Trofeo del día: lo mismo, pero solo lo evaluamos contra ese único trofeo. */
    fun trophyOfDay(today: LocalDate): AchievementMeta {
        if (generated.isEmpty()) return AchievementMeta("daily_fallback", "Trofeo del día", "Cargá un gasto", "🎯")
        // Seed estable por día para que todo el mundo vea el mismo "trofeo del día".
        val seed = today.year.toLong() * 1000L + today.dayOfYear
        val rnd = Random(seed)
        val pick = generated[rnd.nextInt(generated.size)]
        return AchievementMeta(
            key = "daily_${today}_${pick.key}",
            title = "Trofeo del día: ${pick.title}",
            description = pick.description,
            emoji = pick.emoji,
        )
    }

    /** Predicado del trofeo del día (mismo del día). */
    fun trophyOfDayPredicate(today: LocalDate): ((EvalContext) -> Boolean)? {
        if (generated.isEmpty()) return null
        val seed = today.year.toLong() * 1000L + today.dayOfYear
        val rnd = Random(seed)
        return generated[rnd.nextInt(generated.size)].predicate
    }

    /** Categorías para filtros en la pantalla de logros. */
    fun categories(): List<String> = generated.map { it.category }.distinct().sorted()

    fun byCategory(category: String): List<AchievementMeta> =
        generated.filter { it.category == category }.map { AchievementMeta(it.key, it.title, it.description, it.emoji) }

    // -----------------------------------------------------------------
    //  Generación combinatoria
    // -----------------------------------------------------------------

    private fun build(): List<GeneratedTrophy> {
        val out = mutableListOf<GeneratedTrophy>()

        // 1. Rachas extendidas (días sin gastar): 200 niveles, cada N días.
        for (n in 1..200) {
            val emoji = pickFromList(rachaEmojis, n)
            out += GeneratedTrophy(
                key = "streak_$n",
                title = "Racha de $n día${if (n == 1) "" else "s"}",
                description = "Aguantaste $n día${if (n == 1) "" else "s"} sin gastar un mango. ${rachaTagline(n)}",
                emoji = emoji,
                category = "Rachas",
                predicate = { ctx -> ctx.home.streakDaysNoSpend >= n },
            )
        }

        // 2. Cantidad de gastos cargados (lifetime). 60 niveles geom.
        val expenseMilestones = listOf(
            1, 5, 10, 25, 50, 75, 100, 150, 200, 250, 300, 400, 500, 600, 750, 1_000,
            1_250, 1_500, 1_750, 2_000, 2_500, 3_000, 3_500, 4_000, 5_000, 6_000, 7_500,
            10_000, 12_500, 15_000, 20_000, 25_000, 30_000, 40_000, 50_000, 75_000, 100_000,
        )
        expenseMilestones.forEachIndexed { i, n ->
            out += GeneratedTrophy(
                key = "exp_count_$n",
                title = "$n gastos cargados",
                description = countTagline(n),
                emoji = pickFromList(countEmojis, i),
                category = "Cargas",
                predicate = { ctx -> ctx.lifetimeExpenseCount >= n },
            )
        }

        // 3. Total acumulado en lucas (1 luca = 1000 pesos).
        val lucaMilestones = listOf<Long>(
            1, 5, 10, 25, 50, 100, 250, 500, 750, 1_000, 1_500, 2_000, 2_500, 3_000,
            5_000, 7_500, 10_000, 15_000, 20_000, 25_000, 50_000, 75_000, 100_000,
            150_000, 200_000, 300_000, 500_000, 750_000, 1_000_000,
        )
        lucaMilestones.forEachIndexed { i, lucas ->
            val pesos = lucas * 1_000L
            out += GeneratedTrophy(
                key = "lucas_acum_$lucas",
                title = "$lucas lucas acumuladas",
                description = "Cargaste un total de $lucas lucas. ${lucasTagline(lucas)}",
                emoji = pickFromList(lucaEmojis, i),
                category = "Acumulado",
                predicate = { ctx -> ctx.lifetimeSpendPesos >= pesos },
            )
        }

        // 4. Por categoría: comida, transporte, vivienda, servicios, ocio, salud, ropa, varios.
        val cats = listOf(
            "comida" to "🍔", "transporte" to "🚌", "vivienda" to "🏠", "servicios" to "💡",
            "ocio" to "🎮", "salud" to "💊", "ropa" to "👕", "delivery" to "🛵",
            "supermercado" to "🛒", "cafe" to "☕", "bar" to "🍻", "viaje" to "✈️",
            "tecnologia" to "💻", "educacion" to "📚", "regalos" to "🎁", "mascota" to "🐶",
            "hogar" to "🛋️", "bebidas" to "🥤", "auto" to "🚗", "varios" to "🧰",
        )
        val catLevels = listOf(1, 5, 10, 25, 50, 100, 200, 500, 1_000)
        cats.forEach { (slug, emoji) ->
            // Conteos
            catLevels.forEach { lvl ->
                out += GeneratedTrophy(
                    key = "cat_${slug}_count_$lvl",
                    title = "Pro de $slug",
                    description = "Cargaste $lvl gastos en $slug.",
                    emoji = emoji,
                    category = "Categorías",
                    predicate = { ctx -> (ctx.countByCategorySlug[slug] ?: 0L) >= lvl },
                )
            }
            // Sumas en lucas
            listOf(5, 25, 100, 500, 2_000, 10_000).forEach { lucas ->
                val pesos = lucas * 1_000L
                out += GeneratedTrophy(
                    key = "cat_${slug}_lucas_$lucas",
                    title = "$lucas lucas en $slug",
                    description = "Te dejaste $lucas lucas en $slug. Eso es estilo de vida.",
                    emoji = emoji,
                    category = "Categorías",
                    predicate = { ctx -> (ctx.sumByCategorySlug[slug] ?: 0L) >= pesos },
                )
            }
        }

        // 5. Hora del día (madrugador, vampiro, mediodía, oficinista...).
        for (hour in 0..23) {
            val (label, emoji) = hourLabel(hour)
            out += GeneratedTrophy(
                key = "hour_$hour",
                title = "Gasto de las $hour",
                description = "Cargaste un gasto a las $hour h. $label",
                emoji = emoji,
                category = "Horarios",
                predicate = { ctx -> ctx.lastExpenseHour == hour },
            )
        }

        // 6. Día de semana.
        DayOfWeek.values().forEach { dow ->
            out += GeneratedTrophy(
                key = "dow_${dow.name}",
                title = "Gasto en ${dowEs(dow)}",
                description = "Cargaste un gasto un ${dowEs(dow)}.",
                emoji = dowEmoji(dow),
                category = "Días",
                predicate = { ctx -> ctx.today.dayOfWeek == dow && ctx.lastExpenseHour != null },
            )
        }

        // 7. Por mes calendario: cargar al menos 1 gasto en cada mes del año.
        for (month in 1..12) {
            out += GeneratedTrophy(
                key = "month_$month",
                title = "Gasto en ${monthEs(month)}",
                description = "Cargaste al menos un gasto en ${monthEs(month)}.",
                emoji = monthEmoji(month),
                category = "Meses",
                predicate = { ctx -> ctx.today.monthValue == month && ctx.lifetimeExpenseCount > 0 },
            )
        }

        // 8. Feriados: cargar/no cargar gasto en cada feriado del año.
        val sampleYear = LocalDate.now().year
        HolidayCalendar.allForYear(sampleYear).forEach { h ->
            val baseKey = "holiday_${h.name.normalize()}"
            out += GeneratedTrophy(
                key = "${baseKey}_spend",
                title = "Gastaste en ${h.name}",
                description = "Cargaste un gasto un ${h.name}. ${h.tagline}",
                emoji = h.emoji,
                category = "Feriados",
                predicate = { ctx ->
                    ctx.holiday?.name == h.name && ctx.lastExpenseHour != null
                },
            )
            out += GeneratedTrophy(
                key = "${baseKey}_clean",
                title = "${h.name} sin patinar",
                description = "Aguantaste ${h.name} sin gastar un mango. Disciplina.",
                emoji = "🧘",
                category = "Feriados",
                predicate = { ctx ->
                    ctx.holiday?.name == h.name && ctx.home.daySpentPesos == 0L
                },
            )
        }

        // 9. Mood / shame / persona usada (cobertura).
        ShameLevel.values().forEach { lvl ->
            out += GeneratedTrophy(
                key = "shame_${lvl.name}",
                title = "Estado: ${shameLabel(lvl)}",
                description = "Llegaste al estado ${shameLabel(lvl)} en el medidor de la app.",
                emoji = shameEmoji(lvl),
                category = "Estados",
                predicate = { ctx -> ctx.home.shameLevel == lvl },
            )
        }
        listOf("termo", "tachero", "abuela", "jefe", "comentarista", "fisura", "streamer").forEach { p ->
            out += GeneratedTrophy(
                key = "persona_$p",
                title = "Personaje: $p",
                description = "Usaste el personaje $p al menos una vez.",
                emoji = personaEmoji(p),
                category = "Personajes",
                predicate = { ctx -> ctx.home.personaKey == p },
            )
        }

        // 10. Combos absurdos / Easter eggs (>200 generados aleatoriamente con seed fija).
        val rnd = Random(42)
        val moods = listOf("triste", "alegre", "embolado", "loco", "bardero", "responsable", "peligro",
            "garca", "luz", "cosmico", "ñoqui", "asadero", "milico", "kiosquero", "barril",
            "rocker", "dj", "punk", "tarot", "esotérico")
        val verbs = listOf("comprar", "patinar", "tirar", "fumar", "dejar", "volar", "escupir",
            "regalar", "perder", "evaporar", "donar al delivery", "rifar", "heredar al kiosco",
            "convidarle al destino", "consagrar al peso")
        val objs = listOf(
            "delivery", "café de especialidad", "alfajor triple", "fernet", "uber al kiosco",
            "vuelta manzana", "sticker random", "skin de juego", "panchito a la madrugada",
            "carga de SUBE", "ropa que no usás", "merch de banda", "spotify familiar trucho",
            "regalo random", "criptomoneda fiufiu", "feriado patrio", "asado del finde",
            "merca del super", "vidrio del auto", "souvenir tucumano",
        )
        for (i in 0 until 480) {
            val mood = moods[rnd.nextInt(moods.size)]
            val verb = verbs[rnd.nextInt(verbs.size)]
            val obj = objs[rnd.nextInt(objs.size)]
            val threshold = rnd.nextInt(2, 30) * 1_000L
            val title = "$mood: $verb en $obj".replaceFirstChar { it.uppercase() }
            out += GeneratedTrophy(
                key = "combo_$i",
                title = title,
                description = "Logro absurdo #$i. Cargaste un gasto chico en una situación rara: $verb $obj cuando estabas $mood.",
                emoji = listOf("🤡", "🎲", "🎯", "🌀", "🔮", "🦦", "🦄", "🌶️", "🌊", "🛸").let { it[rnd.nextInt(it.size)] },
                category = "Random",
                predicate = { ctx ->
                    // Predicados arbitrarios pero estables: mod del día y monto chico
                    ctx.home.daySpentPesos in 1L..threshold &&
                        ctx.today.dayOfYear % 7 == i % 7
                },
            )
        }

        // 11. Logros de "no patinar" en finde / día específico.
        DayOfWeek.values().forEach { dow ->
            out += GeneratedTrophy(
                key = "clean_${dow.name}",
                title = "${dowEs(dow)} sin gastar",
                description = "Aguantaste un ${dowEs(dow)} entero sin cargar un mango.",
                emoji = "🌱",
                category = "Limpios",
                predicate = { ctx ->
                    ctx.today.dayOfWeek == dow && ctx.home.daySpentPesos == 0L
                },
            )
        }

        // 12. Metas de meses verdes (cumplir meta global por X meses seguidos).
        listOf(1, 2, 3, 6, 12).forEach { months ->
            out += GeneratedTrophy(
                key = "green_months_$months",
                title = "$months meses en verde",
                description = "Cerraste $months meses sin romper la meta global.",
                emoji = "✅",
                category = "Metas",
                predicate = { ctx ->
                    val pct = ctx.home.globalProgress?.toDouble() ?: 1.1
                    val ok = pct < 1.0 && ctx.home.globalGoalPesos != null
                    // Aproximación: solo se desbloquea si tenés >= months meses cargados,
                    // no podemos saberlo exactamente con HomeUiModel; lo dejamos conservador.
                    ok && months == 1
                },
            )
        }

        return out
    }

    // -----------------------------------------------------------------
    // Helpers de presentación
    // -----------------------------------------------------------------

    private fun rachaTagline(n: Int): String = when {
        n < 3 -> "Recién arrancando."
        n < 7 -> "Bien ahí."
        n < 14 -> "Modo rata activado."
        n < 30 -> "Inimputable."
        n < 60 -> "Casi monje."
        n < 100 -> "Esto ya es disciplina militar."
        else -> "Sos otra especie."
    }

    private fun countTagline(n: Int): String = when {
        n < 50 -> "Estás tomando ritmo, $n cargas."
        n < 500 -> "Vas como locomotora, $n cargas."
        n < 5_000 -> "Esto es vicio sano, $n cargas."
        else -> "Te conoce el INDEC, $n cargas."
    }

    private fun lucasTagline(n: Long): String = when {
        n < 50L -> "Esto es plata de kiosco."
        n < 500L -> "Empieza a doler."
        n < 5_000L -> "Plata de adulto."
        else -> "Plata de empresa familiar."
    }

    private val rachaEmojis = listOf("🌱", "🔥", "🐀", "🧘", "🥷", "🦾", "🏆", "👑", "🌟", "🛡️")
    private val countEmojis = listOf("📒", "📚", "📊", "📈", "🧮", "📋", "📦", "🗄️", "💾", "🧾")
    private val lucaEmojis = listOf("💵", "💴", "💶", "💷", "💸", "🏦", "💳", "🪙", "🤑", "💰")

    private fun pickFromList(list: List<String>, index: Int): String =
        list[((index % list.size) + list.size) % list.size]

    private fun hourLabel(hour: Int): Pair<String, String> = when (hour) {
        in 0..4 -> "Modo vampiro." to "🧛"
        in 5..7 -> "Madrugador." to "🌅"
        in 8..11 -> "Hora de oficina." to "💼"
        12 -> "Hora del almuerzo." to "🍽️"
        in 13..17 -> "Tarde de gastos." to "☕"
        in 18..21 -> "After office." to "🌆"
        else -> "Trasnoche." to "🌙"
    }

    private fun dowEs(dow: DayOfWeek) = when (dow) {
        DayOfWeek.MONDAY -> "lunes"
        DayOfWeek.TUESDAY -> "martes"
        DayOfWeek.WEDNESDAY -> "miércoles"
        DayOfWeek.THURSDAY -> "jueves"
        DayOfWeek.FRIDAY -> "viernes"
        DayOfWeek.SATURDAY -> "sábado"
        DayOfWeek.SUNDAY -> "domingo"
    }

    private fun dowEmoji(dow: DayOfWeek) = when (dow) {
        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> "🥳"
        else -> "📆"
    }

    private fun monthEs(m: Int) = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )[m - 1]

    private fun monthEmoji(m: Int) = listOf(
        "🎆", "💞", "🌷", "🐣", "🛠️", "🥶",
        "🥶", "🎈", "🌸", "🍂", "🦃", "🎄",
    )[m - 1]

    private fun shameLabel(lvl: ShameLevel) = lvl.label

    private fun shameEmoji(lvl: ShameLevel) = lvl.emoji

    private fun personaEmoji(p: String) = when (p) {
        "termo" -> "🧉"
        "tachero" -> "🚖"
        "abuela" -> "👵"
        "jefe" -> "🧑‍💼"
        "comentarista" -> "🎤"
        "fisura" -> "🤪"
        "streamer" -> "🎮"
        else -> "🎭"
    }

    private fun String.normalize(): String =
        this.lowercase()
            .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
            .replace("ñ", "n").replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
}
