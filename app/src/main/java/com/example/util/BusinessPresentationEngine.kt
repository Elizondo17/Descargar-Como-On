package com.example.util

import android.content.Context
import com.example.data.model.PsaPricingConfig
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToLong

enum class PresentationBlock(
    val id: String,
    val number: Int,
    val shortTitle: String,
    val fullTitle: String,
    val description: String
) {
    OPPORTUNITY(
        id = "opportunity",
        number = 1,
        shortTitle = "1. Oportunidad",
        fullTitle = "Bloque 1 • Oportunidad y Sistema",
        description = "Cuadrante E-D-A-I, Flex Marketing Plan y Rol del Distribuidor"
    ),
    EARNINGS_CAREER(
        id = "earnings",
        number = 2,
        shortTitle = "2. Ganancias",
        fullTitle = "Bloque 2 • Cómo Se Gana y Carrera Empresarial",
        description = "Distribución Personal, Red, 7 Escalones, 9 Países y 15 Patrocinios"
    ),
    PRODUCTS_TRUST(
        id = "products",
        number = 3,
        shortTitle = "3. Productos",
        fullTitle = "Bloque 3 • Productos, Avales y Alcance Global",
        description = "Equipos, Mapa Interactivo del Hogar, Certificaciones, Empresa B y 9 Países"
    ),
    METHOD_TRAINING(
        id = "method",
        number = 4,
        shortTitle = "4. Método",
        fullTitle = "Bloque 4 • Método, Plan Semanal y Formación",
        description = "8 Acciones + 4 Soporte, Full Time vs ½ Tiempo, 4 Pilares y Ganar-Ganar"
    ),
    VISION_CLOSE(
        id = "close",
        number = 5,
        shortTitle = "5. Cierre",
        fullTitle = "Bloque 5 • Visión, Cierre 1–10 y Material",
        description = "Valor del Tiempo, Reflexión 1/5 años, Termómetro 1–10 y WhatsApp"
    )
}

enum class CashflowQuadrant(
    val letter: String,
    val title: String,
    val subtitle: String,
    val sideSummary: String,
    val isPassiveSide: Boolean
) {
    E(
        letter = "E",
        title = "EMPLEADO",
        subtitle = "Seguridad - Certidumbre",
        sideSummary = "95% PERSONAS • 5% DINERO • TIEMPO POR DINERO • INGRESOS ACTIVOS",
        isPassiveSide = false
    ),
    D(
        letter = "D",
        title = "DUEÑO",
        subtitle = "Libertad",
        sideSummary = "5% PERSONAS • 95% DINERO • LIBERTAD FINANCIERA • INGRESOS PASIVOS",
        isPassiveSide = true
    ),
    A(
        letter = "A",
        title = "AUTOEMPLEADO",
        subtitle = "Independencia - Control",
        sideSummary = "95% PERSONAS • 5% DINERO • TIEMPO POR DINERO • INGRESOS ACTIVOS",
        isPassiveSide = false
    ),
    I(
        letter = "I",
        title = "INVERSIONISTA",
        subtitle = "Rentabilidad",
        sideSummary = "5% PERSONAS • 95% DINERO • LIBERTAD FINANCIERA • INGRESOS PASIVOS",
        isPassiveSide = true
    )
}

data class FlexBenefitOption(
    val id: String,
    val title: String
)

enum class CareerRankId(val code: String, val title: String, val teamSizeLabel: String, val scaleFromSenior4: Double) {
    DJ("dj", "Distribuidor Junior", "Sin ganancias pasivas", 0.0),
    D("D", "Distribuidor", "3 personas", 178_000.0 / 950_000.0),
    DC("DC", "Distribuidor Calificado", "14 personas", 1_780_000.0 / 950_000.0),
    CE("CE", "Coordinador de Equipo", "45 personas", 5_700_000.0 / 950_000.0),
    LE("LE", "Líder de Equipo", "185 personas", 15_000_000.0 / 950_000.0),
    LJ("LJ", "Líder Ejecutivo", "750 personas", 60_000_000.0 / 950_000.0),
    EM("EM", "Empresario PSA", "3.000 personas", 223_000_000.0 / 950_000.0)
}

data class CareerRankProjection(
    val rank: CareerRankId,
    val estimatedMonthlyIncome: Double
)

data class CountryCareerRow(
    val countryCode: String,
    val countryName: String,
    val flag: String,
    val pb150: String,
    val pb600: String,
    val pb2400: String,
    val pb10000: String,
    val pb40000: String,
    val isActiveCountry: Boolean,
    val isDynamicFromCatalog: Boolean
)

enum class HomeZone(val label: String) {
    COCINA("Cocina y Mesada"),
    BANO("Baño y Lavatorio"),
    AIRE("Living y Dormitorio"),
    LAVADERO("Tanque y Lavadero"),
    EXTERIOR("Pileta y Aire Libre")
}

data class HomeProductZoneSpot(
    val id: String,
    val displayName: String,
    val zone: HomeZone,
    val benefitLine: String,
    val catalogPrice: Double
)

enum class WeeklyPlanMode(
    val label: String,
    val title: String,
    val demosCount: Int,
    val closesCount: Int,
    val pnsCount: Int,
    val followUpsCount: Int
) {
    FULL_TIME(
        label = "Tiempo Completo",
        title = "Plan semanal a tiempo completo:",
        demosCount = 6,
        closesCount = 6,
        pnsCount = 5,
        followUpsCount = 5
    ),
    HALF_TIME(
        label = "½ Tiempo (4hs L a V)",
        title = "Plan semanal ½ tiempo: 4hs diarias de L a V",
        demosCount = 4,
        closesCount = 4,
        pnsCount = 2,
        followUpsCount = 2
    )
}

data class PresentationQuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class ShareableBusinessMaterial(
    val id: String,
    val title: String,
    val category: String
)

data class CareerMilestoneAward(
    val id: String,
    val pbPoints: Int,
    val awardTitle: String,
    val linkedRank: CareerRankId,
    val teamRequirementLabel: String,
    val incentiveDescription: String,
    val calculatedAmount: Double,
    val monthlyRankIncome: Double
)

