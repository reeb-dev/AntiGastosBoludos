package com.antigastos.boludos.domain

import java.security.SecureRandom
import kotlin.random.Random

/**
 * Pool de frases para la ruleta. Cada giro mezcla [SecureRandom] + nanoTime para
 * que no se sienta “siempre lo mismo”, incluso antes de que la IA reescriba.
 */
object RuletaCatalog {

    enum class Tipo(val emoji: String, val label: String) {
        DESAFIO("🎯", "Desafío"),
        INSULTO("💀", "Cariñito argentino"),
        PREDICCION("🔮", "Predicción falopa"),
        CONSEJO("💸", "Consejo financiero"),
    }

    data class Frase(val tipo: Tipo, val texto: String)

    private val desafios = listOf(
        "Hoy no gastes en café, Warren Buffet de Constitución.",
        "Cociná en casa, no pidas Rappi. Repito: NO PIDAS RAPPI.",
        "Llevate la vianda. Sí, vos.",
        "Hoy hacés vaquita con vos mismo: cero compras impulsivas.",
        "Caminá una cuadra de más antes de tomar el bondi.",
        "Hoy meté el celu en un cajón 30 minutos. Tu billetera te lo agradece.",
        "Tocá pasto. Literalmente, salí.",
        "Si tenés que comprar algo, espera 24hs. Si todavía lo querés, recién ahí.",
        "Cancelá un servicio que no usás. Hoy. Ya.",
        "Andate a dormir antes de las 12. Bajá la app.",
        "Hoy solo efectivo: sacá un monto y no pases de ahí.",
        "Dejá el carrito de Mercado Libre abierto sin comprar. Entrená el cerebro.",
        "Un día sin apps de delivery. Modo supervivencia creativa.",
        "Hacé la lista del chino y no te salgas ni un caramelito.",
        "Hoy el agua de la canilla es tu bebida energética.",
        "Si te tentó algo caro, hacé 20 flexiones y reconsiderá.",
        "Llevá termo y no compres bebida en la calle.",
        "Hoy no hay ‘solo una birrita’. Cero birrita.",
        "Anotá cada peso que salga. Hasta el colectivo.",
        "Probá un día sin redes: menos FOMO, menos compras boludas.",
        "Cociná con lo que hay en la heladera. MasterChef pobre.",
        "Regalá o vendé una cosa que no usás. Liberá espacio y guita.",
        "Hoy no hay ‘promo 12 cuotas’. Ni en pedo.",
        "Salí a caminar cuando quieras comprar por aburrimiento.",
        "Hacé presupuesto de bolsillo en un papelito y llevá eso.",
    )

    private val insultos = listOf(
        "Sos un fundido con onda.",
        "Tu billetera te denuncia por maltrato.",
        "Tenés más cuotas que un libro de matemáticas.",
        "Tu yo del futuro está armando un grupo de autoayuda por tu culpa.",
        "Cocinás 0 veces por mes y te quejás del sueldo. Casualidad nada.",
        "Te conoce mejor el de Mercado Libre que tu familia.",
        "Sos termo financiero nivel olímpico.",
        "Más boludo que comprar campera de cuero en febrero.",
        "Tu economía está nivel 'Gimnasia y Esgrima de La Plata 2018'.",
        "Sos influencer del despilfarro.",
        "Tu tarjeta pide asilo político.",
        "Gastás como si el aguinaldo fuera mensual.",
        "Sos el sponsor oficial de tu propia ruina.",
        "Tenés la disciplina de un pibe con heladera llena de delivery.",
        "Tu presupuesto es más fictión que un capítulo de Simpsons.",
        "Sos el cliente VIP del ‘después pago’.",
        "La inflación te mira y se ríe de vos.",
        "Comprás cosas para sentir algo. Spoiler: no funciona.",
        "Tu banco te ama. Vos no deberías.",
        "Sos un imán de gastos hormiga.",
        "Tenés más suscripciones que amigos que te contestan.",
        "Tu carrito tiene más drama que una novela turca.",
        "Gastás en cosas que no recordás ni en qué caja están.",
        "Sos el rey del ‘es solo una luca’. Sí, claro.",
        "Tu futuro yo te está bloqueando en Instagram.",
        "Tu billetera pidió licencia psiquiátrica.",
        "Tu economía personal está en terapia, ya van 6 sesiones.",
        "Tu nivel de ratoneo es extremo. Y todavía estamos a mitad de mes.",
        "Estás tan seco que la cuenta del banco te ofrece psicólogo.",
        "Te arrancaron la cabeza con esa compra, capo.",
        "Tu tarjeta tiene más viajes que Messi en Mundial.",
        "Tu chanchito se rompió solo para no ver más esto.",
        "Caja de ahorro del desastre: saldo emocional cero.",
        "Reservas estratégicas: birra fría y mate cebado, nada más.",
        "Con esa guita en 2001 eras Rockefeller. 😭",
        "Te quedaste en bolas y todavía falta sueldo.",
        "Reventaste la billetera y la billetera te pasó factura.",
        "La biyuya no alcanza ni para el bondi a quejarte al banco.",
    )

