package com.antigastos.boludos.domain

import kotlin.random.Random

/**
 * Pone "voz" a un [Consejo] usando una de las personalidades del catálogo.
 *
 * Funciona 100% offline: cada personalidad aporta una *intro* corta (saludo +
 * coletilla) que se antepone al body. Cuando la IA está habilitada, el
 * `HomeViewModel` reemplaza el body con la versión generada por el modelo
 * usando la misma `personaKey` — pero el voicer asegura que SIEMPRE haya algo
 * con sabor, sin pegar a la API.
 */
object ConsejoVoicer {

    /**
     * Devuelve el [Consejo] re-escrito con el saludo de [persona]. No vuelve a
     * envolver el body si ya tiene saludo (idempotente para misma persona).
     */
    fun voice(consejo: Consejo, persona: Persona, rng: Random = Random.Default): Consejo {
        val intro = pickIntro(persona, consejo.kind, rng)
        val newBody = "$intro ${consejo.body.trim()}".trim()
        return consejo.copy(
            body = newBody,
            personaKey = persona.key,
            personaEmoji = persona.emoji,
            personaName = persona.displayName,
        )
    }

    /** Saludo + coletilla por personaje. Se elige uno random. */
    private fun pickIntro(persona: Persona, kind: ConsejoKind, rng: Random): String {
        val pool = when (persona.key) {
            "termo" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("Mate caliente y orgullo:", "Bien ahí, salió cebada perfecta:")
                ConsejoKind.SUGERENCIA -> listOf("Mientras se calienta el agua, escuchá:", "Mate de testigo:")
                ConsejoKind.FRENO -> listOf("Frenamos un cebado:", "Antes del próximo mate, leé:")
                ConsejoKind.EMERGENCIA -> listOf("Apagá el termo, esto es serio:", "Sin mate hasta que escuches esto:")
            }
            "tachero" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("Te aviso, hermano:", "Te lo digo manejando, eh:")
                ConsejoKind.SUGERENCIA -> listOf("Mientras esperamos el semáforo:", "Te lo digo yo, hermano:")
                ConsejoKind.FRENO -> listOf("Frenazo de tachero:", "Bajá un cambio, hermano:")
                ConsejoKind.EMERGENCIA -> listOf("Bajamos a los gritos:", "Te llevo de urgencia:")
            }
            "abuela" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("Mi nene/a, qué orgullosa estoy:", "Vení que te doy un beso:")
                ConsejoKind.SUGERENCIA -> listOf("Sentate, escuchame bien:", "Te dejo el flan y te digo:")
                ConsejoKind.FRENO -> listOf("Pará la mano, mocoso/a:", "No me hagas enojar:")
                ConsejoKind.EMERGENCIA -> listOf("Ay nene/a, qué hacés:", "Esto a mí no me gusta:")
            }
            "jefe" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("Buen trabajo, equipo:", "Merece reconocimiento:")
                ConsejoKind.SUGERENCIA -> listOf("Sin vueltas, tomá nota:", "Mirá, de jefe a jefe:")
                ConsejoKind.FRENO -> listOf("Frenamos eso ya:", "No, así no se hace:")
                ConsejoKind.EMERGENCIA -> listOf("Reunión urgente:", "Esto es código rojo:")
            }
            "comentarista" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("¡Y se la lleva al ángulo!:", "Goooool de la administración:")
                ConsejoKind.SUGERENCIA -> listOf("Atención al pelotazo:", "Pelota al medio:")
                ConsejoKind.FRENO -> listOf("¡Falta clarísima!:", "Tarjeta amarilla:")
                ConsejoKind.EMERGENCIA -> listOf("¡Roja directa!:", "VAR, VAR, VAR:")
            }
            "fisura" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("Eh loco posta:", "Me la robaste, capo:")
                ConsejoKind.SUGERENCIA -> listOf("Loco, escuchame:", "Te tiro una posta:")
                ConsejoKind.FRENO -> listOf("Pará loco, pará:", "Ehh fisura, fisura:")
                ConsejoKind.EMERGENCIA -> listOf("Loco la cagaste:", "Esto no, posta no:")
            }
            "streamer" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("Atención chat, GG:", "Pog, capo, pog:")
                ConsejoKind.SUGERENCIA -> listOf("Chat, atención:", "Va con donate:")
                ConsejoKind.FRENO -> listOf("Eh chat, vamos al replay:", "Stop stop stop:")
                ConsejoKind.EMERGENCIA -> listOf("CHAT EMERGENCIA:", "Modo subscriber-only:")
            }
            "madre" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("Te quiero mucho, mi amor:", "Hijito/a, qué orgullo:")
                ConsejoKind.SUGERENCIA -> listOf("Hijito/a, escuchá:", "Vení, sentate:")
                ConsejoKind.FRENO -> listOf("Pará un poquito:", "Por dios, pará:")
                ConsejoKind.EMERGENCIA -> listOf("Hijito/a, qué hiciste:", "Voy a tu casa, esperame:")
            }
            "motoquero" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("Casco arriba:", "Ruta libre, capo:")
                ConsejoKind.SUGERENCIA -> listOf("Cuidate en la curva:", "Frenada técnica:")
                ConsejoKind.FRENO -> listOf("Bajá la velocidad:", "Te vas de cola:")
                ConsejoKind.EMERGENCIA -> listOf("Casco roto, frená YA:", "Te vas a comer el cordón:")
            }
            "kiosquero" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("Vecino/a, hoy te ganaste un Mantecol:", "Bien ahí, casero:")
                ConsejoKind.SUGERENCIA -> listOf("Vecino/a, vení:", "Te lo tiro al pasar:")
                ConsejoKind.FRENO -> listOf("Pará, vecino/a:", "Te corto el fiado:")
                ConsejoKind.EMERGENCIA -> listOf("No te fío más:", "Vecino/a, basta:")
            }
            "psicologo" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("Me gusta lo que escucho:", "Acompañemos esto:")
                ConsejoKind.SUGERENCIA -> listOf("Respiremos un segundo:", "Tomemos perspectiva:")
                ConsejoKind.FRENO -> listOf("Ahí pisaste un patrón:", "Notemos lo que pasa:")
                ConsejoKind.EMERGENCIA -> listOf("Esto requiere atención:", "Sentémonos un rato:")
            }
            "cunadocripto" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("Hold mode on:", "We are early, capo:")
                ConsejoKind.SUGERENCIA -> listOf("Cuñado modo on:", "Te tiro una alpha:")
                ConsejoKind.FRENO -> listOf("FUD, no escuches:", "Sell the news, hermano:")
                ConsejoKind.EMERGENCIA -> listOf("Liquidación inminente:", "Margin call, hermano:")
            }
            "profegym" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("Foco hermano, así se hace:", "Repetición perfecta:")
                ConsejoKind.SUGERENCIA -> listOf("Vamos a entrenar la guita:", "Foco, hermano:")
                ConsejoKind.FRENO -> listOf("Pará la serie:", "Mala técnica, te lesionás:")
                ConsejoKind.EMERGENCIA -> listOf("Lesión grave:", "Para de levantar peso, hermano:")
            }
            "cheta" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("Tipo, hello, qué divino:", "Re, te re envidio:")
                ConsejoKind.SUGERENCIA -> listOf("Eh, tipo:", "Tipo amor, escuchá:")
                ConsejoKind.FRENO -> listOf("Tipo no, no, no:", "Eh boluda/o, no:")
                ConsejoKind.EMERGENCIA -> listOf("Tipo emergencia, OMG:", "Eh, qué desastre, tipo:")
            }
            "trapero" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("Real talk uoh:", "Money money:")
                ConsejoKind.SUGERENCIA -> listOf("Yeah, escuchá:", "Trap & finance, uo:")
                ConsejoKind.FRENO -> listOf("Pará rapero:", "Stop the beat:")
                ConsejoKind.EMERGENCIA -> listOf("Game over:", "Bad bunny moment:")
            }
            "vidente" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("Las cartas brillan:", "El sol del tarot:")
                ConsejoKind.SUGERENCIA -> listOf("Las cartas dicen:", "Veo en la bola:")
                ConsejoKind.FRENO -> listOf("La luna negra advierte:", "Las cartas tiemblan:")
                ConsejoKind.EMERGENCIA -> listOf("La torre invertida:", "Energía oscura, cuidado:")
            }
            "predicador" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("¡Hermano/a, aleluya!:", "¡Bendito sea!:")
                ConsejoKind.SUGERENCIA -> listOf("Hermanos, escuchen:", "El versículo dice:")
                ConsejoKind.FRENO -> listOf("¡Arrepiéntete, hermano!:", "Hermano, peligro:")
                ConsejoKind.EMERGENCIA -> listOf("¡Apocalipsis financiero!:", "¡Hermano, hay que confesar!:")
            }
            "profe" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("Excelente, alumno/a:", "10 felicitado:")
                ConsejoKind.SUGERENCIA -> listOf("Atención, anoten:", "Para la prueba:")
                ConsejoKind.FRENO -> listOf("Llamada de atención:", "A dirección, alumno/a:")
                ConsejoKind.EMERGENCIA -> listOf("Sanción disciplinaria:", "Citamos al apoderado:")
            }
            "vendedorseguros" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("Para tu tranquilidad:", "Cliente preferido:")
                ConsejoKind.SUGERENCIA -> listOf("Le cuento:", "Mire qué interesante:")
                ConsejoKind.FRENO -> listOf("Atención al riesgo:", "Hay un siniestro en camino:")
                ConsejoKind.EMERGENCIA -> listOf("Siniestro total:", "Hay que activar la póliza:")
            }
            "subteman" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("Próxima estación: ahorro:", "Combinación con éxito:")
                ConsejoKind.SUGERENCIA -> listOf("Atención al andén:", "Próxima estación: tip:")
                ConsejoKind.FRENO -> listOf("Cierre de puertas:", "Servicio limitado, atención:")
                ConsejoKind.EMERGENCIA -> listOf("Servicio interrumpido:", "Atención: descarrilaste:")
            }
            "cumbiero" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("Hermanito, ahí va:", "Mi sangre, escuchá:")
                ConsejoKind.SUGERENCIA -> listOf("Negro de mi alma:", "Hermanito, pará la oreja:")
                ConsejoKind.FRENO -> listOf("Mi sangre, frená:", "Negro, pará la cumbia:")
                ConsejoKind.EMERGENCIA -> listOf("Cumbia triste, hermanito:", "Mi sangre, esto va mal:")
            }
            "carnicero" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("Vecino/a, te aplaudo:", "Bien ahí, te guardo carnaza:")
                ConsejoKind.SUGERENCIA -> listOf("Vení, vecino/a:", "Te tiro una en confianza:")
                ConsejoKind.FRENO -> listOf("Vecino/a, frená:", "Te corto el fiado:")
                ConsejoKind.EMERGENCIA -> listOf("Vecino/a, cuidado:", "No te puedo seguir fiando:")
            }
            "mecanico" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("Bien afinado, capo:", "Motor punto a punto:")
                ConsejoKind.SUGERENCIA -> listOf("Vecino/a, mirá:", "Hacele service vos:")
                ConsejoKind.FRENO -> listOf("Frená el auto:", "Pará el motor:")
                ConsejoKind.EMERGENCIA -> listOf("Motor fundido:", "Auxilio mecánico:")
            }
            "gato" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("Ronroneo:", "Miau aprobatorio:")
                ConsejoKind.SUGERENCIA -> listOf("Miau:", "Te observo, biped:")
                ConsejoKind.FRENO -> listOf("Bufido:", "Maullido de queja:")
                ConsejoKind.EMERGENCIA -> listOf("Bufido grave:", "Sssst, humano:")
            }
            "travesti" -> when (kind) {
                ConsejoKind.PREMIO -> listOf("Amor mío, te aplaudo:", "Bombón, divina/o:")
                ConsejoKind.SUGERENCIA -> listOf("Amor mío, escuchá:", "Reina, te tiro:")
                ConsejoKind.FRENO -> listOf("Amor, paremos:", "Tesoro, NO:")
                ConsejoKind.EMERGENCIA -> listOf("Amor, esto es CAOS:", "Reina mía, parate:")
            }
            "politico" -> when (kind) {
                ConsejoKind.PREMIO -> listOf(
                    "Compañero, yo siempre confié en usted (pero la foto es mía):",
                    "Querido vecino, este logro es de todos, especialmente mío:",
                )
                ConsejoKind.SUGERENCIA -> listOf(
                    "Le prometo lo siguiente (probablemente no lo cumpla):",
                    "Compañero, le pido un voto de confianza:",
                )
                ConsejoKind.FRENO -> listOf(
                    "Convocamos a sesión extraordinaria (sin quórum):",
                    "Decretamos paquete de medidas (ninguna se va a aplicar):",
                )
                ConsejoKind.EMERGENCIA -> listOf(
                    "Heredamos esta crisis del anterior:",
                    "Estado de emergencia económica (en su billetera):",
                )
            }
            "diez" -> when (kind) {
                ConsejoKind.PREMIO -> listOf(
                    "Bien ahí, papá:",
                    "Se siente, hermano:",
                    "Vamos vamos vamos:",
                )
                ConsejoKind.SUGERENCIA -> listOf(
                    "Tranquilo, papá:",
                    "Escuchame, hermano:",
                    "Andá pa' allá bobo, pero antes:",
                )
                ConsejoKind.FRENO -> listOf(
                    "Pará la pelota, papá:",
                    "Andá pa' allá, bobo:",
                    "Tranquilo, despejá:",
                )
                ConsejoKind.EMERGENCIA -> listOf(
                    "Muchachos, hay que cerrar el partido:",
                    "Se siente, hermano, pero esto es serio:",
                    "Pará todo, papá:",
                )
            }
            "geniopotrero" -> when (kind) {
                ConsejoKind.PREMIO -> listOf(
                    "CHE mirá lo que hiciste, hermano:",
                    "Gracias a Dios y al esfuerzo:",
                    "Te abrazo, papá:",
                )
                ConsejoKind.SUGERENCIA -> listOf(
                    "Escuchá un segundo desde el potrero:",
                    "La zurda del mes te dice:",
                    "Ni crack ni santo, pero mirá:",
                )
                ConsejoKind.FRENO -> listOf(
                    "HERMANO frená el carro:",
                    "Te lo digo con el corazón:",
                    "Eso no fue gambeta, fue patada:",
                )
                ConsejoKind.EMERGENCIA -> listOf(
                    "CHE esto está jugado mal:",
                    "Sin árbitro que te salve:",
                    "Del arrabal te gritan:",
                )
            }
            else -> listOf("Mirá:", "Escuchá:")
        }
        return pool.random(rng)
    }
}
