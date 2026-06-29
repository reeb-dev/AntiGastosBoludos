package com.antigastos.boludos.domain

import kotlin.random.Random

/**
 * Diccionario rioplatense de plata. Sirve para que la app no repita "lucas/pesos"
 * hasta el cansancio: rotamos labels, sinónimos, frases hechas y micro-quips.
 *
 * Cada función acepta un [Random] o, en su defecto, deriva un seed estable
 * por día (o por persona) para que la "rotación" se sienta variada pero no
 * caótica.
 */
object MoneySlang {

    // === Pool genérico de sinónimos para "plata" ===
    private val NOUNS_GENERIC = listOf(
        "guita",
        "plata",
        "biyuya",
        "biyu",
        "mangos",
        "morlacos",
        "tarasca",
        "teca",
        "mosca",
        "billete",
        "pasta",
        "tela",
        "fangote",
        "billetín",
        "lucas",
        "crocantes",
        "papiros",
        "cobres",
        "rupias",
        "guanacos",
        "moneda",
        "viyuya",
        "sope",
    )

    private val ADJECTIVES = listOf(
        "fresca",
        "dulce",
        "rápida",
        "salvadora",
        "del mes",
        "del finde",
        "crocante",
        "tibia",
        "bendita",
    )

    /**
     * Slang preferido por persona. Cada personaje tira a un sub-set: la cheta
     * dice "morlacos", el cuñado "verdes", el trapero "biyuya", el político
     * "reservas estratégicas", etc. Esto matchea el tono de cada uno y le baja
     * la previsibilidad al copy.
     */
    private val NOUNS_BY_PERSONA: Map<String, List<String>> = mapOf(
        "termo" to listOf("guita", "lucas", "mangos", "biyu"),
        "tachero" to listOf("guita", "mangos", "morlacos", "viyuya"),
        "abuela" to listOf("plata", "moneda", "monedas", "billete"),
        "jefe" to listOf("capital", "guita", "lucas", "plata"),
        "comentarista" to listOf("lucas", "morlacos", "biyu", "papiros"),
        "fisura" to listOf("biyu", "guita", "mosca", "mangos"),
        "streamer" to listOf("créditos", "rupias", "lucas", "biyu"),
        "madre" to listOf("monedas", "plata", "billete", "guita"),
        "motoquero" to listOf("lucas", "mangos", "biyu", "tarasca"),
        "kiosquero" to listOf("guita", "moneda", "mangos", "fiado"),
        "psicologo" to listOf("plata", "capital simbólico", "guita", "mangos"),
        "cunadocripto" to listOf("verdes", "stables", "lucas", "papiros"),
        "profegym" to listOf("reps", "biyu", "lucas", "mangos"),
        "cheta" to listOf("morlacos", "verdes", "billete", "plata"),
        "trapero" to listOf("biyuya", "crocantes", "mangos", "lucas"),
        "vidente" to listOf("monedas", "papiros", "biyuya", "energía"),
        "predicador" to listOf("ofrenda", "diezmo", "guita", "billete"),
        "profe" to listOf("plata", "moneda", "lucas", "guita"),
        "vendedorseguros" to listOf("capital", "plata", "morlacos", "billete"),
        "subteman" to listOf("moneda", "tarjeta", "plata", "lucas"),
        "cumbiero" to listOf("biyu", "guita", "mangos", "moneda"),
        "carnicero" to listOf("mangos", "moneda", "guita", "lucas"),
        "mecanico" to listOf("mangos", "guita", "biyu", "lucas"),
        "gato" to listOf("atún", "lucas", "moneda", "biyu"),
        "travesti" to listOf("morlacos", "billete", "biyu", "guita"),
        "politico" to listOf("reservas", "fondos", "partidas", "morlacos"),
    )

    /** Sinónimo aleatorio de "plata". */
    fun noun(rng: Random = Random.Default): String = NOUNS_GENERIC.random(rng)

    /** Adjetivo lunfa para acompañar la guita. */
    fun adjective(rng: Random = Random.Default): String = ADJECTIVES.random(rng)

    /**
     * Sinónimo a usar pegado al número en headers/heros. Si no hay [personaKey]
     * o no se reconoce, cae a [NOUNS_GENERIC]. El [seed] se usa para que el
     * mismo render en el mismo "día/sesión" no parpadee distinto entre recomp.
     */
    fun nounForPersona(personaKey: String?, seed: Long = 0L): String {
        val pool = NOUNS_BY_PERSONA[personaKey] ?: NOUNS_GENERIC
        val rng = Random(seed.takeIf { it != 0L } ?: System.currentTimeMillis() / (1000 * 60 * 60))
        return pool.random(rng)
    }