    private val predicciones = listOf(
        "Esta semana vas a comprar algo y arrepentirte en 3 días. Cábala.",
        "Veo en mi bola de cristal: una promo te va a manipular. RESISTÍ.",
        "Las estrellas dicen que el delivery te va a tentar a las 22:47. Tené cuidado.",
        "El horóscopo financiero dice: hoy te ofrecen cuotas sin interés. NO.",
        "Predicción: si abrís Mercado Libre antes de dormir, mañana llorás.",
        "Mercurio retrógrado financiero. No firmes nada.",
        "Te veo pidiendo un alfajor y un café. NO LO HAGAS.",
        "Hay un 87% de probabilidad de que mañana digas 'me lo merezco'. NO TE LO MERECÉS.",
        "Veo un debit en tu cuenta que te va a doler. Anda preparándote.",
        "Cábala: si lográs no gastar hoy, mañana te llega un sobre con guita.",
        "Urano en tu casa 12: vas a justificar un gasto con ‘inversión’.",
        "Júpiter dice: alguien te va a mandar un link de oferta. Bloquealo.",
        "Veo un viernes con fiaca y un pedido de pizza inevitabe… o no. Vos decidís.",
        "Las cartas muestran un 2x1 que no necesitás.",
        "Predicción: tu yo del lunes odia a tu yo del domingo.",
        "Saturno te mira feo si tocás la tarjeta después de las 23.",
        "Hoy la suerte favorece a los que no abren bancos en el celu.",
        "Veo un mensaje de ‘últimas unidades’. Es mentira. Siempre hay más.",
        "Tu horóscopo dice ‘ahorrá’. El universo no está de humor.",
        "Las runas hablan: no compres el gadget nuevo. Todavía.",
        "Predicción: vas a encontrar plata en un bolsillo… y gastarla en 4 minutos.",
        "Veo una app de comida abriéndose sola. Brujería del hambre.",
        "Marte en cuadratura: pelea con vos mismo antes de pagar.",
        "Las estrellas alinean un rembolso… si no lo gastás en boludeces.",
        "Predicción falopa: si no gastás hoy, el universo te debe un favor.",
        "Veo en tu futuro un 'me lo merezco'. Cancelalo a tiempo.",
        "Las cartas anuncian: los crocantes se van a esfumar el viernes.",
        "Predicción: vas a decir 'es solo una luca' tres veces y te van a salir 30.",
        "El horóscopo dice 'modo ahorro'. Tu billetera dice 'modo plegaria'.",
        "Augurio: tus reservas estratégicas son birra y dignidad.",
    )

    private val consejos = listOf(
        "Pagá en efectivo. Te va a doler más, vas a gastar menos.",
        "Si lo viste y lo querés, no lo necesitás.",
        "Diversificá: 50% supervivencia, 30% capricho, 20% colchón.",
        "El mejor descuento del mundo: no comprar.",
        "Anotá CADA gasto. Hasta el cafecito. La realidad duele.",
        "Negociá los servicios cada 6 meses. Internet, streaming, telefonía.",
        "Vendé lo que no usás hace 6 meses. Marketplace.",
        "El presupuesto es la mejor app antifraude contra vos mismo.",
        "Si te tienta, salí del shopping. Físico o virtual.",
        "Ahorrá automatizado. Lo que no ves, no gastás.",
        "Separá guita en otra cuenta apenas cobrás. Pagáte primero.",
        "Regla 48hs en compras >30 lucas. Siempre.",
        "Comprá genérico en almacén: el sabor es el mismo, el precio no.",
        "Batch cooking domingo: te salvás de delivery toda la semana.",
        "Compará precio por kilo, no por paquete bonito.",
        "Arma fondo de emergencia antes de invertir en cosas locas.",
        "Dólar MEP o cuenta remunerada: no dejes todo en caja de ahorro.",
        "Si tenés deuda cara, pagá eso antes de ‘invertir’ en birra craft.",
        "Automatizá un débito chico a un FCI. Empezá con lo mínimo.",
        "Revisá suscripciones: sobran tres que ni recordás.",
        "Evitá comprar hambriento: comé y después decidís.",
        "Llevá lista al super: el marketing te come vivo sin lista.",
        "Un día al mes ‘sin gasto hormiga’: suma más de lo que creés.",
        "Renegociá el celular: un llamado te ahorra meses.",
        "Si no entendés el producto financiero, no metas plata.",
    )

    private val secure = SecureRandom()

    fun spin(): Frase {
        val salt = secure.nextLong() xor System.nanoTime() xor
            Runtime.getRuntime().freeMemory().xor(Runtime.getRuntime().totalMemory())
        val rng = Random(salt)

        // A veces mezclamos sub-pool para más sorpresa (misma categoría, distinta vibra).
        val tipo = Tipo.entries[rng.nextInt(Tipo.entries.size)]
        val texto = when (tipo) {
            Tipo.DESAFIO -> desafios.random(rng)
            Tipo.INSULTO -> insultos.random(rng)
            Tipo.PREDICCION -> predicciones.random(rng)
            Tipo.CONSEJO -> consejos.random(rng)
        }
        return Frase(tipo = tipo, texto = texto)
    }
}
