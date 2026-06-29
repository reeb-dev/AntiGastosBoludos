package com.antigastos.boludos.domain

import com.antigastos.boludos.domain.model.HomeUiModel
import kotlin.random.Random

/**
 * Catálogo grande de consejos financieros, con tres ejes:
 *
 *  - **Frenazo / hábito**: cuando el usuario está gastando demás, ofrecemos
 *    técnicas concretas (50/30/20, sobre, 24hs rule, batch cooking, etc.).
 *  - **Crecimiento (AR)**: cuando hay margen, sugerimos qué hacer para que la
 *    plata no quede quieta — UVA, FCI, dólar MEP, stablecoins, Cedears, cuentas
 *    remuneradas (Mercado Pago, Naranja X, Ualá), Cocos, IOL, etc.
 *  - **Mindset**: educación financiera general (deudas, fondo de emergencia,
 *    multiplicar ingresos, monetizar skills, freelance).
 *
 * Cada [Advice] declara qué condición del [HomeUiModel] lo hace relevante. El
 * engine arma una lista de candidatos y elige al azar — así nunca repetís el
 * mismo dos días seguidos. Todos los textos son cortos, en español rioplatense,
 * sin jerga financiera críptica.
 */
internal data class Advice(
    val id: String,
    val kind: ConsejoKind,
    val title: String,
    val bodyResolver: (HomeUiModel, Map<String, String>) -> String,
    val appliesTo: (HomeUiModel) -> Boolean = { true },
    /** Probabilidad relativa de salir si matchea. */
    val weight: Int = 1,
    val durationMs: Long = 5500,
)

internal object FinancialAdviceCatalog {

    /**
     * Devuelve el consejo más relevante para el estado, eligiendo al azar entre
     * los candidatos que aplican. Devuelve null si nada matchea (ej: estado sin
     * datos suficientes).
     */
    fun pick(state: HomeUiModel, rng: Random): Consejo? {
        val vars = computeVars(state)
        val candidates = ALL.filter { it.appliesTo(state) }
        if (candidates.isEmpty()) return null
        val total = candidates.sumOf { it.weight }
        var roll = rng.nextInt(total)
        for (advice in candidates) {
            roll -= advice.weight
            if (roll < 0) {
                return Consejo(
                    id = advice.id,
                    kind = advice.kind,
                    title = advice.title,
                    body = advice.bodyResolver(state, vars),
                    durationMs = advice.durationMs,
                )
            }
        }
        return null
    }

    /** Variables auxiliares que los `bodyResolver` pueden usar sin recalcular. */
    private fun computeVars(state: HomeUiModel): Map<String, String> {
        val daysInMonth = state.yearMonth.lengthOfMonth()
        val today = java.time.LocalDate.now()
        val todayDay = if (
            today.year == state.yearMonth.year &&
            today.monthValue == state.yearMonth.monthValue
        ) today.dayOfMonth else daysInMonth
        val daysLeft = (daysInMonth - todayDay + 1).coerceAtLeast(1)

        val totalLucas = MoneyFormat.pesosToLucas(state.totalPesos)
        val daySpentLucas = MoneyFormat.pesosToLucas(state.daySpentPesos)
        val goalPesos = state.globalGoalPesos ?: 0L
        val remainingPesos = (goalPesos - state.totalPesos).coerceAtLeast(0L)
        val dailyAllowanceLucas = if (goalPesos > 0L) {
            MoneyFormat.pesosToLucas(remainingPesos / daysLeft)
        } else 0L
        val topName = state.topCategories.firstOrNull()?.name ?: "—"
        val topSlug = state.topCategories.firstOrNull()?.slug ?: ""

        return mapOf(
            "totalLucas" to totalLucas.toString(),
            "daySpentLucas" to daySpentLucas.toString(),
            "daysLeft" to daysLeft.toString(),
            "dailyAllowanceLucas" to dailyAllowanceLucas.toString(),
            "remainingLucas" to MoneyFormat.pesosToLucas(remainingPesos).toString(),
            "topName" to topName,
            "topSlug" to topSlug,
            "streak" to state.streakDaysNoSpend.toString(),
            "expenseCount" to state.expenseCount.toString(),
        )
    }

    // -------------------- HÁBITOS / FRENO --------------------