data class WeeklyProspectingPlanState(
    val mode: WeeklyPlanMode = WeeklyPlanMode.HALF_TIME,
    val demosPerWeek: Int = WeeklyPlanMode.HALF_TIME.demosCount,
    val closesPerWeek: Int = WeeklyPlanMode.HALF_TIME.closesCount,
    val pnsPerWeek: Int = WeeklyPlanMode.HALF_TIME.pnsCount,
    val followUpsPerWeek: Int = WeeklyPlanMode.HALF_TIME.followUpsCount,
    val activeDaysPerWeek: Int = 5,
    val selectedScheduleSlot: String = "Tarde (14:00 - 18:00)",
    val targetRank: CareerRankId = CareerRankId.DC,
    val selectedReferencePurifierId: String = "senior4"
) {
    val totalWeeklyActions: Int
        get() = demosPerWeek + closesPerWeek + pnsPerWeek + followUpsPerWeek

    companion object {
        private const val PREFS_NAME = "psa_career_weekly_plan"
        private const val KEY_MODE = "mode"
        private const val KEY_DEMOS = "demos"
        private const val KEY_CLOSES = "closes"
        private const val KEY_PNS = "pns"
        private const val KEY_FOLLOWUPS = "followups"
        private const val KEY_DAYS = "days"
        private const val KEY_SLOT = "slot"
        private const val KEY_TARGET_RANK = "target_rank"
        private const val KEY_PURIFIER_ID = "purifier_id"

        fun forMode(
            mode: WeeklyPlanMode,
            preserving: WeeklyProspectingPlanState = WeeklyProspectingPlanState()
        ): WeeklyProspectingPlanState {
            return preserving.copy(
                mode = mode,
                demosPerWeek = mode.demosCount,
                closesPerWeek = mode.closesCount,
                pnsPerWeek = mode.pnsCount,
                followUpsPerWeek = mode.followUpsCount,
                activeDaysPerWeek = if (mode == WeeklyPlanMode.FULL_TIME) 6 else 5
            )
        }

        fun loadFromPrefs(context: Context): WeeklyProspectingPlanState {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val modeName = prefs.getString(KEY_MODE, WeeklyPlanMode.HALF_TIME.name)
            val mode = WeeklyPlanMode.entries.firstOrNull { it.name == modeName } ?: WeeklyPlanMode.HALF_TIME
            val rankCode = prefs.getString(KEY_TARGET_RANK, CareerRankId.DC.code)
            val rank = CareerRankId.entries.firstOrNull { it.code == rankCode } ?: CareerRankId.DC
            return WeeklyProspectingPlanState(
                mode = mode,
                demosPerWeek = prefs.getInt(KEY_DEMOS, mode.demosCount).coerceIn(0, 30),
                closesPerWeek = prefs.getInt(KEY_CLOSES, mode.closesCount).coerceIn(0, 30),
                pnsPerWeek = prefs.getInt(KEY_PNS, mode.pnsCount).coerceIn(0, 25),
                followUpsPerWeek = prefs.getInt(KEY_FOLLOWUPS, mode.followUpsCount).coerceIn(0, 25),
                activeDaysPerWeek = prefs.getInt(KEY_DAYS, if (mode == WeeklyPlanMode.FULL_TIME) 6 else 5).coerceIn(1, 7),
                selectedScheduleSlot = prefs.getString(KEY_SLOT, "Tarde (14:00 - 18:00)") ?: "Tarde (14:00 - 18:00)",
                targetRank = rank,
                selectedReferencePurifierId = prefs.getString(KEY_PURIFIER_ID, "senior4") ?: "senior4"
            )
        }

        fun saveToPrefs(context: Context, state: WeeklyProspectingPlanState) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_MODE, state.mode.name)
                .putInt(KEY_DEMOS, state.demosPerWeek)
                .putInt(KEY_CLOSES, state.closesPerWeek)
                .putInt(KEY_PNS, state.pnsPerWeek)
                .putInt(KEY_FOLLOWUPS, state.followUpsPerWeek)
                .putInt(KEY_DAYS, state.activeDaysPerWeek)
                .putString(KEY_SLOT, state.selectedScheduleSlot)
                .putString(KEY_TARGET_RANK, state.targetRank.code)
                .putString(KEY_PURIFIER_ID, state.selectedReferencePurifierId)
                .apply()
        }
    }
}

data class CareerEnterpriseSummary(
    val referencePurifierId: String,
    val referencePurifierName: String,
    val referenceUnitPrice: Double,
    val unitDirectProfit: Double,
    val catalogScaleFactor: Double,
    val ranks: List<CareerRankProjection>,
    val milestoneAwards: List<CareerMilestoneAward>,
    val weeklyTotalActions: Int,
    val monthlyDemosProjected: Int,
    val monthlyClosesProjected: Int,
    val monthlyPnsProjected: Int,
    val monthlyFollowUpsProjected: Int,
    val projectedWeeklyPersonalIncome: Double,
    val projectedMonthlyPersonalIncome: Double,
    val projectedAnnualPersonalIncome: Double,
    val targetAward: CareerMilestoneAward,
    val estimatedMonthsToTargetRank: Int,
    val estimatedWeeksToFirstRank: Int
)

data class DynamicPresentationMetrics(
    val referenceUnitName: String,
    val referenceUnitPrice: Double,
    val personalOneProductProfit: Double,
    val personalTenProductsProfit: Double,
    val networkOneDistTenProductsProfit: Double,
    val networkTenDistTenProductsProfit: Double,
    val careerRanks: List<CareerRankProjection>,
    val activeCatalogFactor: Double
)

data class PresentationSlideSpec(
    val index: Int,
    val block: PresentationBlock,
    val pdfPageNumber: Int,
    val badge: String,
    val title: String,
    val subtitle: String,
    val questionPrompt: String = ""
)

