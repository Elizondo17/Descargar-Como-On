package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PsaSlotStatus {
    FREE,       // Disponible para visitas
    BOOKED,     // Agendado con un cliente
    BLOCKED,    // Bloqueado por el distribuidor (descanso / trámite)
    BUFFER      // Margen de seguridad automático (traslado/demora de visita previa)
}

enum class PsaVisitStatus {
    AGENDADA,           // Cita fijada
    CONFIRMADA,          // Cliente confirmó asistencia familiar
    EN_DEMOSTRACION,     // Distribuidor en la casa realizando la demo
    CERRADA_EXITO,       // Venta concretada
    REPROGRAMAR,        // Cliente pidió cambio de fecha
    CANCELADA           // Cancelada
}

data class PsaTimeSlot(
    val id: String,
    val dateKey: String,          // e.g. "2026-11-06"
    val displayDate: String,      // e.g. "Viernes 6 Nov"
    val startTime: String,        // e.g. "16:30"
    val endTime: String,          // e.g. "17:30"
    val status: PsaSlotStatus = PsaSlotStatus.FREE,
    val assignedCustomerName: String = "",
    val assignedCustomerPhone: String = "",
    val assignedAddress: String = "",
    val visitId: String? = null
)

@Entity(tableName = "psa_home_visits")
data class PsaHomeVisit(
    @PrimaryKey
    val id: String,
    val customerName: String,
    val phone: String,
    val address: String,
    val neighborhood: String = "",
    val scheduledDate: String,    // "2026-11-06"
    val scheduledTime: String,    // "16:30"
    val modelInterest: String = "PSA Senior 4", // Senior 4, Quantum, Vero, Ducha
    val status: PsaVisitStatus = PsaVisitStatus.AGENDADA,
    val chlorinePpm: Float = 0.0f, // Resultado test ortotoluidina
    val bottlesPerWeek: Int = 3,
    val quotedPlan: String = "",
    val notes: String = "",
    val createdAtMs: Long = System.currentTimeMillis(),
    val ownerUid: String = "",
    val waterSource: String = "AGUA_CORRIENTE",
    val hardnessPpm: Int = 0,
    val realLitersPerDay: Double = 0.0,
    val cartSummary: String = "",
    val soldCartSummary: String = "",
    val testPhotoPath: String = "",
    val diagnosticTimestampMs: Long = 0L,
    val updatedAtMs: Long = System.currentTimeMillis()
)

enum class PsaComparisonType(val label: String, val icon: String, val unitVolume: String) {
    BIDON_20L("Bidón", "🛢️", "bidones"),
    BOTELLA_2L("Botella", "🍾", "botellas"),
    PERSONALIZADO("Envase a Medida", "💧", "envases")
}

data class PsaContainerConfig(
    val type: PsaComparisonType = PsaComparisonType.BIDON_20L,
    val capacityLiters: Double = 20.0,
    val unitPrice: Double = 4000.0,
    val unitsPerWeek: Int = 3
) {
    val totalLitersPerWeek: Double get() = unitsPerWeek * capacityLiters
    val totalLitersPerMonth: Double get() = totalLitersPerWeek * 4.33
    val monthlyCost: Double get() = unitsPerWeek * 4.33 * unitPrice
    val yearlyCost: Double get() = monthlyCost * 12.0
    val costPerLiter: Double get() = if (capacityLiters > 0) unitPrice / capacityLiters else 0.0
}

data class PsaPurifierModel(
    val id: String,
    val name: String,
    val price: Double,
    val installmentsCount: Int = 12,
    val lifespanMonths: Int = 36, // ej. 36 meses = 3 años
    val description: String = "",
    val category: String = "Cocina", // Cocina, Baño, Entrada, Portátil, Repuestos
    val capacityLiters: Int = 36000,
    val officialUrl: String = "https://www.psa.com.ar",
    val anmatRegistration: String = "R.N.P.U.D. N° 0250001",
    val highlightFeature: String = "Retención >99.5% de Cloro"
) {
    fun getMonthlyInstallment(): Double = if (installmentsCount > 0) price / installmentsCount else price
    fun getLifespanYears(): Double = lifespanMonths / 12.0
    fun getCostPerLiter(): Double = if (capacityLiters > 0) price / capacityLiters.toDouble() else 0.0
}

