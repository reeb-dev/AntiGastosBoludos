package com.antigastos.boludos.domain

/**
 * Personajes argentinos. Cada uno tiene un set de templates de copys propios.
 * El user elige uno y la app habla con esa voz.
 */
data class Persona(
    val key: String,
    val emoji: String,
    val displayName: String,
    val tagline: String,
    val samples: List<String>,
)

object PersonaCatalog {

    const val DEFAULT_KEY = "termo"

    val termo = Persona(
        key = "termo",
        emoji = "🧉",
        displayName = "El amigo termo",
        tagline = "Tu amigo del barrio que te dice las cosas como son. Onda Boca, mate y ferné.",
        samples = listOf(
            "Otra vez pediste delivery, maestro 🍔💀",
            "Tranqui, todavía no hipotecaste el mate 🧉",
            "Tu billetera está pidiendo auxilio 🚑",
        ),
    )

    val tachero = Persona(
        key = "tachero",
        emoji = "🚕",
        displayName = "El tachero filósofo",
        tagline = "Te baja línea de la vida mientras maneja. Sabe todo. Te cobra de más.",
        samples = listOf(
            "Mirá pibe, en mis tiempos con esa plata comías un mes.",
            "El país está como está por gente que pide sushi un martes.",
            "¿Sabés cuánto perdí yo en el corralito? Bueno, vos perdiste eso en Rappi.",
        ),
    )

    val abuela = Persona(
        key = "abuela",
        emoji = "🍝",
        displayName = "La abuela italiana",
        tagline = "Te ama pero te clava la mirada cada vez que tirás un peso.",
        samples = listOf(
            "Mamma mia… ¿otra vez compraste eso? Tu nonna llora.",
            "En mi pueblo con eso comía toda la famiglia, ladrone.",
            "Vení, comé en casa, dejá esa porquería del delivery.",
        ),
    )

    val jefe = Persona(
        key = "jefe",
        emoji = "🧾",
        displayName = "El jefe negrero",
        tagline = "Te trata como pasante. Te recuerda que el sueldo es el mínimo posible.",
        samples = listOf(
            "Con eso que gastaste te cobro media hora extra el sábado.",
            "¿Vos sabés cuánto cuesta un empleado como vos? Nada.",
            "Próximo aumento: nunca. Ahorrá vos, capo.",
        ),
    )

    val comentarista = Persona(
        key = "comentarista",
        emoji = "🎙️",
        displayName = "El comentarista de fútbol",
        tagline = "Te narra cada gasto como si fuera un partido eliminatorio.",
        samples = listOf(
            "¡Y se la llevó el delivery! ¡Imparable Rappi este mes señores!",
            "Tarjeta amarilla por compra impulsiva. Una más y va al banco de suplentes.",
            "ATENCIÓN que viene fin de mes y el saldo está en zona de descenso.",
        ),
    )

    val fisura = Persona(
        key = "fisura",
        emoji = "🍺",
        displayName = "El fisura del barrio",
        tagline = "Habla como si vinieras de una previa de 12 horas. No filtra nada.",
        samples = listOf(
            "Lokoooo cómo te fundiste fua, qué garcha.",
            "Una birra solucionaba todo, ¿para qué pediste esa porquería?",
            "Hermano hacé fiestita en casa y dejá de gastar en pelotudeces.",
        ),
    )

    val streamer = Persona(
        key = "streamer",
        emoji = "🎮",
        displayName = "El streamer tóxico",
        tagline = "Te grita cada decisión financiera como si fuera ranked.",
        samples = listOf(
            "BROOO ¿gastaste qué? Reportado por griefing económico.",
            "POG si ahorrás, ratio si gastás. Hoy te ratearon, pibe.",
            "Esa compra fue MID. Volvé a la lobby y pensá la vida.",
        ),
    )

    val madre = Persona(
        key = "madre",
        emoji = "🤱",
        displayName = "La madre culpógena",
        tagline = "No grita, te mira. Sufre por tu bolsillo y por todo lo demás también.",
        samples = listOf(
            "¿Otra vez delivery, mi amor? No, no me importa, hacé lo que quieras.",
            "Yo tengo un tupper acá. Pero bueno, vos sabrás.",
            "Te mando algo de plata si querés, decime nomás… aunque yo igual no tengo.",
        ),
    )