    /** Frase completa: "una biyu fresca", "unos morlacos", "un fangote", etc. */
    fun phrase(rng: Random = Random.Default): String {
        val pick = rng.nextInt(0, 6)
        return when (pick) {
            0 -> "un fangote"
            1 -> "unos ${NOUNS_GENERIC.random(rng)}"
            2 -> "la ${NOUNS_GENERIC.random(rng)} ${ADJECTIVES.random(rng)}"
            3 -> NOUNS_GENERIC.random(rng)
            4 -> "una ${listOf("biyu", "guita", "tarasca", "mosca", "moneda").random(rng)}"
            else -> "${NOUNS_GENERIC.random(rng)} ${ADJECTIVES.random(rng)}"
        }
    }

    // === Labels rotativos para reemplazar "Saldo" / "Te queda" ===

    /** Para títulos del tipo "SALDO" en headers. */
    fun balanceLabel(rng: Random = Random.Default): String = listOf(
        "Guita disponible",
        "Morlacos restantes",
        "Caja de ahorro del desastre",
        "Fondos para sobrevivir",
        "Tesoro nacional",
        "Reserva estratégica",
        "Bolsillo en uso",
        "Crocantes en mano",
        "Biyuya en pie",
        "Reserva del aguinaldo",
        "Capital de la patria personal",
    ).random(rng)

    /** Para mostrar el resultado del cálculo "Te queda X". */
    fun remainingLabel(over: Boolean, rng: Random = Random.Default): String = if (over) {
        listOf(
            "Te falta",
            "Estás en rojo por",
            "Debés recuperar",
            "Quedaste corto",
            "Modo rojo:",
        ).random(rng)
    } else {
        listOf(
            "Te queda",
            "Aguanta",
            "Bancás todavía",
            "Reserva del mes:",
            "Tesoro en pie:",
        ).random(rng)
    }

    /** Variante para subtítulo del estado de la billetera. */
    fun walletStateLabel(over: Boolean, broke: Boolean, rng: Random = Random.Default): String = when {
        over && broke -> listOf(
            "Billetera detonada",
            "Economía personal en terapia",
            "Billetera pidiendo licencia psiquiátrica",
            "Tarjeta llorando en arameo",
        ).random(rng)
        over -> listOf(
            "Billetera caliente",
            "Tarjeta exhausta",
            "Caja en rojo",
            "Reservas tocando fondo",
        ).random(rng)
        broke -> listOf(
            "Estás seco",
            "No tenés un mango",
            "Andas crocante",
            "Tenés dos pesos",
        ).random(rng)
        else -> listOf(
            "Billetera estable",
            "Bolsillo respirando",
            "Tesoro nacional sano",
            "Modo ahorro activo",
        ).random(rng)
    }

    // === Frases hechas (panels, recap, share, easter eggs) ===

    /** Frases típicas argentinas listas para usar. Las uso en empty states / banners. */
    fun broughtToYouQuip(rng: Random = Random.Default): String = listOf(
        "No tengo un mango.",
        "Estoy seco.",
        "Me patiné toda la guita.",
        "Está saladísimo.",
        "Me arrancaron la cabeza.",
        "Quedé en bolas.",
        "No llego a fin de mes.",
        "Tengo dos pesos.",
        "Quemé la tarjeta.",
        "Ando crocante.",
        "Estoy modo ahorro.",
        "Reventé la billetera.",
        "La biyuya no alcanza.",
        "Ando ratoneando.",
        "Se fue todo al carajo.",
    ).random(rng)

    /**
     * Quip estilo "te fumaste 40 lucas en delivery", recibe el monto en lucas
     * y, opcionalmente, la categoría top para personalizar.
     */
    fun spentQuip(lucas: Long, topCategory: String? = null, rng: Random = Random.Default): String {
        if (lucas <= 0) return "Mes virgen, todavía no tocaste un mango."
        val cat = topCategory?.takeIf { it.isNotBlank() } ?: "boludeces"
        return listOf(
            "Te fumaste $lucas lucas en $cat.",
            "Volaron $lucas lucas en $cat. Salud.",
            "Se patinaron $lucas lucas en $cat.",
            "$lucas lucas perdidas en $cat — recibo no, lágrima sí.",
            "Con $lucas lucas en $cat, en 2001 eras Rockefeller. 😭",
            "Reventaste $lucas lucas en $cat. Aplauso lento.",
            "Tu billetera donó $lucas lucas a $cat. Sin recibo.",
        ).random(rng)
    }