    private val habitAdvice = listOf(
        Advice(
            id = "habit_50_30_20",
            kind = ConsejoKind.SUGERENCIA,
            title = "📐 Regla 50 / 30 / 20",
            bodyResolver = { _, _ ->
                "Probá repartir el sueldo: 50% gastos fijos, 30% gustos, 20% ahorro. Es vieja pero funciona."
            },
        ),
        Advice(
            id = "habit_envelope",
            kind = ConsejoKind.SUGERENCIA,
            title = "✉️ Plata en sobres",
            bodyResolver = { _, _ ->
                "Separá el sueldo en sobres (físicos o cuentas distintas): comida, salidas, ahorro. Lo que no está en el sobre, no se gasta."
            },
        ),
        Advice(
            id = "habit_pay_yourself_first",
            kind = ConsejoKind.SUGERENCIA,
            title = "🏦 Pagate vos primero",
            bodyResolver = { _, _ ->
                "Apenas cobrás, mandá el 10–20% al ahorro. Vivís con el resto. Si esperás a fin de mes, no sobra nunca."
            },
        ),
        Advice(
            id = "habit_24h_rule",
            kind = ConsejoKind.SUGERENCIA,
            title = "⏳ Regla de las 24hs",
            bodyResolver = { _, _ ->
                "¿Compra impulsiva de más de 30 lucas? Esperá 24hs. El 80% de las veces ya no la querés tanto."
            },
            appliesTo = { it.daySpentPesos > 30_000L },
            weight = 2,
        ),
        Advice(
            id = "habit_track_subs",
            kind = ConsejoKind.SUGERENCIA,
            title = "📺 Limpiá las suscripciones",
            bodyResolver = { _, _ ->
                "Anotá TODAS tus suscripciones (Netflix, Spotify, gym, apps). Bajá las que no usás hace 30 días. Suelen sumar 15–25 lucas / mes."
            },
        ),
        Advice(
            id = "habit_batch_cooking",
            kind = ConsejoKind.SUGERENCIA,
            title = "🍲 Cociná en tandas",
            bodyResolver = { _, _ ->
                "Domingo a la noche cociná para 4 días: guiso, pollo, arroz. Te ahorrás 4 deliveries y comés mejor."
            },
            appliesTo = { it.topCategories.firstOrNull()?.slug?.contains("delivery", ignoreCase = true) == true ||
                it.topCategories.firstOrNull()?.slug?.contains("comida", ignoreCase = true) == true },
            weight = 3,
        ),
        Advice(
            id = "habit_cash_only_week",
            kind = ConsejoKind.SUGERENCIA,
            title = "💵 Semana solo efectivo",
            bodyResolver = { _, _ ->
                "Probá una semana sin tarjeta. Sacás un monto fijo y bancás con eso. Sentís físicamente cada peso."
            },
        ),
        Advice(
            id = "habit_no_spend_weekend",
            kind = ConsejoKind.SUGERENCIA,
            title = "🛑 Finde sin gastar",
            bodyResolver = { _, _ ->
                "Un fin de semana al mes sin gastar nada: peli en casa, mate, caminar. Suma una luca larga al ahorro."
            },
        ),
        Advice(
            id = "habit_unsub_emails",
            kind = ConsejoKind.SUGERENCIA,
            title = "📧 Bajá los mails de promo",
            bodyResolver = { _, _ ->
                "Desuscribite de los newsletters de tiendas. Si no ves la oferta, no la deseás. Cerebro feliz, billetera más feliz."
            },
        ),
        Advice(
            id = "habit_freezer_friend",
            kind = ConsejoKind.SUGERENCIA,
            title = "🧊 El freezer es tu amigo",
            bodyResolver = { _, _ ->
                "Compra grande en carnicería, divididas y al freezer. Te baja el costo del kilo entre 20% y 35%."
            },
        ),
        Advice(
            id = "habit_water_first",
            kind = ConsejoKind.SUGERENCIA,
            title = "💧 Vaso de agua antes",
            bodyResolver = { _, _ ->
                "Antes de pedir delivery, tomá un vaso de agua y esperá 10 min. Mucha hambre era ansiedad. Ahorrás 8–15 lucas por noche."
            },
            appliesTo = { it.topCategories.firstOrNull()?.slug?.contains("delivery", ignoreCase = true) == true },
            weight = 2,
        ),
    )

    // -------------------- CRECIMIENTO ECONÓMICO (AR) --------------------

