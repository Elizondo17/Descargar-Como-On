package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PsaHomeVisit
import com.example.data.model.PsaSlotStatus
import com.example.data.model.PsaVisitStatus
import com.example.util.PsaScheduleManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PsaScheduleTest {

    @Test
    fun `generate schedule creates slots and merges booked visits`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val sampleVisit = PsaHomeVisit(
            id = "visit-test-1",
            customerName = "Laura Gomez",
            phone = "+54911223344",
            address = "Av. Santa Fe 1234",
            neighborhood = "Palermo",
            scheduledDate = "2026-11-06",
            scheduledTime = "16:30",
            modelInterest = "PSA Senior 4",
            status = PsaVisitStatus.CONFIRMADA
        )

        val slots = PsaScheduleManager.generateScheduleForDays(
            context = context,
            daysAhead = 7,
            bookedVisits = listOf(sampleVisit)
        )

        // 7 days * 6 slots = 42 slots
        assertEquals(42, slots.size)

        // Check booked visit slot
        val bookedSlot = slots.find { it.dateKey == "2026-11-06" && it.startTime == "16:30" }
        if (bookedSlot != null) {
            assertEquals(PsaSlotStatus.BOOKED, bookedSlot.status)
            assertEquals("Laura Gomez", bookedSlot.assignedCustomerName)
            assertEquals("+54911223344", bookedSlot.assignedCustomerPhone)
        }
    }

    @Test
    fun `get next available slots filters correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val slots = PsaScheduleManager.generateScheduleForDays(context, 3, emptyList())

        val nextTwo = PsaScheduleManager.getNextAvailableSlots(slots, 2)
        assertEquals(2, nextTwo.size)
        assertTrue(nextTwo.all { it.status == PsaSlotStatus.FREE })
    }

    @Test
    fun `generate appointment proposal message injects real available slots`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val slots = PsaScheduleManager.generateScheduleForDays(context, 3, emptyList())
        val nextTwo = PsaScheduleManager.getNextAvailableSlots(slots, 2)

        val message = PsaScheduleManager.generateAppointmentProposalMessage(
            clientName = "Martin",
            neighborhood = "Belgrano",
            availableSlots = nextTwo
        )

        assertTrue(message.contains("Martin"))
        assertTrue(message.contains("Belgrano"))
        assertTrue(message.contains("Opción 1:"))
        assertTrue(message.contains("Opción 2:"))
    }

    @Test
    fun `savings calculator computes monthly and yearly bottle costs`() {
        val result = PsaScheduleManager.calculateSavings(bottlesPerWeek = 4, bottlePrice = 3000.0)

        // 4 * 4.33 = ~17.32 bottles/month * 3000 = ~$51,960/month
        assertTrue(result.monthlyCost > 50000.0)
        assertTrue(result.yearlyCost > 600000.0)
        assertTrue(result.monthlySavings > 0.0)
        assertTrue(result.impactMessage.contains("4 bidones de 20L por semana"))
    }

    @Test
    fun `toggle slot blocked state updates preferences`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val slotId = "slot_test_blocking_123"

        val blockedFirst = PsaScheduleManager.toggleSlotBlocked(context, slotId)
        assertTrue(blockedFirst)

        val unblocked = PsaScheduleManager.toggleSlotBlocked(context, slotId)
        assertFalse(unblocked)
    }

    @Test
    fun `psa initial knowledge contains winning objection handlers`() {
        val knowledge = PsaScheduleManager.getInitialPsaKnowledge()
        assertTrue(knowledge.size >= 4)
        assertTrue(knowledge.any { it.category == "PSA_DEMO_CLORO" })
        assertTrue(knowledge.any { it.category == "PSA_AHORRO_BIDONES" })
        assertTrue(knowledge.any { it.category == "PSA_OBJECION_PAREJA" })
    }

    @Test
    fun `custom script and proposal can be modified and saved in-app`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val customText = "Hola vecina! ¿Querés que pasemos a revisar el agua?"
        PsaScheduleManager.saveCustomProposal(context, customText)
        assertEquals(customText, PsaScheduleManager.getCustomProposal(context))

        PsaScheduleManager.clearCustomProposal(context)
        assertEquals(null, PsaScheduleManager.getCustomProposal(context))

        val customScript = "Mi guión personalizado con amor"
        PsaScheduleManager.saveCustomScript(context, "script_1", customScript)
        assertEquals(customScript, PsaScheduleManager.getCustomScript(context, "script_1", "default"))
    }

    @Test
    fun `booked visit automatically blocks succeeding slot as BUFFER margin`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Generate today's dateKey
        val todayKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

        // Book slot at 16:30 for today
        val sampleVisit = PsaHomeVisit(
            id = "visit-buffer-1",
            customerName = "Carlos Perez",
            phone = "+54911998877",
            address = "Cabildo 2000",
            neighborhood = "Belgrano",
            scheduledDate = todayKey,
            scheduledTime = "16:30",
            modelInterest = "PSA Senior 4",
            status = PsaVisitStatus.AGENDADA
        )

        val slots = PsaScheduleManager.generateScheduleForDays(
            context = context,
            daysAhead = 7,
            bookedVisits = listOf(sampleVisit)
        )

        val daySlots = slots.filter { it.dateKey == todayKey }
        val bookedIndex = daySlots.indexOfFirst { it.startTime == "16:30" }
        assertTrue("Booked slot at 16:30 should be found in daySlots", bookedIndex >= 0)
        assertEquals(PsaSlotStatus.BOOKED, daySlots[bookedIndex].status)

        // The succeeding slot (18:00) MUST be automatically marked as BUFFER
        val nextSlot = daySlots[bookedIndex + 1]
        assertEquals("18:00", nextSlot.startTime)
        assertEquals(PsaSlotStatus.BUFFER, nextSlot.status)
        assertTrue(nextSlot.assignedCustomerName.contains("Margen de seguridad"))

        // getNextAvailableSlots MUST NOT propose BUFFER slots
        val nextAvailable = PsaScheduleManager.getNextAvailableSlots(daySlots, 10)
        assertFalse(nextAvailable.any { it.status == PsaSlotStatus.BUFFER })
        assertFalse(nextAvailable.any { it.status == PsaSlotStatus.BOOKED })
    }

    @Test
    fun `psa pricing configuration persists and updates calculations`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val customConfig = com.example.data.model.PsaPricingConfig(
            bidonPrice = 4500.0,
            bottlePrice = 1500.0,
            senior4Price = 300000.0,
            quantumPrice = 360000.0,
            veroPrice = 110000.0,
            duchaPrice = 140000.0,
            installmentsCount = 12
        )

        PsaScheduleManager.savePsaPricingConfig(context, customConfig)
        val loaded = PsaScheduleManager.getPsaPricingConfig(context)

        assertEquals(4500.0, loaded.bidonPrice, 0.01)
        assertEquals(300000.0, loaded.senior4Price, 0.01)
        assertEquals(25000.0, loaded.getInstallmentFor(loaded.senior4Price), 0.01)

        val savings = PsaScheduleManager.calculateSavings(
            bottlesPerWeek = 3,
            bottlePrice = loaded.bidonPrice,
            purifierPrice = loaded.senior4Price,
            installmentsCount = loaded.installmentsCount
        )

        assertEquals(25000.0, savings.suggestedPsaMonthlyPayment, 0.01)
        assertTrue(savings.monthlyCost > 50000.0)
    }

    @Test
    fun `calculateSavings supports both bidon and bottle comparison with purifier lifespan`() {
        // Test comparison with Botellas 2L ($1,200) for 36 months lifespan
        val savingsBottle = PsaScheduleManager.calculateSavings(
            bottlesPerWeek = 10,
            bottlePrice = 1200.0,
            purifierPrice = 280000.0,
            installmentsCount = 12,
            comparisonType = com.example.data.model.PsaComparisonType.BOTELLA_2L,
            lifespanMonths = 36
        )

        // 10 * 4.33 = 43.3 bottles/mo * 1200 = $51,960/mo
        // Over 36 months = 51,960 * 36 = $1,870,560
        assertTrue(savingsBottle.lifespanTotalBottleCost > 1800000.0)
        assertEquals(36, savingsBottle.lifespanMonths)
        assertEquals(com.example.data.model.PsaComparisonType.BOTELLA_2L, savingsBottle.comparisonType)
        // Total savings = Lifespan cost - Purifier price ($280,000)
        assertTrue(savingsBottle.totalLifespanSavings > 1500000.0)
        assertTrue(savingsBottle.impactMessage.contains("botellas de 2L"))
    }

    @Test
    fun `custom purifier models can be added persisted and listed`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val customModel = com.example.data.model.PsaPurifierModel(
            id = "custom_purifier_special",
            name = "PSA AquaPlus Pro 5000",
            price = 420000.0,
            installmentsCount = 18,
            lifespanMonths = 48,
            description = "Filtro de ultrafiltración 4 años"
        )

        val config = com.example.data.model.PsaPricingConfig(
            bidonPrice = 3500.0,
            bottlePrice = 1200.0,
            defaultLifespanMonths = 48,
            defaultComparisonType = com.example.data.model.PsaComparisonType.BIDON_20L,
            customPurifiers = listOf(customModel)
        )

        PsaScheduleManager.savePsaPricingConfig(context, config)
        val loaded = PsaScheduleManager.getPsaPricingConfig(context)

        assertEquals(48, loaded.defaultLifespanMonths)
        assertEquals(com.example.data.model.PsaComparisonType.BIDON_20L, loaded.defaultComparisonType)
        assertEquals(1, loaded.customPurifiers.size)
        assertEquals("PSA AquaPlus Pro 5000", loaded.customPurifiers[0].name)
        assertEquals(420000.0, loaded.customPurifiers[0].price, 0.01)

        val allModels = loaded.getAllPurifiers()
        val expectedBaseCount = com.example.data.model.PsaPricingConfig().getAllPurifiers().size
        assertEquals(expectedBaseCount + 1, allModels.size)
        assertTrue(allModels.any { it.name == "PSA AquaPlus Pro 5000" })
        assertTrue(allModels.any { it.id == "c3" })
        assertTrue(allModels.any { it.id == "ducha" })
        assertTrue(allModels.any { it.id == "pileta" })
        assertTrue(allModels.any { it.id == "aire" })
        assertTrue(allModels.any { it.id == "senik" })
        assertTrue(allModels.any { it.id == "quantum" })
    }

    @Test
    fun `base purifier prices and weekly goal persist and update properly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = com.example.data.model.PsaPricingConfig(
            senior4Price = 960000.0,
            quantumPrice = 1610000.0,
            veroPrice = 560000.0,
            duchaPrice = 330000.0,
            bidonPrice = 4000.0,
            bottlePrice = 1500.0,
            weeklyVisitsGoal = 15
        )

        PsaScheduleManager.savePsaPricingConfig(context, config)
        val loaded = PsaScheduleManager.getPsaPricingConfig(context)

        assertEquals(960000.0, loaded.senior4Price, 0.01)
        assertEquals(1610000.0, loaded.quantumPrice, 0.01)
        assertEquals(560000.0, loaded.veroPrice, 0.01)
        assertEquals(330000.0, loaded.duchaPrice, 0.01)
        assertEquals(15, loaded.weeklyVisitsGoal)

        val models = loaded.getAllPurifiers()
        val senior = models.find { it.id == "senior4" }
        assertNotNull(senior)
        assertEquals(960000.0, senior!!.price, 0.01)
    }

    @Test
    fun `daily slots can be customized and custom time visits generate slots with buffer`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val customSlots = listOf("10:00" to "11:00", "15:00" to "16:00", "19:00" to "20:00")
        PsaScheduleManager.saveDailySlots(context, customSlots)

        val loadedSlots = PsaScheduleManager.getDailySlots(context)
        assertEquals(3, loadedSlots.size)
        assertEquals("10:00", loadedSlots[0].first)

        val todayKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val customVisit = PsaHomeVisit(
            id = "visit-free-time",
            customerName = "Mario Rossi",
            phone = "+54911009988",
            address = "Rivadavia 4500",
            neighborhood = "Caballito",
            scheduledDate = todayKey,
            scheduledTime = "12:30", // Custom free-time outside standard slots
            modelInterest = "PSA Quantum",
            status = com.example.data.model.PsaVisitStatus.AGENDADA
        )

        val schedule = PsaScheduleManager.generateScheduleForDays(context, 1, listOf(customVisit))
        val bookedSlot = schedule.find { it.dateKey == todayKey && it.startTime == "12:30" }
        assertNotNull("Free time slot at 12:30 should be created", bookedSlot)
        assertEquals(com.example.data.model.PsaSlotStatus.BOOKED, bookedSlot!!.status)

        // Succeeding slot (15:00) should be BUFFER
        val nextSlot = schedule.find { it.dateKey == todayKey && it.startTime == "15:00" }
        assertNotNull(nextSlot)
        assertEquals(com.example.data.model.PsaSlotStatus.BUFFER, nextSlot!!.status)
    }

    @Test
    fun `weekly visits stats computes count, badges, and progress accurately`() {
        val todayKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val visits = listOf(
            PsaHomeVisit(id = "v1", customerName = "A", phone = "", address = "", neighborhood = "", scheduledDate = todayKey, scheduledTime = "10:00", modelInterest = "", status = com.example.data.model.PsaVisitStatus.AGENDADA),
            PsaHomeVisit(id = "v2", customerName = "B", phone = "", address = "", neighborhood = "", scheduledDate = todayKey, scheduledTime = "12:00", modelInterest = "", status = com.example.data.model.PsaVisitStatus.AGENDADA),
            PsaHomeVisit(id = "v3", customerName = "C", phone = "", address = "", neighborhood = "", scheduledDate = todayKey, scheduledTime = "16:00", modelInterest = "", status = com.example.data.model.PsaVisitStatus.CANCELADA)
        )

        // 2 active visits out of 10
        val stats = PsaScheduleManager.getWeeklyVisitsStats(visits, 10)
        assertEquals(2, stats.bookedVisitsCount)
        assertEquals(10, stats.goal)
        assertEquals(0.2f, stats.progressPercent, 0.01f)
        assertEquals("Primer Paso", stats.badgeTitle)
        assertEquals("🥉", stats.badgeIcon)
        assertTrue(stats.motivationalMessage.contains("Faltan 8 citas"))
    }
}