data class PsaSavingsResult(
    val consumptionPerWeek: Int,
    val unitPrice: Double,
    val comparisonType: PsaComparisonType = PsaComparisonType.BIDON_20L,
    val monthlyCost: Double,
    val yearlyCost: Double,
    val fiveYearCost: Double,
    val lifespanMonths: Int = 36,
    val lifespanTotalBottleCost: Double = 0.0,
    val purifierTotalPrice: Double = 0.0,
    val suggestedPsaMonthlyPayment: Double,
    val monthlySavings: Double,
    val totalLifespanSavings: Double = 0.0,
    val impactMessage: String,
    val totalLitersPerMonth: Double = 0.0,
    val costPerLiterBottle: Double = 0.0,
    val costPerLiterPsa: Double = 0.0,
    val plasticBottlesAvoidedYear: Int = 0
)

enum class PsaScriptCategory {
    GANCHO_TEST_CLORO,
    DOLOR_BIDON,
    COCINA_SALUD,
    CIERRE_DOBLE_OPCION,
    RECORDATORIO_ANTI_PLANTON,
    OBJECION_PAREJA,
    OBJECION_PRECIO,
    OBJECION_AGUA_HERVIDA
}

data class PsaScriptTemplate(
    val id: String,
    val title: String,
    val category: PsaScriptCategory,
    val description: String,
    val templateBody: String,
    val successRate: Int = 92
)

enum class PsaMessageTone(val label: String, val icon: String) {
    CALIDO_HUMANO("🤗 Cálido y Humano", "🤗"),
    CORTO_DIRECTO("⚡ Corto y Directo", "⚡"),
    VECINO_AMIGABLE("🤝 Vecino Amigable", "🤝")
}

data class PsaHardnessStage(
    val ppmRange: String,
    val title: String,
    val hardnessCategory: String,
    val icon: String,
    val energyOverconsumptionPct: Int,
    val pipeObstructionPct: Int,
    val description: String,
    val healthEffect: String,
    val solutionText: String
)

data class PsaPostSaleCustomer(
    val id: String,
    val name: String,
    val phone: String,
    val address: String = "",
    val installedModelName: String,
    val installDate: String, // "YYYY-MM-DD"
    val monthsElapsed: Int = 6,
    val targetRecambioMonths: Int = 12, // 12 meses prefiltro / 36 meses núcleo
    val notes: String = ""
)