data class BusinessPresentationSessionState(
    val activeSlideIndex: Int = 0,
    val currentQuadrant: CashflowQuadrant? = null,
    val desiredQuadrant: CashflowQuadrant? = CashflowQuadrant.D,
    val selectedPriorityId: String? = null,
    val selectedTripleImpactPillars: Set<String> = setOf("Valor Social", "Valor Ambiental", "Valor Económico"),
    val personalTop10ProspectsNote: String = "",
    val networkTop10ProspectsNote: String = "",
    val selectedCareerRank: CareerRankId = CareerRankId.DC,
    val selectedSponsorshipStage: Int = 1,
    val activeHomeZone: HomeZone? = null,
    val selectedProductInterestIds: Set<String> = setOf("senior4"),
    val ownedProductIds: Set<String> = emptySet(),
    val selectedProductBenefit: String = "Salud",
    val selectedCertIndex: Int = 0,
    val selectedMarketCountryCode: String = "AR",
    val activeActionStepNumber: Int = 1,
    val quizAnswers: Map<String, Int> = emptyMap(),
    val weeklyPlanMode: WeeklyPlanMode = WeeklyPlanMode.HALF_TIME,
    val weeklyDemosGoal: Int = WeeklyPlanMode.HALF_TIME.demosCount,
    val weeklyClosesGoal: Int = WeeklyPlanMode.HALF_TIME.closesCount,
    val weeklyPnsGoal: Int = WeeklyPlanMode.HALF_TIME.pnsCount,
    val weeklyFollowUpsGoal: Int = WeeklyPlanMode.HALF_TIME.followUpsCount,
    val weeklyActiveDays: Int = 5,
    val selectedScheduleSlot: String = "Tarde (14:00 - 18:00)",
    val selectedTrainingPillarIndex: Int = 0,
    val selectedWinWinStepIndex: Int = 0,
    val reflectionCheckedIndices: Set<Int> = emptySet(),
    val closingScore: Int = 8,
    val closingGapReason: String = "",
    val followUpDate: String = "",
    val followUpTime: String = "",
    val selectedMaterialIds: Set<String> = setOf("campus", "oportunidad_pdf", "catalogo")
)

object BusinessPresentationEngine {

    private const val AR_BASELINE_SENIOR4 = 950_000.0
    private const val PERSONAL_UNIT_RATIO = 337_000.0 / AR_BASELINE_SENIOR4 // ~35.47% margen PDF
    private const val NETWORK_1X10_RATIO = 390_000.0 / AR_BASELINE_SENIOR4  // ~41.05% bono sobre 10u

    val flexBenefits: List<FlexBenefitOption> = listOf(
        FlexBenefitOption("dinero", "Elegir cuánto dinero generar o incluso llegar a ser financieramente libre"),
        FlexBenefitOption("tiempo", "Administrar tus tiempos"),
        FlexBenefitOption("habilidades", "Desarrollar nuevas habilidades"),
        FlexBenefitOption("ayudar", "Ayudar a otras personas"),
        FlexBenefitOption("dueno", "Ser dueño de un negocio"),
        FlexBenefitOption("sustentable", "Generar un impacto sustentable")
    )

    val distributorRolesList: List<String> = listOf(
        "Empresa de Triple impacto",
        "Presente en 9 países",
        "Fabrica y garantiza los productos",
        "Investigación y desarrollo",
        "Reciclado",
        "Capacitación"
    )

    val productBenefitsList: List<Pair<String, String>> = listOf(
        "Versatilidad" to "Soluciones específicas para cocina, baño, tanque, aire, pileta y traslado.",
        "Sustentabilidad" to "Reduce miles de botellas y bidones plásticos descartables por familia.",
        "Economía" to "Costo por litro muchísimo menor al agua envasada desde los primeros meses.",
        "Comodidad" to "Agua purificada al instante en tu canilla sin cargar ni esperar repartos.",
        "Salud" to "Retención de cloro, sedimentos y contaminantes para beber, lavar y cocinar."
    )

    val certificationsList: List<Pair<String, String>> = listOf(
        "Inscriptos en AESAN" to "Registro sanitario europeo en seguridad alimentaria y nutrición.",
        "AENOR UNE 149101" to "Norma europea para equipos de acondicionamiento de agua en consumo humano.",
        "1º Líder" to "Marca líder de purificadores domésticos en la región.",
        "ISO 9001:2015" to "Sistema de gestión de la calidad certificado en procesos y fabricación.",
        "Water Quality Association" to "Miembro activo de la Asociación Internacional de Calidad del Agua.",
        "LATU" to "Certificación y ensayos del Laboratorio Tecnológico del Uruguay.",
        "dictuc" to "Validación técnica y ensayos de laboratorio de referencia en Chile.",
        "IBMETRO" to "Verificación metrológica oficial en Bolivia.",
        "ANMAT" to "Administración Nacional de Medicamentos, Alimentos y Tecnología Médica (Argentina)."
    )

    data class CountryMarketStat(
        val code: String,
        val countryName: String,
        val flag: String,
        val potentialMarket: String,
        val activeNetworkOrUsers: String
    )

    val potentialMarketCountries: List<CountryMarketStat> = listOf(
        CountryMarketStat("AR", "Argentina", "🇦🇷", "15.235.964", "2.570.000"),
        CountryMarketStat("UY", "Uruguay", "🇺🇾", "1.101.685", "58.300"),
        CountryMarketStat("CL", "Chile", "🇨🇱", "6.473.468", "13.000"),
        CountryMarketStat("BO", "Bolivia", "🇧🇴", "3.008.147", "11.800"),
        CountryMarketStat("CO", "Colombia", "🇨🇴", "16.993.759", "6.200"),
        CountryMarketStat("PE", "Perú", "🇵🇪", "7.998.000", "2.000"),
        CountryMarketStat("EC", "Ecuador", "🇪🇨", "4.999.000", "1.000"),
        CountryMarketStat("PY", "Paraguay", "🇵🇾", "1.699.830", "400"),
        CountryMarketStat("ES", "España", "🇪🇸", "18.754.800", "100")
    )

    val eightProductiveActions: List<Pair<Int, String>> = listOf(
        1 to "LISTA DE NOMBRES",
        2 to "FECHAR DEMOS",
        3 to "DEMOSTRAR PRODUCTOS",
        4 to "CERRAR DEMOS",
        5 to "FECHAR PNS",
        6 to "PRESENTAR EL NEGOCIO",
        7 to "CIERRE Y SEGUIMIENTO",
        8 to "ENSEÑAR LO ANTERIOR A LOS NUEVOS DIPS"
    )

    val fourSupportActions: List<String> = listOf(
        "AUTOLIDERAZGO",
        "TRABAJO DEL SER",
        "FIDELIZAR USUARIOS",
        "HACER COMUNIDAD"
    )

