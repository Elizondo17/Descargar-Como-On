package com.example.util

import com.example.data.model.PsaPricingConfig
import com.example.data.model.PsaPurifierModel
import com.example.data.model.SalesKnowledgeItem
import com.example.data.repository.PsaFinancingConfig
import java.text.Normalizer
import java.util.Locale

enum class PsaAiAudience(val label: String, val badge: String) {
    VENDEDOR("Vendedor", "Modo Comercial y Demo"),
    CLIENTE("Cliente / Usuario", "Modo Claro para Cliente")
}

enum class PsaKeyQuestionCategory(val label: String) {
    ALL("Todas"),
    DIAGNOSTICO("Diagnóstico"),
    EQUIPOS("Equipos"),
    POZO_SARRO("Pozo y Sarro"),
    RECAMBIOS("Recambios"),
    OBJECIONES("Objeciones"),
    INSTALACION("Instalación y Uso")
}

data class PsaKeyQuestion(
    val id: String,
    val category: PsaKeyQuestionCategory,
    val shortLabel: String,
    val fullQuestion: String
)

data class PsaDemoAiContext(
    val sourceOrigin: String = "Workspace", // "Demostración", "Cotizador", "Agenda"
    val waterSource: String = "AGUA_CORRIENTE",
    val chlorinePpm: Float = 1.0f,
    val hardnessPpm: Int = 0,
    val realLitersPerDay: Double = 0.0,
    val annualBottledSpend: Double = 0.0,
    val cartModelIds: List<String> = emptyList(),
    val cartSummary: String = "",
    val clientName: String = ""
)

data class PsaAiStructuredAnswer(
    val headline: String,
    val recommendedModelName: String,
    val priceAndInstallmentText: String,
    val technicalSummary: String,
    val demoOrClientTip: String,
    val objectionOrNextStep: String,
    val shareableText: String
)

data class PsaAiChatEntry(
    val id: String,
    val question: String,
    val audience: PsaAiAudience,
    val answer: PsaAiStructuredAnswer,
    val timestampMs: Long = System.currentTimeMillis()
)

private data class PsaCatalogDeepGuide(
    val modelId: String,
    val aliases: List<String>,
    val categoryTag: String,
    val filtrationTech: String,
    val removesSummary: String,
    val lifespanDetail: String,
    val installationNote: String,
    val sellerDemoTip: String,
    val clientExplanation: String,
    val replacementSchedule: String
)

/**
 * Motor 100% interno (sin conexión ni API keys) con el catálogo completo PSA,
 * fichas técnicas, mantenimiento, argumentos comerciales y recomendaciones dinámicas
 * cruzadas con el país activo, precios vigentes, cuotas y test de agua en pantalla.
 */
object PsaInternalKnowledgeEngine {