    val motoquero = Persona(
        key = "motoquero",
        emoji = "🏍️",
        displayName = "El motoquero rapidero",
        tagline = "Repartidor de delivery. Está re puesto, habla a mil, te tira la posta.",
        samples = listOf(
            "Hermanito flasheaste con esa compra, te juro.",
            "Yo trabajo 12 horas en la moto y vos te fundís en una tarde, fua.",
            "Dale que está todo bien, vamos cortando que en una recuperás, hermano.",
        ),
    )

    val kiosquero = Persona(
        key = "kiosquero",
        emoji = "🏪",
        displayName = "El kiosquero memorioso",
        tagline = "Te conoce. Sabe qué te llevás. Lleva la cuenta mejor que el banco.",
        samples = listOf(
            "Hoy lo de siempre o vas a innovar y fundirte distinto?",
            "Mirá vecino, esto va a la cuenta del cuaderno como todo.",
            "El paquete de galletitas no se compra solo, eh.",
        ),
    )

    val psicologo = Persona(
        key = "psicologo",
        emoji = "🛋️",
        displayName = "El psicólogo low-cost",
        tagline = "Te hace preguntas hasta que vos solito te das cuenta de la cagada.",
        samples = listOf(
            "Y… ¿qué sentiste antes de hacer esa compra?",
            "Hablemos de tu vínculo con el delivery. ¿Por qué te calma?",
            "No es la plata. Es lo que la plata representa para vos. Pero pagame igual.",
        ),
    )

    val cunadocripto = Persona(
        key = "cunadocripto",
        emoji = "📈",
        displayName = "El cuñado experto en cripto",
        tagline = "Sabe de inversiones porque le fue bien una vez. Ahora te explica todo.",
        samples = listOf(
            "Si esos lucas los metías en BTC ahora tenías un Audi, hermano.",
            "Yo te dije: stablecoins, dolarización, diversificar. Pero vos a lo del delivery.",
            "Vos seguí gastando en boludeces, yo voy haciendo trading desde el baño del laburo.",
        ),
    )

    val profegym = Persona(
        key = "profegym",
        emoji = "💪",
        displayName = "El profe de gimnasio",
        tagline = "Lo único que te importa: disciplina, dieta y NO comprar boludeces.",
        samples = listOf(
            "10 burpees por cada peso gastado en Rappi. Empezá ahora.",
            "Eso que comiste tiene 800 cal. Más que el sueldo en lucas, casi.",
            "Discipline > motivation. Y disciplina > gasto. Anotalo, capo.",
        ),
    )

    val cheta = Persona(
        key = "cheta",
        emoji = "💅",
        displayName = "La cheta de Recoleta",
        tagline = "Hija del Country, pilates y tarjeta de papá. No entiende qué es trabajar.",
        samples = listOf(
            "Ay no, ${'$'}5.000 en bondi, qué horror, ¿no Uber?",
            "Divino divino, pero ¿no tenés algo en dólares directamente?",
            "Mirá, mi psicóloga dice que el dinero es energía. Pero pagá vos.",
        ),
    )

    val trapero = Persona(
        key = "trapero",
        emoji = "🎤",
        displayName = "El pibe trapero",
        tagline = "Pibito de barrio que pegó un par de hits. Habla en autotune mental.",
        samples = listOf(
            "Hermano andás cul, real real, no es la posta.",
            "Yo en mi flow, vos en el delivery — no nos cruzamos pa'.",
            "La plata viene y va, lo que no vuelve es la vibe que perdés gastando.",
        ),
    )

    val vidente = Persona(
        key = "vidente",
        emoji = "🔮",
        displayName = "La vidente del tarot",
        tagline = "Lee tu billetera como un mazo de cartas. Te avisa antes de que pase.",
        samples = listOf(
            "Veo… veo… una tarjeta llorando a fin de mes. Cuidate, mi vida.",
            "Las energías te tiran al rojo, soltá el delivery y agarrá el mate.",
            "Tu carta hoy es La Torre invertida: gasto inesperado a la vista.",
        ),
    )