    val quickQuizQuestions: List<PresentationQuizQuestion> = listOf(
        PresentationQuizQuestion(
            id = "q_demos",
            question = "En Distribución Personal, si hacés 30 Demos, ¿cuántos hogares suelen decir que sí?",
            options = listOf("5 hogares", "10 hogares", "20 hogares"),
            correctIndex = 1,
            explanation = "Regla oficial PSA: 30 Demos proyectan 10 hogares que eligen el purificador."
        ),
        PresentationQuizQuestion(
            id = "q_first_step",
            question = "¿Cuál es la Acción Productiva N° 1 del método exitoso y duplicable?",
            options = listOf("Lista de nombres", "Subir anuncios", "Comprar stock masivo"),
            correctIndex = 0,
            explanation = "Todo arranca con tu Lista de Nombres (Acción 1) antes de fechar demos (Acción 2)."
        ),
        PresentationQuizQuestion(
            id = "q_half_time",
            question = "En el Plan Semanal ½ tiempo (4hs diarias L a V), ¿cuántas Demos y PNs se proponen?",
            options = listOf("6 Demos y 5 PNs", "4 Demos y 2 PNs", "2 Demos y 1 PN"),
            correctIndex = 1,
            explanation = "½ tiempo: 4 Demostraciones, 4 Cierres, 2 Presentaciones del Negocio y 2 Seguimientos."
        )
    )

    val trainingPillars: List<Triple<String, String, String>> = listOf(
        Triple("Empresa", "Teoría", "Campus MiPSA, manuales oficiales, VIVO PSA y capacitaciones de producto."),
        Triple("Línea Ascendente", "Práctica", "Acompañamiento en tus primeras demostraciones, cierres y PNs."),
        Triple("Equipo", "Sinergia • Experiencia • Reconocimiento • Motivación", "Reunión de Equipo y talleres compartidos para crecer acompañado."),
        Triple("Autoformación", "Estudiar videos y libros sugeridos", "Desarrollo diario de mentalidad, comunicación y liderazgo personal.")
    )

    val winWinCascade: List<String> = listOf(
        "Cuando un D que vos patrocinas gana por su comercialización:",
        "Una familia gana en salud, practicidad, economía y seguridad",
        "La empresa gana un usuario",
        "Vos ganas tus ingresos pasivos (Flex)",
        "Todos ganamos en ecología"
    )

    val reflectionSevenQuestions: List<String> = listOf(
        "¿Pensas que todos necesitan los purificadores?",
        "¿Crees que pocos los tienen?",
        "¿Te parece que podrás manejar tus tiempos?",
        "¿Pensas que la ganancia de la distribución personal es buena?",
        "¿Crees que la ganancia por el desarrollo de la red te puede hacer libre financieramente?",
        "¿Te gustaría crecer ayudando a otros a mejorar su salud y su economía?",
        "¿Te parece que en este negocio tendrás desarrollo personal y profesional?"
    )

    val shareableMaterials: List<ShareableBusinessMaterial> = listOf(
        ShareableBusinessMaterial("campus", "Link al Campus para Invitados, en MiPSA -> Mi Negocio", "Acceso"),
        ShareableBusinessMaterial("oportunidad_pdf", "Oportunidad de Negocio.pdf", "Documento"),
        ShareableBusinessMaterial("presentacion_empresa", "Presentación de la Empresa", "Documento"),
        ShareableBusinessMaterial("video_invitacion", "Video Invitación de la Empresa", "Video"),
        ShareableBusinessMaterial("catalogo", "Solución integral o catálogo virtual de PSA", "Catálogo"),
        ShareableBusinessMaterial("podcast", "Podcast PSA", "Audio"),
        ShareableBusinessMaterial("video_manifiesto", "Video del Manifiesto PSA", "Video"),
        ShareableBusinessMaterial("video_evolucion", "Video de evolución PSA", "Video"),
        ShareableBusinessMaterial("ficha_medios", "Ficha de medios Activos", "Ficha técnica"),
        ShareableBusinessMaterial("ficha_cocinar", "Ficha de conoces los beneficios de cocinar con Agua Pura", "Ficha técnica")
    )