    private val catalogGuides: List<PsaCatalogDeepGuide> = listOf(
        PsaCatalogDeepGuide(
            modelId = "senior4",
            aliases = listOf("senior 4", "senior4", "senior"),
            categoryTag = "Cocina • Agua de red",
            filtrationTech = "Carbón activado granular impregnado en plata + zeolitas naturales + prefiltro Fipor intercambiante.",
            removesSummary = "Retiene >99,5% de cloro residual, trihalometanos (THMs), sedimentos, óxido y sabor/olor a lavandina.",
            lifespanDetail = "36.000 litros o 36 meses de vida útil con caudal continuo sobre mesada.",
            installationNote = "Se conecta en 5 minutos al pico de la canilla estándar con adaptador bypass (sin romper ni plomería).",
            sellerDemoTip = "Ideal tras el Clorímetro OTO: muestra la gota amarilla en red vs. 0,0 ppm en el Senior 4 y compara su costo diario de 3 años contra bidones.",
            clientExplanation = "Tu agua de red sale al instante lista para tomar, cebar mate, preparar mamaderas y cocinar, sin olor ni gusto químico.",
            replacementSchedule = "Prefiltro Fipor cada 3 a 6 meses (lavable en superficie según turbidez) y recambio de equipo/núcleo al cumplir 36 meses."
        ),
        PsaCatalogDeepGuide(
            modelId = "senik",
            aliases = listOf("senik"),
            categoryTag = "Cocina Premium • Agua de red",
            filtrationTech = "Unidad filtrante de carbón activado en bloque de alta compresión + medios bacteriostáticos PSA.",
            removesSummary = "Cloro, subproductos orgánicos (THMs), partículas finas, compuestos volátiles y metales traza.",
            lifespanDetail = "36.000 litros (36 meses) con estética moderna de mesada.",
            installationNote = "Conexión directa por bypass a la grifería de cocina; incluye kit universal de adaptadores.",
            sellerDemoTip = "Posicionalo frente al Senior 4 cuando el cliente valora diseño premium y máxima retención en cocinas modernas.",
            clientExplanation = "Combina estética minimalista sobre mesada con purificación de grado familiar por 3 años sin depender de bidones.",
            replacementSchedule = "Prefiltro Fipor cada 3–6 meses y núcleo principal a los 36 meses o 36.000 L."
        ),
        PsaCatalogDeepGuide(
            modelId = "quantum",
            aliases = listOf("quantum"),
            categoryTag = "Cocina • Red con dureza moderada",
            filtrationTech = "Carbón activado + zeolitas + resina de intercambio/acondicionamiento para mitigar sarro en cocina.",
            removesSummary = "Cloro libre, sedimentos, turbidez y ayuda a contener incrustaciones de cal/sarro en pava y cafetera.",
            lifespanDetail = "36.000 litros (36 meses) configurable sobre o bajo mesada según versión.",
            installationNote = "Puede instalarse junto a la bacha con válvula derivadora; mantiene presión estable.",
            sellerDemoTip = "Recomiéndalo si en el hogar comentan que la pava eléctrica junta sarro pero aún tienen agua corriente de red.",
            clientExplanation = "Purifica el agua para beber y cocinar mientras cuida tu pava y cafetera de la acumulación rápida de sarro.",
            replacementSchedule = "Prefiltro Fipor periódico cada 3–6 meses y renovación de unidad a los 36 meses."
        ),
        PsaCatalogDeepGuide(
            modelId = "croma",
            aliases = listOf("croma", "senior clasico", "clasico"),
            categoryTag = "Cocina • Línea Clásica",
            filtrationTech = "Carbón activado bacteriostático + prefiltro Fipor en formato clásico confiable.",
            removesSummary = "Cloro, sedimentos, óxido de cañerías viejas y malos olores.",
            lifespanDetail = "30.000 litros / 36 meses de respaldo familiar.",
            installationNote = "Instalación rápida sobre mesada sin obras ni herramientas complejas.",
            sellerDemoTip = "Excelente opción de entrada cuando el cliente busca bajar la cuota mensual sin resignar garantía oficial de 3 años.",
            clientExplanation = "Modelo probado en miles de hogares: sencillo, durable y mucho más económico que comprar botellas todas las semanas.",
            replacementSchedule = "Fipor cada 3–6 meses y recambio general a los 36 meses."
        ),
        PsaCatalogDeepGuide(
            modelId = "vero",
            aliases = listOf("vero"),
            categoryTag = "Cocina Compacta • Diseño",
            filtrationTech = "Carbón activado de alta eficiencia + prefiltro en carcasa esbelta.",
            removesSummary = "Cloro, sabor a lavandina, partículas suspendidas y compuestos orgánicos.",
            lifespanDetail = "36.000 litros (36 meses) con huella reducida sobre la mesada.",
            installationNote = "Se adapta perfecto a cocinas chicas o bachas angostas sin ocupar lugar útil.",
            sellerDemoTip = "Úsalo para cerrar clientes con departamento o mesada chica donde dicen 'no tengo espacio'.",
            clientExplanation = "Ocupa mínimo espacio junto a tu canilla y te da miles de litros de agua segura para mate, café y cocina.",
            replacementSchedule = "Cambio de prefiltro Fipor cada 3–6 meses y núcleo a los 36 meses."
        ),
        PsaCatalogDeepGuide(
            modelId = "s1000",
            aliases = listOf("s1000", "s-1000", "s 1000"),
            categoryTag = "Cocina • Alto Caudal",
            filtrationTech = "Medios filtrantes de carbón activado y zeolitas de alto volumen de contacto.",
            removesSummary = "Cloro, turbidez, sedimentos gruesos/finos y contaminantes orgánicos.",
            lifespanDetail = "36.000 litros (36 meses) con caudal constante para familias grandes o uso intensivo.",
            installationNote = "Conexión directa a canilla estándar sobre mesada.",
            sellerDemoTip = "Ideal en familias de 5+ personas o cocinas donde cargan ollas grandes todos los días.",
            clientExplanation = "Pensado para hogares numerosos: llena rápido botellas, jarras y ollas con agua pura constante.",
            replacementSchedule = "Revisión/cambio de Fipor cada 3–6 meses y núcleo a los 36 meses."
        ),
        PsaCatalogDeepGuide(
            modelId = "mini",
            aliases = listOf("mini", "compacto"),
            categoryTag = "Cocina Ultracompacta • Parejas/Monoambiente",
            filtrationTech = "Carbón activado bacteriostático compacto con retención de cloro y partículas.",
            removesSummary = "Cloro residual, olor y gusto desagradable del agua de red.",
            lifespanDetail = "18.000 litros o 24 meses de vida útil.",
            installationNote = "Ultraliviano y fácil de trasladar si el cliente alquila o se muda.",
            sellerDemoTip = "Arma perfecta contra la objeción 'vivo en alquiler y me mudo pronto': te lo llevás en 2 minutos.",
            clientExplanation = "Ideal para 1 o 2 personas o departamentos en alquiler: se instala sin agujerear nada y te lo llevás si te mudás.",
            replacementSchedule = "Prefiltro y limpieza periódica; renovación cada 24 meses."
        ),
        PsaCatalogDeepGuide(
            modelId = "c3",
            aliases = listOf("c3", "lavatorio"),
            categoryTag = "Baño / Lavatorio",
            filtrationTech = "Carbón activado impregnado en plata adaptado a canilla de baño.",
            removesSummary = "Cloro y partículas al cepillar dientes, lavar la cara o higiene de bebés.",
            lifespanDetail = "20.000 litros / 24 meses.",
            installationNote = "Se acopla al pico del lavatorio de baño en minutos.",
            sellerDemoTip = "Excelente agregado de carrito (+1 equipo) cuando el cliente tiene piel sensible, rosácea o niños chicos.",
            clientExplanation = "Evita que el cloro irrite el rostro al lavarte o cepillarte todos los días.",
            replacementSchedule = "Control visual del prefiltro y renovación a los 24 meses."
        ),
        PsaCatalogDeepGuide(
            modelId = "ducha",
            aliases = listOf("ducha", "declorador", "bano", "piel", "pelo", "cabello"),
            categoryTag = "Baño • Cuidado Piel y Cabello",
            filtrationTech = "Medio declorador específico que neutraliza cloro libre incluso con agua caliente.",
            removesSummary = "Cloro en agua caliente y vapor (principal vía de absorción dérmica y respiratoria de THMs en ducha).",
            lifespanDetail = "20.000 litros / 18 meses.",
            installationNote = "Se enrosca entre el brazo de pared y el cabezal de ducha estándar sin herramientas.",
            sellerDemoTip = "Conectalo al Módulo 2 (THMs y Ducha): al calentar agua en la ducha se libera cloro gaseoso; es el complemento más fácil de sumar al carrito.",
            clientExplanation = "Reduce la resequedad de piel, picazón, frizz y daño en el cabello causados por bañarse con agua clorada caliente.",
            replacementSchedule = "Reemplazo de cartucho/equipo cada 18 meses."
        ),
        PsaCatalogDeepGuide(
            modelId = "ropot",
            aliases = listOf("ropot", "osmosis"),
            categoryTag = "Cocina • Agua de Pozo / Alta Dureza",
            filtrationTech = "Ósmosis Inversa multietapa bajo mesada con membrana semipermeable de alta selectividad.",
            removesSummary = "Reduce hasta >95% de sales disueltas (TDS), nitratos, arsénico, sulfatos, dureza extrema y metales pesados.",
            lifespanDetail = "36.000 litros / 36 meses con tanque presurizado y grifería dedicada.",
            installationNote = "Instalación bajo mesada con canilla exclusiva de agua purificada en bacha.",
            sellerDemoTip = "Indispensable en zonas con agua salobre, pozo o altos TDS donde un carbón activado común no alcanza para bajar sales/arsénico.",
            clientExplanation = "Convierte agua pesada, salada o de pozo en agua liviana y segura para tomar, bebés e infusiones.",
            replacementSchedule = "Prefiltros semestrales/anuales según sedimentos de napa y membrana según conductividad."
        ),
        PsaCatalogDeepGuide(
            modelId = "rotech",
            aliases = listOf("rotech", "uv", "ro tech", "pozo", "napa", "arsenico", "bacterias"),
            categoryTag = "Cocina • Pozo + Lámpara UV Bactericida",
            filtrationTech = "Ósmosis Inversa multietapa + cámara con Lámpara UV germicida que inactiva microorganismos.",
            removesSummary = "Elimina arsénico, nitratos, exceso de sales, dureza severa Y virus/bacterias/quistes de agua de pozo sin cloro.",
            lifespanDetail = "36.000 litros / 36 meses con doble barrera fisicoquímica + microbiológica.",
            installationNote = "Bajo mesada con alimentación eléctrica estándar para cámara UV y canilla dedicada.",
            sellerDemoTip = "Cuando el switch está en Agua de Pozo o la tira supera 300–500 ppm, el ROtech·UV es la recomendación principal por seguridad bacteriológica + arsénico.",
            clientExplanation = "Máxima seguridad para agua de pozo o tanque sin red: quita exceso de sales/sarro y desinfecta bacterias al instante con luz UV.",
            replacementSchedule = "Prefiltros cada 6–12 meses, lámpara UV anual y membrana según uso/napa."
        ),
        PsaCatalogDeepGuide(
            modelId = "domus",
            aliases = listOf("domus", "ablandador"),
            categoryTag = "Hogar Completo • Ablandador Automático",
            filtrationTech = "Ablandamiento por intercambio iónico automático con regeneración programada.",
            removesSummary = "Calcio y magnesio (dureza/sarro) en TODA la cañería, termotanque, caldera, lavarropas y griferías.",
            lifespanDetail = "150.000 litros / ciclo prolongado con válvulas automáticas.",
            installationNote = "Se instala en línea de entrada general del hogar junto al tanque o ingreso principal.",
            sellerDemoTip = "Si en pozo detectás >400 ppm y sarro en duchas/termotanque, combiná Domus (casa) + ROtech·UV (consumo en cocina).",
            clientExplanation = "Protege termotanque, cañerías y electrodomésticos contra el sarro blanco y deja la ropa y piel más suaves.",
            replacementSchedule = "Recarga periódica de sal regenerante y control preventivo anual."
        ),
        PsaCatalogDeepGuide(
            modelId = "descalcificador",
            aliases = listOf("descalcificador", "r-200", "r200", "sarro"),
            categoryTag = "Hogar • Antisarro Físico",
            filtrationTech = "Tratamiento físico R-200 que modifica la cristalización de carbonatos para evitar adherencia.",
            removesSummary = "Previene incrustaciones calcáreas en calefones, termotanques, grifería y electrodomésticos sin químicos.",
            lifespanDetail = "100.000+ litros / larga duración hasta 60 meses sin recargas eléctricas.",
            installationNote = "Intercalado en entrada de alimentación de electrodoméstico o línea de agua.",
            sellerDemoTip = "Solución económica y sin mantenimiento de sal cuando el cliente quiere cuidar termotanque/lavarropas del sarro.",
            clientExplanation = "Cuida tu calefón y lavarropas evitando que el sarro tape serpentinas y aumente la factura de luz/gas.",
            replacementSchedule = "Mantenimiento mínimo; larga vida operativa."
        ),
        PsaCatalogDeepGuide(
            modelId = "maxima",
            aliases = listOf("maxima", "entrada general", "tanque"),
            categoryTag = "Entrada General • Toda la Casa",
            filtrationTech = "Filtración de gran caudal en ingreso general de agua domiciliaria.",
            removesSummary = "Barro, arena, óxido, sedimentos de tanque y cloro en toda la vivienda.",
            lifespanDetail = "150.000 litros / 36 meses.",
            installationNote = "Colocación en línea principal de bajada de tanque o entrada domiciliaria.",
            sellerDemoTip = "Ideal para casas con tanques que juntan sedimento u hogares que quieren agua prefiltrada en todas las canillas.",
            clientExplanation = "Actúa como escudo de entrada para que barro y sedimentos no entren a las cañerías, baños ni cocina.",
            replacementSchedule = "Limpieza/cambio de elemento filtrante según sedimentos de zona."
        ),
        PsaCatalogDeepGuide(
            modelId = "pileta",
            aliases = listOf("pileta", "piscina", "iontrix"),
            categoryTag = "Exterior • Piscinas",
            filtrationTech = "Ionización solar cobre-plata con celda fotovoltaica flotante.",
            removesSummary = "Controla algas y bacterias reduciendo hasta 90% la dosis habitual de cloro químico.",
            lifespanDetail = "Hasta 100.000 litros de piscina; larga duración solar.",
            installationNote = "Flota libremente en la pileta sin cables ni instalación eléctrica.",
            sellerDemoTip = "Destaca el ahorro en bidones de cloro de verano y adiós ojos rojos o trajes de baño decolorados.",
            clientExplanation = "Mantiene la pileta cristalina con energía solar y muchísimo menos olor e irritación por cloro.",
            replacementSchedule = "Limpieza simple de electrodos según temporada y reposición cuando se consumen."
        ),
        PsaCatalogDeepGuide(
            modelId = "aire",
            aliases = listOf("aire", "airpurifier", "air purifier", "hepa"),
            categoryTag = "Hogar • Aire Interior",
            filtrationTech = "Filtración multicapa HEPA + carbón activado + ionización ambiental.",
            removesSummary = "Polvo fino, polen, ácaros, pelo de mascotas, humo y olores domésticos.",
            lifespanDetail = "Uso continuo silencioso de bajo consumo.",
            installationNote = "Enchufar y usar en dormitorio, living u oficina sin instalación.",
            sellerDemoTip = "Cross-sell excelente para familias con alergias, rinitis, mascotas o bebés.",
            clientExplanation = "Mejora el descanso y reduce polvo/alérgenos suspendidos en dormitorios y salas de estar.",
            replacementSchedule = "Aspirado suave del prefiltro y recambio periódico de filtro HEPA según uso."
        )
    )