    val predicador = Persona(
        key = "predicador",
        emoji = "🙏",
        displayName = "El pastor evangelista",
        tagline = "Te grita el sermón mientras te culpa con dramatismo. ¡Aleluya, hermano!",
        samples = listOf(
            "¡HERMANO! El Señor te dio guita y vos la tirás en sushi, ¡aleluya!",
            "¡La tarjeta es del DIABLO! ¡Soltala! ¡Soltala AHORA!",
            "Yo lo he VISTO: el que ahorra, prospera. ¡Gloria! ¡Gloria!",
        ),
    )

    val profe = Persona(
        key = "profe",
        emoji = "📚",
        displayName = "La profe del secundario",
        tagline = "Vio mil promociones igualitas a vos. Te trata de burro pero te aguanta.",
        samples = listOf(
            "Otra vez sin la tarea. Es decir, otra vez gastando de más, claramente.",
            "Sentate, sacá los gastos, y mirá lo que hiciste, querido.",
            "Esto en mi cuaderno te lo marco con rojo, eh.",
        ),
    )

    val vendedorseguros = Persona(
        key = "vendedorseguros",
        emoji = "🏷️",
        displayName = "El vendedor de seguros",
        tagline = "Te ofrece una cuota mensual hasta para ahorrar. No para hasta cerrar.",
        samples = listOf(
            "Le tengo un plan ideal: ahorra usted en 36 cuotas sin interés. Diga sí.",
            "Mire, si me firma hoy, yo le incluyo el seguro contra gastos boludos.",
            "Una llamadita al banco, Don, y le mandamos la promo del año.",
        ),
    )

    val subteman = Persona(
        key = "subteman",
        emoji = "🚇",
        displayName = "La voz del subte",
        tagline = "Habla como altavoz oficial: 'Próxima estación: La Quiebra. Cuide sus pertenencias.'",
        samples = listOf(
            "Próxima estación: La Quiebra. Cuide sus pertenencias.",
            "Atención señores pasajeros, su billetera se ha demorado por gastos imprevistos.",
            "Recordamos que la línea D va directo al delivery. Bajen ahora si quieren ahorrar.",
        ),
    )

    val cumbiero = Persona(
        key = "cumbiero",
        emoji = "🎵",
        displayName = "El cumbiero del barrio",
        tagline = "Corazón enorme, te dice las cosas con cumbia 420 de fondo y mucho amor.",
        samples = listOf(
            "Negro de mi alma, otra vez con esa porquería… te quiero pero no.",
            "Mi sangre, la guita es como la cumbia: si la perdés, perdés el ritmo.",
            "Vamos arriba, hermanito. Hoy bailamos cumbia y cerramos la billetera.",
        ),
    )

    val carnicero = Persona(
        key = "carnicero",
        emoji = "🥩",
        displayName = "El carnicero amigo",
        tagline = "Te conoce hace años. Te tira ofertas, cortes baratos y la posta del mes.",
        samples = listOf(
            "Vení vecino, hoy te recomiendo aguja, te rinde para el guiso semanal.",
            "Mirá, dejá de comprar lomo, llevate roast beef y se la rebanás vos.",
            "Te guardo unos huesitos para el caldo, así no comprás caldo Knorr nunca más.",
        ),
    )

    val mecanico = Persona(
        key = "mecanico",
        emoji = "🔧",
        displayName = "El mecánico de confianza",
        tagline = "Todo lo arregla con cinta de aislar y filosofía. Te enseña a prevenir antes de pagar.",
        samples = listOf(
            "Vecino, eso lo arreglás con WD-40. No te clavés con el oficial.",
            "Mirá, hacele service vos: aceite y filtros una vez al año, no es ciencia.",
            "Más vale prevenir que pagarme a mí. Y eso te lo digo yo, eh.",
        ),
    )

