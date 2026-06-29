package com.antigastos.boludos.domain

import kotlin.random.Random

/**
 * Mapea (templateId, mood, slug) a una palabra clave para Tenor.
 * Apunta a GIFs en tendencia argentina: memes, fútbol, política, streamers, novelas, etc.
 */
object GifKeyword {

    private val cheerPool = listOf(
        "festejo argentina",
        "messi celebracion",
        "diego maradona festejo",
        "argentina campeon meme",
        "pulpo argentino feliz",
        "bizarrap aprobado",
        "luisana lopilato si",
        "marcelo tinelli aplauso",
    )

    private val burnPool = listOf(
        "decepcion argentina meme",
        "guillermo francella enojado",
        "ricardo fort plata",
        "moria casan no",
        "showmatch indignado",
        "bandana golpe meme",
        "no me gusta argentina",
        "cris morena meme",
    )

    private val alarmPool = listOf(
        "javier milei loco",
        "casa rosada fuego meme",
        "bandera argentina fuego",
        "indec inflacion meme",
        "billetera vacia meme",
        "tarjeta rota",
        "alerta meme",
        "carlos calvo grita",
    )

    private val neutralPool = listOf(
        "argentina random meme",
        "mate argentino",
        "kiosco argentino",
        "subte buenos aires",
        "asado argentino",
        "argentina vida cotidiana",
    )

    private val deliveryPool = listOf(
        "delivery argentina",
        "rappi argentina meme",
        "pedidos ya argentina",
        "pizza argentina meme",
        "empanada meme",
        "sushi argentina",
        "hamburguesa argentina",
    )

    private val transportPool = listOf(
        "uber argentina meme",
        "subte sube colectivo",
        "cabify argentina",
        "taxi porteno",
    )

    private val coffeePool = listOf(
        "cafecito porteno",
        "cafeteria argentina",
        "starbucks argentina meme",
        "havanna cafe",
    )

    private val streamingPool = listOf(
        "netflix argentina meme",
        "spotify argentina",
        "disney plus argentina",
        "streaming meme",
    )

    private val shoppingPool = listOf(
        "mercado libre meme",
        "shein argentina",
        "compra impulsiva meme",
        "carrito virtual meme",
    )

    private val supermarketPool = listOf(
        "supermercado coto carrefour",
        "carrito supermercado",
        "ofertas supermercado argentina",
    )

    private val nightOutPool = listOf(
        "salida boliche argentina",
        "previa argentina meme",
        "fernet con coca",
        "cumbia 420 fiesta",
        "cuarteto fiesta",
    )

    private val savingPool = listOf(
        "chanchito plata",
        "ahorro plata argentina",
        "dolar blue meme",
        "guita guardada meme",
        "rata ahorrativo",
    )

    private val footballPool = listOf(
        "messi argentina",
        "maradona meme",
        "boca river meme",
        "argentina campeon mundo",
        "pulpo argentino futbol",
    )

    private val musicPool = listOf(
        "bizarrap argentina",
        "duki argentina",
        "tini argentina",
        "rock nacional argentina",
        "wos meme",
        "lali esposito meme",
    )

    private val tvPool = listOf(
        "tinelli showmatch",
        "guillermo francella",
        "moria casan",
        "ricardo fort meme",
        "gran hermano argentina meme",
    )

    private val barrioPool = listOf(
        "barrio argentino meme",
        "quilombo argentino meme",
        "kiosko esquina meme",
        "amigos argentina meme",
        "asado con amigos",
    )

    fun queryFor(
        templateId: String?,
        mood: CopyMood,
        slug: String?,
        seed: Long = System.currentTimeMillis(),
    ): String {
        val rng = Random(seed)
        val byTemplate = templateBased(templateId, rng)
        if (byTemplate != null) return byTemplate

        val bySlug = slugBased(slug, rng)
        if (bySlug != null) return bySlug

        return when (mood) {
            CopyMood.CHEER -> cheerPool.random(rng)
            CopyMood.BURN -> burnPool.random(rng)
            CopyMood.ALARM -> alarmPool.random(rng)
            CopyMood.NEUTRAL -> neutralPool.random(rng)
        }
    }

    private fun templateBased(templateId: String?, rng: Random): String? {
        val id = templateId?.lowercase() ?: return null
        return when {
            id.startsWith("delivery") || id.contains("rappi") || id.contains("sushi") || id.contains("empanada") -> deliveryPool.random(rng)
            id.startsWith("uber") || id.contains("sube") || id.contains("transporte") -> transportPool.random(rng)
            id.startsWith("cafe") || id.contains("coffee") || id.contains("cafecito") -> coffeePool.random(rng)
            id.contains("streaming") || id.contains("netflix") || id.contains("spotify") -> streamingPool.random(rng)
            id.contains("mli") || id.contains("mercado_libre") || id.contains("shein") || id.contains("anxiety") || id.contains("dopamine") || id.contains("caprichito") || id.contains("unnecessary") -> shoppingPool.random(rng)
            id.contains("supermerc") || id.contains("ticket") -> supermarketPool.random(rng)
            id.contains("salida") || id.contains("boliche") || id.contains("birra") || id.contains("previa") -> nightOutPool.random(rng)
            id.contains("ahorro") || id.contains("save") || id.contains("rata") || id.contains("monje") || id.contains("streak") || id.contains("zero_spend") || id.contains("vaquita") -> savingPool.random(rng)
            id.contains("futbol") || id.contains("messi") || id.contains("maradona") || id.contains("boca") || id.contains("river") -> footballPool.random(rng)
            id.contains("musica") || id.contains("recital") || id.contains("bizarrap") || id.contains("duki") || id.contains("tini") -> musicPool.random(rng)
            id.contains("tv") || id.contains("show") || id.contains("tinelli") || id.contains("francella") || id.contains("moria") || id.contains("fort") || id.contains("gran_hermano") -> tvPool.random(rng)
            id.contains("barrio") || id.contains("amigo") || id.contains("kiosco") || id.contains("kiosquero") -> barrioPool.random(rng)
            id.contains("uber_vs_sube") -> transportPool.random(rng)
            id.contains("aguinaldo") || id.contains("salary") || id.contains("end_month") -> savingPool.random(rng)
            else -> null
        }
    }

    private fun slugBased(slug: String?, rng: Random): String? {
        val s = slug?.lowercase() ?: return null
        return when {
            s.contains("delivery") || s.contains("comida") -> deliveryPool.random(rng)
            s.contains("transp") || s.contains("uber") || s.contains("sube") -> transportPool.random(rng)
            s.contains("cafe") -> coffeePool.random(rng)
            s.contains("ocio") || s.contains("salida") || s.contains("fiesta") -> nightOutPool.random(rng)
            s.contains("sus") || s.contains("streaming") -> streamingPool.random(rng)
            s.contains("mercado") || s.contains("super") -> supermarketPool.random(rng)
            s.contains("compra") || s.contains("shopping") -> shoppingPool.random(rng)
            else -> null
        }
    }
}