data class PsaPricingConfig(
    val bidonPrice: Double = 4800.0,            // Bidón 20L
    val bottlePrice: Double = 1800.0,           // Botella 2L
    val senior4Price: Double = 950000.0,        // PSA Senior 4 sobre mesada (36.000 L)
    val quantumPrice: Double = 1298000.0,       // PSA Quantum sobre/bajo mesada
    val senikPrice: Double = 1298000.0,         // PSA Senik sobre mesada
    val cromaPrice: Double = 781000.0,          // PSA Senior Clásico / Croma
    val veroPrice: Double = 549000.0,           // PSA Vero bajo / sobre mesada
    val s1000Price: Double = 1100000.0,         // PSA S-1000 II purificador clásico
    val miniPrice: Double = 427000.0,           // PSA Mini compacto
    val c3Price: Double = 248000.0,             // PSA C3 lavatorio / baño
    val duchaPrice: Double = 216000.0,          // PSA Ducha declorador
    val descalcificadorPrice: Double = 240000.0,// PSA Descalcificador R-200 antisarro
    val domusPrice: Double = 1650000.0,         // PSA Domus ablandador automático integral
    val maximaPrice: Double = 1150000.0,        // PSA Máxima entrada de tanque / integral
    val piletaPrice: Double = 890000.0,         // PSA Iontrix3 purificador de pileta
    val airePrice: Double = 690000.0,           // PSA AirPurifier purificador de aire
    val ropotPrice: Double = 1490000.0,         // PSA Ropot ósmosis inversa
    val rotechPrice: Double = 2150000.0,        // PSA ROtech·UV ósmosis inversa + luz UV
    val botellaTermicaPrice: Double = 92000.0,  // PSA Portátil / Botella PSA
    val repuestoSeniorPrice: Double = 103000.0, // Repuesto FIPOR / Núcleo oficial
    val installmentsCount: Int = 12,            // Cantidad de cuotas fijas por defecto
    val defaultLifespanMonths: Int = 36,        // Duración estándar (36 meses = 3 años)
    val defaultComparisonType: PsaComparisonType = PsaComparisonType.BIDON_20L,
    val customPurifiers: List<PsaPurifierModel> = emptyList(),
    val liveCatalogItems: List<PsaPurifierModel> = emptyList(),
    val weeklyVisitsGoal: Int = 10              // Meta semanal de citas de demostración
) {
    fun getInstallmentFor(price: Double, cuotas: Int = installmentsCount): Double =
        if (cuotas > 0) price / cuotas else price

    fun getAllPurifiers(): List<PsaPurifierModel> {
        val defaults = listOf(
            PsaPurifierModel(
                id = "senior4",
                name = "PSA Senior 4 (Sobre Mesada)",
                price = senior4Price,
                installmentsCount = installmentsCount,
                lifespanMonths = 36,
                description = "Carbón activado en bloque, zeolitas y prefiltro Fipor.",
                category = "Cocina",
                capacityLiters = 36000,
                officialUrl = "https://tienda.psa.com.ar/purificadores-de-agua.html",
                anmatRegistration = "RNPUD 0250001",
                highlightFeature = "36.000 Litros (3 años)"
            ),
            PsaPurifierModel(
                id = "senik",
                name = "PSA Senik",
                price = if (senikPrice > 0.0) senikPrice else quantumPrice,
                installmentsCount = installmentsCount,
                lifespanMonths = 36,
                description = "Purificador de alto rendimiento con cartucho bacteriostático.",
                category = "Cocina",
                capacityLiters = 36000,
                officialUrl = "https://tienda.psa.com.ar/purificadores-de-agua.html",
                anmatRegistration = "RNPUD 0250042",
                highlightFeature = "Línea Premium PSA"
            ),
            PsaPurifierModel(
                id = "quantum",
                name = "PSA Quantum",
                price = if (quantumPrice > 0.0) quantumPrice else senikPrice,
                installmentsCount = installmentsCount,
                lifespanMonths = 36,
                description = "Reducción avanzada de cloro, metales y dureza en cocina.",
                category = "Cocina",
                capacityLiters = 36000,
                officialUrl = "https://tienda.psa.com.ar/purificadores-de-agua.html",
                anmatRegistration = "RNPUD 0250045",
                highlightFeature = "Protección antisarro cocina"
            ),
            PsaPurifierModel(
                id = "croma",
                name = "PSA Senior Clásico / Croma",
                price = cromaPrice,
                installmentsCount = installmentsCount,
                lifespanMonths = 36,
                description = "Modelo clásico confiable sobre mesada.",
                category = "Cocina",
                capacityLiters = 30000,
                officialUrl = "https://tienda.psa.com.ar/purificadores-de-agua.html",
                anmatRegistration = "RNPUD 0250035",
                highlightFeature = "Diseño clásico PSA"
            ),
            PsaPurifierModel(
                id = "vero",
                name = "PSA Vero",
                price = veroPrice,
                installmentsCount = installmentsCount,
                lifespanMonths = 36,
                description = "Diseño moderno compacto de alta eficiencia.",
                category = "Cocina",
                capacityLiters = 36000,
                officialUrl = "https://tienda.psa.com.ar/purificadores-de-agua.html",
                anmatRegistration = "RNPUD 0250029",
                highlightFeature = "Diseño elegante para cocina"
            ),
            PsaPurifierModel(
                id = "s1000",
                name = "PSA S-1000 II",
                price = s1000Price,
                installmentsCount = installmentsCount,
                lifespanMonths = 36,
                description = "Purificador de alto rendimiento y larga vida útil.",
                category = "Cocina",
                capacityLiters = 36000,
                officialUrl = "https://tienda.psa.com.ar/purificadores-de-agua.html",
                anmatRegistration = "RNPUD 0250012",
                highlightFeature = "Alto caudal continuo"
            ),
            PsaPurifierModel(
                id = "mini",
                name = "PSA Mini (Compacto)",
                price = miniPrice,
                installmentsCount = installmentsCount,
                lifespanMonths = 24,
                description = "Formato compacto para cocinas con espacio reducido.",
                category = "Cocina",
                capacityLiters = 18000,
                officialUrl = "https://tienda.psa.com.ar/purificadores-de-agua.html",
                anmatRegistration = "RNPUD 0250022",
                highlightFeature = "Ultracompacto de cocina"
            ),
            PsaPurifierModel(
                id = "c3",
                name = "PSA C3 (Baño / Lavatorio)",
                price = c3Price,
                installmentsCount = installmentsCount,
                lifespanMonths = 24,
                description = "Purificador bacteriostático para canilla de baño y lavatorio.",
                category = "Baño",
                capacityLiters = 20000,
                officialUrl = "https://www.psa.com.ar",
                anmatRegistration = "RNPUD 0250016",
                highlightFeature = "Cuidado facial e higiene diaria"
            ),
            PsaPurifierModel(
                id = "ducha",
                name = "PSA Ducha (Declorador)",
                price = duchaPrice,
                installmentsCount = installmentsCount,
                lifespanMonths = 18,
                description = "Filtro declorador para ducha y cuidado de piel y cabello.",
                category = "Baño",
                capacityLiters = 20000,
                officialUrl = "https://tienda.psa.com.ar/purificadores-de-agua.html",
                anmatRegistration = "RNPUD 0250018",
                highlightFeature = "Protección de piel y cabello"
            ),
            PsaPurifierModel(
                id = "pileta",
                name = "PSA Iontrix3 (Pileta)",
                price = piletaPrice,
                installmentsCount = installmentsCount,
                lifespanMonths = 36,
                description = "Purificador solar por ionización cobre-plata para piscinas.",
                category = "Pileta",
                capacityLiters = 100000,
                officialUrl = "https://www.psa.com.ar",
                anmatRegistration = "Certificación PSA",
                highlightFeature = "Hasta 90% menos cloro en pileta"
            ),
            PsaPurifierModel(
                id = "aire",
                name = "PSA AirPurifier (Aire)",
                price = airePrice,
                installmentsCount = installmentsCount,
                lifespanMonths = 36,
                description = "Purificador de aire interior con filtro HEPA e ionizador.",
                category = "Aire",
                capacityLiters = 0,
                officialUrl = "https://www.psa.com.ar",
                anmatRegistration = "Certificación PSA",
                highlightFeature = "Aire puro libre de alérgenos y humo"
            ),
            PsaPurifierModel(
                id = "descalcificador",
                name = "PSA Descalcificador R-200",
                price = descalcificadorPrice,
                installmentsCount = installmentsCount,
                lifespanMonths = 60,
                description = "Tratamiento antisarro para electrodomésticos y cañerías.",
                category = "Hogar",
                capacityLiters = 100000,
                officialUrl = "https://www.psa.com.ar",
                anmatRegistration = "Dispositivo Físico Certificado",
                highlightFeature = "Protección antisarro"
            ),
            PsaPurifierModel(
                id = "domus",
                name = "PSA Domus (Ablandador Automático)",
                price = domusPrice,
                installmentsCount = installmentsCount,
                lifespanMonths = 60,
                description = "Ablandador automático integral que reduce dureza, cal y sarro en baños, cañerías y electrodomésticos.",
                category = "Hogar",
                capacityLiters = 150000,
                officialUrl = "https://www.psa.com.ar",
                anmatRegistration = "Oficial PSA Hogar",
                highlightFeature = "Cero sarro en toda la casa"
            ),
            PsaPurifierModel(
                id = "maxima",
                name = "PSA Máxima (Entrada General)",
                price = maximaPrice,
                installmentsCount = installmentsCount,
                lifespanMonths = 36,
                description = "Filtración integral en la entrada general del hogar.",
                category = "Entrada",
                capacityLiters = 150000,
                officialUrl = "https://www.psa.com.ar",
                anmatRegistration = "RNPUD 0250055",
                highlightFeature = "Toda la casa protegida"
            ),
            PsaPurifierModel(
                id = "ropot",
                name = "PSA Ropot (Ósmosis Inversa)",
                price = ropotPrice,
                installmentsCount = installmentsCount,
                lifespanMonths = 36,
                description = "Sistema de ósmosis inversa para aguas con alta salinidad, dureza, arsénico y nitratos.",
                category = "Cocina",
                capacityLiters = 36000,
                officialUrl = "https://www.psa.com.ar",
                anmatRegistration = "RNPUD 0250060",
                highlightFeature = "Máxima pureza ante alta dureza"
            ),
            PsaPurifierModel(
                id = "rotech",
                name = "PSA ROtech·UV (Ósmosis + UV)",
                price = rotechPrice,
                installmentsCount = installmentsCount,
                lifespanMonths = 36,
                description = "Ósmosis inversa de última generación con lámpara UV bactericida: elimina arsénico, nitratos, exceso de sales, virus y bacterias en agua de pozo o napa.",
                category = "Cocina",
                capacityLiters = 36000,
                officialUrl = "https://www.psa.com.ar",
                anmatRegistration = "Oficial PSA Ósmosis + UV",
                highlightFeature = "Ósmosis Inversa + UV Bactericida"
            )
        )
        // Include extra main units imported from official list (e.g. Senior 4 BM) without mixing cheap parts
        val existingIds = defaults.map { it.id }.toSet()
        val extraLive = liveCatalogItems.filter { it.id !in existingIds && !it.category.equals("Repuestos", ignoreCase = true) }
        return defaults + extraLive + customPurifiers
    }
}