    fun getKeyQuestions(audience: PsaAiAudience): List<PsaKeyQuestion> {
        val isSeller = audience == PsaAiAudience.VENDEDOR
        return listOf(
            PsaKeyQuestion(
                id = "kq_diag_ideal",
                category = PsaKeyQuestionCategory.DIAGNOSTICO,
                shortLabel = if (isSeller) "¿Qué equipo recomiendo según esta prueba?" else "¿Qué equipo me conviene para mi agua?",
                fullQuestion = "Según el test actual de agua y país activo, ¿qué equipo PSA recomendar y a qué cuota?"
            ),
            PsaKeyQuestion(
                id = "kq_roi_bidon",
                category = PsaKeyQuestionCategory.DIAGNOSTICO,
                shortLabel = "Ahorro real vs bidones/botellas",
                fullQuestion = "¿Cómo justificar el ahorro mensual frente a comprar bidones o botellas?"
            ),
            PsaKeyQuestion(
                id = "kq_senior_vs_quantum",
                category = PsaKeyQuestionCategory.EQUIPOS,
                shortLabel = "Senior 4 vs Senik vs Quantum",
                fullQuestion = "¿Cuál es la diferencia técnica y de uso entre Senior 4, Senik y Quantum?"
            ),
            PsaKeyQuestion(
                id = "kq_combo_ducha",
                category = PsaKeyQuestionCategory.EQUIPOS,
                shortLabel = "¿Por qué sumar PSA Ducha / C3?",
                fullQuestion = "¿Qué aporta sumar PSA Ducha o C3 al equipo de cocina?"
            ),
            PsaKeyQuestion(
                id = "kq_pozo_rotech",
                category = PsaKeyQuestionCategory.POZO_SARRO,
                shortLabel = "Agua de pozo / arsénico / bacterias",
                fullQuestion = "¿Por qué en agua de pozo o napa se necesita Ropot o ROtech·UV?"
            ),
            PsaKeyQuestion(
                id = "kq_sarro_domus",
                category = PsaKeyQuestionCategory.POZO_SARRO,
                shortLabel = "Dureza alta y sarro (Domus vs R-200)",
                fullQuestion = "¿Cuándo ofrecer Domus vs Descalcificador R-200 para el sarro?"
            ),
            PsaKeyQuestion(
                id = "kq_fipor_cambios",
                category = PsaKeyQuestionCategory.RECAMBIOS,
                shortLabel = "¿Cada cuánto cambiar Fipor y núcleo?",
                fullQuestion = "¿Cada cuántos meses se cambia el prefiltro Fipor y el equipo/núcleo PSA?"
            ),
            PsaKeyQuestion(
                id = "kq_garantia_anmat",
                category = PsaKeyQuestionCategory.INSTALACION,
                shortLabel = "Garantía, certificación e instalación",
                fullQuestion = "¿Cómo es la instalación, la garantía oficial de 3 años y la certificación?"
            ),
            PsaKeyQuestion(
                id = "kq_obj_precio",
                category = PsaKeyQuestionCategory.OBJECIONES,
                shortLabel = "Objeción: 'Es caro / lo pienso'",
                fullQuestion = "Si el cliente dice que el equipo es caro o que lo va a pensar, ¿qué argumento usar?"
            ),
            PsaKeyQuestion(
                id = "kq_obj_hervir",
                category = PsaKeyQuestionCategory.OBJECIONES,
                shortLabel = "Objeción: 'Yo hiervo el agua'",
                fullQuestion = "¿Por qué hervir el agua de red no reemplaza a un purificador PSA?"
            ),
            PsaKeyQuestion(
                id = "kq_obj_alquiler",
                category = PsaKeyQuestionCategory.OBJECIONES,
                shortLabel = "Objeción: 'Alquilo / me mudo'",
                fullQuestion = "¿Qué responder si el cliente vive en alquiler o no quiere romper la mesada?"
            ),
            PsaKeyQuestion(
                id = "kq_agua_mineral",
                category = PsaKeyQuestionCategory.OBJECIONES,
                shortLabel = "Agua purificada PSA vs mineral/mesa",
                fullQuestion = "¿Qué diferencia hay entre agua purificada PSA, agua mineral y agua de mesa en bidón?"
            )
        )
    }

    fun answerKeyQuestion(
        keyQuestion: PsaKeyQuestion,
        audience: PsaAiAudience,
        pricingConfig: PsaPricingConfig,
        financingConfig: PsaFinancingConfig,
        country: PsaCountryConfig,
        demoContext: PsaDemoAiContext,
        savedKnowledge: List<SalesKnowledgeItem> = emptyList()
    ): PsaAiStructuredAnswer {
        return answerQuestion(
            rawQuestion = "${keyQuestion.id} ${keyQuestion.fullQuestion}",
            displayQuestion = keyQuestion.fullQuestion,
            audience = audience,
            pricingConfig = pricingConfig,
            financingConfig = financingConfig,
            country = country,
            demoContext = demoContext,
            savedKnowledge = savedKnowledge
        )
    }

