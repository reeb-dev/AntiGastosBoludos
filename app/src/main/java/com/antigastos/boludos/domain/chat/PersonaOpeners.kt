package com.antigastos.boludos.domain.chat

import kotlin.random.Random

/**
 * Aperturas y muletillas por personaje para que el motor offline suene
 * distinto aunque el cuerpo del mensaje sea contextual compartido.
 */
internal object PersonaOpeners {

    fun greetOpener(personaKey: String, rng: Random): String =
        pick(personaKey, greetOpeners, rng)

    /** Envuelve un texto factual con la voz del personaje. */
    fun voice(personaKey: String, body: String, intent: ChatIntent, rng: Random): String {
        if (body.isBlank()) return body
        val prefix = when (intent) {
            ChatIntent.GREETING, ChatIntent.THANKS -> pick(personaKey, greetOpeners, rng)
            ChatIntent.COMPLAINT -> pick(personaKey, toughOpeners, rng)
            else -> pick(personaKey, chatOpeners, rng)
        }
        val suffix = when (personaKey) {
            "gato" -> if (rng.nextDouble() < 0.35) " Miau." else ""
            "predicador" -> if (rng.nextDouble() < 0.4) " ¡Aleluya!" else ""
            "comentarista" -> if (rng.nextDouble() < 0.3) " Señores." else ""
            else -> ""
        }
        return "$prefix $body$suffix".replace("  ", " ").trim()
    }

    private fun pick(personaKey: String, map: Map<String, List<String>>, rng: Random): String {
        val options = map[personaKey] ?: map["termo"]!!
        return options.random(rng)
    }

    private val greetOpeners = mapOf(
        "termo" to listOf("Epa maestro,", "Che,", "Acá ando,"),
        "tachero" to listOf("Mirá pibe,", "Escuchame,", "Che boludo,"),
        "abuela" to listOf("Bambino,", "Ay caro mio,", "Ma cosa,"),
        "jefe" to listOf("Pasante,", "Mirá,", "Atención:"),
        "comentarista" to listOf("Señores,", "Atención que arrancamos,", "Desde el estudio,"),
        "fisura" to listOf("Lokooo,", "Hermano,", "Fua che,"),
        "streamer" to listOf("BRO,", "Chat,", "Pog,"),
        "madre" to listOf("Mi amor,", "Hijito,", "Bueno,"),
        "motoquero" to listOf("Hermano,", "Flaco,", "Dale que te cuento,"),
        "kiosquero" to listOf("Vecino,", "Mirá,", "Che,"),
        "psicologo" to listOf("Contame,", "Interesante,", "Veamos,"),
        "cunadocripto" to listOf("Hermano,", "Mirá la posta,", "Te explico,"),
        "profegym" to listOf("Capo,", "Dale,", "Escuchá,"),
        "cheta" to listOf("Ay amor,", "Divino,", "Mirá,"),
        "trapero" to listOf("Hermano,", "Real real,", "Che,"),
        "vidente" to listOf("Mi vida,", "Veo que,", "Las cartas dicen:"),
        "predicador" to listOf("¡HERMANO!", "¡Gloria!", "Escuchá la palabra:"),
        "profe" to listOf("Querido,", "Alumno,", "Atención:"),
        "vendedorseguros" to listOf("Don,", "Doña,", "Le comento,"),
        "subteman" to listOf("Atención pasajeros:", "Próxima estación:", "Señores pasajeros,"),
        "cumbiero" to listOf("Negro de mi alma,", "Hermano,", "Mi sangre,"),
        "carnicero" to listOf("Vecino,", "Mirá,", "Che,"),
        "mecanico" to listOf("Vecino,", "Mirá,", "Te digo,"),
        "gato" to listOf("Miau.", "Humano,", "Desde el sillón:"),
        "travesti" to listOf("Amor mío,", "Bombón,", "Reina,"),
        "politico" to listOf("Compañero,", "Mirá,", "Te prometo,"),
        "diez" to listOf("Tranquilo papá,", "Muchachos,", "Che hermano,"),
        "geniopotrero" to listOf("Hermano,", "Papá,", "Del potrero te digo:"),
    )

    private val chatOpeners = greetOpeners

    private val toughOpeners = mapOf(
        "termo" to listOf("Uh maestro,", "Che, pará,", "Mirá,"),
        "tachero" to listOf("Pibe, en serio,", "Escuchame bien,", "Mirá,"),
        "psicologo" to listOf("Respirá.", "Veamos esto:", "Contame sin drama:"),
        "predicador" to listOf("¡HERMANO ESCUCHÁ!", "¡El Señor te habla!", "¡PARÁ!"),
        "gato" to listOf("Bufido.", "Miau serio:", "Humano, no:"),
    ).let { extra ->
        greetOpeners.mapValues { (k, v) -> extra[k] ?: v }
    }
}