data class PsaWeeklyGoalStats(
    val bookedVisitsCount: Int,
    val goal: Int,
    val progressPercent: Float,
    val badgeTitle: String,
    val badgeIcon: String,
    val motivationalMessage: String
)

enum class PsaLeadStatus(val label: String, val icon: String) {
    PENDING("Pendiente", "⏳"),
    INTERESTED("Interesado", "🌟"),
    NO_ANSWER("No respondió", "📵"),
    RESCHEDULED("Reprogramado", "📅"),
    APPOINTMENT_SET("Cita Coordinada", "🎯"),
    NOT_INTERESTED("No interesado", "🚫")
}

enum class PsaContactChannel(val label: String, val icon: String) {
    CALL("Llamada", "📞"),
    MESSAGE("WhatsApp / Mensaje", "💬")
}

@Entity(tableName = "psa_contact_leads")
data class PsaContactLead(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val address: String = "",
    val neighborhood: String = "",
    val notes: String = "",
    val source: String = "Referido", // Referido, Familiar, Vecino, Redes, Puerta a Puerta
    val callAttemptsCount: Int = 0,
    val messageAttemptsCount: Int = 0,
    val lastContactDateMs: Long = 0L,
    val lastContactChannel: String = "", // "CALL", "MESSAGE"
    val status: PsaLeadStatus = PsaLeadStatus.PENDING,
    val rescheduleDate: String = "", // "YYYY-MM-DD"
    val rescheduleNote: String = "",
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "psa_contact_interactions")
data class PsaContactInteraction(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val leadId: String,
    val leadName: String,
    val channel: String, // "CALL", "MESSAGE"
    val result: String,  // "SUCCESS", "NO_ANSWER", "RESCHEDULED", "REJECTED"
    val dateKey: String, // "YYYY-MM-DD"
    val timestampMs: Long = System.currentTimeMillis(),
    val note: String = ""
)