    fun answerQuestion(
        rawQuestion: String,
        displayQuestion: String = rawQuestion,
        audience: PsaAiAudience,
        pricingConfig: PsaPricingConfig,
        financingConfig: PsaFinancingConfig,
        country: PsaCountryConfig,
        demoContext: PsaDemoAiContext,
        savedKnowledge: List<SalesKnowledgeItem> = emptyList()
    ): PsaAiStructuredAnswer {
        val norm = normalizeText(rawQuestion)
        val allModels = pricingConfig.getAllPurifiers()
        val fmt = { amount: Double -> PsaCountryRegistry.formatCurrency(amount, country) }
        val cuotas = pricingConfig.installmentsCount.coerceAtLeast(1)

        // Check if user selected specific cart models or diagnostic is Agua de Pozo
        val isWellWater = demoContext.waterSource.equals("AGUA_DE_POZO", ignoreCase = true)
        val cartModels = demoContext.cartModelIds.mapNotNull { id -> allModels.firstOrNull { it.id == id } }

        // 1. Direct match by model name/alias if the user asked about a specific unit
        val matchedGuideByAlias = catalogGuides.firstOrNull { guide ->
            guide.aliases.any { alias -> norm.contains(normalizeText(alias)) }
        }

        // 2. Intent detection when no single alias dominates or multi-model comparison is requested
        val isComparison = norm.contains("diferencia") || norm.contains(" vs ") || norm.contains("compar")
        val isDiagIdeal = norm.contains("kq_diag_ideal") ||
            (norm.contains("prueba") && norm.contains("recom")) ||
            norm.contains("cual me conviene") ||
            norm.contains("recomendar") ||
            norm.contains("equipo ideal")
        val isRoiBidon = norm.contains("kq_roi_bidon") ||
            norm.contains("ahorro") || norm.contains("bidon") || norm.contains("botella") || norm.contains("amortiz")
        val isWellTopic = norm.contains("kq_pozo_rotech") ||
            norm.contains("pozo") || norm.contains("napa") || norm.contains("arsen") || norm.contains("nitrat")
        val isSarroTopic = norm.contains("kq_sarro_domus") ||
            (norm.contains("sarro") && !norm.contains("hervir")) || norm.contains("dureza") || norm.contains("calefon")
        val isFiporTopic = norm.contains("kq_fipor_cambios") ||
            norm.contains("fipor") || norm.contains("recambio") || norm.contains("repuesto") ||
            norm.contains("nucleo") || norm.contains("mantenimiento") || norm.contains("cada cuanto")
        val isWarrantyInstall = norm.contains("kq_garantia_anmat") ||
            norm.contains("garant") || norm.contains("anmat") || norm.contains("certific") || norm.contains("instala")
        val isObjPrice = norm.contains("kq_obj_precio") ||
            norm.contains("caro") || norm.contains("pienso") || norm.contains("presupuesto") || norm.contains("cuota")
        val isObjBoil = norm.contains("kq_obj_hervir") ||
            norm.contains("hervir") || norm.contains("hierv")
        val isObjRent = norm.contains("kq_obj_alquiler") ||
            norm.contains("alquil") || norm.contains("mudo") || norm.contains("romper")
        val isWaterTypes = norm.contains("kq_agua_mineral") ||
            norm.contains("mineral") || norm.contains("mesa")
        val isShowerCombo = norm.contains("kq_combo_ducha") ||
            (norm.contains("ducha") && (norm.contains("sumar") || norm.contains("c3") || norm.contains("bano")))
        val isSeniorVsQuantum = norm.contains("kq_senior_vs_quantum") ||
            (isComparison && (norm.contains("senior") || norm.contains("senik") || norm.contains("quantum")))

        // Best winning memory match from local DB if present
        val memorySnippet = savedKnowledge
            .filter { it.category.startsWith("PSA_") }
            .maxByOrNull { it.successScore }
            ?.winningContent
            ?.take(220)
            .orEmpty()

        return when {
            isDiagIdeal -> buildDiagnosticRecommendationAnswer(
                audience = audience,
                allModels = allModels,
                cartModels = cartModels,
                isWellWater = isWellWater,
                demoContext = demoContext,
                pricingConfig = pricingConfig,
                financingConfig = financingConfig,
                fmt = fmt,
                cuotas = cuotas,
                country = country
            )

            isSeniorVsQuantum -> buildComparisonSeniorSenikQuantum(
                audience = audience,
                allModels = allModels,
                cuotas = cuotas,
                fmt = fmt,
                country = country
            )

            isShowerCombo -> buildShowerAndBathComboAnswer(
                audience = audience,
                allModels = allModels,
                cuotas = cuotas,
                fmt = fmt
            )

            isWellTopic && !isComparison -> buildWellWaterAnswer(
                audience = audience,
                allModels = allModels,
                demoContext = demoContext,
                cuotas = cuotas,
                fmt = fmt
            )

            isSarroTopic && !isComparison -> buildHardnessSarroAnswer(
                audience = audience,
                allModels = allModels,
                demoContext = demoContext,
                cuotas = cuotas,
                fmt = fmt
            )

            isFiporTopic -> buildReplacementsAnswer(
                audience = audience,
                pricingConfig = pricingConfig,
                cuotas = cuotas,
                fmt = fmt
            )

            isRoiBidon -> buildRoiVsBottlesAnswer(
                audience = audience,
                allModels = allModels,
                cartModels = cartModels,
                isWellWater = isWellWater,
                demoContext = demoContext,
                pricingConfig = pricingConfig,
                cuotas = cuotas,
                fmt = fmt
            )

            isObjPrice -> buildPriceObjectionAnswer(
                audience = audience,
                allModels = allModels,
                cartModels = cartModels,
                isWellWater = isWellWater,
                pricingConfig = pricingConfig,
                financingConfig = financingConfig,
                cuotas = cuotas,
                fmt = fmt,
                memorySnippet = memorySnippet
            )

            isObjBoil -> buildBoilingMythAnswer(
                audience = audience,
                allModels = allModels,
                cuotas = cuotas,
                fmt = fmt
            )

            isObjRent -> buildRentalInstallAnswer(
                audience = audience,
                allModels = allModels,
                cuotas = cuotas,
                fmt = fmt
            )

            isWaterTypes -> buildWaterTypesComparisonAnswer(
                audience = audience,
                allModels = allModels,
                cuotas = cuotas,
                fmt = fmt
            )

            isWarrantyInstall -> buildWarrantyAndInstallAnswer(
                audience = audience,
                allModels = allModels,
                isWellWater = isWellWater,
                cuotas = cuotas,
                fmt = fmt
            )

            matchedGuideByAlias != null -> buildSpecificModelAnswer(
                guide = matchedGuideByAlias,
                model = allModels.firstOrNull { it.id == matchedGuideByAlias.modelId } ?: allModels.first(),
                audience = audience,
                cuotas = cuotas,
                fmt = fmt,
                country = country
            )

            else -> buildContextualFallbackAnswer(
                displayQuestion = displayQuestion,
                audience = audience,
                allModels = allModels,
                cartModels = cartModels,
                isWellWater = isWellWater,
                demoContext = demoContext,
                pricingConfig = pricingConfig,
                cuotas = cuotas,
                fmt = fmt,
                country = country,
                memorySnippet = memorySnippet
            )
        }
    }