    private val growthAdvice = listOf(
        Advice(
            id = "growth_remunerada",
            kind = ConsejoKind.SUGERENCIA,
            title = "💰 Cuenta remunerada",
            bodyResolver = { _, _ ->
                "La plata en caja de ahorro pierde contra inflación. Pasala a cuenta remunerada (Mercado Pago, Naranja X, Ualá): te paga interés diario sin hacer nada."
            },
            weight = 2,
        ),
        Advice(
            id = "growth_fci_money_market",
            kind = ConsejoKind.SUGERENCIA,
            title = "📈 FCI Money Market",
            bodyResolver = { _, _ ->
                "Si juntás ahorros en pesos, metelos en un fondo común money market (Cocos, IOL, Mercado Pago Inversiones). Liquidez al instante y mejor rendimiento que el plazo fijo tradicional."
            },
            weight = 2,
        ),
        Advice(
            id = "growth_plazo_uva",
            kind = ConsejoKind.SUGERENCIA,
            title = "🏛️ Plazo fijo UVA",
            bodyResolver = { _, _ ->
                "Si no necesitás la plata por 90 días, plazo fijo UVA: ajusta por inflación + un puntito. Le ganás mejor que el plazo tradicional."
            },
        ),
        Advice(
            id = "growth_dolar_mep",
            kind = ConsejoKind.SUGERENCIA,
            title = "💵 Comprate dólar MEP",
            bodyResolver = { _, _ ->
                "Dólar MEP en cualquier broker (IOL, Cocos, Bull): compras bono en pesos, vendés en dólares. Sin límite ni \"$200\". Más barato que el blue."
            },
            weight = 2,
        ),
        Advice(
            id = "growth_stablecoins",
            kind = ConsejoKind.SUGERENCIA,
            title = "🪙 USDT como ahorro",
            bodyResolver = { _, _ ->
                "Una stablecoin (USDT, USDC, DAI) es dolarizar sin trámite. La plata queda en tu mano, en cualquier exchange. Cuidado con la custodia: usá billeteras conocidas."
            },
        ),
        Advice(
            id = "growth_cedears",
            kind = ConsejoKind.SUGERENCIA,
            title = "🧠 Cedears: invertís en mundo",
            bodyResolver = { _, _ ->
                "Comprá Cedears: son acciones de Apple, Google, Coca-Cola, etc., compradas en pesos pero atadas al dólar. Diversificás afuera sin sacar la plata del país."
            },
            weight = 2,
        ),
        Advice(
            id = "growth_emergency_fund",
            kind = ConsejoKind.SUGERENCIA,
            title = "🛡️ Fondo de emergencia",
            bodyResolver = { _, _ ->
                "Antes de invertir, juntá 3 meses de gastos fijos en algo líquido (FCI o cuenta remunerada). Ese colchón te salva de pedir préstamo cuando se jode todo."
            },
            weight = 2,
        ),
        Advice(
            id = "growth_aguinaldo",
            kind = ConsejoKind.SUGERENCIA,
            title = "🎁 Aguinaldo no se gasta",
            bodyResolver = { _, _ ->
                "Cuando llegue el medio aguinaldo, tirá el 70% a dólar MEP / FCI. El otro 30% gozalo. No fundas el aguinaldo en una salida boluda."
            },
        ),
        Advice(
            id = "growth_diversify_3_buckets",
            kind = ConsejoKind.SUGERENCIA,
            title = "🪣 Tres baldes",
            bodyResolver = { _, _ ->
                "Repartí ahorros: 1) liquidez en pesos para gasto, 2) dólar MEP/USDT para refugio, 3) Cedears o bonos para crecer. No tengas todo en uno."
            },
        ),
        Advice(
            id = "growth_freelance",
            kind = ConsejoKind.SUGERENCIA,
            title = "💼 Vendé un skill",
            bodyResolver = { _, _ ->
                "Diseño, edición de video, código, traducción, fotos para redes: monetizá un skill. 2 horas extra por semana = 1 luca larga al mes."
            },
        ),
        Advice(
            id = "growth_invoice_dollars",
            kind = ConsejoKind.SUGERENCIA,
            title = "🌎 Facturá en dólares",
            bodyResolver = { _, _ ->
                "Si laburás free, intentá clientes del exterior (Upwork, Toptal, Workana). Cobrás en USD, ajustás por inflación."
            },
        ),
        Advice(
            id = "growth_skill_courses",
            kind = ConsejoKind.SUGERENCIA,
            title = "📚 Invertí en vos",
            bodyResolver = { _, _ ->
                "Curso barato (YouTube/Coursera) + certificación oficial = aumento de sueldo. La mejor ROI suele ser tu cabeza."
            },
        ),
        Advice(
            id = "growth_track_net_worth",
            kind = ConsejoKind.SUGERENCIA,
            title = "📊 Anotá tu patrimonio",
            bodyResolver = { _, _ ->
                "Una vez por mes anotá: efectivo + bancos + cripto + dólares + inversiones - deudas. Lo que no se mide, no crece."
            },
        ),
        Advice(
            id = "growth_no_credit_for_food",
            kind = ConsejoKind.SUGERENCIA,
            title = "🚫 Crédito para consumo, no",
            bodyResolver = { _, _ ->
                "Tarjeta de crédito sirve para: cuotas sin interés en algo durable. NO para comer, divertirte o vacaciones. Esa cuenta no la pagás más."
            },
        ),
        Advice(
            id = "growth_passive_pricing",
            kind = ConsejoKind.SUGERENCIA,
            title = "🔁 Renegociá todo",
            bodyResolver = { _, _ ->
                "Una vez por año: llamá a celular, internet, prepaga, banco. Pedí baja y te dan descuento mágico. 5 llamadas = una luca por mes."
            },
        ),
        Advice(
            id = "growth_compound_interest",
            kind = ConsejoKind.SUGERENCIA,
            title = "🌱 Interés compuesto",
            bodyResolver = { _, _ ->
                "Ahorrar 5 lucas por mes a 10% anual durante 30 años = 110 lucas. Empezá ya, aunque sea poquito. El tiempo es la magia."
            },
        ),
    )