    val allSlides: List<PresentationSlideSpec> = listOf(
        // Bloque 1: Oportunidad y Sistema (PDF 1..4)
        PresentationSlideSpec(0, PresentationBlock.OPPORTUNITY, 1, "Lámina 1 / 23", "Presentación del Negocio", "Oportunidad de desarrollo personal, profesional y económico"),
        PresentationSlideSpec(1, PresentationBlock.OPPORTUNITY, 2, "Lámina 2 / 23", "Las personas generamos dinero de 4 formas:", "Cuadrante E-D-A-I: Ingresos Activos vs Ingresos Pasivos", "¿En qué cuadrante estás y en cuál querés estar?"),
        PresentationSlideSpec(2, PresentationBlock.OPPORTUNITY, 3, "Lámina 3 / 23", "Te proponemos desarrollarte en un sistema alternativo, el Flex Marketing Plan", "Sus principales beneficios son:", "¿Cuál de estos es tu prioridad?"),
        PresentationSlideSpec(3, PresentationBlock.OPPORTUNITY, 4, "Lámina 4 / 23", "Como Distribuidores Independientes PSA, nuestro rol es concientizar sobre cómo los purificadores y la Oportunidad de Negocio puede mejorarle la vida a las personas", "Nos asociamos con PSA y le compramos los productos"),

        // Bloque 2: Ganancias y Carrera (PDF 5, 6, 7, 16, 17)
        PresentationSlideSpec(4, PresentationBlock.EARNINGS_CAREER, 5, "Lámina 5 / 23", "DISTRIBUCIÓN PERSONAL:", "Llamamos a conocidos para visitarlos, demostrarles los purificadores y que respondan una encuesta; luego les pedimos que nos recomienden a sus contactos", "¿Quiénes pueden ser las primeras 10 personas a las que les mostremos nuestros purificadores?"),
        PresentationSlideSpec(5, PresentationBlock.EARNINGS_CAREER, 6, "Lámina 6 / 23", "DISTRIBUCIÓN EN RED:", "Desarrollando la carrera empresarial PSA, podrás generar ingresos pasivos, que te permitirán tener mayor Libertad.", "¿Quiénes querés que sean los 10 primeros en escuchar la propuesta?"),
        PresentationSlideSpec(6, PresentationBlock.EARNINGS_CAREER, 7, "Lámina 7 / 23", "Carrera Empresarial", "Flex Marketing Plan • (Para este ejemplo cada persona comercializa 1 Purificador Semanal)", "¿Cuál categoría te permite el estilo de vida que deseas?"),
        PresentationSlideSpec(7, PresentationBlock.EARNINGS_CAREER, 16, "Lámina 8 / 23", "CARRERA EMPRESARIAL EN PSA", "¿En qué categoría se cumplen tus sueños?", "¿En qué categoría se cumplen tus sueños?"),
        PresentationSlideSpec(8, PresentationBlock.EARNINGS_CAREER, 17, "Lámina 9 / 23", "15 PATROCINIOS EFECTIVOS", "1era, 2da y 3era Etapa de Patrocinio • D -> DC -> CE"),

        // Bloque 3: Productos y Avales (PDF 11..15)
        PresentationSlideSpec(9, PresentationBlock.PRODUCTS_TRUST, 11, "Lámina 10 / 23", "Comunidad y Equipos PSA", "Bee • MasterSOL • Los Alpinistas"),
        PresentationSlideSpec(10, PresentationBlock.PRODUCTS_TRUST, 12, "Lámina 11 / 23", "PRODUCTOS", "PSA ofrece una solución para cada necesidad, brindando a las personas la posibilidad de que puedan cuidarse con un Sistema de Purificación.", "¿Qué purificadores estas usando y cuáles te gustaría empezar a usar?"),
        PresentationSlideSpec(11, PresentationBlock.PRODUCTS_TRUST, 13, "Lámina 12 / 23", "BENEFICIOS DE LOS PRODUCTOS", "CERTIFICACIONES Y AVALES • Inscriptos en AESAN", "¿Quiénes te parece que necesitan nuestros purificadores?"),
        PresentationSlideSpec(12, PresentationBlock.PRODUCTS_TRUST, 14, "Lámina 13 / 23", "Empresa B Certificada", "B. significa beneficios para todos • Best for the World 2022 Comunidad"),
        PresentationSlideSpec(13, PresentationBlock.PRODUCTS_TRUST, 15, "Lámina 14 / 23", "Mercado Potencial en 9 Países", "Presencia regional y oportunidades de expansión"),

        // Bloque 4: Método y Formación (PDF 18..22)
        PresentationSlideSpec(14, PresentationBlock.METHOD_TRAINING, 18, "Lámina 15 / 23", "El método para el Desarrollo Exitoso y Duplicable:", "8 Acciones Productivas y 4 Acciones Soporte"),
        PresentationSlideSpec(15, PresentationBlock.METHOD_TRAINING, 19, "Lámina 16 / 23", "Plan semanal a tiempo completo:", "6 Demostraciones • 6 Cierres • 5 PNs • 5 Seguimientos", "¿En qué horarios podrías hacer las Acciones del negocio PSA?"),
        PresentationSlideSpec(16, PresentationBlock.METHOD_TRAINING, 20, "Lámina 17 / 23", "Plan semanal ½ tiempo: 4hs diarias de L a V", "4 Demostraciones • 4 Cierres • 2 PNs • 2 Seguimientos", "¿En qué horarios podrías hacer las Acciones del negocio PSA?"),
        PresentationSlideSpec(17, PresentationBlock.METHOD_TRAINING, 21, "Lámina 18 / 23", "Los 4 pilares de la Formación", "Las Herramientas están a tu alcance, el negocio depende de vos", "¿Cuán importante te parece que es adquirir las Habilidades del Negocio?"),
        PresentationSlideSpec(18, PresentationBlock.METHOD_TRAINING, 22, "Lámina 19 / 23", "SISTEMA GANAR-GANAR:", "se crece ayudando a las personas de tu Red a crecer", "¿Cómo podes ayudar a las personas de tu red a crecer?"),

        // Bloque 5: Visión, Cierre y Material (PDF 23, 8, 9, 10)
        PresentationSlideSpec(19, PresentationBlock.VISION_CLOSE, 23, "Lámina 20 / 23", "EL VALOR DEL TIEMPO", "El desarrollo de una Red de Distribuidores en PSA te permite hacer la transición a la libertad de tiempo y dinero", "De 1 a 10, ¿Te parece que el negocio puede ser para vos?"),
        PresentationSlideSpec(20, PresentationBlock.VISION_CLOSE, 8, "Lámina 21 / 23", "¿Cómo te ves en 1 año si seguís haciendo lo que venís haciendo? …y en 5 años?", "Respecto de lo que estuvimos hablando:"),
        PresentationSlideSpec(21, PresentationBlock.VISION_CLOSE, 9, "Lámina 22 / 23", "De 1 a 10, ¿Te parece que el negocio puede ser para vos?", "Te proponemos un segundo encuentro, agendemos fecha y hora", "¿Qué te separa del 10?"),
        PresentationSlideSpec(22, PresentationBlock.VISION_CLOSE, 10, "Lámina 23 / 23", "Material para compartir con el invitado:", "Selecciona los recursos y envíalos directo por WhatsApp")
    )

    fun firstSlideIndexOfBlock(block: PresentationBlock): Int =
        allSlides.indexOfFirst { it.block == block }.coerceAtLeast(0)

    fun calculateDynamicMetrics(
        pricingConfig: PsaPricingConfig,
        activeCountry: PsaCountryConfig
    ): DynamicPresentationMetrics {
        val defaultCountryPrice = PsaCountryRegistry.defaultPricingForCountry(activeCountry.code).senior4Price.coerceAtLeast(1.0)
        val liveSenior4 = pricingConfig.senior4Price.takeIf { it > 0.0 } ?: defaultCountryPrice
        val catalogFactor = liveSenior4 / defaultCountryPrice

        val unitProfit = roundFriendly(liveSenior4 * PERSONAL_UNIT_RATIO, liveSenior4)
        val tenProductsProfit = unitProfit * 10.0
        val netOneDist = roundFriendly(liveSenior4 * NETWORK_1X10_RATIO, liveSenior4)
        val netTenDist = netOneDist * 10.0

        val ranks = CareerRankId.entries.map { rank ->
            val projected = if (rank.scaleFromSenior4 <= 0.0) {
                0.0
            } else {
                roundFriendly(liveSenior4 * rank.scaleFromSenior4, liveSenior4)
            }
            CareerRankProjection(rank = rank, estimatedMonthlyIncome = projected)
        }

        return DynamicPresentationMetrics(
            referenceUnitName = "PSA Senior 4",
            referenceUnitPrice = liveSenior4,
            personalOneProductProfit = unitProfit,
            personalTenProductsProfit = tenProductsProfit,
            networkOneDistTenProductsProfit = netOneDist,
            networkTenDistTenProductsProfit = netTenDist,
            careerRanks = ranks,
            activeCatalogFactor = catalogFactor
        )
    }