    private fun buildDiagnosticRecommendationAnswer(
        audience: PsaAiAudience,
        allModels: List<PsaPurifierModel>,
        cartModels: List<PsaPurifierModel>,
        isWellWater: Boolean,
        demoContext: PsaDemoAiContext,
        pricingConfig: PsaPricingConfig,
        financingConfig: PsaFinancingConfig,
        fmt: (Double) -> String,
        cuotas: Int,
        country: PsaCountryConfig
    ): PsaAiStructuredAnswer {
        val recommended = when {
            cartModels.isNotEmpty() -> cartModels.first()
            isWellWater && demoContext.hardnessPpm >= 350 -> findModel(allModels, "rotech")
            isWellWater -> findModel(allModels, "ropot")
            demoContext.chlorinePpm >= 1.8f -> findModel(allModels, "senik")
            demoContext.realLitersPerDay >= 14.0 -> findModel(allModels, "senior4")
            else -> findModel(allModels, "senior4")
        }
        val activeCartTotal = if (cartModels.isNotEmpty()) cartModels.sumOf { it.price } else recommended.price
        val activeCartLabel = if (cartModels.isNotEmpty()) {
            cartModels.joinToString(" + ") { it.name }
        } else {
            recommended.name
        }
        val cuotaBase = activeCartTotal / cuotas
        val cash10 = activeCartTotal * 0.90
        val dailyCost = activeCartTotal / 1095.0
        val diagnosticRead = if (isWellWater) {
            val hardText = if (demoContext.hardnessPpm > 0) "${demoContext.hardnessPpm} ppm de dureza" else "agua de pozo/napa"
            "Diagnóstico activo (${country.code}): $hardText${if (demoContext.realLitersPerDay > 0) " • consumo ${String.format(Locale.US, "%.1f", demoContext.realLitersPerDay)} L/día" else ""}."
        } else {
            val clText = String.format(Locale.US, "%.1f ppm de cloro libre", demoContext.chlorinePpm)
            "Diagnóstico activo (${country.code}): Agua de red con $clText${if (demoContext.realLitersPerDay > 0) " • consumo ${String.format(Locale.US, "%.1f", demoContext.realLitersPerDay)} L/día" else ""}."
        }
        val wellExtra = if (isWellWater && demoContext.hardnessPpm >= 350 && cartModels.none { it.id == "domus" }) {
            " Si hay sarro visible en baños/termotanque, sumar PSA Domus o R-200 protege toda la cañería."
        } else {
            ""
        }
        val interestNote = if (financingConfig.defaultInterest6Months == 0f) {
            "6 cuotas sin interés o $cuotas cuotas de ${fmt(cuotaBase)}"
        } else {
            "$cuotas cuotas ref. de ${fmt(cuotaBase)}"
        }
        val priceLine = "$activeCartLabel: lista ${fmt(activeCartTotal)} • $interestNote • Contado (-10% ref.): ${fmt(cash10)} (~${fmt(dailyCost)}/día a 3 años)."
        val tip = if (audience == PsaAiAudience.VENDEDOR) {
            val clientPrefix = demoContext.clientName.takeIf { it.isNotBlank() }?.let { "Para $it: " } ?: ""
            "${clientPrefix}No presentes primero el precio total; muestra que cuesta ${fmt(dailyCost)} por día frente al gasto de bidones.${wellExtra}"
        } else {
            "Con ${activeCartLabel} resolvés el agua de todos los días directamente en la canilla, con costo diario mínimo (~${fmt(dailyCost)}/día) y sin cargar bidones.${wellExtra}"
        }
        val nextStep = if (audience == PsaAiAudience.VENDEDOR) {
            "Cierra con doble opción en Cotizar: tarjeta en $cuotas cuotas de ${fmt(cuotaBase)} o efectivo con descuento (${fmt(cash10)})."
        } else {
            "Incluye garantía oficial de 36 meses y no requiere romper mesada ni llamar plomero en agua de red."
        }

        val shareable = buildString {
            append("💧 *Recomendación PSA (${country.name})*\n")
            if (demoContext.clientName.isNotBlank()) append("Cliente: ${demoContext.clientName}\n")
            append("• $diagnosticRead\n")
            append("• Equipo sugerido: *$activeCartLabel*\n")
            append("• Inversión hoy: ${fmt(activeCartTotal)} ($interestNote)\n")
            append("• Equivalente diario: *${fmt(dailyCost)}/día* durante 36 meses.")
        }

        return PsaAiStructuredAnswer(
            headline = if (isWellWater) "Recomendación para Agua de Pozo / Dureza" else "Recomendación según Test de Cloro y Consumo",
            recommendedModelName = activeCartLabel,
            priceAndInstallmentText = priceLine,
            technicalSummary = "$diagnosticRead ${guideFor(recommended.id).filtrationTech} ${guideFor(recommended.id).removesSummary}",
            demoOrClientTip = tip,
            objectionOrNextStep = nextStep,
            shareableText = shareable
        )
    }

    private fun buildComparisonSeniorSenikQuantum(
        audience: PsaAiAudience,
        allModels: List<PsaPurifierModel>,
        cuotas: Int,
        fmt: (Double) -> String,
        country: PsaCountryConfig
    ): PsaAiStructuredAnswer {
        val senior = findModel(allModels, "senior4")
        val senik = findModel(allModels, "senik")
        val quantum = findModel(allModels, "quantum")
        val vero = findModel(allModels, "vero")

        val priceLine = buildString {
            append("Precios vigentes (${country.code}): ")
            append("Senior 4 ${fmt(senior.price)} (${fmt(senior.price / cuotas)}/cuota) • ")
            append("Senik ${fmt(senik.price)} • ")
            append("Quantum ${fmt(quantum.price)} • ")
            append("Vero ${fmt(vero.price)}.")
        }
        val tech = "• Senior 4 (36.000 L / 36m): el clásico más versátil para familias de 2–6 personas sobre mesada.\n" +
            "• Senik (36.000 L / 36m): bloque de carbón comprimido con línea estética premium y máximo pulido de sabor.\n" +
            "• Quantum (36.000 L / 36m): optimizado para redes con dureza moderada/sarro en pava y opción bajo o sobre mesada.\n" +
            "• Vero (36.000 L / 36m): compacto y accesible para cocinas pequeñas."
        val tip = if (audience == PsaAiAudience.VENDEDOR) {
            "Usa el efecto ancla: muestra primero Senik/Quantum para resaltar el diseño y deja Senior 4 o Vero como alternativa rápida de cierre."
        } else {
            "Todos retienen >99,5% del cloro por 3 años (36.000 L); elegí Vero si priorizás espacio/precio, Senior 4 por trayectoria familiar o Quantum/Senik por terminación y control de dureza."
        }
        val next = if (audience == PsaAiAudience.VENDEDOR) {
            "Si comentan sarro en la pava eléctrica -> Quantum; si cuidan la estética -> Senik; si buscan la mejor relación precio/caudal -> Senior 4."
        } else {
            "Todos se instalan en 5 minutos con adaptador universal incluido y tienen 36 meses de garantía PSA."
        }
        return PsaAiStructuredAnswer(
            headline = "Comparativa Cocina Red: Senior 4 vs Senik vs Quantum",
            recommendedModelName = "Senior 4 / Senik / Quantum",
            priceAndInstallmentText = priceLine,
            technicalSummary = tech,
            demoOrClientTip = tip,
            objectionOrNextStep = next,
            shareableText = "💧 *Comparativa PSA (${country.code})*\n$tech\n\n$priceLine"
        )
    }

    private fun buildShowerAndBathComboAnswer(
        audience: PsaAiAudience,
        allModels: List<PsaPurifierModel>,
        cuotas: Int,
        fmt: (Double) -> String
    ): PsaAiStructuredAnswer {
        val ducha = findModel(allModels, "ducha")
        val c3 = findModel(allModels, "c3")
        val senior = findModel(allModels, "senior4")
        val comboTotal = senior.price + ducha.price
        val priceLine = "PSA Ducha ${fmt(ducha.price)} (${fmt(ducha.price / cuotas)}/cuota) • PSA C3 ${fmt(c3.price)} • Combo Cocina + Ducha ref.: ${fmt(comboTotal)} (${fmt(comboTotal / cuotas)}/cuota)."
        val tech = "Al ducharte con agua caliente el poro se dilata y el cloro libre se volatiliza en vapor generando trihalometanos (THMs) que se absorben por piel y respiración."
        val tip = if (audience == PsaAiAudience.VENDEDOR) {
            "Preséntalo justo después del Módulo 2 (THMs y Ducha): 'Ya cuidamos lo que tomás en la cocina; con solo ${fmt(ducha.price / cuotas)} por mes protegés la piel y el pelo de toda la familia'."
        } else {
            "El filtro PSA Ducha se enrosca en 1 minuto sin herramientas, dura 18 meses y reduce piel seca, irritación ocular y frizz en el cabello."
        }
        val next = "PSA C3 complementa el lavatorio del baño para cepillado dental, lavado facial e higiene de bebés."
        return PsaAiStructuredAnswer(
            headline = "Complemento Salud: PSA Ducha & PSA C3",
            recommendedModelName = "${ducha.name} + ${c3.name}",
            priceAndInstallmentText = priceLine,
            technicalSummary = tech,
            demoOrClientTip = tip,
            objectionOrNextStep = next,
            shareableText = "🚿 *Cuidado Integral Cocina + Ducha PSA*\n$tech\n$priceLine"
        )
    }