data class PsaAchievementBadge(
    val id: String,
    val title: String,
    val icon: String,
    val description: String,
    val isUnlocked: Boolean = false
)

data class PsaDailyGoalStats(
    val dateKey: String,
    val totalContactsToday: Int,
    val callsCount: Int,
    val messagesCount: Int,
    val successCount: Int,
    val noAnswerCount: Int,
    val rescheduledCount: Int,
    val dailyGoal: Int = 20,
    val progressPercent: Float = if (dailyGoal > 0) (totalContactsToday.toFloat() / dailyGoal).coerceIn(0f, 1f) else 0f,
    val currentStreakDays: Int = 1,
    val unlockedBadges: List<PsaAchievementBadge> = emptyList()
)

enum class PsaPostSaleStatus {
    ACTIVO,
    ATENDIDO
}

enum class ReplacementUrgency(val label: String) {
    VENCIDO("Recambio Vencido"),
    PROXIMO("Próximo a Vencer"),
    AL_DIA("Al Día")
}

@Entity(tableName = "psa_post_sale_installations")
data class PsaPostSaleInstallation(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val ownerUid: String = "",
    val contactId: String = "",
    val visitId: String = "",
    val customerName: String,
    val customerPhone: String = "",
    val purifierId: String = "senior4",
    val purifierName: String = "PSA Senior 4",
    val installedAtMs: Long = System.currentTimeMillis(),
    val replacementCycleMonths: Int = 12,
    val nextReplacementAtMs: Long = calculateNextReplacementTimestamp(installedAtMs, replacementCycleMonths),
    val lastNotifiedDayKey: String = "",
    val status: PsaPostSaleStatus = PsaPostSaleStatus.ACTIVO,
    val notes: String = "",
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    fun daysUntilReplacement(nowMs: Long = System.currentTimeMillis()): Int {
        val diff = nextReplacementAtMs - nowMs
        return kotlin.math.floor(diff.toDouble() / 86_400_000.0).toInt()
    }

    fun getUrgency(nowMs: Long = System.currentTimeMillis()): ReplacementUrgency {
        val days = daysUntilReplacement(nowMs)
        return when {
            days <= 0 -> ReplacementUrgency.VENCIDO
            days <= 15 -> ReplacementUrgency.PROXIMO
            else -> ReplacementUrgency.AL_DIA
        }
    }
}