    // -------------------- MINDSET / EDUCATIVO --------------------

    private val mindsetAdvice = listOf(
        Advice(
            id = "mindset_lifestyle_creep",
            kind = ConsejoKind.SUGERENCIA,
            title = "🎈 Cuidado con el lifestyle creep",
            bodyResolver = { _, _ ->
                "Cuando te aumentan, lo natural es subir el nivel de vida. Subí solo el 50% del aumento, el resto al ahorro. Ahí está el truco."
            },
        ),
        Advice(
            id = "mindset_value_per_hour",
            kind = ConsejoKind.SUGERENCIA,
            title = "⏱ Pensá en horas trabajadas",
            bodyResolver = { state, _ ->
                "Antes de gastar, calculá cuántas horas de laburo te cuesta. Esa salida de 80 lucas son 8 horas curradas. ¿Las vale?"
            },
        ),
        Advice(
            id = "mindset_avoid_lottery",
            kind = ConsejoKind.SUGERENCIA,
            title = "🎲 No es la quiniela",
            bodyResolver = { _, _ ->
                "Crecer económicamente es 99% hábito boring (ahorrar, invertir consistente) y 1% suerte. Si el plan es \"que me pegue Bitcoin\", no es plan."
            },
        ),
        Advice(
            id = "mindset_retire_early",
            kind = ConsejoKind.SUGERENCIA,
            title = "🌅 Independencia financiera",
            bodyResolver = { _, _ ->
                "Si juntás 25x tus gastos anuales invertidos, podés vivir de eso. Suena lejos: lo importante es saber el norte."
            },
        ),
        Advice(
            id = "mindset_buy_time",
            kind = ConsejoKind.SUGERENCIA,
            title = "🕰 Comprate tiempo, no cosas",
            bodyResolver = { _, _ ->
                "Las cosas pierden gracia en una semana. El tiempo libre, la salud y los amigos no. Invertí más en eso."
            },
        ),
    )

    // -------------------- CONTEXTUALES POR CATEGORÍA --------------------