    private fun buildWellWaterAnswer(
        audience: PsaAiAudience,
        allModels: List<PsaPurifierModel>,
        demoContext: PsaDemoAiContext,
        cuotas: Int,
        fmt: (Double) -> String
    ): PsaAiStructuredAnswer {
        val rotech = findModel(allModels, "rotech")
        val ropot = findModel(allModels, "ropot")
        val domus = findModel(allModels, "domus")
        val priceLine = "ROtech·UV: ${fmt(rotech.price)} (${fmt(rotech.price / cuotas)}/cuota) • Ropot: ${fmt(ropot.price)} (${fmt(ropot.price / cuotas)}/cuota) • Domus casa completa: ${fmt(domus.price)}."
        val tech = "El agua de pozo o napa NO trae cloro desinfectante ni suele controlar arsénico, nitratos o exceso de sales disueltas. Un purificador estándar de carbón NO frena bacterias ni sales disueltas: se necesita Ósmosis Inversa (Ropot) y luz UV germicida (ROtech·UV)."
        val tip = if (audience == PsaAiAudience.VENDEDOR) {
            val hardInfo = if (demoContext.hardnessPpm > 0) "Con ${demoContext.hardnessPpm} ppm medidos en esta casa, " else ""
            "${hardInfo}explica que el carbón activado no quita nitratos ni arsénico; ROtech·UV garantiza pureza química y microbiológica bajo mesada."
        } else {
            "Con ROtech·UV obtenés agua liviana, sin gusto salobre, libre de sarro, arsénico, nitratos y bacterias directo desde una canilla exclusiva en tu cocina."
        }
        val next = "Si las cañerías o termotanque también acumulan cal blanca, combiná PSA Domus en la entrada general."
        return PsaAiStructuredAnswer(
            headline = "Protocolo Agua de Pozo / Napa: ROtech·UV y Ropot",
            recommendedModelName = rotech.name,
            priceAndInstallmentText = priceLine,
            technicalSummary = tech,
            demoOrClientTip = tip,
            objectionOrNextStep = next,
            shareableText = "🧪 *Solución PSA para Agua de Pozo*\n$tech\n$priceLine"
        )
    }

    private fun buildHardnessSarroAnswer(
        audience: PsaAiAudience,
        allModels: List<PsaPurifierModel>,
        demoContext: PsaDemoAiContext,
        cuotas: Int,
        fmt: (Double) -> String
    ): PsaAiStructuredAnswer {
        val domus = findModel(allModels, "domus")
        val r200 = findModel(allModels, "descalcificador")
        val quantum = findModel(allModels, "quantum")
        val priceLine = "Domus: ${fmt(domus.price)} (${fmt(domus.price / cuotas)}/cuota) • R-200: ${fmt(r200.price)} (${fmt(r200.price / cuotas)}/cuota) • Quantum cocina: ${fmt(quantum.price)}."
        val tech = "• Más de 300–400 ppm en tira o sarro en duchas/termotanque -> PSA Domus (ablandador automático integral de toda la vivienda).\n" +
            "• Protección puntual de calefón/lavarropas con menor inversión -> Descalcificador R-200.\n" +
            "• Red urbana con sarro leve solo en pava -> PSA Quantum."
        val tip = if (audience == PsaAiAudience.VENDEDOR) {
            val hard = if (demoContext.hardnessPpm > 0) "La tira actual marca ${demoContext.hardnessPpm} ppm: " else ""
            "${hard}muestra el Módulo 4 (Sarro): apenas 2 mm de sarro en resistencias suben hasta 20–30% el consumo de gas/luz y acortan la vida del termotanque."
        } else {
            "Controlar el sarro desde la entrada evita que se tapen duchas, griferías nuevas, termotanques y lavarropas."
        }
        val next = "Recuerda: el ablandador protege las instalaciones pero para beber agua de pozo con sales/arsénico se complementa en cocina con Ropot o ROtech·UV."
        return PsaAiStructuredAnswer(
            headline = "Control de Dureza y Sarro: Domus vs R-200",
            recommendedModelName = if (demoContext.hardnessPpm >= 350) domus.name else r200.name,
            priceAndInstallmentText = priceLine,
            technicalSummary = tech,
            demoOrClientTip = tip,
            objectionOrNextStep = next,
            shareableText = "🪨 *Protección Antisarro PSA*\n$tech\n$priceLine"
        )
    }

    private fun buildReplacementsAnswer(
        audience: PsaAiAudience,
        pricingConfig: PsaPricingConfig,
        cuotas: Int,
        fmt: (Double) -> String
    ): PsaAiStructuredAnswer {
        val repPrice = pricingConfig.repuestoSeniorPrice
        val priceLine = "Repuesto Fipor / Núcleo oficial de referencia: ${fmt(repPrice)} (${fmt(repPrice / cuotas)} en $cuotas cuotas si se financia)."
        val tech = "• Prefiltro Fipor: retiene barro, óxido y sedimentos gruesos para que no saturen el carbón. Enjuagar por fuera cuando oscurezca y reemplazar cada 3 a 6 meses según la turbidez del barrio.\n" +
            "• Equipo / Núcleo bacteriostático (Senior 4, Senik, Quantum, Vero, Croma): vida útil nominal de 36 meses o 36.000 litros.\n" +
            "• Ducha: 18 meses • C3 / Mini: 24 meses • Ósmosis ROtech·UV: prefiltros 6–12 meses y lámpara UV cada 12 meses."
        val tip = if (audience == PsaAiAudience.VENDEDOR) {
            "Usa el recambio como argumento de confianza y seguimiento posventa: registrá la instalación en la pestaña Agenda > Posventa para recibir la alerta automática antes de los 12 meses."
        } else {
            "Mantener el Fipor limpio alarga la vida útil del purificador y asegura buen caudal y agua siempre cristalina."
        }
        val next = "No abrir el cuerpo sellado del purificador ni usar agua caliente (>35 °C) a través de equipos de carbón activado."
        return PsaAiStructuredAnswer(
            headline = "Plan Oficial de Recambios y Cuidado",
            recommendedModelName = "Fipor + Núcleo / Lámpara UV",
            priceAndInstallmentText = priceLine,
            technicalSummary = tech,
            demoOrClientTip = tip,
            objectionOrNextStep = next,
            shareableText = "🔧 *Mantenimiento PSA*\n$tech"
        )
    }

