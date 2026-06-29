package com.antigastos.boludos.domain

import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

enum class CopyMood { CHEER, NEUTRAL, BURN, ALARM }

data class CopyContext(
    val monthLabel: String,
    val totalLucas: Int,
    val topCategoryName: String?,
    val topCategorySlug: String?,
    val topCategoryLucas: Int,
    val deltaLucasVsPrevMonth: Int?,
    val pctOfMonthlyGoal: Double?,
    val daySpentLucas: Int = 0,
    val dayOfMonth: Int = 1,
    val daysInMonth: Int = 30,
    val expenseCount: Int = 0,
    val lastExpenseSlug: String? = null,
    val lastExpenseLucas: Int = 0,
    val lucasByCategory: Map<String, Int> = emptyMap(),
)

data class CopyTemplate(
    val id: String,
    val mood: CopyMood,
    val matches: (CopyContext) -> Boolean,
    val render: (CopyContext) -> String,
)

data class CopyChoice(
    val templateId: String,
    val text: String,
    val mood: CopyMood,
)

/**
 * Motor de mensajes con tono rioplatense. Las plantillas tienen IDs estables
 * para poder votarlas y darles peso en el muestreo.
 */
object CopyEngine {

    val templates: List<CopyTemplate> = listOf(
        // ====== Estados base ======
        CopyTemplate(
            id = "zero_spend",
            mood = CopyMood.NEUTRAL,
            matches = { it.totalLucas == 0 },
            render = { "Este mes no registraste gastos. ¿Arrancamos antes de que vuele todo?" },
        ),
        CopyTemplate(
            id = "default_high",
            mood = CopyMood.BURN,
            matches = { it.totalLucas >= 200 },
            render = { "Este mes se te fue como ${it.totalLucas} lucas. Acá no juzgamos… mucho." },
        ),
        CopyTemplate(
            id = "default_mid",
            mood = CopyMood.NEUTRAL,
            matches = { it.totalLucas in 80..199 },
            render = { "Van ${it.totalLucas} lucas en ${it.monthLabel}. Ni tan mal ni tan bien." },
        ),
        CopyTemplate(
            id = "default_low",
            mood = CopyMood.CHEER,
            matches = { it.totalLucas in 1..79 },
            render = { "Llevás ${it.totalLucas} lucas en ${it.monthLabel}. Controlado." },
        ),

        // ====== Delivery ======
        CopyTemplate(
            id = "delivery_high",
            mood = CopyMood.BURN,
            matches = { it.topCategorySlug == "delivery" && it.topCategoryLucas >= 30 },
            render = { "Mirá vo': ${it.topCategoryLucas} lucas solo en delivery este mes 💀" },
        ),
        CopyTemplate(
            id = "delivery_addict",
            mood = CopyMood.BURN,
            matches = { it.topCategorySlug == "delivery" && it.topCategoryLucas >= 30 },
            render = { "Te la pasás pidiendo comida: ${it.topCategoryLucas} lucas en delivery." },
        ),
        CopyTemplate(
            id = "delivery_no_oven",
            mood = CopyMood.NEUTRAL,
            matches = { it.topCategorySlug == "delivery" },
            render = { "${it.topCategoryLucas} lucas en delivery en ${it.monthLabel}. El horno no existe, ¿no?" },
        ),
        CopyTemplate(
            id = "delivery_again_event",
            mood = CopyMood.BURN,
            matches = { it.lastExpenseSlug == "delivery" },
            render = { "Otra vez pediste delivery, maestro 🍔💀" },
        ),
        CopyTemplate(
            id = "delivery_rappi_salary",
            mood = CopyMood.ALARM,
            matches = { it.topCategorySlug == "delivery" && it.topCategoryLucas >= 60 },
            render = { "Se te fue un sueldo en rappi, campeón." },
        ),
        CopyTemplate(
            id = "delivery_lower",
            mood = CopyMood.BURN,
            matches = { it.topCategorySlug == "delivery" && it.topCategoryLucas >= 20 },
            render = { "Fua amigo, bajale al delivery." },
        ),
        CopyTemplate(
            id = "delivery_sushi",
            mood = CopyMood.BURN,
            matches = { it.lastExpenseSlug == "delivery" && it.lastExpenseLucas >= 15 },
            render = { "Todo para decir que pidió sushi otra vez 🍣" },
        ),
        CopyTemplate(
            id = "delivery_not_food_group",
            mood = CopyMood.BURN,
            matches = { it.topCategorySlug == "delivery" },
            render = { "El delivery no es un grupo alimenticio 🍕" },
        ),
        CopyTemplate(
            id = "empanada_count",
            mood = CopyMood.NEUTRAL,
            matches = { it.lastExpenseSlug == "delivery" || it.lastExpenseSlug == "super" },
            render = { "Cada empanada cuenta 🥟" },
        ),

        // ====== Boludeces / compras impulsivas ======
        CopyTemplate(
            id = "boludeces",
            mood = CopyMood.BURN,
            matches = { it.topCategorySlug == "boludeces" },
            render = { "El podrío ganó: ${it.topCategoryLucas} lucas en boludeces." },
        ),
        CopyTemplate(
            id = "ant_to_elephant",
            mood = CopyMood.BURN,
            matches = { it.expenseCount >= 12 && (it.lucasByCategory["boludeces"] ?: 0) >= 10 },
            render = { "Tus gastos hormiga ya son gastos elefante 🐘" },
        ),
        CopyTemplate(
            id = "patinaste_15",
            mood = CopyMood.BURN,
            matches = { (it.lucasByCategory["boludeces"] ?: 0) >= 10 },
            render = {
                val l = it.lucasByCategory["boludeces"] ?: 15
                "Te patinaste $l lucas en boludeces esta semana."
            },
        ),
        CopyTemplate(
            id = "anxiety_buy",
            mood = CopyMood.BURN,
            matches = { it.lastExpenseSlug == "boludeces" },
            render = { "Compraste por ansiedad otra vez, ¿no?" },
        ),
        CopyTemplate(
            id = "unnecessary_alert",
            mood = CopyMood.ALARM,
            matches = { it.lastExpenseSlug == "boludeces" && it.lastExpenseLucas >= 5 },
            render = { "La app detectó una compra totalmente innecesaria 🚨" },
        ),
        CopyTemplate(
            id = "not_invest_caprichito",
            mood = CopyMood.NEUTRAL,
            matches = { it.lastExpenseSlug == "boludeces" },
            render = { "No era inversión, era caprichito." },
        ),
        CopyTemplate(
            id = "guilty_pleasure_80",
            mood = CopyMood.BURN,
            matches = { (it.lucasByCategory["boludeces"] ?: 0) + (it.lucasByCategory["salidas"] ?: 0) >= 60 },
            render = {
                val sum = (it.lucasByCategory["boludeces"] ?: 0) + (it.lucasByCategory["salidas"] ?: 0)
                "Ese 'gustito' ya suma $sum lucas."
            },
        ),
        CopyTemplate(
            id = "invest_no",
            mood = CopyMood.NEUTRAL,
            matches = { it.topCategorySlug == "boludeces" },
            render = { "Invertir: ❌ Comprar pelotudeces en oferta: ✅" },
        ),
        CopyTemplate(
            id = "dopamine",
            mood = CopyMood.BURN,
            matches = { it.lastExpenseSlug == "boludeces" || it.lastExpenseSlug == "salidas" },
            render = { "Gasto detectado: dopamina instantánea." },
        ),
        CopyTemplate(
            id = "not_needed_close",
            mood = CopyMood.NEUTRAL,
            matches = { it.lastExpenseSlug == "boludeces" },
            render = { "No necesitabas eso. Pero estuvo cerca." },
        ),
        CopyTemplate(
            id = "cv_adult_kid",
            mood = CopyMood.BURN,
            matches = { (it.lucasByCategory["boludeces"] ?: 0) >= 30 },
            render = { "Tu CV dice adulto. Tus gastos dicen adolescente." },
        ),
        CopyTemplate(
            id = "mli_too_known",
            mood = CopyMood.BURN,
            matches = { (it.lucasByCategory["boludeces"] ?: 0) + (it.lucasByCategory["otros"] ?: 0) >= 25 },
            render = { "Mercado Libre te conoce demasiado bien 📦" },
        ),

        // ====== Salidas ======
        CopyTemplate(
            id = "salidas",
            mood = CopyMood.BURN,
            matches = { it.topCategorySlug == "salidas" },
            render = { "Salidas ${it.topCategoryLucas} lucas. La previa sale cara." },
        ),
        CopyTemplate(
            id = "broke_aesthetic",
            mood = CopyMood.BURN,
            matches = { it.topCategorySlug == "salidas" && it.topCategoryLucas >= 30 },
            render = { "Te fundiste pero aesthetic ✨" },
        ),

        // ====== Transporte ======
        CopyTemplate(
            id = "uber_vs_sube",
            mood = CopyMood.BURN,
            matches = { (it.lucasByCategory["transporte"] ?: 0) >= 25 },
            render = { "Gastaste más en Uber que en SUBE este mes 🚕" },
        ),

        // ====== Servicios ======
        CopyTemplate(
            id = "fixed_union",
            mood = CopyMood.BURN,
            matches = { (it.lucasByCategory["servicios"] ?: 0) >= 30 },
            render = { "Tus gastos fijos ya están armando sindicato." },
        ),
        CopyTemplate(
            id = "less_streaming",
            mood = CopyMood.NEUTRAL,
            matches = { (it.lucasByCategory["servicios"] ?: 0) >= 15 },
            render = { "Menos streaming y más ahorro, titan." },
        ),

        // ====== Super / promos ======
        CopyTemplate(
            id = "promo_2x1",
            mood = CopyMood.NEUTRAL,
            matches = { (it.lucasByCategory["super"] ?: 0) >= 25 },
            render = { "Che… ¿de verdad necesitabas otra promo 2x1?" },
        ),

        // ====== Ahorro / día sin gastar / racha ======
        CopyTemplate(
            id = "zero_today",
            mood = CopyMood.CHEER,
            matches = { it.daySpentLucas == 0 && it.expenseCount > 0 },
            render = { "Hoy no gastaste nada. ¿Te sentís bien?" },
        ),
        CopyTemplate(
            id = "mode_rat",
            mood = CopyMood.CHEER,
            matches = { it.daySpentLucas == 0 && it.expenseCount > 0 },
            render = { "Modo rata activado 🐀" },
        ),
        CopyTemplate(
            id = "kiosk_misses_you",
            mood = CopyMood.CHEER,
            matches = { it.daySpentLucas == 0 && it.expenseCount >= 3 },
            render = { "El kiosquero ya te extraña." },
        ),
        CopyTemplate(
            id = "mli_skipped",
            mood = CopyMood.CHEER,
            matches = { it.daySpentLucas == 0 && it.expenseCount > 0 },
            render = { "Hoy sobreviviste sin Mercado Libre. Bien ahí." },
        ),
        CopyTemplate(
            id = "saved_5_cooking",
            mood = CopyMood.CHEER,
            matches = { it.daySpentLucas == 0 && it.expenseCount > 0 },
            render = { "Te ahorraste 5 lucas cocinando en casa 👏" },
        ),
        CopyTemplate(
            id = "saving_hobby",
            mood = CopyMood.CHEER,
            matches = { it.daySpentLucas == 0 },
            render = { "Ahorrar también es un hobby 😎" },
        ),
        CopyTemplate(
            id = "xp_no_buy",
            mood = CopyMood.CHEER,
            matches = { it.daySpentLucas == 0 && it.expenseCount > 0 },
            render = { "Si no comprás boludeces hoy, ganás XP." },
        ),
        CopyTemplate(
            id = "impulse_dodge",
            mood = CopyMood.CHEER,
            matches = { it.daySpentLucas == 0 && it.expenseCount >= 2 },
            render = { "Hoy esquivaste una compra impulsiva. Orgullo nacional." },
        ),
        CopyTemplate(
            id = "battle_won",
            mood = CopyMood.CHEER,
            matches = { it.daySpentLucas == 0 && it.expenseCount > 0 },
            render = { "Hoy ganaste la batalla contra el 'comprar ahora' 🛒" },
        ),
        CopyTemplate(
            id = "cooking_100xp",
            mood = CopyMood.CHEER,
            matches = { it.daySpentLucas == 0 && it.expenseCount > 0 },
            render = { "Cocinaste en casa: +100 puntos de estabilidad financiera." },
        ),

        // ====== Gastos altos / billetera sufrida ======
        CopyTemplate(
            id = "card_help",
            mood = CopyMood.ALARM,
            matches = { it.totalLucas >= 150 },
            render = { "La tarjeta está pidiendo auxilio 🚑" },
        ),
        CopyTemplate(
            id = "wallet_done",
            mood = CopyMood.ALARM,
            matches = { it.totalLucas >= 150 },
            render = { "La billetera: 'hasta acá llegué'" },
        ),
        CopyTemplate(
            id = "wallet_owner",
            mood = CopyMood.ALARM,
            matches = { it.totalLucas >= 200 },
            render = { "Tu billetera pidió cambiar de dueño." },
        ),
        CopyTemplate(
            id = "homebanking_closed",
            mood = CopyMood.ALARM,
            matches = { it.totalLucas >= 200 },
            render = { "El homebanking ya no quiere abrir." },
        ),
        CopyTemplate(
            id = "future_self_crying",
            mood = CopyMood.BURN,
            matches = { it.totalLucas >= 120 },
            render = { "Tu yo del futuro está llorando." },
        ),
        CopyTemplate(
            id = "balance_faith",
            mood = CopyMood.ALARM,
            matches = { it.totalLucas >= 180 },
            render = { "Saldo actual: fe y esperanza." },
        ),
        CopyTemplate(
            id = "bank_hardcore",
            mood = CopyMood.ALARM,
            matches = { it.totalLucas >= 180 },
            render = { "Tu cuenta bancaria está en dificultad hardcore 🎮" },
        ),
        CopyTemplate(
            id = "thermo_nuclear",
            mood = CopyMood.ALARM,
            matches = { it.lastExpenseLucas >= 50 },
            render = { "Se detectó un gasto termo nuclear ☢️" },
        ),

        // ====== Metas ======
        CopyTemplate(
            id = "goal_over_115",
            mood = CopyMood.BURN,
            matches = { (it.pctOfMonthlyGoal ?: 0.0) >= 1.15 },
            render = {
                val pct = it.pctOfMonthlyGoal ?: 1.15
                "Pasaste la meta un ${((pct - 1) * 100).toInt()}%. tranqui nomás 😅"
            },
        ),
        CopyTemplate(
            id = "goal_over_100",
            mood = CopyMood.BURN,
            matches = {
                val p = it.pctOfMonthlyGoal ?: 0.0
                p in 1.0..1.149
            },
            render = { "Rompiste la meta del mes. Next: ahorrar o asumirlo." },
        ),
        CopyTemplate(
            id = "goal_near_85",
            mood = CopyMood.NEUTRAL,
            matches = {
                val p = it.pctOfMonthlyGoal ?: 0.0
                p in 0.85..0.999
            },
            render = {
                val pct = it.pctOfMonthlyGoal ?: 0.85
                "Te queda poquito cupo de meta: ${(pct * 100).toInt()}% usado."
            },
        ),
        CopyTemplate(
            id = "goal_under_50",
            mood = CopyMood.CHEER,
            matches = { (it.pctOfMonthlyGoal ?: 1.0) <= 0.5 },
            render = { "Vas bien contra la meta: todavía te queda aire." },
        ),
        CopyTemplate(
            id = "delta_up",
            mood = CopyMood.BURN,
            matches = { it.deltaLucasVsPrevMonth != null && it.deltaLucasVsPrevMonth >= 5 },
            render = { "Subiste ~${it.deltaLucasVsPrevMonth} lucas vs el mes pasado. Ojo ahí." },
        ),
        CopyTemplate(
            id = "delta_down",
            mood = CopyMood.CHEER,
            matches = { it.deltaLucasVsPrevMonth != null && it.deltaLucasVsPrevMonth <= -5 },
            render = { "Bajaste ~${abs(it.deltaLucasVsPrevMonth ?: 0)} lucas vs el mes pasado. Capeón." },
        ),

        // ====== Fin de mes / aguinaldo / supervivencia ======
        CopyTemplate(
            id = "end_of_month_survival",
            mood = CopyMood.ALARM,
            matches = { it.dayOfMonth >= 23 && (it.pctOfMonthlyGoal ?: 0.0) >= 0.7 },
            render = { "Se viene fin de mes y vos en modo supervivencia." },
        ),
        CopyTemplate(
            id = "alfajor_quick",
            mood = CopyMood.BURN,
            matches = { it.dayOfMonth >= 20 && (it.pctOfMonthlyGoal ?: 0.0) >= 0.8 },
            render = { "Te duró menos la plata que un alfajor arriba de la mesa." },
        ),
        CopyTemplate(
            id = "saving_politician",
            mood = CopyMood.BURN,
            matches = { (it.pctOfMonthlyGoal ?: 0.0) >= 0.95 },
            render = { "Nivel de ahorro: político honesto." },
        ),
        CopyTemplate(
            id = "rice_week",
            mood = CopyMood.ALARM,
            matches = { (it.pctOfMonthlyGoal ?: 0.0) >= 0.9 && it.dayOfMonth >= 18 },
            render = { "Estás a un pedido más de comer arroz toda la semana." },
        ),
        CopyTemplate(
            id = "aguinaldo_3_days",
            mood = CopyMood.BURN,
            matches = { (it.pctOfMonthlyGoal ?: 0.0) >= 1.0 },
            render = { "Si seguís así, el aguinaldo dura 3 días." },
        ),
        CopyTemplate(
            id = "pray_economy",
            mood = CopyMood.ALARM,
            matches = { (it.pctOfMonthlyGoal ?: 0.0) >= 1.1 },
            render = { "La economía personal está en etapa 'rezar'." },
        ),
        CopyTemplate(
            id = "not_salary",
            mood = CopyMood.NEUTRAL,
            matches = { (it.pctOfMonthlyGoal ?: 0.0) >= 1.0 },
            render = { "Mepa que el problema no es el sueldo…" },
        ),
        CopyTemplate(
            id = "feet_admin",
            mood = CopyMood.BURN,
            matches = { (it.pctOfMonthlyGoal ?: 0.0) >= 1.05 },
            render = { "Estás administrando la plata con los pies." },
        ),
        CopyTemplate(
            id = "boca_2018",
            mood = CopyMood.BURN,
            matches = { (it.pctOfMonthlyGoal ?: 0.0) >= 1.0 },
            render = { "Tus finanzas están en modo Boca post 2018 ⚽" },
        ),
        CopyTemplate(
            id = "jean_tight",
            mood = CopyMood.BURN,
            matches = { (it.pctOfMonthlyGoal ?: 0.0) >= 0.9 },
            render = { "Tu economía está más ajustada que jean recién lavado." },
        ),
        CopyTemplate(
            id = "figuritas",
            mood = CopyMood.ALARM,
            matches = { (it.pctOfMonthlyGoal ?: 0.0) >= 1.2 },
            render = { "A este ritmo, terminás pagando en figuritas." },
        ),
        CopyTemplate(
            id = "savings_not_fail",
            mood = CopyMood.BURN,
            matches = { (it.pctOfMonthlyGoal ?: 0.0) >= 1.0 },
            render = { "Modo 'no tocar ahorros' fallido." },
        ),
        CopyTemplate(
            id = "alive_30",
            mood = CopyMood.NEUTRAL,
            matches = { it.dayOfMonth in 10..28 },
            render = { "Objetivo del mes: llegar vivo al 30." },
        ),

        // ====== Cafecito (gastos hormiga) ======
        CopyTemplate(
            id = "coffee_moto",
            mood = CopyMood.BURN,
            matches = { it.expenseCount >= 10 && it.totalLucas >= 50 },
            render = { "Ese cafecito diario ya vale una moto usada ☕" },
        ),
        CopyTemplate(
            id = "coffee_premium",
            mood = CopyMood.NEUTRAL,
            matches = { it.expenseCount >= 8 },
            render = { "¿Otro cafecito premium? Bill Gates tampoco tanto." },
        ),

        // ====== Misceláneo / filosofía rioplatense ======
        CopyTemplate(
            id = "vemos_que_pasa",
            mood = CopyMood.NEUTRAL,
            matches = { it.totalLucas in 50..200 },
            render = { "Economía nivel: 'vemos qué pasa'." },
        ),
        CopyTemplate(
            id = "cash_immune",
            mood = CopyMood.NEUTRAL,
            matches = { it.totalLucas > 0 },
            render = { "Sos inimputable si pagás en efectivo." },
        ),
        CopyTemplate(
            id = "taxes_problem",
            mood = CopyMood.NEUTRAL,
            matches = { it.totalLucas > 0 },
            render = { "El problema no son los impuestos… bueno, un poco sí 🇦🇷" },
        ),
        CopyTemplate(
            id = "mate_safe",
            mood = CopyMood.CHEER,
            matches = { it.totalLucas in 1..120 },
            render = { "Tranqui, todavía no hipotecaste el mate 🧉" },
        ),
        CopyTemplate(
            id = "streak_3",
            mood = CopyMood.CHEER,
            matches = { it.daySpentLucas == 0 && it.expenseCount >= 5 },
            render = { "Racha de ahorro: 3 días sin delirios financieros 🔥" },
        ),

        // ====== Lunfardo / slang puro ======
        CopyTemplate(
            id = "slang_patinaste",
            mood = CopyMood.BURN,
            matches = { it.totalLucas >= 50 },
            render = {
                val verb = MoneySlang.spentVerb()
                "$verb ${it.totalLucas} lucas en ${it.monthLabel}. Te ${listOf("la fumaste", "la patinaste", "la quemaste").random()}."
            },
        ),
        CopyTemplate(
            id = "slang_fangote",
            mood = CopyMood.BURN,
            matches = { it.totalLucas >= 100 },
            render = { "Salió un fangote: ${it.totalLucas} lucas y contando." },
        ),
        CopyTemplate(
            id = "slang_costo_huevo",
            mood = CopyMood.BURN,
            matches = { it.lastExpenseLucas >= 30 },
            render = { "Ese gasto te costó un huevo 🥚" },
        ),
        CopyTemplate(
            id = "slang_dejaste_medio_sueldo",
            mood = CopyMood.BURN,
            matches = { it.lastExpenseLucas >= 40 },
            render = { "Dejaste medio sueldo en ese ${it.lastExpenseSlug ?: "gasto"}." },
        ),
        CopyTemplate(
            id = "slang_morlacos",
            mood = CopyMood.NEUTRAL,
            matches = { it.totalLucas in 30..199 },
            render = { "Llevás ${it.totalLucas} morlacos quemados este mes." },
        ),
        CopyTemplate(
            id = "slang_estas_seco",
            mood = CopyMood.ALARM,
            matches = { (it.pctOfMonthlyGoal ?: 0.0) >= 1.1 },
            render = { "Estás seco 🌵 — ${MoneySlang.statusForGoalPct(it.pctOfMonthlyGoal ?: 1.1)}." },
        ),
        CopyTemplate(
            id = "slang_hasta_las_manos",
            mood = CopyMood.ALARM,
            matches = { (it.pctOfMonthlyGoal ?: 0.0) >= 1.0 },
            render = { "Estás hasta las manos 💀 con la guita." },
        ),
        CopyTemplate(
            id = "slang_no_un_mango",
            mood = CopyMood.ALARM,
            matches = { (it.pctOfMonthlyGoal ?: 0.0) >= 1.0 },
            render = { "No te queda un mango. Apretá el cinturón, capo." },
        ),
        CopyTemplate(
            id = "slang_vivir_al_dia",
            mood = CopyMood.BURN,
            matches = { it.dayOfMonth >= 22 && (it.pctOfMonthlyGoal ?: 0.0) >= 0.8 },
            render = { "Estás viviendo al día. Llegar al 30 es la meta." },
        ),
        CopyTemplate(
            id = "slang_rascar_olla",
            mood = CopyMood.BURN,
            matches = { it.dayOfMonth >= 25 && (it.pctOfMonthlyGoal ?: 0.0) >= 0.85 },
            render = { "Toca rascar la olla y hacer vaquita 🐄." },
        ),
        CopyTemplate(
            id = "slang_chanchito",
            mood = CopyMood.BURN,
            matches = { (it.pctOfMonthlyGoal ?: 0.0) >= 1.05 },
            render = { "Rompiste el chanchito 🐷. Mal ahí." },
        ),
        CopyTemplate(
            id = "slang_tarjetazo",
            mood = CopyMood.ALARM,
            matches = { it.lastExpenseLucas >= 30 },
            render = { "Tarjeteada 💳 marca ACME — ese gasto te dejó tildado." },
        ),
        CopyTemplate(
            id = "slang_cuotear_todo",
            mood = CopyMood.BURN,
            matches = { it.lastExpenseLucas >= 50 },
            render = { "Andá pensando en cuotearlo, porque pagar al toque es bravo." },
        ),
        CopyTemplate(
            id = "slang_bicicleta",
            mood = CopyMood.NEUTRAL,
            matches = { (it.pctOfMonthlyGoal ?: 0.0) >= 0.95 },
            render = { "Bicicleta financiera 🚴 nivel pro." },
        ),
        CopyTemplate(
            id = "slang_magia",
            mood = CopyMood.NEUTRAL,
            matches = { (it.pctOfMonthlyGoal ?: 0.0) in 0.7..0.95 },
            render = { "Hay que hacer magia con la plata para llegar al 30." },
        ),
        CopyTemplate(
            id = "slang_oferta_oferta",
            mood = CopyMood.NEUTRAL,
            matches = { (it.lucasByCategory["super"] ?: 0) >= 20 },
            render = { "Te volviste experto en vivir de oferta en oferta." },
        ),
        CopyTemplate(
            id = "slang_estirar_chicle",
            mood = CopyMood.NEUTRAL,
            matches = { it.dayOfMonth >= 20 && (it.pctOfMonthlyGoal ?: 0.0) >= 0.7 },
            render = { "Estás estirando la biyu como chicle 😄" },
        ),
        CopyTemplate(
            id = "slang_dulce",
            mood = CopyMood.CHEER,
            matches = { (it.pctOfMonthlyGoal ?: 0.0) <= 0.4 && it.totalLucas > 0 },
            render = { "Vas dulce con la biyu, ${MoneySlang.adjective()}." },
        ),
        CopyTemplate(
            id = "slang_pala",
            mood = CopyMood.CHEER,
            matches = { it.daySpentLucas == 0 && it.expenseCount >= 8 && (it.pctOfMonthlyGoal ?: 1.0) <= 0.6 },
            render = { "Estás levantando la guita en pala 🏗️" },
        ),
        CopyTemplate(
            id = "slang_aguinaldo",
            mood = CopyMood.NEUTRAL,
            matches = { it.dayOfMonth in 25..31 },
            render = { "Pensá en el aguinaldo como un canuto, no como sueldo extra." },
        ),
        CopyTemplate(
            id = "slang_canuto",
            mood = CopyMood.CHEER,
            matches = { (it.pctOfMonthlyGoal ?: 1.0) <= 0.5 && it.dayOfMonth >= 15 },
            render = { "Andá guardando un canuto para fin de mes 🤫" },
        ),
        CopyTemplate(
            id = "slang_billetin_quemado",
            mood = CopyMood.BURN,
            matches = { it.lastExpenseLucas in 5..29 },
            render = { "Otro billetín quemado en ${it.lastExpenseSlug ?: "lo de siempre"}." },
        ),
        CopyTemplate(
            id = "slang_biyu_fantasma",
            mood = CopyMood.BURN,
            matches = { it.expenseCount >= 15 },
            render = { "Biyu fantasma 👻: cargaste ${it.expenseCount} gastos y no sabés bien dónde se fue." },
        ),
        CopyTemplate(
            id = "slang_se_llevo_torta",
            mood = CopyMood.BURN,
            matches = { it.lastExpenseLucas >= 40 },
            render = { "Se llevó una torta ese gasto, eh 🎂" },
        ),
        CopyTemplate(
            id = "slang_tirando_manteca",
            mood = CopyMood.ALARM,
            matches = { it.totalLucas >= 250 },
            render = { "Tirando manteca al techo ✨ — ${it.totalLucas} lucas en el mes." },
        ),

        // ====== ⚽ Fútbol argentino ======
        CopyTemplate("futbol_lesionada", CopyMood.ALARM, { it.totalLucas >= 150 }) { "La billetera está lesionada ⚽" },
        CopyTemplate("futbol_partido_inteligente", CopyMood.CHEER, { it.daySpentLucas == 0 && it.expenseCount > 0 }) { "Hoy jugaste un partido inteligente." },
        CopyTemplate("futbol_entraste_fuerte", CopyMood.BURN, { it.lastExpenseSlug in setOf("super", "boludeces") && it.lastExpenseLucas >= 15 }) { "Entraste fuerte al shopping." },
        CopyTemplate("futbol_evitable", CopyMood.BURN, { it.lastExpenseSlug == "boludeces" }) { "Ese gasto era evitable, muchachos." },
        CopyTemplate("futbol_tarjeta_var", CopyMood.BURN, { it.totalLucas >= 100 }) { "La tarjeta pide VAR 📺" },
        CopyTemplate("futbol_clavaron_cuota", CopyMood.BURN, { it.lastExpenseLucas >= 30 }) { "Fuiste al ataque y te clavaron la cuota." },
        CopyTemplate("futbol_fin_mes_trabado", CopyMood.BURN, { it.dayOfMonth >= 25 && (it.pctOfMonthlyGoal ?: 0.0) >= 0.8 }) { "Fin de mes trabado y luchado." },
        CopyTemplate("futbol_defendiste_bolsillo", CopyMood.CHEER, { (it.pctOfMonthlyGoal ?: 1.0) <= 0.5 && it.dayOfMonth >= 15 }) { "Defendiste mejor el bolsillo que la selección." },
        CopyTemplate("futbol_amague_marketing", CopyMood.BURN, { it.lastExpenseSlug == "boludeces" }) { "Te comiste un amague del marketing." },
        CopyTemplate("futbol_compra_innecesaria", CopyMood.ALARM, { it.lastExpenseSlug == "boludeces" }) { "Compra innecesaria confirmada." },
        CopyTemplate("futbol_mercado_pases", CopyMood.BURN, { it.dayOfMonth >= 25 && (it.pctOfMonthlyGoal ?: 0.0) >= 0.8 }) { "Se viene un mercado de pases complicado." },
        CopyTemplate("futbol_ahorros_cambio", CopyMood.BURN, { it.totalLucas >= 100 }) { "Tus ahorros están pidiendo cambio." },
        CopyTemplate("futbol_raspar_puntos", CopyMood.BURN, { it.dayOfMonth >= 20 && (it.pctOfMonthlyGoal ?: 0.0) >= 0.8 }) { "Hoy tocó raspar puntos." },
        CopyTemplate("futbol_delivery_clasico", CopyMood.BURN, { it.topCategorySlug == "delivery" }) { "El delivery ganó el clásico." },
        CopyTemplate("futbol_dirigente_desesperado", CopyMood.ALARM, { it.totalLucas >= 250 }) { "Gastaste como dirigente desesperado." },
        CopyTemplate("futbol_primera_ronda", CopyMood.ALARM, { (it.pctOfMonthlyGoal ?: 0.0) >= 1.1 }) { "Tu economía quedó afuera en primera ronda." },
        CopyTemplate("futbol_semana_perfecta", CopyMood.CHEER, { it.daySpentLucas == 0 && it.streakCheap() }) { "Semana perfecta: cero compras boludas." },
        CopyTemplate("futbol_humo", CopyMood.BURN, { it.lastExpenseSlug == "boludeces" }) { "No era compra, era humo 💨" },
        CopyTemplate("futbol_resumen_picante", CopyMood.BURN, { it.totalLucas >= 150 }) { "El resumen vino picante 🌶️" },
        CopyTemplate("futbol_sin_mediocampo", CopyMood.BURN, { (it.pctOfMonthlyGoal ?: 0.0) >= 0.95 }) { "Estás jugando sin mediocampo financiero." },

        // ====== 🎤 Música / recitales / rock nacional ======
        CopyTemplate("musica_muchachos", CopyMood.BURN, { it.totalLucas >= 150 }) { "Muchaaaachos… se fue todo el sueldo 🎶" },
        CopyTemplate("musica_desafinada", CopyMood.BURN, { it.totalLucas >= 100 }) { "Tu billetera quedó desafinada 🎸" },
        CopyTemplate("musica_tono_menor", CopyMood.NEUTRAL, { it.totalLucas in 40..149 }) { "Gastos en tono menor." },
        CopyTemplate("musica_acustico", CopyMood.CHEER, { it.totalLucas in 1..70 && it.dayOfMonth >= 10 }) { "Este mes viene acústico 🎶" },
        CopyTemplate("musica_solo_desaparecio", CopyMood.ALARM, { it.lastExpenseLucas >= 40 }) { "La tarjeta hizo solo y desapareció." },
        CopyTemplate("musica_guitarra_bar", CopyMood.ALARM, { (it.pctOfMonthlyGoal ?: 0.0) >= 1.1 }) { "Más roto que guitarra de bar 🎸" },
        CopyTemplate("musica_no_pogo", CopyMood.CHEER, { it.daySpentLucas == 0 }) { "Hoy no hubo pogo financiero." },
        CopyTemplate("musica_modo_recital", CopyMood.ALARM, { it.daySpentLucas >= 30 }) { "Modo recital: gastaste todo en una noche 🎤" },
        CopyTemplate("musica_covers", CopyMood.NEUTRAL, { it.expenseCount >= 8 && it.totalLucas in 40..150 }) { "Tu economía está tocando covers." },
        CopyTemplate("musica_hit_un_dia", CopyMood.BURN, { it.lastExpenseLucas >= 25 }) { "Ese gasto fue un hit de un solo día." },
        CopyTemplate("musica_emocion", CopyMood.BURN, { it.lastExpenseSlug == "boludeces" }) { "Compraste por emoción." },
        CopyTemplate("musica_playlist_triste", CopyMood.BURN, { it.lastExpenseSlug in setOf("boludeces", "otros") }) { "Playlist triste + Mercado Libre = peligro." },
        CopyTemplate("musica_billetera_descanso", CopyMood.ALARM, { it.totalLucas >= 200 }) { "La billetera pide descanso 🎤" },
        CopyTemplate("musica_gira_internacional", CopyMood.BURN, { (it.pctOfMonthlyGoal ?: 0.0) >= 1.0 }) { "Se viene gira internacional… de deudas." },
        CopyTemplate("musica_mucho_flow", CopyMood.BURN, { it.totalLucas >= 120 && (it.pctOfMonthlyGoal ?: 1.0) >= 0.7 }) { "Mucho flow, poco ahorro." },
        CopyTemplate("musica_blues", CopyMood.ALARM, { (it.pctOfMonthlyGoal ?: 0.0) >= 1.05 }) { "Tu cuenta está cantando blues 🎷" },
        CopyTemplate("musica_estrella_trap", CopyMood.BURN, { it.daySpentLucas >= 25 }) { "Compraste como estrella de trap 💎" },
        CopyTemplate("musica_after_7am", CopyMood.ALARM, { it.totalLucas >= 250 }) { "Nivel de gastos: after hasta las 7 AM." },
        CopyTemplate("musica_hit_verano", CopyMood.BURN, { it.dayOfMonth in 5..15 && (it.pctOfMonthlyGoal ?: 0.0) >= 0.6 }) { "Tu sueldo duró menos que hit del verano." },
        CopyTemplate("musica_cafecito_deluxe", CopyMood.NEUTRAL, { it.expenseCount >= 7 }) { "Ese cafecito salió edición deluxe ☕" },

        // ====== 📺 TV argentina / espectáculo ======
        CopyTemplate("tv_resumen_picante", CopyMood.BURN, { it.totalLucas >= 150 }) { "Se picó el resumen." },
        CopyTemplate("tv_tremendo", CopyMood.BURN, { it.totalLucas >= 100 }) { "Tremendo lo que gastaste." },
        CopyTemplate("tv_un_monton", CopyMood.BURN, { it.lastExpenseLucas >= 25 }) { "Esto es un montón." },
        CopyTemplate("tv_en_placa", CopyMood.ALARM, { (it.pctOfMonthlyGoal ?: 0.0) >= 1.0 }) { "Tu economía está en placa 🚨" },
        CopyTemplate("tv_nominada", CopyMood.ALARM, { it.totalLucas >= 200 }) { "La tarjeta quedó nominada." },
        CopyTemplate("tv_no_entenderias", CopyMood.NEUTRAL, { it.lastExpenseSlug == "salidas" }) { "No lo entenderías 🚬" },
        CopyTemplate("tv_alta_compra", CopyMood.BURN, { it.lastExpenseSlug == "boludeces" && it.lastExpenseLucas >= 10 }) { "Alta compra al pedo." },
        CopyTemplate("tv_jugador_delivery", CopyMood.BURN, { it.topCategorySlug == "delivery" }) { "Qué jugador el delivery." },
        CopyTemplate("tv_todo_mal", CopyMood.ALARM, { (it.pctOfMonthlyGoal ?: 0.0) >= 1.15 }) { "Todo mal salió." },
        CopyTemplate("tv_banco_mira", CopyMood.BURN, { it.totalLucas >= 180 }) { "El banco te está mirando raro 🏦" },
        CopyTemplate("tv_mucha_confianza", CopyMood.BURN, { it.lastExpenseLucas >= 30 }) { "Gastaste con mucha confianza." },
        CopyTemplate("tv_safaste_impulso", CopyMood.CHEER, { it.daySpentLucas == 0 && it.expenseCount >= 2 }) { "Hoy safaste de una compra impulsiva." },
        CopyTemplate("tv_billetera_basta", CopyMood.ALARM, { it.totalLucas >= 200 }) { "La billetera: 'hasta acá llegué'." },
        CopyTemplate("tv_ansiedad_otra_vez", CopyMood.BURN, { it.lastExpenseSlug == "boludeces" }) { "Compraste por ansiedad. Otra vez." },
        CopyTemplate("tv_escalo_rapido", CopyMood.ALARM, { it.lastExpenseLucas >= 40 }) { "Eso escaló rapidísimo." },
        CopyTemplate("tv_prime_time", CopyMood.BURN, { it.daySpentLucas >= 20 }) { "Nivel de gasto: prime time." },
        CopyTemplate("tv_en_vivo", CopyMood.NEUTRAL, { it.expenseCount >= 5 }) { "Economía en vivo y en directo." },
        CopyTemplate("tv_sponsor", CopyMood.ALARM, { (it.pctOfMonthlyGoal ?: 0.0) >= 1.0 }) { "Tu cuenta necesita un sponsor." },
        CopyTemplate("tv_show_estabilidad", CopyMood.BURN, { it.totalLucas >= 150 }) { "Mucho show, poca estabilidad." },
        CopyTemplate("tv_cine", CopyMood.NEUTRAL, { it.lastExpenseSlug == "salidas" }) { "Ese gasto fue cine 🚬" },

        // ====== 😂 Memes argentinos ======
        CopyTemplate("meme_sin_pruebas", CopyMood.BURN, { it.totalLucas >= 100 }) { "No tengo pruebas pero tampoco ahorros." },
        CopyTemplate("meme_plata_delivery", CopyMood.BURN, { it.topCategorySlug == "delivery" }) { "La plata no desaparece… se pide delivery." },
        CopyTemplate("meme_no_era_cafe", CopyMood.NEUTRAL, { it.expenseCount >= 10 }) { "El problema no era el café diario." },
        CopyTemplate("meme_no_se_compra", CopyMood.CHEER, { it.daySpentLucas == 0 }) { "Hoy no se compra nada y listo." },
        CopyTemplate("meme_modo_rata", CopyMood.CHEER, { it.daySpentLucas == 0 && it.expenseCount >= 3 }) { "Modo rata supervivencia activado 🐀" },
        CopyTemplate("meme_se_gasto_solo", CopyMood.BURN, { it.lastExpenseLucas >= 20 }) { "Se gastó solo 😔" },
        CopyTemplate("meme_delirio", CopyMood.ALARM, { it.lastExpenseSlug == "boludeces" && it.lastExpenseLucas >= 15 }) { "La app detectó un delirio financiero." },
        CopyTemplate("meme_otro_gustito", CopyMood.BURN, { it.lastExpenseSlug == "salidas" }) { "Otro gustito y dormís en el sillón." },
        CopyTemplate("meme_cuidados_intensivos", CopyMood.ALARM, { it.totalLucas >= 200 }) { "Tu billetera está en cuidados intensivos 🏥" },
        CopyTemplate("meme_no_era_necesidad", CopyMood.BURN, { it.lastExpenseSlug == "boludeces" }) { "No era necesidad, era dopamina." },
        CopyTemplate("meme_economia_fulera", CopyMood.BURN, { (it.pctOfMonthlyGoal ?: 0.0) >= 0.95 }) { "La economía personal está fulera." },
        CopyTemplate("meme_porque_si", CopyMood.BURN, { it.lastExpenseSlug == "boludeces" }) { "Compraste porque sí." },
        CopyTemplate("meme_yo_futuro_odia", CopyMood.BURN, { it.lastExpenseLucas >= 25 }) { "Tu yo del futuro odia esta compra." },
        CopyTemplate("meme_ansiedad_tarjeta", CopyMood.BURN, { it.lastExpenseSlug == "boludeces" }) { "La ansiedad manejó la tarjeta 💳" },
        CopyTemplate("meme_saldo_fe", CopyMood.ALARM, { (it.pctOfMonthlyGoal ?: 0.0) >= 1.0 }) { "Saldo disponible: fe." },
        CopyTemplate("meme_objetivo_sobrevivir", CopyMood.NEUTRAL, { it.dayOfMonth >= 20 && (it.pctOfMonthlyGoal ?: 0.0) >= 0.7 }) { "Objetivo del mes: sobrevivir." },
        CopyTemplate("meme_homebanking_cansado", CopyMood.ALARM, { it.totalLucas >= 200 }) { "Tu homebanking está cansado." },
        CopyTemplate("meme_cocinaste_orgullo", CopyMood.CHEER, { it.daySpentLucas == 0 && it.expenseCount >= 1 }) { "Hoy cocinaste en casa: orgullo nacional." },
        CopyTemplate("meme_descuento_manipulo", CopyMood.NEUTRAL, { it.lastExpenseSlug == "super" || it.lastExpenseSlug == "boludeces" }) { "Ese descuento te manipuló." },
        CopyTemplate("meme_promo_gano", CopyMood.NEUTRAL, { it.lastExpenseSlug == "super" }) { "La promo te ganó psicológicamente." },

        // ====== 🧉 Estilo barrio / amigo ======
        CopyTemplate("barrio_bajale_cambio", CopyMood.BURN, { it.daySpentLucas >= 20 }) { "Amigo… bajale un cambio." },
        CopyTemplate("barrio_pasto", CopyMood.BURN, { (it.pctOfMonthlyGoal ?: 0.0) >= 1.0 }) { "Te estás yendo al pasto." },
        CopyTemplate("barrio_guarda_mangos", CopyMood.NEUTRAL, { it.dayOfMonth in 5..18 && it.totalLucas in 30..150 }) { "Guardá unos mangos, rey." },
        CopyTemplate("barrio_no_llegamos", CopyMood.BURN, { it.dayOfMonth >= 22 && (it.pctOfMonthlyGoal ?: 0.0) >= 0.85 }) { "Así no llegamos al finde." },
        CopyTemplate("barrio_no_delivery", CopyMood.BURN, { it.topCategorySlug == "delivery" && it.topCategoryLucas >= 25 }) { "No podés vivir a delivery." },
        CopyTemplate("barrio_apreta_cinturon", CopyMood.BURN, { (it.pctOfMonthlyGoal ?: 0.0) >= 0.9 }) { "Apretá el cinturón, titan." },
        CopyTemplate("barrio_lindo_resumen", CopyMood.BURN, { it.totalLucas >= 150 }) { "Todo lindo hasta el resumen." },
        CopyTemplate("barrio_estuviste_solido", CopyMood.CHEER, { it.daySpentLucas == 0 && it.expenseCount >= 1 }) { "Hoy estuviste sólido 👌" },
        CopyTemplate("barrio_no_quemes_biyu", CopyMood.BURN, { it.daySpentLucas >= 15 }) { "No quemes la biyu." },
        CopyTemplate("barrio_controla_manija", CopyMood.BURN, { it.expenseCount >= 12 }) { "Controlá la manija 🎰" },
        CopyTemplate("barrio_fideos", CopyMood.NEUTRAL, { (it.pctOfMonthlyGoal ?: 0.0) >= 0.95 }) { "Ya fue, cocinate unos fideos 🍝" },
        CopyTemplate("barrio_rockefeller", CopyMood.BURN, { it.lastExpenseLucas >= 30 }) { "Tranqui Rockefeller 🎩" },
        CopyTemplate("barrio_vibra", CopyMood.BURN, { it.lastExpenseSlug == "boludeces" }) { "Te fundiste por vibra." },
        CopyTemplate("barrio_un_fangote", CopyMood.BURN, { it.lastExpenseLucas >= 35 }) { "Eso salió un fangote." },
        CopyTemplate("barrio_no_era_por_ahi", CopyMood.NEUTRAL, { it.lastExpenseSlug == "boludeces" }) { "No era por ahí." },
        CopyTemplate("barrio_fua_tremendo", CopyMood.ALARM, { it.lastExpenseLucas >= 50 }) { "Fua amigo, tremendo gasto." },
        CopyTemplate("barrio_cobras_euros", CopyMood.BURN, { it.totalLucas >= 200 }) { "Compraste como si cobrabas en euros." },
        CopyTemplate("barrio_administraste_bien", CopyMood.CHEER, { it.daySpentLucas == 0 && (it.pctOfMonthlyGoal ?: 1.0) <= 0.6 }) { "Hoy administraste bien." },
        CopyTemplate("barrio_pilotando", CopyMood.CHEER, { (it.pctOfMonthlyGoal ?: 1.0) <= 0.65 && it.dayOfMonth >= 15 }) { "La estás piloteando." },
        CopyTemplate("barrio_seco_humedad", CopyMood.ALARM, { (it.pctOfMonthlyGoal ?: 0.0) >= 1.1 }) { "Quedaste seco más rápido que la humedad." },

        // ====== 🔥 Behavior buckets ======
        // Si ahorró
        CopyTemplate("behavior_masterclass", CopyMood.CHEER, { it.daySpentLucas == 0 && it.streakDaysProxy() >= 2 }) { "Masterclass de estabilidad financiera 👏" },
        CopyTemplate("behavior_esquivaste_falopa", CopyMood.CHEER, { it.daySpentLucas == 0 && it.expenseCount >= 3 }) { "Hoy esquivaste una compra falopa." },
        CopyTemplate("behavior_3_dias", CopyMood.CHEER, { it.daySpentLucas == 0 && it.streakDaysProxy() >= 3 }) { "3 días sin delirios económicos 🔥" },
        CopyTemplate("behavior_recuperando_control", CopyMood.CHEER, { it.daySpentLucas == 0 && (it.pctOfMonthlyGoal ?: 1.0) <= 0.7 }) { "Estás recuperando el control." },
        CopyTemplate("behavior_modo_responsable", CopyMood.CHEER, { it.daySpentLucas == 0 && it.expenseCount >= 5 }) { "Modo responsable desbloqueado 🎯" },
        // Si gastó mucho
        CopyTemplate("behavior_para_mano", CopyMood.ALARM, { it.daySpentLucas >= 30 }) { "Pará la mano 🛑" },
        CopyTemplate("behavior_eso_dolio", CopyMood.BURN, { it.lastExpenseLucas >= 40 }) { "Eso dolió." },
        CopyTemplate("behavior_pidiendo_hielo", CopyMood.ALARM, { it.totalLucas >= 200 }) { "La tarjeta quedó pidiendo hielo 🧊" },
        CopyTemplate("behavior_no_show", CopyMood.BURN, { it.lastExpenseLucas >= 30 }) { "No hacía falta tanto show." },
        CopyTemplate("behavior_aguinaldo", CopyMood.ALARM, { it.daySpentLucas >= 40 }) { "Gastaste como si fuera aguinaldo." },
        // Si pidió delivery
        CopyTemplate("behavior_delivery_saludos", CopyMood.BURN, { it.lastExpenseSlug == "delivery" }) { "El delivery ya te manda saludos 👋" },
        CopyTemplate("behavior_pedidosya_conoce", CopyMood.BURN, { it.topCategorySlug == "delivery" && it.topCategoryLucas >= 25 }) { "PedidosYa te conoce demasiado." },
        CopyTemplate("behavior_gano_fiaca", CopyMood.NEUTRAL, { it.lastExpenseSlug == "delivery" }) { "Hoy ganó la fiaca." },
        CopyTemplate("behavior_combo_caro", CopyMood.BURN, { it.lastExpenseSlug == "delivery" && it.lastExpenseLucas >= 15 }) { "Ese combo salió caro, campeón." },
        CopyTemplate("behavior_cocina_arroz", CopyMood.NEUTRAL, { it.topCategorySlug == "delivery" && it.topCategoryLucas >= 30 }) { "Cociná aunque sea arroz 😄" },
        // Si cumple metas
        CopyTemplate("behavior_ascendiste", CopyMood.CHEER, { (it.pctOfMonthlyGoal ?: 1.0) <= 0.5 && it.dayOfMonth >= 20 }) { "Ascendiste de categoría financiera 🏆" },
        CopyTemplate("behavior_semana_historica", CopyMood.CHEER, { it.daySpentLucas == 0 && it.streakDaysProxy() >= 5 }) { "Semana histórica." },
        CopyTemplate("behavior_economia_cine", CopyMood.CHEER, { (it.pctOfMonthlyGoal ?: 1.0) <= 0.6 && it.dayOfMonth >= 15 }) { "Economía ordenada. Cine 🎬" },
        CopyTemplate("behavior_titular_indiscutido", CopyMood.CHEER, { it.daySpentLucas == 0 && it.expenseCount >= 5 }) { "Hoy sos titular indiscutido." },
        CopyTemplate("behavior_yo_futuro_festeja", CopyMood.CHEER, { (it.pctOfMonthlyGoal ?: 1.0) <= 0.55 && it.dayOfMonth >= 20 }) { "Tu yo del futuro festeja 🎉" },
    )