    /** Frase de "nivel de gasto" tipo medidor. */
    fun spendLevelLabel(pct: Double, rng: Random = Random.Default): String = when {
        pct <= 0.4 -> listOf(
            "Nivel rata: orgulloso.",
            "Modo ahorro encendido.",
            "Vas dulce, papu.",
        ).random(rng)
        pct <= 0.85 -> listOf(
            "Nivel ratón: contenido.",
            "Vas raspando con estilo.",
            "Equilibrista financiero.",
        ).random(rng)
        pct <= 1.0 -> listOf(
            "Nivel ratoneo: extremo.",
            "Estás en zona roja.",
            "Raspando el plato.",
        ).random(rng)
        pct <= 1.2 -> listOf(
            "Tu billetera pidió licencia psiquiátrica.",
            "Tarjeta en terapia intensiva.",
            "Modo emergencia 🚨",
        ).random(rng)
        else -> listOf(
            "Reventaste todo. Nivel: leyenda del despilfarro.",
            "Tu yo del futuro armó un grupo de autoayuda por tu culpa.",
            "Con esta guita en 2001 eras Rockefeller. 😭",
        ).random(rng)
    }

    /**
     * Convierte un monto en pesos a una expresión lunfarda variable.
     * Ej:
     *   500     → "500 mangos"
     *   1500    → "1.5 lucas" / "una luca y media"
     *   12000   → "12 lucas" / "12 morlacos en lucas"
     *   1_000_000 → "un palo"
     *   2_000_000 → "dos palos"
     */
    fun amount(pesosLong: Long, rng: Random = Random.Default): String {
        val pesos = pesosLong.coerceAtLeast(0L)
        return when {
            pesos == 0L -> "cero mangos"
            pesos < 1_000L -> {
                val v = pesos.toInt()
                when (v) {
                    100 -> listOf("una gamba", "100 mangos", "una Evita", "un Roca").random(rng)
                    500 -> listOf("una quina", "500 mangos").random(rng)
                    10 -> listOf("un dieguito", "10 mangos").random(rng)
                    else -> "$v ${listOf("mangos", "morlacos", "mosca", "crocantes").random(rng)}"
                }
            }
            pesos < 1_000_000L -> {
                val lucas = (pesos / 1000.0)
                val isWhole = pesos % 1000L == 0L
                val n = if (isWhole) lucas.toInt().toString() else "%.1f".format(lucas)
                listOf(
                    "$n lucas",
                    "$n palos verdes (de los chiquitos)",
                    "$n morlacos en lucas",
                    "$n biyu",
                    "$n crocantes",
                ).random(rng).let { phrase ->
                    if (phrase.startsWith("1 ") && pesos < 1_500L) phrase.replace("lucas", "luca")
                    else phrase
                }
            }
            else -> {
                val palos = pesos / 1_000_000.0
                val isWhole = pesos % 1_000_000L == 0L
                val n = if (isWhole) palos.toInt().toString() else "%.1f".format(palos)
                when (n) {
                    "1" -> listOf("un palo", "un palo verde", "1 palo").random(rng)
                    else -> listOf("$n palos", "$n palos verdes").random(rng)
                }
            }
        }
    }

    /** Verbo coloquial para describir lo que hizo con la plata. */
    fun spentVerb(rng: Random = Random.Default): String =
        listOf(
            "te patinaste",
            "te quemaste",
            "te volaste",
            "te volaron",
            "te fusilaron",
            "tiraste manteca al techo de",
            "reventaste",
            "te arrancaron la cabeza con",
        ).random(rng)

    /** Estado financiero según fracción de meta usada. */
    fun statusForGoalPct(pct: Double, rng: Random = Random.Default): String = when {
        pct <= 0.4 -> listOf(
            "vas dulce",
            "vas con la biyu fresca",
            "vas levantando la biyu",
            "ando crocante (en el buen sentido)",
        ).random(rng)
        pct <= 0.85 -> listOf(
            "vas raspando",
            "vas haciendo magia con la plata",
            "estás haciendo rendir los mangos",
            "ando ratoneando con estilo",
        ).random(rng)
        pct <= 1.0 -> listOf(
            "estás corto",
            "estás ajustado",
            "estás raspando la olla",
            "te queda lo justo y nada más",
        ).random(rng)
        pct <= 1.2 -> listOf(
            "quedaste en rojo",
            "estás hasta las manos 💀",
            "estás seco 🌵",
            "no tenés un mango",
        ).random(rng)
        else -> listOf(
            "reventaste la tarjeta",
            "te patinaste todos los morlacos",
            "rompiste el chanchito y todo 🐷",
            "te arrancaron la cabeza, posta",
        ).random(rng)
    }
}