    private val categoryAdvice = listOf(
        Advice(
            id = "cat_delivery_high",
            kind = ConsejoKind.FRENO,
            title = "🍔 Mucho delivery, eh",
            bodyResolver = { state, vars ->
                val name = vars["topName"] ?: "delivery"
                "Tu top este mes es $name. Una rotisería te baja el ticket a la mitad y comés más rico que Rappi."
            },
            appliesTo = {
                val s = it.topCategories.firstOrNull()?.slug?.lowercase() ?: ""
                s.contains("delivery") || s.contains("comida") || s.contains("rappi") || s.contains("pedidos")
            },
            weight = 4,
        ),
        Advice(
            id = "cat_coffee_high",
            kind = ConsejoKind.FRENO,
            title = "☕ Mate vs Starbucks",
            bodyResolver = { _, _ ->
                "El cafecito de tienda son 4 lucas. 5 días/semana = 80 al mes. Mate en termo y te quedan 80 lucas para invertir."
            },
            appliesTo = {
                val s = it.topCategories.firstOrNull()?.slug?.lowercase() ?: ""
                s.contains("cafe") || s.contains("café") || s.contains("starbucks")
            },
            weight = 3,
        ),
        Advice(
            id = "cat_uber_high",
            kind = ConsejoKind.FRENO,
            title = "🚖 Uber: ojo al ticket",
            bodyResolver = { _, _ ->
                "Subte/colectivo te sale ~10% de un Uber. Reservá Uber para emergencia o salidas tarde, no como default."
            },
            appliesTo = {
                val s = it.topCategories.firstOrNull()?.slug?.lowercase() ?: ""
                s.contains("uber") || s.contains("taxi") || s.contains("cabify") || s.contains("didi")
            },
            weight = 3,
        ),
        Advice(
            id = "cat_alcohol_high",
            kind = ConsejoKind.FRENO,
            title = "🍺 La birra suma rápido",
            bodyResolver = { _, _ ->
                "Boliche y cervezas: 30–50 lucas en una noche. Hace previa fuerte en casa y vas con el rebote, no con el sueldo."
            },
            appliesTo = {
                val s = it.topCategories.firstOrNull()?.slug?.lowercase() ?: ""
                s.contains("alcoh") || s.contains("birra") || s.contains("cerveza") || s.contains("boliche") || s.contains("salida")
            },
            weight = 3,
        ),
        Advice(
            id = "cat_subscription_high",
            kind = ConsejoKind.SUGERENCIA,
            title = "🔁 Combos de streaming",
            bodyResolver = { _, _ ->
                "Compartí Netflix/HBO/Disney en pareja o con amigos. Pagás $1 cada uno y tenés todo. Lo de 'no compartir' es marketing."
            },
            appliesTo = {
                val s = it.topCategories.firstOrNull()?.slug?.lowercase() ?: ""
                s.contains("strea") || s.contains("netflix") || s.contains("spoti") || s.contains("suscrip")
            },
            weight = 2,
        ),
    )

    // -------------------- PREMIO / RACHA --------------------

    private val rewardAdvice = listOf(
        Advice(
            id = "reward_streak_3",
            kind = ConsejoKind.PREMIO,
            title = "🔥 Racha de 3 días",
            bodyResolver = { state, _ ->
                "Llevás ${state.streakDaysNoSpend} días sin gastar. La constancia es la diferencia entre ahorrar y soñar con ahorrar."
            },
            appliesTo = { it.streakDaysNoSpend in 3..6 },
            weight = 5,
        ),
        Advice(
            id = "reward_streak_7",
            kind = ConsejoKind.PREMIO,
            title = "🏆 Una semana entera",
            bodyResolver = { state, _ ->
                "${state.streakDaysNoSpend} días sin tocar la billetera. Mandate un autoregalo chico bajo presupuesto y seguí."
            },
            appliesTo = { it.streakDaysNoSpend >= 7 },
            weight = 6,
        ),
        Advice(
            id = "reward_zero_today",
            kind = ConsejoKind.PREMIO,
            title = "🎯 Día sin gastar",
            bodyResolver = { _, _ ->
                "Hoy no gastaste un peso. Tomate un mate, pone un disco y disfrutá saber que mañana tenés un día más para invertir."
            },
            appliesTo = { it.daySpentPesos == 0L && it.expenseCount >= 2 },
            weight = 3,
        ),
        Advice(
            id = "reward_under_goal",
            kind = ConsejoKind.PREMIO,
            title = "✅ Vas debajo de la meta",
            bodyResolver = { _, vars ->
                val r = vars["remainingLucas"] ?: "0"
                "Te quedan $r lucas en la meta del mes. Si no los gastás, mandalos a un FCI: que crezcan solos."
            },
            appliesTo = { state ->
                val pct = state.globalProgress
                pct != null && pct < 0.7f && state.expenseCount >= 5
            },
            weight = 4,
        ),
    )

    // -------------------- LISTA MAESTRA --------------------

    val ALL: List<Advice> = habitAdvice + growthAdvice + mindsetAdvice + categoryAdvice + rewardAdvice
}