data class SellerPerformanceStats(
    val ownerUid: String = "",
    val sellerName: String = "",
    val sellerEmail: String = "",
    val totalContacts: Int = 0,
    val totalVisits: Int = 0,
    val completedDemos: Int = 0,
    val closedSales: Int = 0,
    val pendingReplacements: Int = 0,
    val messagesSuccess: Int = 0,
    val messagesFailed: Int = 0,
    val messagesTotal: Int = 0,
    val callsAttempted: Int = 0,
    val callsConnected: Int = 0,
    val lastMessageAtMs: Long = 0L,
    val lastActivityAtMs: Long = 0L,
    val lastActivityLabel: String = "",
    val recentActivityJson: String = "[]",
    val parentEmail: String = "",
    val supervisorChainEmails: List<String> = emptyList(),
    val hierarchyDepth: Int = 0,
    val updatedAtMs: Long = 0L
) {
    val messagingSuccessRate: Float
        get() = if (messagesTotal > 0) (messagesSuccess * 100f / messagesTotal) else 0f

    val messagingFailureRate: Float
        get() = if (messagesTotal > 0) (messagesFailed * 100f / messagesTotal) else 0f

    val callConnectionRate: Float
        get() = if (callsAttempted > 0) (callsConnected * 100f / callsAttempted) else 0f

    val parsedRecentBuckets: List<SellerActivityBucket>
        get() = HierarchyTreeBuilder.parseActivityBuckets(recentActivityJson)
}

fun calculateNextReplacementTimestamp(installedAtMs: Long, months: Int): Long {
    val safeMonths = when (months) {
        6, 12, 24, 36 -> months
        else -> 12
    }
    val cal = java.util.Calendar.getInstance().apply {
        timeInMillis = installedAtMs
        add(java.util.Calendar.MONTH, safeMonths)
    }
    return cal.timeInMillis
}

enum class AgendaTimeWindowFilter(val label: String) {
    ALL("Todo"),
    MORNING("Mañana"),
    AFTERNOON("Tarde"),
    EVENING("Noche")
}

enum class AgendaVisitStatusFilter(val label: String) {
    ALL("Todas"),
    PENDING("Pendientes"),
    COMPLETED("Realizadas"),
    CANCELLED("Canceladas");

    fun matchesVisitStatus(status: PsaVisitStatus): Boolean = when (this) {
        ALL -> true
        PENDING -> status == PsaVisitStatus.AGENDADA ||
            status == PsaVisitStatus.CONFIRMADA ||
            status == PsaVisitStatus.EN_DEMOSTRACION ||
            status == PsaVisitStatus.REPROGRAMAR
        COMPLETED -> status == PsaVisitStatus.CERRADA_EXITO
        CANCELLED -> status == PsaVisitStatus.CANCELADA
    }
}

data class ScheduleIntervalConfig(
    val workStartTime: String = "09:00",
    val workEndTime: String = "20:30",
    val demoDurationMinutes: Int = 60,
    val travelBufferMinutes: Int = 60
) {
    companion object {
        val DURATION_OPTIONS = listOf(30, 45, 60, 90, 120)
        val BUFFER_OPTIONS = listOf(0, 30, 45, 60, 90)
    }
}

enum class AgendaSyncEntityType {
    VISIT,
    POST_SALE,
    CONTACT
}

enum class AgendaSyncOperation {
    UPSERT,
    DELETE
}

@Entity(tableName = "pending_agenda_sync")
data class PendingAgendaSyncEntity(
    @PrimaryKey
    val queueKey: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val ownerUid: String = "",
    val queuedAtMs: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastError: String = ""
) {
    companion object {
        fun buildKey(type: AgendaSyncEntityType, entityId: String): String =
            "${type.name}:$entityId"
    }
}

data class AgendaSyncStatus(
    val isSyncing: Boolean = false,
    val pendingCount: Int = 0,
    val lastSyncedAtMs: Long = 0L,
    val lastErrorMessage: String? = null
)