    private data class BaselineCareerNumbers(
        val code: String,
        val name: String,
        val flag: String,
        val pb150: Double,
        val pb600: Double,
        val pb2400: Double,
        val pb10000: Double,
        val pb40000: Double
    )

    private val pdfCareerBaselines = listOf(
        BaselineCareerNumbers("AR", "Argentina", "🇦🇷", 1_720_500.0, 5_510_000.0, 14_500_000.0, 58_500_000.0, 215_000_000.0),
        BaselineCareerNumbers("BO", "Bolivia", "🇧🇴", 9_000.0, 29_000.0, 76_000.0, 306_500.0, 1_142_000.0),
        BaselineCareerNumbers("CL", "Chile", "🇨🇱", 1_042_500.0, 3_336_000.0, 8_757_000.0, 35_445_000.0, 130_000_000.0),
        BaselineCareerNumbers("CO", "Colombia", "🇨🇴", 4_470_000.0, 14_305_000.0, 37_550_000.0, 151_980_000.0, 568_130_000.0),
        BaselineCareerNumbers("EC", "Ecuador", "🇪🇨", 990.0, 3_200.0, 8_300.0, 33_600.0, 123_000.0),
        BaselineCareerNumbers("ES", "España", "🇪🇸", 1_200.0, 3_900.0, 10_000.0, 40_800.0, 150_000.0),
        BaselineCareerNumbers("PY", "Paraguay", "🇵🇾", 6_825_000.0, 21_840_000.0, 57_330_000.0, 232_050_000.0, 873_000_000.0),
        BaselineCareerNumbers("PE", "Perú", "🇵🇪", 3_855.0, 12_340.0, 32_400.0, 131_070.0, 491_000.0),
        BaselineCareerNumbers("UY", "Uruguay", "🇺🇾", 52_000.0, 168_000.0, 440_000.0, 1_785_000.0, 6_700_000.0)
    )

    fun buildCareerTableForActiveCatalog(
        pricingConfig: PsaPricingConfig,
        activeCountry: PsaCountryConfig
    ): List<CountryCareerRow> {
        val defaultActivePrice = PsaCountryRegistry.defaultPricingForCountry(activeCountry.code).senior4Price.coerceAtLeast(1.0)
        val liveActivePrice = pricingConfig.senior4Price.takeIf { it > 0.0 } ?: defaultActivePrice
        val factor = liveActivePrice / defaultActivePrice

        return pdfCareerBaselines.map { base ->
            val countryCfg = PsaCountryRegistry.getByCode(base.code)
            val isActive = base.code.equals(activeCountry.code, ignoreCase = true)
            val multiplier = if (isActive) factor else 1.0
            val fmt: (Double) -> String = { amount ->
                val scaled = if (isActive) roundFriendly(amount * multiplier, liveActivePrice) else amount
                PsaCountryRegistry.formatCurrency(scaled, countryCfg)
            }
            CountryCareerRow(
                countryCode = base.code,
                countryName = base.name,
                flag = base.flag,
                pb150 = fmt(base.pb150),
                pb600 = fmt(base.pb600),
                pb2400 = fmt(base.pb2400),
                pb10000 = fmt(base.pb10000),
                pb40000 = fmt(base.pb40000),
                isActiveCountry = isActive,
                isDynamicFromCatalog = isActive
            )
        }
    }

