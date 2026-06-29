package com.antigastos.boludos.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.TemporalAdjusters

/**
 * Calendario de feriados argentinos.
 *
 *  - Fijos: Año Nuevo, Día del Trabajador, etc.
 *  - Calculados: Carnaval, Viernes Santo (Easter - 2 días), Pascua, Corpus.
 *  - Movibles ("trasladables"): cuando caen martes/miércoles/jueves se
 *    pueden mover al lunes anterior. Por simplicidad lo dejamos en la
 *    fecha original pero marcamos `type = MOVABLE`.
 *  - Observancias del bolsillo: día del padre, día del amigo, día del
 *    niño, navidad, año nuevo, halloween → no son feriado pero pesan en
 *    el gasto y dan logros temáticos.
 *
 * Devuelve el feriado/observancia para una fecha dada o `null` si no hay.
 */
object HolidayCalendar {

    enum class Type {
        /** Feriado nacional inamovible. */
        NATIONAL,
        /** Religioso (Viernes Santo, Pascua, Corpus). */
        RELIGIOUS,
        /** No laborable (al criterio del empleador). */
        NON_WORKING,
        /** Trasladable a lunes (turismo). */
        MOVABLE,
        /** Observancia comercial / cultural (no es feriado real). */
        OBSERVANCE,
    }

    /**
     * Cuánto pesa este día sobre el "presupuesto". Multiplicador sobre
     * el allowance diario base.
     *  - 1.0 = un día normal
     *  - 2.5 = día de gasto fuerte permitido (Navidad, Año Nuevo)
     *  - 0.6 = día tranqui pretendido (Jueves Santo, Viernes Santo)
     */
    data class Holiday(
        val date: LocalDate,
        val name: String,
        val emoji: String,
        val type: Type,
        val spendingFactor: Double,
        /** Texto para el banner del Home. */
        val tagline: String,
    )

    /** Devuelve el feriado/observancia más relevante de [date] o null. */
    fun forDate(date: LocalDate): Holiday? = allForYear(date.year).firstOrNull { it.date == date }

    /** Lista completa de un año (feriados nacionales + observancias). */
    fun allForYear(year: Int): List<Holiday> {
        val fixed = fixedHolidays(year)
        val religious = religiousHolidays(year)
        val observances = observances(year)
        return (fixed + religious + observances).sortedBy { it.date }
    }

    /**
     * Feriados oficiales nacionales (sin observancias). Útil para el
     * cálculo de "es feriado real".
     */
    fun nationalHolidaysForYear(year: Int): List<Holiday> {
        val fixed = fixedHolidays(year).filter { it.type != Type.OBSERVANCE }
        val religious = religiousHolidays(year)
        return (fixed + religious).sortedBy { it.date }
    }

    /**
     * Próximo feriado/observancia a partir de [from] inclusive, mirando
     * hasta 12 meses adelante.
     */
    fun nextHoliday(from: LocalDate): Holiday? {
        val thisYear = allForYear(from.year).firstOrNull { it.date >= from }
        if (thisYear != null) return thisYear
        return allForYear(from.year + 1).firstOrNull()
    }

    fun isLongWeekend(date: LocalDate): Boolean {
        val nat = nationalHolidaysForYear(date.year)
        // ventana de viernes a lunes
        return nat.any { h ->
            val d = h.date
            d.dayOfWeek == DayOfWeek.MONDAY || d.dayOfWeek == DayOfWeek.FRIDAY ||
                (date == d.minusDays(1) && d.dayOfWeek == DayOfWeek.MONDAY) ||
                (date == d.plusDays(1) && d.dayOfWeek == DayOfWeek.FRIDAY)
        }
    }

    // ---------- Fijos ----------

    private fun fixedHolidays(year: Int): List<Holiday> = listOf(
        Holiday(LocalDate.of(year, 1, 1), "Año Nuevo", "🎆", Type.NATIONAL, 2.5,
            "Año Nuevo: 'arranco mejor el año' dura 3 horas. Después gastás igual."),
        Holiday(LocalDate.of(year, 3, 24), "Memoria por la Verdad y la Justicia", "🕊️",
            Type.NATIONAL, 0.7, "Día solemne. Bajá el ritmo, no es feriado de shopping."),
        Holiday(LocalDate.of(year, 4, 2), "Veteranos y Caídos en Malvinas", "🇦🇷",
            Type.NATIONAL, 0.8, "Veteranos de Malvinas. Día de respeto."),
        Holiday(LocalDate.of(year, 5, 1), "Día del Trabajador", "🛠️", Type.NATIONAL, 1.4,
            "Día del Trabajador. Asado o pizza, vos decidís."),
        Holiday(LocalDate.of(year, 5, 25), "Revolución de Mayo", "🥧", Type.NATIONAL, 1.5,
            "25 de Mayo. Locro y patriotismo."),
        Holiday(LocalDate.of(year, 6, 17), "Paso a la Inmortalidad de Güemes", "🐎",
            Type.MOVABLE, 1.0, "Güemes. Probable finde largo."),
        Holiday(LocalDate.of(year, 6, 20), "Día de la Bandera", "🎌", Type.NATIONAL, 1.2,
            "Día de la Bandera. Belgrano de fondo."),
        Holiday(LocalDate.of(year, 7, 9), "Día de la Independencia", "⭐", Type.NATIONAL, 1.5,
            "9 de Julio. Acto, asado y un Cabildo abierto."),
        Holiday(LocalDate.of(year, 8, 17), "Paso a la Inmortalidad de San Martín", "⚔️",
            Type.MOVABLE, 1.0, "San Martín. Finde largo casi seguro."),
        Holiday(LocalDate.of(year, 10, 12), "Diversidad Cultural", "🌎",
            Type.MOVABLE, 1.1, "Diversidad cultural. Posible finde largo."),
        Holiday(LocalDate.of(year, 11, 20), "Día de la Soberanía", "🛡️",
            Type.MOVABLE, 1.0, "Soberanía Nacional. Vuelta de Obligado."),
        Holiday(LocalDate.of(year, 12, 8), "Inmaculada Concepción", "✝️",
            Type.RELIGIOUS, 1.2, "Inmaculada Concepción. Para muchos, día libre."),
        Holiday(LocalDate.of(year, 12, 25), "Navidad", "🎄", Type.NATIONAL, 3.0,
            "Navidad. Vitel toné, sidra y regalos. Hoy se gasta sin culpa."),
    )

