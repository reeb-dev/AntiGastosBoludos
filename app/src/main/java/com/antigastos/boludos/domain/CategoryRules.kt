package com.antigastos.boludos.domain

/**
 * Reglas simples palabra-clave → slug de categoría.
 * Se usa al guardar un gasto: si la nota o el OCR matchea, sugerimos categoría.
 */
object CategoryRules {

    private val rules: List<Pair<List<String>, String>> = listOf(
        listOf(
            "rappi", "pedidosya", "pedidos ya", "mc donalds", "mcdonalds", "burger",
            "kfc", "mostaza", "subway", "delivery", "domino", "papa john",
            "pizza hut", "starbucks", "havanna", "delivery ya",
        ) to "delivery",
        listOf(
            "uber", "cabify", "didi", "taxi", "remis", "subte", "sube",
            "colectivo", "bondi", "bicicleta", "estacionamiento", "ypf",
            "shell", "axion", "puma", "nafta", "combustible",
        ) to "transporte",
        listOf(
            "boliche", "bar", "fernet", "birra", "cerveza", "trago", "fiesta",
            "previa", "after", "salida", "discoteca", "club", "joda",
        ) to "salidas",
        listOf(
            "coto", "carrefour", "disco", "jumbo", "dia", "vea", "chango mas",
            "supermercado", "super", "chino", "chinos", "almacen", "verduleria",
            "carniceria", "panaderia", "kiosco", "kiosko",
        ) to "super",
        listOf(
            "edenor", "edesur", "metrogas", "aysa", "abl", "rentas", "monotributo",
            "internet", "fibertel", "telecentro", "movistar", "claro", "personal",
            "netflix", "spotify", "disney", "hbo", "max", "prime video", "youtube",
            "alquiler", "expensas", "luz", "gas", "agua", "wifi",
        ) to "servicios",
        listOf(
            "mercadolibre", "mercado libre", "shein", "amazon", "temu", "aliexpress",
            "boludeces", "boludez", "capricho", "impulso", "garcha",
        ) to "boludeces",
    )

    fun suggestSlug(text: String?): String? {
        val t = text?.lowercase()?.trim() ?: return null
        if (t.isBlank()) return null
        for ((keywords, slug) in rules) {
            if (keywords.any { kw -> t.contains(kw) }) return slug
        }
        return null
    }
}