    fun calculateCareerEnterpriseSummary(
        pricingConfig: PsaPricingConfig,
        activeCountry: PsaCountryConfig,
        planState: WeeklyProspectingPlanState = WeeklyProspectingPlanState()
    ): CareerEnterpriseSummary {
        val defaultCountrySenior4 = PsaCountryRegistry
            .defaultPricingForCountry(activeCountry.code)
            .senior4Price
            .coerceAtLeast(1.0)
        val catalogItems = pricingConfig.getAllPurifiers()
        val selectedItem = catalogItems.firstOrNull { it.id == planState.selectedReferencePurifierId }
            ?: catalogItems.firstOrNull { it.id == "senior4" }
            ?: catalogItems.firstOrNull()

        val referencePrice = (selectedItem?.price?.takeIf { it > 0.0 }
            ?: pricingConfig.senior4Price.takeIf { it > 0.0 }
            ?: defaultCountrySenior4).coerceAtLeast(1.0)
        val referenceId = selectedItem?.id ?: "senior4"
        val referenceName = selectedItem?.name ?: "PSA Senior 4"
        val scaleFactor = referencePrice / defaultCountrySenior4

        val unitProfit = roundFriendly(referencePrice * PERSONAL_UNIT_RATIO, referencePrice)
        val ranks = CareerRankId.entries.map { rank ->
            val projected = if (rank.scaleFromSenior4 <= 0.0) {
                0.0
            } else {
                roundFriendly(referencePrice * rank.scaleFromSenior4, referencePrice)
            }
            CareerRankProjection(rank = rank, estimatedMonthlyIncome = projected)
        }
        val incomeByRank = ranks.associate { it.rank to it.estimatedMonthlyIncome }

        val countryBaseline = pdfCareerBaselines.firstOrNull {
            it.code.equals(activeCountry.code, ignoreCase = true)
        } ?: pdfCareerBaselines.first()

        val milestoneAwards = listOf(
            CareerMilestoneAward(
                id = "pb_150",
                pbPoints = 150,
                awardTitle = "Calificación 150 PB",
                linkedRank = CareerRankId.D,
                teamRequirementLabel = "3 personas activas",
                incentiveDescription = "Habilitación Flex Plan + Bono Calificación Inicial",
                calculatedAmount = roundFriendly(countryBaseline.pb150 * scaleFactor, referencePrice),
                monthlyRankIncome = incomeByRank[CareerRankId.D] ?: 0.0
            ),
            CareerMilestoneAward(
                id = "pb_600",
                pbPoints = 600,
                awardTitle = "Coordinación 600 PB",
                linkedRank = CareerRankId.DC,
                teamRequirementLabel = "14 personas en red",
                incentiveDescription = "Bono Coordinación + Encuentro Regional PSA",
                calculatedAmount = roundFriendly(countryBaseline.pb600 * scaleFactor, referencePrice),
                monthlyRankIncome = incomeByRank[CareerRankId.DC] ?: 0.0
            ),
            CareerMilestoneAward(
                id = "pb_2400",
                pbPoints = 2400,
                awardTitle = "Liderazgo 2.400 PB",
                linkedRank = CareerRankId.CE,
                teamRequirementLabel = "45 personas en red",
                incentiveDescription = "Bono Liderazgo + Premios de Equipo CE",
                calculatedAmount = roundFriendly(countryBaseline.pb2400 * scaleFactor, referencePrice),
                monthlyRankIncome = incomeByRank[CareerRankId.CE] ?: 0.0
            ),
            CareerMilestoneAward(
                id = "pb_10000",
                pbPoints = 10000,
                awardTitle = "Ejecutivo 10.000 PB",
                linkedRank = CareerRankId.LE,
                teamRequirementLabel = "185–750 personas",
                incentiveDescription = "Bono Ejecutivo + Incentivo Internacional",
                calculatedAmount = roundFriendly(countryBaseline.pb10000 * scaleFactor, referencePrice),
                monthlyRankIncome = incomeByRank[CareerRankId.LE] ?: 0.0
            ),
            CareerMilestoneAward(
                id = "pb_40000",
                pbPoints = 40000,
                awardTitle = "Empresario 40.000 PB",
                linkedRank = CareerRankId.EM,
                teamRequirementLabel = "3.000 personas en red",
                incentiveDescription = "Premio Máximo Empresario PSA + Regalías Flex",
                calculatedAmount = roundFriendly(countryBaseline.pb40000 * scaleFactor, referencePrice),
                monthlyRankIncome = incomeByRank[CareerRankId.EM] ?: 0.0
            )
        )

        val targetAward = milestoneAwards.firstOrNull { it.linkedRank == planState.targetRank }
            ?: if (planState.targetRank == CareerRankId.LJ) {
                milestoneAwards[3]
            } else {
                milestoneAwards.first()
            }

        val weeklyIncome = planState.closesPerWeek * unitProfit
        val monthlyIncome = weeklyIncome * 4.0
        val annualIncome = monthlyIncome * 12.0

        val weeklyMomentum = (planState.pnsPerWeek * 1.4) +
            (planState.followUpsPerWeek * 0.7) +
            (planState.closesPerWeek * 0.35) +
            (planState.demosPerWeek * 0.15)
        val effectiveWeeklyScore = max(0.5, weeklyMomentum)

        val targetComplexityWeight = when (planState.targetRank) {
            CareerRankId.DJ -> 1.0
            CareerRankId.D -> 6.0
            CareerRankId.DC -> 24.0
            CareerRankId.CE -> 54.0
            CareerRankId.LE -> 110.0
            CareerRankId.LJ -> 185.0
            CareerRankId.EM -> 300.0
        }
        val estimatedWeeks = ceil(targetComplexityWeight / effectiveWeeklyScore).toInt().coerceIn(1, 260)
        val estimatedMonths = ceil(estimatedWeeks / 4.0).toInt().coerceIn(1, 60)

        val firstRankWeeks = ceil(6.0 / effectiveWeeklyScore).toInt().coerceIn(1, 52)

        return CareerEnterpriseSummary(
            referencePurifierId = referenceId,
            referencePurifierName = referenceName,
            referenceUnitPrice = referencePrice,
            unitDirectProfit = unitProfit,
            catalogScaleFactor = scaleFactor,
            ranks = ranks,
            milestoneAwards = milestoneAwards,
            weeklyTotalActions = planState.totalWeeklyActions,
            monthlyDemosProjected = planState.demosPerWeek * 4,
            monthlyClosesProjected = planState.closesPerWeek * 4,
            monthlyPnsProjected = planState.pnsPerWeek * 4,
            monthlyFollowUpsProjected = planState.followUpsPerWeek * 4,
            projectedWeeklyPersonalIncome = weeklyIncome,
            projectedMonthlyPersonalIncome = monthlyIncome,
            projectedAnnualPersonalIncome = annualIncome,
            targetAward = targetAward,
            estimatedMonthsToTargetRank = estimatedMonths,
            estimatedWeeksToFirstRank = firstRankWeeks
        )
    }