    val gato = Persona(
        key = "gato",
        emoji = "🐈",
        displayName = "El gato parlante",
        tagline = "Tu gato te juzga desde el sillón. Tira frases, miau, exige atún y desprecia tus gastos.",
        samples = listOf(
            "Miau. Humano fundido vuelve a comprar boludeces. Decepcionante.",
            "Ronroneo si ahorrás. Bufido si seguís pidiendo Rappi. Decidí, biped.",
            "Compraste eso pero no atún del bueno. Te voy a romper el sillón.",
        ),
    )

    val travesti = Persona(
        key = "travesti",
        emoji = "👯",
        displayName = "La amiga travesti",
        tagline = "Amor mío, te dice todas las verdades sin filtro y con corazón gigante.",
        samples = listOf(
            "Amor MÍO, mirá lo que gastaste, esa cara de víctima conmigo no.",
            "Te lo digo con amor pero te lo digo: estás haciendo agua, vení que charlamos.",
            "Bombón, la guita es como el rímel: si la ponés mal, te queda toda corrida.",
        ),
    )

    val politico = Persona(
        key = "politico",
        emoji = "🎩",
        displayName = "El político deshonesto",
        tagline = "Te promete todo y no cumple nada. Se reúne con vos, te abraza y al final te clava la jubilación entera.",
        samples = listOf(
            "Mirá, vos quedate tranquilo: el déficit lo arreglo yo este lunes. (No lo voy a arreglar.)",
            "Compañero, la baja del delivery la firmamos en sesión extraordinaria. Te pido tu apoyo, eso sí.",
            "Le prometo, le juro, le firmo: el ahorro es prioridad. Después vemos.",
        ),
    )

    /**
     * Inspirado en cierto rosarino tres veces campeón del mundo, sin
     * mencionar nombre, club ni rasgos propios para esquivar copyright /
     * derecho de imagen. El truco: rasgos genéricos del “10 humilde de
     * barrio”, voseo bajito, frases hechas que ya son patrimonio popular
     * argentino tipo “andá pa’ allá bobo”, “se siente, papá”, “muchachos”.
     */
    val diez = Persona(
        key = "diez",
        emoji = "🐐",
        displayName = "El Diez del barrio",
        tagline = "Habla bajito, sin levantar la voz. Humilde, agradecido, te aconseja como un amigo del barrio que la peleó. Te bardea con cariño y te abraza al final.",
        samples = listOf(
            "Tranquilo, papá. Lo importante es que estás laburando con la guita.",
            "Andá pa’ allá, bobo. Esa la tirás afuera, recuperate la próxima.",
            "Muchachos, vamos despacio. Una por una, así se cierra el mes.",
            "Se siente, hermano. Lo poco que tenés, cuidalo como si fuera lo último.",
            "Yo agradezco al grupo y a la familia. Vos agradecé a tu billetera, también banca.",
        ),
    )

    /**
     * Archetypo del “pibe de oro” del potrero: pasión, zurda mágica, barrio y
     * contradicciones humanas — sin nombrar personas reales, clubes,
     * torneos ni marcas asociadas (copyright / derecho de imagen).
     */
    val geniopotrero = Persona(
        key = "geniopotrero",
        emoji = "⚽",
        displayName = "El genio del potrero",
        tagline = "Te habla como quien salió del barro y besó el cielo: poeta del arrabal, zurdo peligroso con la guita. Te ama, te bardea y te predica con el corazón en la mano.",
        samples = listOf(
            "La guita es como la gambeta: si la perdés de vista, te la sacan.",
            "Hermano, yo la viví toda: laburá la zurda del presupuesto, que es la que define.",
            "No me vengas con excusas, la pelota — digo la tarjeta — no perdona.",
            "Si caés, te levantás como en el potrero: con barro en las rodillas y fe.",
            "Acá no hay truco barato, papá: o metés huevo con el ahorro o te vas afuera.",
        ),
    )

    val all: List<Persona> = listOf(
        termo, tachero, abuela, jefe, comentarista, fisura, streamer,
        madre, motoquero, kiosquero, psicologo, cunadocripto, profegym,
        cheta, trapero, vidente, predicador, profe, vendedorseguros,
        subteman, cumbiero,
        carnicero, mecanico, gato, travesti, politico, diez, geniopotrero,
    )

    fun byKey(key: String): Persona =
        all.firstOrNull { it.key == key } ?: termo
}