    private fun buildRoiVsBottlesAnswer(
        audience: PsaAiAudience,
        allModels: List<PsaPurifierModel>,
        cartModels: List<PsaPurifierModel>,
        isWellWater: Boolean,
        demoContext: PsaDemoAiContext,
        pricingConfig: PsaPricingConfig,
        cuotas: Int,
        fmt: (Double) -> String
    ): PsaAiStructuredAnswer {
        val target = cartModels.firstOrNull()
            ?: findModel(allModels, if (isWellWater) "rotech" else "senior4")
        val monthlyBidonRef = (pricingConfig.bidonPrice * 8.66).coerceAtLeast(1000.0) // ~2 bidones/semana
        val annualSpend = if (demoContext.annualBottledSpend > 0.0) demoContext.annualBottledSpend else monthlyBidonRef * 12.0
        val threeYearSpend = annualSpend * 3.0
        val threeYearSavings = (threeYearSpend - target.price).coerceAtLeast(0.0)
        val cuota = target.price / cuotas

        val priceLine = "${target.name}: $cuotas cuotas de ${fmt(cuota)} (Total ${fmt(target.price)}) vs. gasto anual est. en bidones de ${fmt(annualSpend)} (${fmt(threeYearSpend)} en 3 años)."
        val tech = "Una familia de 3–4 personas que además usa agua segura para mate, café y cocinar consume mucho más que 1 bidón semanal; en 36 meses termina gastando ${fmt(threeYearSpend)} sin quedarse con ningún bien."
        val tip = if (audience == PsaAiAudience.VENDEDOR) {
            "Frase clave de cierre: 'El purificador ya lo estás pagando todos los meses en el reparto de bidones; con PSA la cuota se termina y el agua pura para beber y cocinar queda en tu casa'."
        } else {
            "En 3 años podés ahorrar alrededor de ${fmt(threeYearSavings)} evitando comprar, almacenar y cargar cientos de envases plásticos."
        }
        val next = "Además evitás microplásticos de bidones expuestos al sol y cocinás arroz, verduras e infusiones con agua sin cloro."
        return PsaAiStructuredAnswer(
            headline = "Ahorro Real y Amortización vs Bidones",
            recommendedModelName = target.name,
            priceAndInstallmentText = priceLine,
            technicalSummary = tech,
            demoOrClientTip = tip,
            objectionOrNextStep = next,
            shareableText = "💰 *Comparativa PSA vs Bidones*\n• Gasto 3 años en envases: ${fmt(threeYearSpend)}\n• ${target.name}: ${fmt(target.price)} ($cuotas cuotas de ${fmt(cuota)})\n• Ahorro estimado: *${fmt(threeYearSavings)}*."
        )
    }

    private fun buildPriceObjectionAnswer(
        audience: PsaAiAudience,
        allModels: List<PsaPurifierModel>,
        cartModels: List<PsaPurifierModel>,
        isWellWater: Boolean,
        pricingConfig: PsaPricingConfig,
        financingConfig: PsaFinancingConfig,
        cuotas: Int,
        fmt: (Double) -> String,
        memorySnippet: String
    ): PsaAiStructuredAnswer {
        val primary = cartModels.firstOrNull() ?: findModel(allModels, if (isWellWater) "ropot" else "senior4")
        val alternative = findModel(allModels, if (isWellWater) "ropot" else "vero")
        val dailyPrimary = primary.price / 1095.0
        val cuotaPrimary = primary.price / cuotas
        val cuotaAlt = alternative.price / cuotas
        val cashDiscount = primary.price * 0.90

        val priceLine = "${primary.name}: ${fmt(cuotaPrimary)}/cuota (~${fmt(dailyPrimary)}/día) o Contado (-10%): ${fmt(cashDiscount)}. Alternativa accesible ${alternative.name}: ${fmt(cuotaAlt)}/cuota."
        val tech = "Separar la inversión total en valor por día (${fmt(dailyPrimary)}/día) + 36 meses de vida útil elimina la sensación de gasto alto único. Si hay presupuesto ajustado, bajar de gama (${alternative.name} o ${findModel(allModels, "croma").name}) mantiene calidad PSA con cuota más chica."
        val tip = if (audience == PsaAiAudience.VENDEDOR) {
            val extra = if (memorySnippet.isNotBlank()) "\nPatrón guardado en tu Memoria IA: \"$memorySnippet\"" else ""
            "1) Valida ('Entiendo que quieras cuidar el presupuesto'). 2) Aíslalo ('¿Es por el monto total o por la cuota mensual?'). 3) Ofrece financiación con anticipo + cuotas en Cotizar o alternativa ${alternative.name}.$extra"
        } else {
            "Por menos de lo que cuesta un café por día (${fmt(dailyPrimary)}) tenés agua purificada ilimitada para tomar y cocinar durante 3 años."
        }
        val next = if (financingConfig.defaultInterest3Months == 0f || financingConfig.defaultInterest6Months == 0f) {
            "Prueba simular en Cotizar un pequeño anticipo en efectivo para bajar la cuota mensual aún más."
        } else {
            "Simula en Cotizar el plan de cuotas y descuento contado en menos de 15 segundos."
        }
        return PsaAiStructuredAnswer(
            headline = "Manejo de Objeción: 'Es caro / Lo tengo que pensar'",
            recommendedModelName = "${primary.name} (Alt: ${alternative.name})",
            priceAndInstallmentText = priceLine,
            technicalSummary = tech,
            demoOrClientTip = tip,
            objectionOrNextStep = next,
            shareableText = "✨ *Agua Pura PSA desde ${fmt(dailyPrimary)} por día*\n• ${primary.name}: $cuotas cuotas de ${fmt(cuotaPrimary)}\n• Alternativa compacta ${alternative.name}: ${fmt(cuotaAlt)}/cuota."
        )
    }

    private fun buildBoilingMythAnswer(
        audience: PsaAiAudience,
        allModels: List<PsaPurifierModel>,
        cuotas: Int,
        fmt: (Double) -> String
    ): PsaAiStructuredAnswer {
        val senior = findModel(allModels, "senior4")
        val priceLine = "${senior.name}: ${fmt(senior.price)} (${fmt(senior.price / cuotas)} en $cuotas cuotas)."
        val tech = "Hervir elimina gérmenes por temperatura, pero NO filtra químicos, óxido ni metales pesados. Al evaporarse parte del agua (15–30%), los nitratos, sales y dureza se CONCENTRAN más, y el cloro al hervir con materia orgánica dispara subproductos llamados trihalometanos (THMs)."
        val tip = if (audience == PsaAiAudience.VENDEDOR) {
            "Demuéstralo en vivo con el Módulo 3 (Hervor): pasa la barra de 0 a 15 minutos para que el cliente vea visualmente cómo se concentra la parte mineral y química al evaporar."
        } else {
            "Hervir sirve de emergencia microbiológica, pero deja metales, sarro concentrado y subproductos del cloro; un purificador retiene todo en frío al instante."
        }
        val next = "Además ahorrás tiempo esperando que enfríe y gas/electricidad de hervir todos los días."
        return PsaAiStructuredAnswer(
            headline = "Por qué Hervir el Agua NO Reemplaza a PSA",
            recommendedModelName = senior.name,
            priceAndInstallmentText = priceLine,
            technicalSummary = tech,
            demoOrClientTip = tip,
            objectionOrNextStep = next,
            shareableText = "🔥 *¿Hervir alcanza?*\n$tech"
        )
    }

    private fun buildRentalInstallAnswer(
        audience: PsaAiAudience,
        allModels: List<PsaPurifierModel>,
        cuotas: Int,
        fmt: (Double) -> String
    ): PsaAiStructuredAnswer {
        val mini = findModel(allModels, "mini")
        val vero = findModel(allModels, "vero")
        val senior = findModel(allModels, "senior4")
        val priceLine = "PSA Mini: ${fmt(mini.price)} (${fmt(mini.price / cuotas)}/cuota) • PSA Vero: ${fmt(vero.price)} • PSA Senior 4: ${fmt(senior.price)}."
        val tech = "Los modelos sobre mesada PSA se acoplan al aireador de la canilla existente con un kit universal incluido. NO hay que perforar mesada, romper azulejos ni llamar plomero; al mudarse se desenrosca en 2 minutos y vuelve a quedar la canilla original."
        val tip = if (audience == PsaAiAudience.VENDEDOR) {
            "Desarma el miedo al instante: 'Es una inversión tuya que se muda con vos en una caja en 2 minutos, no queda pegada al departamento'."
        } else {
            "No perdés depósito ni pedís permiso al propietario: se instala y se retira sin dejar ninguna marca."
        }
        val next = "Si el monoambiente es chico o vive 1–2 personas, PSA Mini o PSA Vero encajan perfecto."
        return PsaAiStructuredAnswer(
            headline = "Alquiler / Mudanza / Instalación Sin Obras",
            recommendedModelName = "${vero.name} / ${mini.name}",
            priceAndInstallmentText = priceLine,
            technicalSummary = tech,
            demoOrClientTip = tip,
            objectionOrNextStep = next,
            shareableText = "📦 *PSA en Alquiler sin Obras*\n$tech"
        )
    }