    /** Heurística: contar como "racha" si hay varios gastos pero hoy no se gastó. */
    private fun CopyContext.streakDaysProxy(): Int =
        if (daySpentLucas == 0 && expenseCount >= 3) (expenseCount / 3).coerceAtMost(7) else 0

    /** True si el usuario viene gastando despacio (pocos lucas en muchos días). */
    private fun CopyContext.streakCheap(): Boolean =
        expenseCount >= 4 && totalLucas <= 80

    private val templateById = templates.associateBy { it.id }

    fun candidates(ctx: CopyContext): List<CopyTemplate> = templates.filter { it.matches(ctx) }

    fun byId(id: String): CopyTemplate? = templateById[id]

    fun moodOf(id: String): CopyMood = templateById[id]?.mood ?: CopyMood.NEUTRAL

    /**
     * Convierte (up, down) en un peso. El neto positivo aumenta exponencialmente
     * la probabilidad; el negativo la baja, con un piso para no eliminar plantillas.
     */
    fun weightForVotes(up: Int, down: Int): Double {
        val net = (up - down).coerceIn(-3, 5)
        return 2.0.pow(net.toDouble())
    }

    fun pickWeighted(
        ctx: CopyContext,
        weights: Map<String, Double>,
        rng: Random = Random.Default,
    ): CopyChoice {
        val cands = candidates(ctx)
        if (cands.isEmpty()) {
            return CopyChoice(
                templateId = "fallback",
                text = "Tu plata, tus reglas. Acá la app mira y aprende.",
                mood = CopyMood.NEUTRAL,
            )
        }
        val w = cands.map { (weights[it.id] ?: 1.0).coerceAtLeast(0.0) }
        val total = w.sum()
        val chosen = if (total <= 0.0) {
            cands.random(rng)
        } else {
            var r = rng.nextDouble() * total
            var pickIdx = cands.lastIndex
            for (i in cands.indices) {
                r -= w[i]
                if (r <= 0.0) {
                    pickIdx = i
                    break
                }
            }
            cands[pickIdx]
        }
        return CopyChoice(templateId = chosen.id, text = chosen.render(ctx), mood = chosen.mood)
    }

    /** Compatibilidad: elige sin pesos, solo random uniforme. */
    fun pickMessage(ctx: CopyContext, rng: Random = Random.Default): String =
        pickWeighted(ctx, emptyMap(), rng).text

    /**
     * Mezcla la frase universal con un mood y lo "tropicaliza" con una frase
     * específica de la personalidad elegida. La idea es que la mitad de las veces
     * gane el persona pack y la otra mitad la frase universal del template.
     */
    fun pickWithPersona(
        ctx: CopyContext,
        weights: Map<String, Double>,
        personaKey: String,
        rng: Random = Random.Default,
    ): CopyChoice {
        val universal = pickWeighted(ctx, weights, rng)
        val pack = PersonaCopyPacks.pool(personaKey, universal.mood, ctx)
        if (pack.isEmpty()) return universal
        // 65% de las veces gana la frase del persona, para que se sienta "el personaje hablando".
        val usePersona = rng.nextDouble() < 0.65
        if (!usePersona) return universal
        return CopyChoice(
            templateId = "persona_${personaKey}_${universal.mood.name.lowercase()}",
            text = pack.random(rng),
            mood = universal.mood,
        )
    }
}
