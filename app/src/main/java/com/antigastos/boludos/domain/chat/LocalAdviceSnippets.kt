package com.antigastos.boludos.domain.chat

import com.antigastos.boludos.domain.CopyContext
import com.antigastos.boludos.domain.CopyMood
import kotlin.random.Random

/** Consejos financieros cortos para respuestas offline (sin IA). */
internal object LocalAdviceSnippets {

    private val general = listOf(
        "Regla de 24 horas: si no lo necesitás mañana, no lo compres hoy.",
        "Batch cooking un domingo: guiso, arroz y pollo. Semana resuelta.",
        "Anotá TODO, incluso el alfajor del kiosco. Lo chico te funde.",
        "Un día sin delivery por semana ya te baja la categoría comida un montón.",
        "Revisá suscripciones: Spotify, gym, apps. Una que no uses = luca al mes.",
        "Efectivo para delivery: cuando ves los billetes, frenás solo.",
        "Lista de super con tope: si no está en la lista, no entra al carrito.",
        "Vaquita con amigos: dividir delivery sale la mitad.",
        "Mercado Pago / billeteras: dejá solo lo del mes, el resto a otro lado.",
        "Compará precios por kilo en el super, no por paquete lindo.",
        "Feriados y findes: planificá antes, no improvises con tarjeta.",
    )

    private val alarm = listOf(
        "Modo supervivencia: vianda, mate y cero pedidos hasta el 30.",
        "Congelá gastos variables: solo transporte y comida básica.",
        "Si pasaste la meta, no te castigues: cortá gustos 5 días y listo.",
        "Llamá al banco/celular: un reclamo te baja la factura más de lo que creés.",
    )

    private val cheer = listOf(
        "Seguí así: un día bueno se convierte en racha si lo repetís.",
        "Meté lo que ahorraste hoy en un sobre. Verlo crece motiva.",
        "Premiate barato: un helado, no un asado de 80 lucas.",
    )

    fun pick(ctx: CopyContext, mood: CopyMood, rng: Random): String {
        val pool = when (mood) {
            CopyMood.ALARM -> alarm + general
            CopyMood.CHEER -> cheer + general
            else -> general
        }
        return pool.random(rng)
    }
}
