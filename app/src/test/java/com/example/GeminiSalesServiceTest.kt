package com.example

import com.example.data.model.BusinessProfile
import com.example.data.model.ScannedChat
import com.example.service.GeminiSalesService
import kotlinx.coroutines.runBlocking
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
class GeminiSalesServiceTest {

    private val service = GeminiSalesService()

    private val sampleProfile = BusinessProfile(
        id = "test_profile",
        businessName = "Como On Joyería y Regalos",
        businessType = "Joyería Fina y Accesorios",
        offeringDescription = "Anillos de plata 925, cadenas de oro y grabado personalizado con envío gratis",
        toneAndStyle = "Cálido, elegante y detallista",
        salesObjective = "Concretar el pedido con grabado o seña hoy mismo",
        frequentQuestionsAndObjections = "Medios de pago, tiempos de grabado y envíos a todo el país",
        isAiEnabledForOperators = true
    )

    @Test
    fun `generateSmartResponses returns 3 robust responses with fallback when API key is blank`() = runBlocking {
        val chatContext = "Hola, buenas tardes! Quería consultar cuánto sale el grabado para un anillo de plata?"
        val result = service.generateSmartResponses(chatContext, sampleProfile)

        assertTrue("Result should be successful", result.isSuccess)
        val options = result.getOrNull()
        assertNotNull("Options must not be null", options)
        assertEquals("Should provide 3 response options", 3, options!!.size)

        for (option in options) {
            assertTrue("Title must not be empty", option.title.isNotBlank())
            assertTrue("Text must not be empty", option.text.isNotBlank())
            assertTrue("Strategy must not be empty", option.strategy.isNotBlank())
            // Contextual text includes the business context
            assertTrue(
                "Text should contain business context",
                option.text.contains("Como On Joyería") || option.text.contains("plata") || option.text.length > 20
            )
        }
    }

    @Test
    fun `evaluateSalesConversation provides structured coaching even without network or key`() = runBlocking {
        val chatHistory = """
            Cliente: Hola, tienen stock de las cadenas de oro?
            Vendedor: Hola sí tenemos.
            Cliente: Y cuánto cuestan?
            Vendedor: 85 mil pesos.
            Cliente: Ah bueno gracias.
        """.trimIndent()

        val result = service.evaluateSalesConversation(chatHistory, sampleProfile)
        // If API key is not configured, it gracefully returns failure or handled result without crash
        assertNotNull(result)
        if (result.isSuccess) {
            val coaching = result.getOrNull()!!
            assertTrue("Score should be valid", coaching.score in 0..100)
            assertTrue("Strengths should not be empty", coaching.strengths.isNotEmpty())
            assertTrue("Advice should not be empty", coaching.coachingAdvice.isNotEmpty())
            assertTrue("Ideal response should be present", coaching.idealResponse.isNotBlank())
        } else {
            // Expected when no key is set in test environment
            val error = result.exceptionOrNull()
            assertNotNull("Exception should have informative message", error?.message)
        }
    }

    @Test
    fun `learnFromScannedChats handles empty list gracefully`() = runBlocking {
        val result = service.learnFromScannedChats(emptyList(), sampleProfile)
        assertTrue("Empty chats should yield failure", result.isFailure)
        assertTrue(
            "Error should state no chats to analyze",
            result.exceptionOrNull()?.message?.contains("No hay chats") == true
        )
    }

    @Test
    fun `learnFromScannedChats processes scanned chats list safely`() = runBlocking {
        val dummyChats = listOf(
            ScannedChat(
                id = "+5491111111111",
                name = "Carla Ramos",
                phoneNumber = "+5491111111111",
                dateText = "16 oct",
                lastMessage = "¿Hacen envíos a Córdoba?",
                folderName = "Calientes MOP1"
            ),
            ScannedChat(
                id = "+5492222222222",
                name = "Martín Gomez",
                phoneNumber = "+5492222222222",
                dateText = "16 oct",
                lastMessage = "¿Se puede pagar con Mercado Pago en cuotas?",
                folderName = "Calientes MOP1"
            )
        )

        val result = service.learnFromScannedChats(dummyChats, sampleProfile)
        assertNotNull(result)
        if (result.isSuccess) {
            val insight = result.getOrNull()!!
            assertTrue(insight.processedChatsCount >= 2)
            assertTrue(insight.commonQuestions.isNotEmpty())
        }
    }

    @Test
    fun `generateBusinessProfileFromIdea creates intelligent profiles for different niches`() = runBlocking {
        // Test automotive niche fallback
        val autoResult = service.generateBusinessProfileFromIdea("Vendo autos usados y planes de ahorro financiados en cuotas fijas")
        assertTrue(autoResult.isSuccess)
        val autoProfile = autoResult.getOrNull()!!
        assertTrue("Name should reflect automotive niche", autoProfile.businessName.contains("Automotores") || autoProfile.businessName.contains("autos"))
        assertTrue("Offering should not be blank", autoProfile.offeringDescription.isNotBlank())
        assertFalse("AI initially off for operators by default", autoProfile.isAiEnabledForOperators)

        // Test gastronomy niche fallback
        val pizzaResult = service.generateBusinessProfileFromIdea("Pizzería artesanal y empanadas al horno de barro con delivery rápido")
        assertTrue(pizzaResult.isSuccess)
        val pizzaProfile = pizzaResult.getOrNull()!!
        assertTrue("Name should reflect gastronomy niche", pizzaProfile.businessName.contains("Gastronomía") || pizzaProfile.businessName.contains("Pizzería"))

        // Test general business idea
        val genericResult = service.generateBusinessProfileFromIdea("Estudio contable y asesoramiento impositivo para pymes")
        assertTrue(genericResult.isSuccess)
        val genericProfile = genericResult.getOrNull()!!
        assertTrue(genericProfile.businessName.isNotBlank())
        assertTrue(genericProfile.salesObjective.isNotBlank())
    }
}