    // ---------- Religiosos calculados ----------

    private fun religiousHolidays(year: Int): List<Holiday> {
        val easter = computeEasterSunday(year)
        val carnavalLunes = easter.minusDays(48)
        val carnavalMartes = easter.minusDays(47)
        val juevesSanto = easter.minusDays(3)
        val viernesSanto = easter.minusDays(2)
        val corpus = easter.plusDays(60)
        return listOf(
            Holiday(carnavalLunes, "Carnaval", "🎭", Type.NATIONAL, 1.8,
                "Carnaval lunes. Si tirás guita, que sea con espuma."),
            Holiday(carnavalMartes, "Carnaval", "🎭", Type.NATIONAL, 1.8,
                "Carnaval martes. Último día para sacarte las ganas."),
            Holiday(juevesSanto, "Jueves Santo", "🐟", Type.NON_WORKING, 0.7,
                "Jueves Santo. Pescado y reflexión. Bajá las compras."),
            Holiday(viernesSanto, "Viernes Santo", "✝️", Type.RELIGIOUS, 0.6,
                "Viernes Santo. Día solemne. Cero compras, cero delivery si podés."),
            Holiday(corpus, "Corpus Christi", "✝️", Type.RELIGIOUS, 1.0,
                "Corpus Christi. Día religioso, en algunos años es feriado."),
        )
    }

    /**
     * Algoritmo de Meeus/Jones/Butcher para el Domingo de Pascua gregoriano.
     */
    private fun computeEasterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }

    // ---------- Observancias culturales (no son feriado, pero pesan) ----------

    private fun observances(year: Int): List<Holiday> = listOf(
        Holiday(LocalDate.of(year, 2, 14), "San Valentín", "❤️", Type.OBSERVANCE, 2.0,
            "Día de los enamorados. Hoy se gasta más, pero no exageres."),
        Holiday(LocalDate.of(year, 7, 20), "Día del Amigo", "🥃", Type.OBSERVANCE, 2.0,
            "Día del Amigo. Asado y birra. Marca registrada argentina."),
        Holiday(LocalDate.of(year, 10, 31), "Halloween", "🎃", Type.OBSERVANCE, 1.4,
            "Halloween. Disfraz improvisado y golosinas."),
        // Día de la Madre (3er domingo de octubre, AR)
        run {
            val firstSun = LocalDate.of(year, 10, 1).with(TemporalAdjusters.firstInMonth(DayOfWeek.SUNDAY))
            val third = firstSun.plusWeeks(2)
            Holiday(third, "Día de la Madre", "👩", Type.OBSERVANCE, 2.2,
                "Día de la Madre. Regalo y almuerzo. Imperdonable olvidarse.")
        },
        // Día del Padre (3er domingo de junio, AR)
        run {
            val firstSun = LocalDate.of(year, 6, 1).with(TemporalAdjusters.firstInMonth(DayOfWeek.SUNDAY))
            val third = firstSun.plusWeeks(2)
            Holiday(third, "Día del Padre", "👨", Type.OBSERVANCE, 2.0,
                "Día del Padre. Algo le tenés que llevar.")
        },
        // Día del Niño (3er domingo de agosto, AR)
        run {
            val firstSun = LocalDate.of(year, 8, 1).with(TemporalAdjusters.firstInMonth(DayOfWeek.SUNDAY))
            val third = firstSun.plusWeeks(2)
            Holiday(third, "Día del Niño", "🎈", Type.OBSERVANCE, 2.0,
                "Día del Niño. Jugueterías llenas.")
        },
        Holiday(LocalDate.of(year, 11, 25), "Black Friday", "🛍️", Type.OBSERVANCE, 1.6,
            "Black Friday. Mucho descuento que no era. Cuidado."),
        Holiday(LocalDate.of(year, 12, 24), "Nochebuena", "🎄", Type.OBSERVANCE, 2.5,
            "Nochebuena. Familia, sidra y compras de último momento."),
        Holiday(LocalDate.of(year, 12, 31), "Año Nuevo (víspera)", "🍾", Type.OBSERVANCE, 2.5,
            "Despedida del año. Un excedente lo entiende cualquiera."),
    )
}