    fun buildHomeProductMap(
        pricingConfig: PsaPricingConfig,
        activeCountry: PsaCountryConfig
    ): List<HomeProductZoneSpot> {
        val defaultSenior = PsaCountryRegistry.defaultPricingForCountry(activeCountry.code).senior4Price.coerceAtLeast(1.0)
        val seniorPrice = pricingConfig.senior4Price.takeIf { it > 0.0 } ?: defaultSenior
        val liveItems = pricingConfig.liveCatalogItems

        fun findLivePrice(vararg keywords: String, fallback: Double): Double {
            val match = liveItems.firstOrNull { item ->
                val lower = item.name.lowercase(Locale.ROOT)
                keywords.all { lower.contains(it) }
            }
            return match?.price?.takeIf { it > 0.0 } ?: fallback
        }

        val scale = seniorPrice / AR_BASELINE_SENIOR4
        val ecoDEstimate = findLivePrice("eco-d", fallback = roundFriendly(1_260_000.0 * scale, seniorPrice))
        val ecoDSmEstimate = findLivePrice("eco-d", "sm", fallback = roundFriendly(995_000.0 * scale, seniorPrice))
        val sodaBurbyEstimate = findLivePrice("soda", fallback = roundFriendly(185_000.0 * scale, seniorPrice))

        return listOf(
            HomeProductZoneSpot("senior4", "Senior 4", HomeZone.COCINA, "Purificador insignia sobre mesada • 36.000 L", seniorPrice),
            HomeProductZoneSpot("s1000", "S-1000 II", HomeZone.COCINA, "Alto caudal continuo para cocina familiar", pricingConfig.s1000Price),
            HomeProductZoneSpot("senik", "Senik", HomeZone.COCINA, "Línea premium bacteriostática sobre mesada", pricingConfig.senikPrice),
            HomeProductZoneSpot("quantum", "Quantum", HomeZone.COCINA, "Reducción avanzada de dureza y sarro en cocina", pricingConfig.quantumPrice),
            HomeProductZoneSpot("vero", "Vero", HomeZone.COCINA, "Diseño moderno adaptable sobre o bajo mesada", pricingConfig.veroPrice),
            HomeProductZoneSpot("mini", "Mini", HomeZone.COCINA, "Formato compacto para cocinas pequeñas", pricingConfig.miniPrice),
            HomeProductZoneSpot("croma", "Senior / Croma", HomeZone.COCINA, "Diseño clásico PSA de probada durabilidad", pricingConfig.cromaPrice),
            HomeProductZoneSpot("eco_d", "Dispenser eco-D", HomeZone.COCINA, "Agua purificada fría y caliente de pie", ecoDEstimate),
            HomeProductZoneSpot("eco_d_sm", "Dispenser eco-D SM", HomeZone.COCINA, "Dispenser sobre mesada frío/caliente", ecoDSmEstimate),
            HomeProductZoneSpot("ropot", "Ropot", HomeZone.COCINA, "Ósmosis inversa para alta salinidad, dureza y arsénico", pricingConfig.ropotPrice),
            HomeProductZoneSpot("sodaburby", "SodaBurby", HomeZone.COCINA, "Gasificador de agua pura en segundos", sodaBurbyEstimate),
            HomeProductZoneSpot("ducha", "PSA Ducha", HomeZone.BANO, "Declorador para cuidado de piel y cabello", pricingConfig.duchaPrice),
            HomeProductZoneSpot("c3", "C3", HomeZone.BANO, "Purificador bacteriostático para lavatorio", pricingConfig.c3Price),
            HomeProductZoneSpot("aire", "PSA Aire", HomeZone.AIRE, "Purificador de aire interior HEPA + ionizador", pricingConfig.airePrice),
            HomeProductZoneSpot("poli2", "Poli 2", HomeZone.LAVADERO, "Protección contra sedimentos y sarro en artefactos", pricingConfig.descalcificadorPrice),
            HomeProductZoneSpot("stopper", "Stopper / Entrada", HomeZone.LAVADERO, "Filtración en línea y entrada general", pricingConfig.maximaPrice),
            HomeProductZoneSpot("iontrix3", "Iontrix 3", HomeZone.EXTERIOR, "Ionizador solar cobre-plata para piletas", pricingConfig.piletaPrice),
            HomeProductZoneSpot("portatil", "Portátil PSA", HomeZone.EXTERIOR, "Botella térmica y purificación portátil", pricingConfig.botellaTermicaPrice)
        )
    }

    fun buildWhatsAppMaterialMessage(
        inviteName: String,
        sellerName: String,
        selectedMaterials: List<ShareableBusinessMaterial>,
        ratingScore: Int,
        followUpDate: String,
        followUpTime: String
    ): String {
        val greetingName = inviteName.trim().ifBlank { "amigo/a" }
        val sellerSig = sellerName.trim().ifBlank { "tu asesor PSA" }
        return buildString {
            append("Hola $greetingName! Gracias por compartir hoy la Presentación del Negocio PSA.\n\n")
            if (selectedMaterials.isNotEmpty()) {
                append("Te envío el material acordado para repasar tranquilo:\n")
                selectedMaterials.forEach { item ->
                    append("• ${item.title}\n")
                }
                append("\n")
            }
            append("Valoración de la charla: $ratingScore/10.\n")
            if (followUpDate.isNotBlank()) {
                val timePart = if (followUpTime.isNotBlank()) " a las $followUpTime" else ""
                append("Nuestro próximo encuentro quedó agendado para el $followUpDate$timePart.\n")
            }
            append("\nQuedo atento a tus dudas. — $sellerSig")
        }.trim()
    }

    fun buildWhatsAppUrl(phone: String, message: String): String {
        val cleanDigits = phone.filter { it.isDigit() }
        val encoded = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
        return if (cleanDigits.length >= 8) {
            "https://wa.me/$cleanDigits?text=$encoded"
        } else {
            "https://wa.me/?text=$encoded"
        }
    }

    fun buildSummaryNotesForInvite(
        state: BusinessPresentationSessionState,
        existingNotes: String = ""
    ): String {
        val parts = mutableListOf<String>()
        state.currentQuadrant?.let { cur ->
            val des = state.desiredQuadrant?.letter ?: "D"
            parts += "Cuadrante: ${cur.letter}->${des}"
        }
        state.selectedPriorityId?.let { priId ->
            val label = flexBenefits.firstOrNull { it.id == priId }?.title
            if (label != null) parts += "Prioridad: $label"
        }
        if (state.selectedProductInterestIds.isNotEmpty()) {
            parts += "Interés productos: ${state.selectedProductInterestIds.joinToString(", ")}"
        }
        parts += "Meta carrera: ${state.selectedCareerRank.title}"
        parts += "Plan semanal: ${state.weeklyPlanMode.label} (${state.weeklyDemosGoal}D/${state.weeklyClosesGoal}C/${state.weeklyPnsGoal}PN/${state.weeklyFollowUpsGoal}S • ${state.selectedScheduleSlot})"
        parts += "Termómetro: ${state.closingScore}/10"
        if (state.closingGapReason.isNotBlank()) {
            parts += "Falta para el 10: ${state.closingGapReason.trim()}"
        }
        val summaryLine = parts.joinToString(" | ")
        return if (existingNotes.isBlank() || existingNotes == "Alta tras Presentación de Negocios") {
            summaryLine
        } else {
            "$summaryLine • $existingNotes"
        }
    }

    private fun roundFriendly(value: Double, referencePrice: Double): Double {
        if (value <= 0.0) return 0.0
        return when {
            referencePrice >= 100_000.0 -> (value / 1_000.0).roundToLong() * 1_000.0
            referencePrice >= 5_000.0 -> (value / 50.0).roundToLong() * 50.0
            else -> (value / 5.0).roundToLong() * 5.0
        }
    }
}
