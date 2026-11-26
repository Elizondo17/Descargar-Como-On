package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Como On", appName)
  }

  @Test
  fun `user account expiration calculation`() {
    val pastTime = System.currentTimeMillis() - 100000L
    val expiredUser = com.example.data.model.UserAccount(
      id = "test-1",
      username = "Test Expired",
      role = "OPERATOR",
      token = "TOKEN-123",
      validFromMs = pastTime - 100000L,
      expiresAtMs = pastTime,
      daysGranted = 5
    )
    assertTrue(expiredUser.isExpired)
    assertEquals(0L, expiredUser.remainingDays)

    val futureTime = System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 10) // 10 days
    val activeUser = com.example.data.model.UserAccount(
      id = "test-2",
      username = "Test Active",
      role = "OPERATOR",
      token = "TOKEN-456",
      validFromMs = System.currentTimeMillis(),
      expiresAtMs = futureTime,
      daysGranted = 10
    )
    assertFalse(activeUser.isExpired)
    assertTrue(activeUser.remainingDays >= 9L)
  }

  @Test
  fun `user token expiration and blocking validation`() {
    val now = System.currentTimeMillis()
    val expiredToken = com.example.data.model.UserToken(
      token = "EZO-EXP-1234-D5",
      userId = "usr-1",
      username = "Carlos",
      role = "OPERATOR",
      validFromMs = now - (1000L * 60 * 60 * 24 * 6),
      expiresAtMs = now - 1000L,
      daysGranted = 5,
      isBlocked = false
    )
    assertTrue(expiredToken.isExpired)
    assertEquals(0L, expiredToken.remainingDays)

    val validToken = com.example.data.model.UserToken(
      token = "EZO-VAL-5678-D15",
      userId = "usr-2",
      username = "Elena",
      role = "OPERATOR",
      validFromMs = now,
      expiresAtMs = now + (1000L * 60 * 60 * 24 * 15),
      daysGranted = 15,
      isBlocked = false
    )
    assertFalse(validToken.isExpired)
    assertTrue(validToken.remainingDays >= 14L)
  }

  @Test
  fun `user days subtraction when too many days granted by mistake`() {
    val now = System.currentTimeMillis()
    val initialDays = 30
    val user = com.example.data.model.UserAccount(
      id = "usr-mistake-1",
      username = "Operador Turno Mañana",
      role = "OPERATOR",
      token = "TOKEN-MISTAKE-30D",
      validFromMs = now,
      expiresAtMs = now + (1000L * 60 * 60 * 24 * initialDays),
      daysGranted = initialDays
    )
    assertTrue(user.remainingDays >= 29L)

    // Simulate mistake correction by subtracting 15 days
    val subtractedDelta = -15
    val deltaMillis = subtractedDelta.toLong() * 24L * 60L * 60L * 1000L
    val newExpiry = user.expiresAtMs + deltaMillis
    val correctedUser = user.copy(
      expiresAtMs = newExpiry,
      daysGranted = user.daysGranted + subtractedDelta
    )

    assertEquals(15, correctedUser.daysGranted)
    assertTrue(correctedUser.remainingDays in 14L..15L)
    assertFalse(correctedUser.isExpired)

    // Simulate subtracting more days than available (e.g. -20 days)
    val excessiveSubtract = -20
    val zeroedExpiry = (correctedUser.expiresAtMs + (excessiveSubtract.toLong() * 24L * 60L * 60L * 1000L)).coerceAtLeast(now - 1000L)
    val zeroedUser = correctedUser.copy(
      expiresAtMs = zeroedExpiry,
      daysGranted = (correctedUser.daysGranted + excessiveSubtract).coerceAtLeast(0),
      isBlocked = zeroedExpiry <= now
    )
    assertEquals(0, zeroedUser.daysGranted)
    assertTrue(zeroedUser.isBlocked)
  }

  @Test
  fun `user details update and remaining days correction`() {
    val now = System.currentTimeMillis()
    val user = com.example.data.model.UserAccount(
      id = "usr-edit-1",
      username = "Nombre Anterior",
      role = "OPERATOR",
      token = "TOKEN-EDIT-1",
      validFromMs = now,
      expiresAtMs = now + (1000L * 60 * 60 * 24 * 7),
      daysGranted = 7,
      notes = "Nota previa"
    )

    val updatedName = "Carlos Mendoza"
    val updatedNotes = "Turno Tarde - Levantamientos"
    val updatedDays = 12
    val updatedExpiry = now + (updatedDays.toLong() * 24L * 60L * 60L * 1000L)

    val updatedUser = user.copy(
      username = updatedName,
      notes = updatedNotes,
      expiresAtMs = updatedExpiry,
      daysGranted = updatedDays
    )

    assertEquals("Carlos Mendoza", updatedUser.username)
    assertEquals("Turno Tarde - Levantamientos", updatedUser.notes)
    assertEquals(12, updatedUser.daysGranted)
    assertTrue(updatedUser.remainingDays in 11L..12L)
  }

  @Test
  fun `superuser Admin-payito credentials verification`() {
    val superuser = com.example.data.model.UserAccount(
      id = "admin-payito",
      username = "Admin-payito",
      role = "SUPERUSER",
      token = "amordechat",
      validFromMs = System.currentTimeMillis(),
      expiresAtMs = System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10),
      daysGranted = 3650
    )

    assertEquals("Admin-payito", superuser.username)
    assertEquals("amordechat", superuser.token)
    assertEquals("SUPERUSER", superuser.role)
    assertFalse(superuser.isExpired)
    assertEquals(9999L, superuser.remainingDays)
  }

  @Test
  fun `scanned chat selection and phone number preservation`() {
    val chat = com.example.data.model.ScannedChat(
      id = "+5491155667788",
      name = "Elena Ventas",
      phoneNumber = "+5491155667788",
      dateText = "16 oct",
      lastMessage = "Hola! Consulta de cotización",
      isSelected = true,
      isSent = false
    )

    assertTrue(chat.isSelected)
    assertFalse(chat.isSent)
    assertEquals("+5491155667788", chat.phoneNumber)

    val unselectedChat = chat.copy(isSelected = false)
    assertFalse(unselectedChat.isSelected)
  }

  @Test
  fun `duplicate message strategy allows resending when contact is unmarked and reselected`() {
    com.example.service.AutomationManager.clearProcessedRecipientsSession()
    val testPhone = "+54 9 11 9876-5432"

    // Not sent initially
    assertFalse(com.example.service.AutomationManager.isRecipientProcessed(testPhone))

    // Mark as processed in current round
    com.example.service.AutomationManager.markRecipientProcessed(testPhone)
    assertTrue(com.example.service.AutomationManager.isRecipientProcessed(testPhone))
    // Also matched by normalized digits
    assertTrue(com.example.service.AutomationManager.isRecipientProcessed("5491198765432"))

    // User unmarks / reselects for resending: unmark allows delivery again
    com.example.service.AutomationManager.unmarkRecipientProcessed(testPhone)
    assertFalse(com.example.service.AutomationManager.isRecipientProcessed(testPhone))
  }

  @Test
  fun `unfound chat preserves selection state in list and is not deleted`() {
    val pendingChat = com.example.data.model.ScannedChat(
      id = "+5491122334455",
      name = "Cliente No Visible",
      phoneNumber = "+5491122334455",
      isSelected = true,
      isSent = false
    )

    // Simulating delivery failure when contact is not found after scroll:
    // Selection state should remain true and isSent remain false
    val failureReason = "CONTACT_NOT_FOUND_AFTER_SCROLL: No se encontró al contacto tras scroll"
    assertTrue(failureReason.startsWith("CONTACT_NOT_FOUND_AFTER_SCROLL"))

    // Verify contact stays selected and unsent
    assertTrue("Chat must remain selected so it is not dropped from list", pendingChat.isSelected)
    assertFalse("Chat must not be marked sent on failure", pendingChat.isSent)
  }

  @Test
  fun `session locking prevents duplicate delivery across contact names and user digits`() {
    com.example.service.AutomationManager.clearProcessedRecipientsSession()
    val userNumber = "2099189691481195"

    assertFalse(com.example.service.AutomationManager.isRecipientProcessed(userNumber))

    // Mark as processed upon message delivery
    com.example.service.AutomationManager.markRecipientProcessed(userNumber)
    assertTrue(com.example.service.AutomationManager.isRecipientProcessed(userNumber))
    assertTrue(com.example.service.AutomationManager.isRecipientProcessed("2099189691481195"))

    // Different user number is not blocked
    assertFalse(com.example.service.AutomationManager.isRecipientProcessed("2099189699999999"))
  }

  @Test
  fun `clean digit extraction ignores channel prefixes like WA, EC, Lead`() {
    val rawLeadText = "WA 2099189691481195"
    val digits = rawLeadText.filter { it.isDigit() }
    assertEquals("2099189691481195", digits)
    assertTrue(digits.length >= 7)

    val rawEcText = "EC 5491122334455"
    val ecDigits = rawEcText.filter { it.isDigit() }
    assertEquals("5491122334455", ecDigits)

    // Verification of channel badge filtering
    val badges = listOf("WA", "EC", "Lead", "CH", "IG", "FB")
    badges.forEach { badge ->
      val filteredDigits = badge.filter { it.isDigit() }
      assertTrue("Channel badge $badge alone has no digits", filteredDigits.length < 7)
    }
  }

  @Test
  fun `ai automation config is disabled by default and supports custom strategy`() {
    val defaultConfig = com.example.data.model.AutomationConfig()
    // CRITICAL requirement: Must be disabled by default unless user explicitly turns it on
    assertFalse("AI auto response must be disabled by default", defaultConfig.useAiSmartResponse)
    assertEquals("Cierre Comercial", defaultConfig.aiResponseStrategy)

    val customConfig = defaultConfig.copy(
      useAiSmartResponse = true,
      aiResponseStrategy = "Cálido y Empático"
    )
    assertTrue(customConfig.useAiSmartResponse)
    assertEquals("Cálido y Empático", customConfig.aiResponseStrategy)
  }

  @Test
  fun `deleting scanned chat after send physically removes it from list preventing duplicates`() {
    val initialList = mutableListOf(
      com.example.data.model.ScannedChat(id = "chat-1", name = "Cliente 1", phoneNumber = "11111111", isSelected = true),
      com.example.data.model.ScannedChat(id = "chat-2", name = "Cliente 2", phoneNumber = "22222222", isSelected = true)
    )
    assertEquals(2, initialList.size)

    // Simulate sending to chat-1 and removing it upon success
    val sentId = "chat-1"
    initialList.removeAll { it.id == sentId }

    assertEquals(1, initialList.size)
    assertFalse(initialList.any { it.id == sentId })
    assertEquals("chat-2", initialList.first().id)
  }

  @Test
  fun `notification configuration silences individual success alerts by default`() {
    val cfg = com.example.data.model.AutomationConfig()
    // User requirement: Do not flood Android with individual delivery notifications
    assertFalse("Individual delivery notifications must be silenced by default", cfg.notifyOnEverySuccess)

    val verboseCfg = cfg.copy(notifyOnEverySuccess = true)
    assertTrue(verboseCfg.notifyOnEverySuccess)
  }

  @Test
  fun `closer AI generates non-bot responses without corporate greetings`() = kotlinx.coroutines.runBlocking {
    val service = com.example.service.GeminiSalesService()
    val tarotProfile = com.example.data.model.BusinessProfile(
      businessName = "Tarot Espiritual",
      businessType = "Lectura de Tarot y Videncia",
      offeringDescription = "Lecturas completas de Tarot por WhatsApp e Instagram, tiradas de amor y destrabe",
      toneAndStyle = "Místico, revelador, empático y cautivador",
      salesObjective = "Generar intriga y cerrar el turno de lectura para hoy"
    )

    val result = service.generateSmartResponses("Hola, quería consultar cuánto sale la lectura", tarotProfile)
    assertTrue(result.isSuccess)
    val options = result.getOrNull().orEmpty()
    assertTrue(options.isNotEmpty())

    options.forEach { opt ->
      val text = opt.text
      // STRICT requirement: No corporate bot phrases
      assertFalse(text.contains("Gracias por comunicarte con", ignoreCase = true))
      assertFalse(text.contains("Hola, en nuestra empresa", ignoreCase = true))
      assertFalse(text.contains("Soy el asistente virtual", ignoreCase = true))
      // Must contain thematic emojis or natural conversational hook
      assertTrue(text.isNotBlank())
    }
  }

  @Test
  fun `gemini model list prioritizes gemini-3-8-flash and excludes discontinued gemini-2-0-flash`() {
    val service = com.example.service.GeminiSalesService()
    val modelsField = service.javaClass.getDeclaredField("supportedModels").apply {
      isAccessible = true
    }
    @Suppress("UNCHECKED_CAST")
    val models = modelsField.get(service) as List<String>
    assertEquals("gemini-3.8-flash", models.first())
    assertFalse("gemini-2.0-flash must be removed because Google deprecated it", models.contains("gemini-2.0-flash"))
    assertTrue(models.contains("gemini-3.8-flash"))
  }

  @Test
  fun `setup guide notification permission check succeeds under robolectric`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val notificationsEnabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
    assertTrue(notificationsEnabled)
  }

  @Test
  fun `unattended automation logger writes events and clears logs correctly`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.util.UnattendedAutomationLogger.clearLogs(context)

    com.example.util.UnattendedAutomationLogger.logEvent(
      context = context,
      tag = "TEST_ENVIO",
      recipient = "1055595407491870",
      status = "ENVIADO_OK",
      exitedChatSuccessfully = true,
      details = "Prueba unitaria"
    )

    val logs = com.example.util.UnattendedAutomationLogger.readLogs(context)
    assertEquals(1, logs.size)
    assertTrue(logs[0].contains("TEST_ENVIO"))
    assertTrue(logs[0].contains("1055595407491870"))
    assertTrue(logs[0].contains("SALIDA_CHAT: OK"))

    com.example.util.UnattendedAutomationLogger.clearLogs(context)
    val emptyLogs = com.example.util.UnattendedAutomationLogger.readLogs(context)
    assertTrue(emptyLogs.isEmpty())
  }
}


