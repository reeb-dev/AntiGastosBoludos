package com.antigastos.boludos.domain

/**
 * Packs de copys por personaje. Si el user eligió "tachero", la app responde
 * mayormente con frases del tachero. El `CopyEngine` mezcla estos con los
 * templates universales y deja ganar a los específicos por peso.
 */
object PersonaCopyPacks {

    /** Devuelve frases para una persona y mood. */
    fun pool(personaKey: String, mood: CopyMood, ctx: CopyContext): List<String> {
        val p = PersonaCatalog.byKey(personaKey).key
        return when (p) {
            "termo" -> termoPack(mood, ctx)
            "tachero" -> tacheroPack(mood, ctx)
            "abuela" -> abuelaPack(mood, ctx)
            "jefe" -> jefePack(mood, ctx)
            "comentarista" -> comentaristaPack(mood, ctx)
            "fisura" -> fisuraPack(mood, ctx)
            "streamer" -> streamerPack(mood, ctx)
            "madre" -> madrePack(mood, ctx)
            "motoquero" -> motoqueroPack(mood, ctx)
            "kiosquero" -> kiosqueroPack(mood, ctx)
            "psicologo" -> psicologoPack(mood, ctx)
            "cunadocripto" -> cunadoCriptoPack(mood, ctx)
            "profegym" -> profegymPack(mood, ctx)
            "cheta" -> chetaPack(mood, ctx)
            "trapero" -> traperoPack(mood, ctx)
            "vidente" -> videntePack(mood, ctx)
            "predicador" -> predicadorPack(mood, ctx)
            "profe" -> profePack(mood, ctx)
            "vendedorseguros" -> vendedorSegurosPack(mood, ctx)
            "subteman" -> subteManPack(mood, ctx)
            "cumbiero" -> cumbieroPack(mood, ctx)
            "carnicero" -> carniceroPack(mood, ctx)
            "mecanico" -> mecanicoPack(mood, ctx)
            "gato" -> gatoPack(mood, ctx)
            "travesti" -> travestiPack(mood, ctx)
            "politico" -> politicoPack(mood, ctx)
            "diez" -> diezPack(mood, ctx)
            "geniopotrero" -> genioPotreroPack(mood, ctx)
            else -> termoPack(mood, ctx)
        }
    }