    private fun buildWaterTypesComparisonAnswer(
        audience: PsaAiAudience,
        allModels: List<PsaPurifierModel>,
        cuotas: Int,
        fmt: (Double) -> String
    ): PsaAiStructuredAnswer {
        val senior = findModel(allModels, "senior4")
        val priceLine = "${senior.name}: ${fmt(senior.price)} (${fmt(senior.price / cuotas)} en $cuotas cuotas • ~${fmt(senior.price / 1095.0)}/día)."
        val tech = "• Agua de Mesa en bidón: suele ser agua de red filtrada con agregado de sodio y envase plástico reutilizado expuesto a traslado/sol.\n" +
            "• Agua Mineral: práctica para salir, pero costosa para cocinar/infusiones diarias y genera gran volumen plástico.\n" +
            "• Agua Purificada PSA: elimina >99,5% cloro y contaminantes en el momento de abrir la canilla, con costo mínimo por litro para beber, mate, café y comidas."
        val tip = if (audience == PsaAiAudience.VENDEDOR) {
            "Apóyate en el Módulo 8A (4 Tipos de Agua) y remarca el uso en cocina y mate/café: nadie cocina fideos con agua mineral cara, pero el cloro sí se absorbe en la cocción."
        } else {
            "Tenés agua fresca recién purificada todo el día para tomar, infusiones y cocinar sin esperar repartidores."
        }
        val next = "Un solo purificador reemplaza miles de botellas plásticas descartables al año."
        return PsaAiStructuredAnswer(
            headline = "PSA vs Agua Mineral vs Agua de Mesa en Bidón",
            recommendedModelName = senior.name,
            priceAndInstallmentText = priceLine,
            technicalSummary = tech,
            demoOrClientTip = tip,
            objectionOrNextStep = next,
            shareableText = "💧 *Comparativa de Aguas*\n$tech"
        )
    }

    private fun buildWarrantyAndInstallAnswer(
        audience: PsaAiAudience,
        allModels: List<PsaPurifierModel>,
        isWellWater: Boolean,
        cuotas: Int,
        fmt: (Double) -> String
    ): PsaAiStructuredAnswer {
        val model = findModel(allModels, if (isWellWater) "rotech" else "senior4")
        val priceLine = "${model.name}: ${fmt(model.price)} (${fmt(model.price / cuotas)} en $cuotas cuotas) • Respaldo ${model.anmatRegistration}."
        val tech = "Equipos certificados (${model.anmatRegistration}) con garantía oficial de fábrica de 36 meses en línea principal (24 meses Mini/C3, 18 meses Ducha). En modelos sobre mesada la colocación toma minutos sin obras; en bajo mesada/ósmosis se instala con grifo independiente."
        val tip = if (audience == PsaAiAudience.VENDEDOR) {
            "Destaca el respaldo oficial y la atención personalizada post-instalación para mantenimiento de Fipor y recambios."
        } else {
            "Contás con garantía oficial PSA, manual de uso y seguimiento programado para que siempre tomes agua segura."
        }
        val next = "Nunca pases agua caliente (>35 °C) ni uses detergentes abrasivos sobre el cuerpo del equipo."
        return PsaAiStructuredAnswer(
            headline = "Garantía Oficial 36 Meses, Registro e Instalación",
            recommendedModelName = model.name,
            priceAndInstallmentText = priceLine,
            technicalSummary = tech,
            demoOrClientTip = tip,
            objectionOrNextStep = next,
            shareableText = "🛡️ *Respaldo PSA*\n$tech"
        )
    }

    private fun buildSpecificModelAnswer(
        guide: PsaCatalogDeepGuide,
        model: PsaPurifierModel,
        audience: PsaAiAudience,
        cuotas: Int,
        fmt: (Double) -> String,
        country: PsaCountryConfig
    ): PsaAiStructuredAnswer {
        val cuota = model.price / cuotas
        val daily = if (model.lifespanMonths > 0) model.price / (model.lifespanMonths * 30.0) else model.price / 1095.0
        val priceLine = "${model.name} (${country.code}): ${fmt(model.price)} • $cuotas cuotas de ${fmt(cuota)} (~${fmt(daily)}/día)."
        val tech = "${guide.categoryTag} (${model.anmatRegistration})\n• Tecnología: ${guide.filtrationTech}\n• Retiene: ${guide.removesSummary}\n• Vida útil: ${guide.lifespanDetail}\n• Instalación: ${guide.installationNote}"
        val tip = if (audience == PsaAiAudience.VENDEDOR) guide.sellerDemoTip else guide.clientExplanation
        val next = "Mantenimiento: ${guide.replacementSchedule}"
        return PsaAiStructuredAnswer(
            headline = "Ficha Experta: ${model.name}",
            recommendedModelName = model.name,
            priceAndInstallmentText = priceLine,
            technicalSummary = tech,
            demoOrClientTip = tip,
            objectionOrNextStep = next,
            shareableText = "💧 *${model.name}*\n• ${guide.removesSummary}\n• ${guide.lifespanDetail}\n• $cuotas cuotas de ${fmt(cuota)} (Contado ${fmt(model.price)})"
        )
    }

    private fun buildContextualFallbackAnswer(
        displayQuestion: String,
        audience: PsaAiAudience,
        allModels: List<PsaPurifierModel>,
        cartModels: List<PsaPurifierModel>,
        isWellWater: Boolean,
        demoContext: PsaDemoAiContext,
        pricingConfig: PsaPricingConfig,
        cuotas: Int,
        fmt: (Double) -> String,
        country: PsaCountryConfig,
        memorySnippet: String
    ): PsaAiStructuredAnswer {
        val primary = cartModels.firstOrNull()
            ?: findModel(allModels, if (isWellWater) "rotech" else "senior4")
        val cuota = primary.price / cuotas
        val guide = guideFor(primary.id)
        val diagLine = if (isWellWater) {
            "Agua de pozo (${if (demoContext.hardnessPpm > 0) "${demoContext.hardnessPpm} ppm dureza" else "control de sales/bacterias"})"
        } else {
            "Agua de red (${String.format(Locale.US, "%.1f ppm cloro", demoContext.chlorinePpm)})"
        }
        val extraMem = if (audience == PsaAiAudience.VENDEDOR && memorySnippet.isNotBlank()) {
            " Argumento destacado en Memoria IA: \"$memorySnippet\""
        } else {
            ""
        }
        return PsaAiStructuredAnswer(
            headline = "Asistencia PSA sobre: \"${displayQuestion.take(64)}\"",
            recommendedModelName = primary.name,
            priceAndInstallmentText = "${primary.name} (${country.code}): ${fmt(primary.price)} • $cuotas cuotas de ${fmt(cuota)} • Bidón ref.: ${fmt(pricingConfig.bidonPrice)}.",
            technicalSummary = "Contexto actual: $diagLine. ${guide.filtrationTech} ${guide.removesSummary}",
            demoOrClientTip = (if (audience == PsaAiAudience.VENDEDOR) guide.sellerDemoTip else guide.clientExplanation) + extraMem,
            objectionOrNextStep = "Tip: toca cualquiera de las Preguntas Clave de arriba o escribe el nombre de un modelo (Senior 4, Senik, Quantum, Vero, Ducha, C3, Domus, R-200, Ropot, ROtech·UV, Iontrix3, AirPurifier) para ver su ficha exacta.",
            shareableText = "💧 *Consulta PSA (${country.code})*\n• Equipo: ${primary.name}\n• ${guide.removesSummary}\n• $cuotas cuotas de ${fmt(cuota)}."
        )
    }

    private fun guideFor(modelId: String): PsaCatalogDeepGuide {
        return catalogGuides.firstOrNull { it.modelId == modelId } ?: catalogGuides.first()
    }

    private fun findModel(allModels: List<PsaPurifierModel>, id: String): PsaPurifierModel {
        return allModels.firstOrNull { it.id == id } ?: allModels.first()
    }

    private fun normalizeText(input: String): String {
        val decomposed = Normalizer.normalize(input.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return decomposed
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("[^a-z0-9\\s._-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