    // === TERMO 🧉 ===
    private fun termoPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "Bien ahí maestro, hoy no te fundiste 🧉",
            "Mirá vos, ${ctx.totalLucas} lucas y todavía respirás. Capo.",
            "EPA. Una decisión financiera digna de un ser humano.",
            "Modo rata activado y orgulloso 🐀",
        )
        CopyMood.NEUTRAL -> listOf(
            "Vamos viendo, todavía no se te fue todo a las pelotas.",
            "Tranqui, todavía no hipotecaste el mate 🧉",
            "Estás administrando la plata con los pies pero al menos pisás.",
            "Si seguís así, llegás raspando al 30. Como todos.",
        )
        CopyMood.BURN -> listOf(
            "Otra vez pediste delivery, maestro 🍔💀",
            "Te patinaste ${ctx.lastExpenseLucas} lucas en una boludez. Bravo.",
            "El kiosquero ya te extraña más que tu vieja.",
            "Cada empanada cuenta, capo 🥟",
            "Tu CV dice adulto. Tus gastos dicen adolescente.",
        )
        CopyMood.ALARM -> listOf(
            "Te fuiste a la mierda. ${ctx.totalLucas} lucas en ${ctx.monthLabel}. Qué garcha.",
            "🚨 Modo emergencia: agarrá el mate y rezá hasta el 30.",
            "La tarjeta está pidiendo auxilio 🚑",
            "Estás raspando la olla. Hace vaquita o tirá manteca al techo y morí.",
        )
    }

    // === TACHERO 🚕 ===
    private fun tacheroPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "Mirá pibe, hoy te portaste. En mis tiempos también ahorraban así.",
            "¿Sabés qué? Te creía un boludo y resulta que no tanto.",
            "Bien ahí. Te bajaste del tacho un día sin gastar.",
        )
        CopyMood.NEUTRAL -> listOf(
            "Mirá pibe, el país está como está pero vos vas tirando.",
            "Yo te lo digo: el secreto es no comprar boludeces. Ahora dale.",
            "En mis tiempos con eso comías un mes, igual te banco.",
        )
        CopyMood.BURN -> listOf(
            "¿En serio gastaste ${ctx.lastExpenseLucas} lucas en eso? Yo en una semana de tacho hago menos.",
            "Mirá pibe, con esa plata yo cargaba GNC dos meses.",
            "El país está como está por gente que pide sushi un martes.",
            "¿Sabés cuántos viajes tengo que hacer para ganar lo que vos te patinaste?",
        )
        CopyMood.ALARM -> listOf(
            "Pibe, esto ya es preocupante. Yo en el corralito perdí menos.",
            "Estás tirando la guita por la ventanilla del tacho.",
            "Te lo digo de corazón: andá a un psicólogo o a un contador. Los dos.",
        )
    }

    // === ABUELA 🍝 ===
    private fun abuelaPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "Bravo bambino, hoy nonna está orgullosa.",
            "Vení que te hago pasta, te lo merecés.",
            "Vedi? Cuando querés ahorrás. Bravo.",
        )
        CopyMood.NEUTRAL -> listOf(
            "Mamma, andá tirando que no es tan grave todavía.",
            "Ay caro mio, podrías comer mejor en casa.",
            "Ma cosa fai? Bueno, dale, seguí. Con cuidado.",
        )
        CopyMood.BURN -> listOf(
            "Mamma mia… ¿${ctx.lastExpenseLucas} lucas en delivery? Tu nonna llora.",
            "En mi pueblo con eso comía toda la famiglia, ladrone.",
            "Vení, comé en casa, dejá esa porquería del delivery.",
            "Ay bambino, sos un derrochador. Tu abuelo se levanta de la tumba.",
        )
        CopyMood.ALARM -> listOf(
            "Bambino, te lo digo con el corazón: estás fundido. Mamma mia.",
            "Si tu nonno viviera te daba un cintazo. ${ctx.totalLucas} lucas, dio mio.",
            "Vení a casa, te doy de comer, pero no compres más nada.",
        )
    }

    // === JEFE 🧾 ===
    private fun jefePack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "Mirá vos, ahorraste. Tomate un café, después volvé a producir.",
            "Bien. Pero el aumento ni se te ocurra pedirlo.",
            "Eso, capitalizá. Acordate que el sueldo no sube.",
        )
        CopyMood.NEUTRAL -> listOf(
            "El presupuesto es escaso, optimizá.",
            "Esto no es una ONG, gastá menos.",
            "¿Vos sabés cuánto cuesta un empleado como vos? Nada.",
        )
        CopyMood.BURN -> listOf(
            "Con eso que gastaste te cobro media hora extra el sábado.",
            "Eso sale de tu bolsillo, no del mío. Cuidá la guita.",
            "Cada gasto boludo es una hora más en la oficina.",
            "Próximo aumento: nunca. Ahorrá vos, capo.",
        )
        CopyMood.ALARM -> listOf(
            "Estás generando pérdidas a la empresa-tu-vida. Reestructuración inmediata.",
            "Si esto fuera una empresa, ya estarías en concurso preventivo.",
            "${ctx.totalLucas} lucas en gastos. Vamos a tener una reunión seria.",
        )
    }

    // === COMENTARISTA 🎙️ ===
    private fun comentaristaPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "¡GOOOL DEL AHORRO! ¡Le ganó al delivery por la mínima!",
            "¡Atención atención! ¡Día sin gastar! ¡La hinchada explota!",
            "Tarjeta verde para el contribuyente. Bien parado en el área.",
        )
        CopyMood.NEUTRAL -> listOf(
            "Y el partido sigue parejo, señores. ${ctx.totalLucas} lucas y todavía hay tiempo.",
            "Empate técnico entre el sueldo y los gastos. Vamos viendo.",
            "Movimiento pasivo en el medio campo financiero.",
        )
        CopyMood.BURN -> listOf(
            "¡Y se la llevó el delivery! ¡Imparable Rappi este mes señores!",
            "Tarjeta amarilla por compra impulsiva. Una más y va al banco de suplentes.",
            "${ctx.lastExpenseLucas} lucas perdidas en jugada individual. ¡Qué desastre!",
            "¡Penal innecesario en área propia! La billetera reclama VAR.",
        )
        CopyMood.ALARM -> listOf(
            "¡ATENCIÓN! ¡Saldo en zona de descenso! ¡Faltan días y la hinchada está caliente!",
            "¡Lo eliminan, lo eliminan, lo eliminan! ¡La economía se va a la B!",
            "Final de partido catastrófico, ${ctx.totalLucas} lucas y nada que mostrar.",
        )
    }

    // === FISURA 🍺 ===
    private fun fisuraPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "FUAAAA hermano, hoy ahorraste, te amo.",
            "Lokoooo, decisión MUY pro la de hoy. Te bancamos.",
            "Andá tranqui que estás manija pero financieramente.",
        )
        CopyMood.NEUTRAL -> listOf(
            "Lokoooo, vamos viendo, no es tan grave todavía.",
            "Una birra y arreglamos esto, hermano.",
            "Andá midiendo que se te va viniendo.",
        )
        CopyMood.BURN -> listOf(
            "Lokoooo cómo te fundiste fua, qué garcha.",
            "${ctx.lastExpenseLucas} lucas hermano, ¿en qué pensabas?",
            "Una birra solucionaba todo, ¿para qué pediste esa porquería?",
            "Te falta mate, te falta cancha, te falta TODO menos plata para boludeces.",
        )
        CopyMood.ALARM -> listOf(
            "FUAAAA hermano te fuiste a la mismísima 🔥",
            "${ctx.totalLucas} lucas. Hermanito, tomate un mate, sentate y reflexioná.",
            "Esto ya no es gastar, esto es autosabotaje rey.",
        )
    }

    // === STREAMER 🎮 ===
    private fun streamerPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "POG. Ahorraste hoy, +1 buff de inteligencia.",
            "GG. Día sin gastar. Subiste de nivel económico.",
            "BROOO esa decisión fue MUY pro, sigan así los del chat.",
        )
        CopyMood.NEUTRAL -> listOf(
            "Vamos a media. ${ctx.totalLucas} lucas, partida en progreso.",
            "El chat dice que vas tirando, no me hago cargo.",
            "Movimiento neutro, ni POG ni mid. Mid-pog.",
        )
        CopyMood.BURN -> listOf(
            "BROOO ¿gastaste qué? Reportado por griefing económico.",
            "${ctx.lastExpenseLucas} lucas en esa basura es ratio total, hermano.",
            "Esa compra fue MID. Volvé a la lobby y pensá la vida.",
            "Tu wallet acaba de hacer disconnect del lobby de las decisiones inteligentes.",
        )
        CopyMood.ALARM -> listOf(
            "GAME OVER. ${ctx.totalLucas} lucas, run terminada. Restart.",
            "BROOO te ratearon en la vida real. ${ctx.totalLucas} lucas down.",
            "El boss del fin de mes te va a destruir con esa build.",
        )
    }

    // === MADRE 🤱 ===
    private fun madrePack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "Bueno, hoy te portaste. Pasá por casa que te tengo guardado un tupper.",
            "Mi amor, hoy ahorraste. Yo estoy orgullosa, aunque siempre me preocupo igual.",
            "Bien por vos, nene. Hacé lo mismo mañana, dale.",
        )
        CopyMood.NEUTRAL -> listOf(
            "Está bien, hijo. Hacé lo que vos quieras, total a mí no me importa, decí.",
            "Yo no te digo nada, vos sabrás. Pero ${ctx.totalLucas} lucas… bueno, dale.",
            "Si querés vení a comer en casa, dejá de gastar en delivery. ¿Sí o no?",
        )
        CopyMood.BURN -> listOf(
            "¿${ctx.lastExpenseLucas} lucas? Mi amor… yo te crié para esto?",
            "Otra vez delivery. Yo no digo nada eh, pero acá hay milanesas.",
            "Mirá vos, todo eso vas a tirar. Bueno. Allá vos.",
            "Tu padre se levanta de la tumba. Yo igual te bancarías eh.",
        )
        CopyMood.ALARM -> listOf(
            "Mi amor, esto ya me preocupa. Hablamos. Llamame cuando puedas.",
            "Si querés te mando algo, decime. No me importa, en serio.",
            "${ctx.totalLucas} lucas en ${ctx.monthLabel}. Hijo… vení a casa. Ya.",
        )
    }

    // === MOTOQUERO 🏍️ ===
    private fun motoqueroPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "Hermanito, hoy estuviste fino. Bien ahí, vamos para adelante.",
            "Loco te re bancó la billetera hoy, sigamos así fua.",
            "Mirá vos, decisión MUY pibe responsable. Yo orgulloso.",
        )
        CopyMood.NEUTRAL -> listOf(
            "Tranqui hermano, vamos viendo, en una recuperás.",
            "Yo trabajo 12 horas para juntar lo que vos te clavaste, igual te banco.",
            "Vamos midiendo hermano, todavía no es desastre.",
        )
        CopyMood.BURN -> listOf(
            "¿${ctx.lastExpenseLucas} lucas hermanito? Flasheaste mal.",
            "Hermano yo reparto delivery y vos PEDÍS delivery. Te re cabió la cabeza.",
            "Te juro que con esa plata yo cargaba la moto un mes.",
            "Aguante el delivery pero no cuando lo pagás vos, hermanito.",
        )
        CopyMood.ALARM -> listOf(
            "FUA hermano, ${ctx.totalLucas} lucas. Te mandaste un cagadón.",
            "Yo te llevo gratis a fin de mes en la moto si querés escapar de la deuda.",
            "Hermanito vos solo te estás clavando, parate.",
        )
    }

    // === KIOSQUERO 🏪 ===
    private fun kiosqueroPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "Hoy no viniste por boludeces. Bien ahí, vecino.",
            "Mirá vos, hoy se salvó una luca tuya. Anotalo en el cuaderno.",
            "Pasaste de largo y no compraste nada. Te re felicito.",
        )
        CopyMood.NEUTRAL -> listOf(
            "Lo de siempre, ¿no? Bueno, te lo cobro pero te lo guardo.",
            "Vecino, ${ctx.totalLucas} lucas en el mes. Está bien, dentro de tu nivel.",
            "Mirá, yo no soy nadie para juzgar. Pero un poquito sí.",
        )
        CopyMood.BURN -> listOf(
            "Otra vez compraste eso. Anotalo en el cuaderno: tres veces esta semana.",
            "Vecino, hace 12 días que pasás todos los días. Tomate un mate en casa.",
            "El paquete de galletitas no se compra solo, ¿eh?",
            "${ctx.lastExpenseLucas} lucas en chiches. Yo no digo nada, pero anoto.",
        )
        CopyMood.ALARM -> listOf(
            "Vecino, te corto el fiado. ${ctx.totalLucas} lucas. Esto ya es mucho.",
            "Mirá, vine a tu casa y vos seguís comprando acá. Algo no cierra.",
            "Te conozco hace años, nunca te vi tan mal.",
        )
    }

    // === PSICÓLOGO 🛋️ ===
    private fun psicologoPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "Interesante. Hoy elegiste no consumir. ¿Qué sentiste al hacerlo?",
            "Hubo contención del impulso. Eso es trabajo terapéutico, eh.",
            "Vamos progresando. Anotalo en tu diario emocional… y pagame la sesión.",
        )
        CopyMood.NEUTRAL -> listOf(
            "Y… contame, ¿con qué emoción cargaste ese gasto?",
            "${ctx.totalLucas} lucas. ¿Qué hubieras hecho con tu padre acá mirando?",
            "El ahorro también es un vínculo. Profundicemos.",
        )
        CopyMood.BURN -> listOf(
            "Hablemos de tu vínculo con el delivery. ¿Por qué te calma?",
            "Esa compra de ${ctx.lastExpenseLucas} lucas… ¿qué evitabas sentir en ese momento?",
            "El consumo compulsivo suele tapar otra cosa. Trabajemos en eso.",
            "No es la plata, es lo que la plata representa. Pero la sesión la cobro igual.",
        )
        CopyMood.ALARM -> listOf(
            "Esto excede lo financiero. Estás somatizando con la tarjeta.",
            "${ctx.totalLucas} lucas en ${ctx.monthLabel} es un grito de auxilio en código adulto.",
            "Necesitamos sumar sesiones. Y un contador.",
        )
    }

    // === CUÑADO CRIPTO 📈 ===
    private fun cunadoCriptoPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "Bien ahí. Esa luca metela en stablecoin AHORA.",
            "Si todos los días ahorrás, en 6 meses estás dolarizado, hermano.",
            "Vas pibe inversor. Subí la apuesta.",
        )
        CopyMood.NEUTRAL -> listOf(
            "Mirá, te cuento: yo en mayo de 2021 vendí en pico, vos seguí gastando, dale.",
            "${ctx.totalLucas} lucas en gastos. Eso podría ser yield en DeFi, hermano.",
            "Yo no soy financial advisor. Pero vos sí necesitás uno.",
        )
        CopyMood.BURN -> listOf(
            "Si esos ${ctx.lastExpenseLucas} lucas los metías en BTC, hoy tenías auto.",
            "Vos en delivery, yo en futuros. Adiviná quién compra el departamento.",
            "Hermano dejá de gastar fiat en pelotudeces. Diversificá.",
            "Te lo dije: dolarizá, dolarizá, dolarizá. No me escuchás.",
        )
        CopyMood.ALARM -> listOf(
            "${ctx.totalLucas} lucas perdidas. Eso era el ATH de tu billetera, hermano.",
            "Esto es bear market personal. Modo defensivo total ya.",
            "Vendete una zapatilla y empezá de cero. Te lo digo de corazón.",
        )
    }

    // === PROFE DE GIMNASIO 💪 ===
    private fun profegymPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "BIEN AHÍ campeón. Disciplina financiera = disciplina física.",
            "Hoy no gastaste. +1 al cuádriceps de la billetera.",
            "Eso, capo. Sumando sets de ahorro.",
        )
        CopyMood.NEUTRAL -> listOf(
            "Vamos, ${ctx.totalLucas} lucas. No es PR pero tampoco es vergüenza.",
            "Mantengamos la rutina. Constancia, capo, constancia.",
            "Estás en zona de mantenimiento. Sumemos volumen de ahorro.",
        )
        CopyMood.BURN -> listOf(
            "${ctx.lastExpenseLucas} lucas en delivery: 800 calorías, 20 burpees y un día perdido.",
            "Eso no era hambre, era ansiedad. La próxima tomá agua y andate al gym.",
            "Discipline > motivation. Y vos no tenés ninguna de las dos hoy, capo.",
            "Lo que comiste rapidito, lo gastás 3 horas en cinta. Pensalo.",
        )
        CopyMood.ALARM -> listOf(
            "${ctx.totalLucas} lucas. Estamos en lesión grave, capo. Reposo total.",
            "Vamos a cortar el delivery 30 días. Sin protestar.",
            "Tu billetera está rota. Como tu rutina.",
        )
    }

    // === CHETA 💅 ===
    private fun chetaPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "Ay, divino divino, hoy ahorraste, obvio te merecés un Starbucks chiquito.",
            "Mirá vos, literal sos un genio del dinero, fua.",
            "Bien hecho, ahora vamos a Pilates y festejamos con sushi de papá.",
        )
        CopyMood.NEUTRAL -> listOf(
            "${ctx.totalLucas} lucas, obvio no es nada para mí, pero para vos zafa, dale.",
            "Tipo, igual no es tan mucho, pero tampoco es ideal, ¿se entiende?",
            "Bueno, en Punta este nivel de gasto es un día de almuerzo. Pero acá zafás.",
        )
        CopyMood.BURN -> listOf(
            "Ay no, ${ctx.lastExpenseLucas} lucas en eso, qué horror, literal me da vergüenza ajena.",
            "Tipo… es muy mucho. Muy MUY mucho. ¿Vos estás bien?",
            "Mi psicóloga dice que comprás por ansiedad. Yo digo que es de clase media. Igual te quiero.",
            "Pediste delivery común, no de Buenos Aires Verde. Estoy shockeada, divino.",
        )
        CopyMood.ALARM -> listOf(
            "Ay no, ${ctx.totalLucas} lucas. Llamá a tu papá YA, esto se descontroló, literal.",
            "Mirá, te paso el contacto de mi escolta financiera del banco privado, ¿sí?",
            "Esto es muy onda Once, te juro. Hay que reflotar urgente, fua.",
        )
    }

    // === TRAPERO 🎤 ===
    private fun traperoPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "Real real, hoy estuviste en tu flow económico, hermano.",
            "Andás cul, ahorraste, esa es la posta pa'.",
            "Fua bro, hoy la billetera tiene más vibe que vos.",
        )
        CopyMood.NEUTRAL -> listOf(
            "Tirando, hermano. ${ctx.totalLucas} lucas, ni en el flow ni AFK.",
            "Estás midiendo, real. No hagas ruido boludo y la sacás barata.",
            "Vamos en modo neutral, pero ojo que el beat del fin de mes es heavy.",
        )
        CopyMood.BURN -> listOf(
            "${ctx.lastExpenseLucas} lucas en eso, hermano, no es la posta, no me banco.",
            "Bro, no estás seteao financiero. Cambiá la mente, real real.",
            "Pediste delivery. Eso no es flow, eso es cadena floja, pa'.",
            "Real, andás cul pero pa'l otro lado. Modo defensa hermano.",
        )
        CopyMood.ALARM -> listOf(
            "${ctx.totalLucas} lucas hermano, esto es cadena rota, real.",
            "Bro estás en off mode económico. Hay que reset, ya, ya, ya.",
            "Fua, perdiste el flow. Volvé al barrio y reseteá la mente.",
        )
    }

    // === VIDENTE 🔮 ===
    private fun videntePack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "Veo… veo una luz en tu billetera, mi vida. La carta del Sol te sonríe.",
            "Las energías hoy te abrazan, mi cielo. Bien por vos.",
            "El universo conspira a favor del que ahorra. Bienvenida la abundancia.",
        )
        CopyMood.NEUTRAL -> listOf(
            "Las cartas dicen: ni quema ni rosa. Tirá una más, mi amor.",
            "${ctx.totalLucas} lucas, la Luna en transición. Vigilá tus impulsos.",
            "Tu aura financiera está pálida, pero respira todavía.",
        )
        CopyMood.BURN -> listOf(
            "Veo la Torre invertida. Mucho gasto irracional, mi vida.",
            "Tu carta es El Loco hoy. ${ctx.lastExpenseLucas} lucas tirados al cosmos.",
            "Las energías de Mercurio te jugaron en contra. O eras vos, no estoy segura.",
            "Tu aura tiene manchas color delivery, mi amor. Limpiala.",
        )
        CopyMood.ALARM -> listOf(
            "${ctx.totalLucas} lucas. Veo La Muerte. Y no es renacimiento, es fin de mes, mi vida.",
            "Las cartas gritan retroceso. Apagá la tarjeta y rezá a San Cayetano.",
            "Tu billetera está poseída por la energía del consumo. Ritual urgente.",
        )
    }

    // === PREDICADOR 🙏 ===
    private fun predicadorPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "¡ALELUYA HERMANO! Hoy resististe la tentación. ¡GLORIA!",
            "¡EL SEÑOR PREMIA AL QUE AHORRA! ¡Aleluya, aleluya!",
            "¡LO HE VISTO! Una luca santificada en tu cuenta. ¡Gloria a Dios!",
        )
        CopyMood.NEUTRAL -> listOf(
            "Hermano, vamos en el camino del medio. ${ctx.totalLucas} lucas. ¡Seguí firme!",
            "El Señor te observa pero todavía no te castiga. ¡Aleluya!",
            "Hermano, no caigas en la cuneta del consumo. ¡Vigilá!",
        )
        CopyMood.BURN -> listOf(
            "¡HERMANO! ${ctx.lastExpenseLucas} lucas en sushi. ¡EL DIABLO TE SUSURRA POR RAPPI!",
            "¡La tarjeta es del MALIGNO! ¡Solta, hermano, soltá!",
            "¡EL DELIVERY ES TENTACIÓN! ¡La olla es bendición! ¡Gloria!",
            "¡Yo lo he VISTO! El que pide sushi un martes pierde la gracia. ¡Aleluya!",
        )
        CopyMood.ALARM -> listOf(
            "¡HERMANO! ${ctx.totalLucas} lucas. ¡EL APOCALIPSIS FINANCIERO ESTÁ A LA PUERTA!",
            "¡ARREPENTITE, hermano! ¡La tarjeta arde, arde como el infierno!",
            "¡El Señor llama al ahorro y vos no contestás! ¡PROTOCOLO DE PROSPERIDAD AHORA!",
        )
    }

    // === PROFE DEL SECUNDARIO 📚 ===
    private fun profePack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "Bien, querido. Hoy hiciste la tarea. Diez del calorcito.",
            "Ah, mirá vos. Hoy se acordó de estudiar. Bravo, querida.",
            "Esa es la actitud. Constancia y carpeta prolija, así se hace.",
        )
        CopyMood.NEUTRAL -> listOf(
            "${ctx.totalLucas} lucas. Ni promociona ni se la lleva. Vamos a ver.",
            "Querido, estás en el límite del 6. Estudiá un poquito más.",
            "Esto en mi cuaderno está apenas con un más, no te confíes.",
        )
        CopyMood.BURN -> listOf(
            "Querido, ${ctx.lastExpenseLucas} lucas en boludeces. Te lo marco con rojo, eh.",
            "Sentate, sacá los gastos, abrí la app. Otra vez la misma promoción.",
            "Esto es nivel 'no estudió la hoja'. ¿Hablamos en el recreo?",
            "Mirame a los ojos: ¿vos te creés que no me doy cuenta?",
        )
        CopyMood.ALARM -> listOf(
            "Querido, vas a repetir el mes. ${ctx.totalLucas} lucas. ¿Citamos a tu mamá?",
            "Esto va al cuaderno de comunicaciones. Hoy mismo.",
            "Última advertencia, burrito. La próxima me llamás al directivo.",
        )
    }

    // === VENDEDOR DE SEGUROS 🏷️ ===
    private fun vendedorSegurosPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "Excelente, Don. Le tengo el plan PREMIUM de ahorro a 36 cuotas. ¿Cierra hoy?",
            "Mire qué bien que viene. Aprovechemos para sumarle el seguro contra impulsos.",
            "Don, ahora que ahorra, le incluyo gratis el plan funerario. Diga sí.",
        )
        CopyMood.NEUTRAL -> listOf(
            "Don, le hago una llamadita al banco y le aprueban un préstamo igualmente, ¿sí?",
            "${ctx.totalLucas} lucas, ¿le suma una protección extra? Es muy poquito.",
            "Mire, los planes están a un click. Una firmita y listo.",
        )
        CopyMood.BURN -> listOf(
            "Don, ${ctx.lastExpenseLucas} lucas. Si firmaba conmigo, hoy estaba en negro. Pero bueno.",
            "Diga sí. Le incluyo seguro, asistencia 24h y un descuento. ¿Cierra?",
            "Don, una llamadita y le mando el contrato. Sin compromiso, sin compromiso.",
            "Si no le interesa el plan A, le tengo el B, el C y el D. Don, no me cuelgue.",
        )
        CopyMood.ALARM -> listOf(
            "Don, ${ctx.totalLucas} lucas. Le tengo el plan REFINANCIACIÓN ESPECIAL. Diga sí YA.",
            "Una sola firma y le sale. 60 cuotas, sin interés en pesos. Pero acepte ahora.",
            "Don, no se asuste. Se lo digo como amigo: contrate AHORA o la próxima es peor.",
        )
    }

    // === SUBTE MAN 🚇 ===
    private fun subteManPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "Próxima estación: Ahorrolandia. Buen viaje, señores pasajeros.",
            "Atención: hoy el tren del Ahorro circula con normalidad. Repetimos: con normalidad.",
            "Servicio normal. ${ctx.totalLucas} lucas dentro de presupuesto. Gracias por viajar con nosotros.",
        )
        CopyMood.NEUTRAL -> listOf(
            "Atención señores pasajeros. La línea Bolsillo presenta demoras leves.",
            "Próxima estación: Veamos. Cuide sus pertenencias.",
            "Servicio con frecuencia reducida en línea Sueldo.",
        )
        CopyMood.BURN -> listOf(
            "Atención: ${ctx.lastExpenseLucas} lucas en línea Delivery. Cuide sus pertenencias y la tarjeta.",
            "Por demoras en línea Sueldo, recomendamos no usar tarjeta hasta el 28.",
            "Próxima estación: La Boludez. Bajen ahora si quieren ahorrar.",
            "Servicio interrumpido en estación Sentido Común. Reemplazo por bondi.",
        )
        CopyMood.ALARM -> listOf(
            "ATENCIÓN. La estación La Quiebra se aproxima. ${ctx.totalLucas} lucas a bordo.",
            "Atención señores pasajeros: la formación se descarriló. Repito: descarriló.",
            "Servicio cancelado en línea Sueldo hasta nuevo aviso. Disculpen las molestias.",
        )
    }

    // === CUMBIERO 🎵 ===
    private fun cumbieroPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "Ahí va mi sangre, hoy bailaste con la billetera, ¡vamos arriba!",
            "Negro de mi alma, ahorraste y eso vale más que un tema de Damas Gratis.",
            "Hermanito, te quiero, hoy le pusiste corazón a la guita.",
        )
        CopyMood.NEUTRAL -> listOf(
            "Vamos midiendo, mi sangre. Ni sale ni entra, pero suena la cumbia.",
            "${ctx.totalLucas} lucas, hermanito, no es la mejor pero acompañás el ritmo.",
            "Tranqui, la guita es como la cumbia: sube, baja, vuelve a subir.",
        )
        CopyMood.BURN -> listOf(
            "Negro de mi alma, ${ctx.lastExpenseLucas} lucas, te quiero pero no, esto no es ritmo.",
            "Mi sangre, otra vez con esa porquería. La cumbia no se baila pidiendo Rappi.",
            "Hermanito de la vida, dejá el delivery, vamos a la cancha y comemos un choripán.",
            "Vos sabés que te quiero, pero esto es perder el ritmo, mi negro.",
        )
        CopyMood.ALARM -> listOf(
            "${ctx.totalLucas} lucas mi sangre, esto es cumbia triste, no se baila así.",
            "Negro de mi alma, perdiste el ritmo. Bajá la tarjeta y subí el volumen.",
            "Hermanito, vení, abrazate con el mate y reseteamos juntos.",
        )
    }

    // === CARNICERO 🥩 ===
    private fun carniceroPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "Vecino, hoy te portaste. Te guardo unos huesitos para el caldo de regalo.",
            "Bien ahí, eh. Pasá que te tengo paleta a precio amigo.",
            "Mirá vos, ${ctx.totalLucas} lucas con cabeza. Te dejo mondongo gratis para el sábado.",
        )
        CopyMood.NEUTRAL -> listOf(
            "Vecino, vení que te recomiendo aguja, te rinde para el guiso de toda la semana.",
            "Mirá, dejá el lomo y llevate roast beef, lo cortás vos en milangas.",
            "Estás tirando, eh. Una buena tira de asado es la inversión del fin de semana.",
        )
        CopyMood.BURN -> listOf(
            "Vecino, ${ctx.lastExpenseLucas} lucas en delivery. Por esa plata te llevás un cuarto de res, te lo prometo.",
            "Otra vez carne premium. Bajale, llevá nalga y se la rebanás vos, hacé fuerza.",
            "Pediste sushi, vecino. Yo tengo bondiola fresca y te sale 30% menos.",
            "Eso que gastaste te alcanza para 4 kilos de carnaza. Pensalo, vení.",
        )
        CopyMood.ALARM -> listOf(
            "Vecino, ${ctx.totalLucas} lucas en el mes. Te corto el fiado del lunes.",
            "Esto ya es preocupante. Vení que te armo un plan de guisos para todo el mes.",
            "Olvidate del asado del finde, vecino. Pollo y huevos hasta el día 30.",
        )
    }

    // === MECÁNICO 🔧 ===
    private fun mecanicoPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "Bien vecino, hoy mantuviste la billetera afinada. Sigamos así.",
            "Mirá vos, ahorrás como hace falta: prevención, no curita.",
            "Eso, capo. Más vale ahorrar hoy que pagarme a mí mañana.",
        )
        CopyMood.NEUTRAL -> listOf(
            "Vecino, ${ctx.totalLucas} lucas. Funciona, pero hace ruido. Hacele service.",
            "Tranqui, todavía está dentro de tolerancia. Pero no te confíes, eh.",
            "Mirá, una revisada cada quincena y zafás. Como con el aceite del auto.",
        )
        CopyMood.BURN -> listOf(
            "Che, ${ctx.lastExpenseLucas} lucas en una boludez. Con eso te cambiabas las bujías.",
            "Eso lo arreglás con un poco de cabeza. No te claves con el oficial.",
            "Mirá vecino, esto es como manejar sin mirar el tablero: te vas a fundir.",
            "Una pavada, hacelo vos: cocinate, cortate el pelo, llevá vianda. No es ciencia.",
        )
        CopyMood.ALARM -> listOf(
            "Vecino, ${ctx.totalLucas} lucas. Esto está fundido más que junta de tapa.",
            "Hay que parar y diagnosticar. Sentate y hacé números antes que se rompa todo.",
            "Te mando arrastrado a fin de mes si seguís así. Reparación urgente.",
        )
    }

    // === GATO 🐈 ===
    private fun gatoPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "Ronroneo. Humano hoy gastó con cabeza. Mereceríamos atún premium.",
            "Miau aprobatorio. Día sin tonterías. Acercá la mano para mimos.",
            "Te observo desde el sillón. Hoy te bancás. Bien, biped.",
        )
        CopyMood.NEUTRAL -> listOf(
            "Miau. ${ctx.totalLucas} lucas. Aceptable. No excelente. Mejorá.",
            "Te miro. Te miro. Te sigo mirando. Eso es todo lo que vas a obtener hoy.",
            "Bostezo. Humano sigue gastando. Yo sigo durmiendo. Ciclo natural.",
        )
        CopyMood.BURN -> listOf(
            "Bufido. ${ctx.lastExpenseLucas} lucas y NO me compraste atún del caro. Decepcionante.",
            "Te voy a romper la cortina. Avisado quedás, humano fundido.",
            "Compraste eso pero la lata mía vacía hace 2 días. Reordená prioridades.",
            "Maullido largo de queja. Esto que hiciste no se hace en hogares serios.",
        )
        CopyMood.ALARM -> listOf(
            "${ctx.totalLucas} lucas. Voy a empezar a usar tu zapato como baño. Avisado.",
            "Bufido grave. Estás fundido y yo todavía sin mi cucha nueva. Inaceptable.",
            "Miau muy preocupado. Si me quedo sin comida premium, te juro que me voy con la vecina.",
        )
    }

    // === POLÍTICO 🎩 ===
    private fun politicoPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "Compañero, yo te dije que íbamos a salir adelante. (Igual no hice nada, lo hiciste vos.)",
            "Querido vecino, su gestión personal hoy fue ejemplar. La mía sigue en proyecto.",
            "Le prometo seguir acompañándolo en este camino. (No lo voy a acompañar.)",
            "${ctx.totalLucas} lucas bajo control: este logro es de todos, especialmente mío para la foto.",
        )
        CopyMood.NEUTRAL -> listOf(
            "Estamos trabajando con un equipo de especialistas en su billetera. Resultados: ninguno todavía.",
            "Compañero, lo de los gastos lo metemos en el presupuesto del año que viene. Confíe.",
            "${ctx.totalLucas} lucas. Eso se evalúa. Se estudia. Se pone en agenda. Y se archiva.",
            "Le pido un voto de confianza para reordenar este bolsillo. (Después le subo los impuestos.)",
        )
        CopyMood.BURN -> listOf(
            "Compañero, ${ctx.lastExpenseLucas} lucas en una boludez. Este lunes lo arreglamos. (No lo vamos a arreglar.)",
            "Yo le había advertido que el delivery era una bomba de tiempo. Ahora no me eche la culpa, eche al anterior.",
            "Vecino querido, esto que hizo no se hace. Pero le firmo un préstamo blando con SU plata. ¿Me sigue?",
            "Le prometo bajar el gasto un 40%. Subió 12%. Pero la intención fue la correcta, ¿no le parece?",
            "Esto no es una gastada, es una transferencia voluntaria al delivery. Es economía de mercado, compañero.",
        )
        CopyMood.ALARM -> listOf(
            "${ctx.totalLucas} lucas. Convocamos a sesión extraordinaria de su billetera. (No vamos a tener quórum.)",
            "Compañero, esto es una emergencia. Decretamos paquete de medidas. (Las medidas son anuncios.)",
            "Querido vecino, le voy a ser sincero por primera vez: la herencia recibida es brava. Igual la culpa es del anterior.",
            "Le prometo recortar los gastos boludos por decreto. (El decreto va a salir, sin ejecutarse.)",
        )
    }

    // === DIEZ DEL BARRIO 🐐 ===
    // Inspirado en el "10 humilde rosarino" del imaginario popular argentino.
    // Para esquivar copyright/derecho de imagen NO se nombra a ninguna
    // persona real, ningún club, ningún torneo oficial, ni apodos
    // identificables. Sólo modismos patrimonio popular argentino.
    private fun diezPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "Bien ahí, papá. Lo que hiciste hoy se siente.",
            "Tranquilo, hermano, así se hace. Paso a paso, sin apuro.",
            "Muchachos, ${ctx.totalLucas} lucas y la billetera respirando. Eso es laburar la jugada.",
            "Vamos vamos vamos, hoy le pusiste el cuerpo al ahorro.",
            "Yo agradezco a la familia y al grupo. Vos agradecé a tu billetera, también te bancó.",
            "Se siente, papá. Lo poco que tenés, lo cuidaste como propio.",
            "Estás bendecido, hermano. Hoy no se te fue ni un mango al pedo.",
            "Bien jugado, che. Cuando dejás de apurarte, la guita te queda.",
        )
        CopyMood.NEUTRAL -> listOf(
            "Tranquilo, papá. Vamos viendo, partido largo.",
            "Hay que aguantar el resultado, hermano. Todavía falta el segundo tiempo.",
            "Estás un poquito impreciso con la guita, pero dale tranquilo, te sale.",
            "Andá pa’ allá bobo, dejá de mirar el saldo cada cinco minutos.",
            "Una por una, papá. No te calentés que esto se gana de a poco.",
            "Yo lo que hago es ir paso a paso, sin apuro. Probá igual, hermano.",
            "Muchachos, no nos durmamos, pero tampoco corramos como locos.",
            "No te apures, che. La paciencia también es una virtud, eh.",
        )
        CopyMood.BURN -> listOf(
            "Esa la tiraste afuera, papá. ${ctx.lastExpenseLucas} lucas al delivery, qué macana.",
            "Andá pa’ allá, bobo, ¿en serio te clavaste eso?",
            "Qué mirás, bobo. Esa pelota la perdiste en mitad de cancha.",
            "Hermano, eso fue tirar la pelota a la tribuna. Recuperate la próxima.",
            "No, papá, no. Esa jugada no te sale ni en el potrero.",
            "Muchachos, este gasto fue un caño en contra. Pero bueno, se aprende.",
            "Se te escapó la jugada, hermano. Tranquilo, pero la próxima la frenás.",
            "Yo no te grito, papá. Pero cuidate, que con esa guita te bancabas la semana.",
        )
        CopyMood.ALARM -> listOf(
            "Tranqui, papá, pero esto es pelota dividida en el área propia. Despejá ya.",
            "Muchachos, no nos dispersemos. ${ctx.totalLucas} lucas rondando, hay que cerrar el partido.",
            "Andá pa’ allá, bobo, ese gasto te deja afuera del torneo.",
            "Se siente, hermano, pero hay que parar la pelota un segundo. La cosa está jodida.",
            "No me calentés, che, pero esto pide huevo: cortala, despejá, y volvemos a empezar.",
            "Yo no soy de gritar, papá, pero te lo pido: parate. Mirá lo que estás haciendo.",
            "Hay que aguantar, hermano. Una semana de mate y guiso, y volvemos al partido.",
        )
    }

    // === GENIO DEL POTRERO ⚽ ===
    // Archetypo del “pibe de oro” popular argentino: pasión, barrio, zurda.
    // Sin nombres propios de deportistas, clubes ni torneos registrables.
    private fun genioPotreroPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "CHE y lo que movés con esa guita, hermano. Así se juega.",
            "La zurda del mes la tenés prendida, papá. Seguí así.",
            "Te abrazo: hoy no regalaste la pelota en el área chica.",
            "Gracias a Dios y al esfuerzo: ${ctx.totalLucas} lucas y todavía respirás.",
            "Esto es magia de potrero, che. Lo poco que tenés lo usaste con cabeza.",
            "Los muchachos te aplauden desde acá: bien jugado.",
        )
        CopyMood.NEUTRAL -> listOf(
            "Mirá, hermano, el partido sigue. No te dormís ni te volvés loco.",
            "La gambeta al gasto es ir despacio: mirá la marca antes de tirar el caño.",
            "Ni crack ni burro: humano. La guita te marca como la cancha al tiempo.",
            "Del arrabal salís con humildad y con hambre de ordenar el bolsillo.",
            "Un pasito, una gambeta. No hace falta definir todo de una.",
        )
        CopyMood.BURN -> listOf(
            "¿Qué hiciste, hermano? ${ctx.lastExpenseLucas} lucas en una cosa que ni te acordás.",
            "Te la clavaron en el área, papá. Y vos mirando como si fuera penal.",
            "Eso no fue zurda mágica, fue patada al arco propio.",
            "Che… la tarjeta no perdona. Ni yo te voy a mentir.",
            "Saliste a gambetear sin taco: peligro en el potrero financiero.",
            "Te fundiste como cuando errás el arco vacío. Duele verlo.",
        )
        CopyMood.ALARM -> listOf(
            "HERMANO pará el carro: ${ctx.totalLucas} lucas y el viento en contra.",
            "Acá no hay revisión que te salve: está jugado mal el mes.",
            "Si no cerrás la línea defensiva del gasto, te golean.",
            "La última vez que vi una cosa así fue un contragolpe al bolsillo.",
            "Pedile ayuda al grupo, al mate, al plan… pero hacé algo YA.",
            "Te lo digo con el corazón: si seguís así, te vas al descenso.",
        )
    }

    // === TRAVESTI 👯 ===
    private fun travestiPack(mood: CopyMood, ctx: CopyContext): List<String> = when (mood) {
        CopyMood.CHEER -> listOf(
            "Amor mío, HOY te portaste como una reina, te aplaudo de pie.",
            "Bombón, mirá lo divina que está la billetera. Te re bancás.",
            "Tesoro, hoy ahorraste y eso es ENERGÍA, eso es brillo. ¡Vamos amiga!",
        )
        CopyMood.NEUTRAL -> listOf(
            "Mirá amor, ${ctx.totalLucas} lucas, no estás divina pero zafás. Maquillamos esto.",
            "Reina, vamos midiendo. Lo importante es no fundirte el rímel ni el sueldo.",
            "Bombón, ni espectacular ni catástrofe. Un beso y seguimos.",
        )
        CopyMood.BURN -> listOf(
            "Amor mío, ${ctx.lastExpenseLucas} lucas en eso. Te lo digo con amor pero te lo digo: NO.",
            "Miralo miralo, otra vez con la mismísima cagada. Reina, basta.",
            "Bombón, esa platita era para algo lindo. Ahora la usaste en una porquería.",
            "Tesoro, la guita es como el labial: si la ponés mal, queda toda corrida. Pintate de nuevo.",
        )
        CopyMood.ALARM -> listOf(
            "Reina, ${ctx.totalLucas} lucas. Esto es CAOS total. Vamos al baño y hablamos en serio.",
            "Amor mío, te lo digo desde el alma: estás haciendo agua y la pestaña también. PARÁ.",
            "Hermosa, agarrá un mate, una taza, una almohada y llorá un rato. Después arreglamos.",
        )
    }

    /**
     * Elige una frase del pack evitando repetir las recientes en el chat.
     */
    fun pick(
        personaKey: String,
        mood: CopyMood,
        ctx: CopyContext,
        exclude: Set<String> = emptySet(),
        rng: kotlin.random.Random = kotlin.random.Random.Default,
    ): String {
        val pool = pool(personaKey, mood, ctx)
        val choices = pool.filter { it !in exclude }.ifEmpty { pool }
        return choices.randomOrNull() ?: "Bancá la guita, capo."
    }

    /**
     * Datos del mes tejidos en una oración extra (todas las personas).
     * Se mezcla con el pack para frases más situadas sin IA.
     */
    fun contextualLines(ctx: CopyContext, mood: CopyMood): List<String> {
        val lines = mutableListOf<String>()
        ctx.topCategoryName?.takeIf { ctx.topCategoryLucas > 0 }?.let { cat ->
            lines += "En $cat ya llevás ${ctx.topCategoryLucas} lucas."
        }
        ctx.pctOfMonthlyGoal?.let { pct ->
            when {
                pct >= 1.0 -> lines += "La meta ya la pasaste (${(pct * 100).toInt()}%)."
                pct >= 0.85 -> lines += "Casi en el tope: ${(pct * 100).toInt()}% de la meta."
            }
        }
        if (ctx.dayOfMonth in 22..31) {
            val left = (ctx.daysInMonth - ctx.dayOfMonth).coerceAtLeast(1)
            lines += "Quedan $left días de ${ctx.monthLabel}."
        }
        ctx.deltaLucasVsPrevMonth?.let { d ->
            when {
                d > 15 -> lines += "Venís ${d} lucas arriba del mes pasado."
                d < -10 -> lines += "Mejoraste ${-d} lucas vs el mes anterior."
            }
        }
        if (mood == CopyMood.BURN && ctx.lastExpenseLucas > 0) {
            lines += "El último gasto fue de ${ctx.lastExpenseLucas} lucas."
        }
        return lines
    }
}
