package com.example

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.TeamNotificationPriority
import com.example.data.model.TeamNotificationType
import com.example.service.EzoFirebaseMessagingService
import com.example.service.NotificationHelper
import com.google.firebase.messaging.RemoteMessage
import org.robolectric.Robolectric
import com.example.ui.components.GeneralSettingsAndHelpDialog
import com.example.ui.theme.FontScaleLevel
import com.example.ui.theme.MyApplicationTheme
import com.example.util.FcmNotificationPreferences
import com.example.util.FcmPayloadRouter
import com.example.util.FcmRoutedMessage
import com.example.util.PsaCountryRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FcmPersonalizedNotificationsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("psa_fcm_preferences", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        FcmNotificationPreferences.clearPersistentFcmBanner(context)
    }

    @Test
    fun `FcmPayloadRouter routes catalog, contact, personalized, and critical messages respecting channel toggles`() {
        val catalogEnabled = FcmPayloadRouter.classifyAndCheck(
            data = mapOf(
                "id" to "cat_1",
                "category" to "CATALOG_UPDATE",
                "priority" to "URGENT",
                "title" to "Precios PSA Diciembre",
                "body" to "Se actualizo el catalogo oficial para Argentina.",
                "sender" to "Admin Central"
            ),
            fallbackTitle = null,
            fallbackBody = null,
            catalogEnabled = true,
            assignedContactsEnabled = true
        )
        assertTrue(catalogEnabled is FcmRoutedMessage.Personalized)
        val catMsg = catalogEnabled as FcmRoutedMessage.Personalized
        assertEquals("cat_1", catMsg.id)
        assertEquals(TeamNotificationType.CATALOG_UPDATE, catMsg.type)
        assertEquals(TeamNotificationPriority.URGENT, catMsg.priority)
        assertEquals("Precios PSA Diciembre", catMsg.title)

        val catalogMuted = FcmPayloadRouter.classifyAndCheck(
            data = mapOf("category" to "PRICING_UPDATE", "title" to "Silenciado"),
            fallbackTitle = null,
            fallbackBody = null,
            catalogEnabled = false,
            assignedContactsEnabled = true
        )
        assertNull(catalogMuted)

        val contactEnabled = FcmPayloadRouter.classifyAndCheck(
            data = mapOf(
                "id" to "lead_99",
                "category" to "ASSIGNED_CONTACT",
                "title" to "Contacto Asignado",
                "body" to "Te asignaron a Maria Gomez."
            ),
            fallbackTitle = null,
            fallbackBody = null,
            catalogEnabled = false,
            assignedContactsEnabled = true
        )
        assertTrue(contactEnabled is FcmRoutedMessage.Personalized)
        assertEquals(TeamNotificationType.ASSIGNED_CONTACT, (contactEnabled as FcmRoutedMessage.Personalized).type)

        val contactMuted = FcmPayloadRouter.classifyAndCheck(
            data = mapOf("category" to "LEAD_ASSIGNED"),
            fallbackTitle = "Contacto",
            fallbackBody = "Silenciado",
            catalogEnabled = true,
            assignedContactsEnabled = false
        )
        assertNull(contactMuted)

        val directMsg = FcmPayloadRouter.classifyAndCheck(
            data = mapOf(
                "category" to "SUPERUSER_DIRECT",
                "title" to "Meta Semanal",
                "body" to "Buen trabajo esta semana."
            ),
            fallbackTitle = null,
            fallbackBody = null,
            catalogEnabled = false,
            assignedContactsEnabled = false
        )
        assertTrue(directMsg is FcmRoutedMessage.Personalized)
        assertEquals(TeamNotificationType.SUPERUSER_DIRECT, (directMsg as FcmRoutedMessage.Personalized).type)

        val criticalMsg = FcmPayloadRouter.classifyAndCheck(
            data = mapOf(
                "category" to "CRITICAL_ALERT",
                "severity" to "CRITICAL",
                "require_pause" to "true",
                "title" to "Pausa Operativa"
            ),
            fallbackTitle = null,
            fallbackBody = "Pausa requerida",
            catalogEnabled = false,
            assignedContactsEnabled = false
        )
        assertTrue(criticalMsg is FcmRoutedMessage.Critical)
        assertTrue((criticalMsg as FcmRoutedMessage.Critical).requirePause)
    }

    @Test
    fun `FcmNotificationPreferences stores independent channel toggles and cached token`() {
        val initial = FcmNotificationPreferences.getSettings(context)
        assertTrue(initial.notifyCatalogUpdates)
        assertTrue(initial.notifyAssignedContacts)

        FcmNotificationPreferences.setCatalogUpdatesEnabled(context, false)
        assertFalse(FcmNotificationPreferences.isCatalogUpdatesEnabled(context))
        assertTrue(FcmNotificationPreferences.isAssignedContactsEnabled(context))

        FcmNotificationPreferences.setAssignedContactsEnabled(context, false)
        assertFalse(FcmNotificationPreferences.isAssignedContactsEnabled(context))

        FcmNotificationPreferences.saveCachedToken(context, "  fcm-token-abc-123  ", 1_750_000_000_000L)
        assertEquals("fcm-token-abc-123", FcmNotificationPreferences.getCachedToken(context))
        assertEquals(1_750_000_000_000L, FcmNotificationPreferences.getCachedTokenUpdatedAtMs(context))
    }

    @Test
    fun `NotificationHelper registers FCM channels and attaches banner tap intent extras`() {
        val helper = NotificationHelper(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        assertNotNull(nm.getNotificationChannel(NotificationHelper.CHANNEL_CATALOG_UPDATES))
        assertNotNull(nm.getNotificationChannel(NotificationHelper.CHANNEL_ASSIGNED_CONTACTS))

        helper.showFcmPersonalizedNotification(
            notifId = "fcm_cat_42",
            title = "Catalogo Actualizado",
            body = "Nuevos valores para Senior 4.",
            type = TeamNotificationType.CATALOG_UPDATE,
            priority = TeamNotificationPriority.URGENT,
            senderName = "Superusuario",
            actionUrl = ""
        )

        val shadowNm = Shadows.shadowOf(nm)
        val posted = shadowNm.allNotifications.firstOrNull()
        assertNotNull(posted)
        assertEquals(NotificationHelper.CHANNEL_CATALOG_UPDATES, posted!!.channelId)
        assertTrue((posted.flags and Notification.FLAG_ONGOING_EVENT) != 0)
        assertTrue((posted.flags and Notification.FLAG_AUTO_CANCEL) == 0)

        val shadowPendingIntent = Shadows.shadowOf(posted.contentIntent)
        val launchIntent = shadowPendingIntent.savedIntent
        assertEquals(FcmNotificationPreferences.ACTION_OPEN_MAIN_BANNER, launchIntent.action)
        assertEquals(MainActivity::class.java.name, launchIntent.component?.className)
        assertTrue(launchIntent.getBooleanExtra(FcmNotificationPreferences.EXTRA_OPEN_FROM_FCM_BANNER, false))
        assertTrue(launchIntent.getBooleanExtra(FcmNotificationPreferences.EXTRA_OPEN_MAIN_SCREEN, false))
        assertEquals(
            helper.persistentNotificationId("fcm_cat_42"),
            launchIntent.getIntExtra(FcmNotificationPreferences.EXTRA_FCM_SYSTEM_NOTIFICATION_ID, 0)
        )
        assertEquals("fcm_cat_42", launchIntent.getStringExtra(FcmNotificationPreferences.EXTRA_FCM_NOTIF_ID))
        assertEquals("Catalogo Actualizado", launchIntent.getStringExtra(FcmNotificationPreferences.EXTRA_FCM_TITLE))
        assertEquals("Nuevos valores para Senior 4.", launchIntent.getStringExtra(FcmNotificationPreferences.EXTRA_FCM_BODY))
        assertEquals(TeamNotificationType.CATALOG_UPDATE.name, launchIntent.getStringExtra(FcmNotificationPreferences.EXTRA_FCM_TYPE))
        assertEquals("Superusuario", launchIntent.getStringExtra(FcmNotificationPreferences.EXTRA_FCM_SENDER))
    }

    @Test
    fun `EzoFirebaseMessagingService onMessageReceived persists top banner and posts ongoing main-screen notification`() {
        val service = Robolectric.buildService(EzoFirebaseMessagingService::class.java)
            .create()
            .get()

        val remoteMessage = RemoteMessage.Builder("123456789@fcm.googleapis.com")
            .setMessageId("msg_push_777")
            .addData("id", "push_catalog_777")
            .addData("category", "CATALOG_UPDATE")
            .addData("priority", "URGENT")
            .addData("title", "Lista PSA Actualizada")
            .addData("body", "Nuevos precios vigentes desde hoy en todo el pais.")
            .addData("sender", "Superusuario Central")
            .build()

        service.onMessageReceived(remoteMessage)

        val persisted = FcmNotificationPreferences.getPersistentFcmBanner(context)
        assertNotNull(persisted)
        assertEquals("push_catalog_777", persisted!!.id)
        assertEquals(TeamNotificationType.CATALOG_UPDATE, persisted.parsedType)
        assertEquals("Lista PSA Actualizada", persisted.title)
        assertEquals("Nuevos precios vigentes desde hoy en todo el pais.", persisted.body)
        assertEquals(persisted, FcmNotificationPreferences.persistentFcmBanner.value)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val posted = Shadows.shadowOf(nm).allNotifications.firstOrNull()
        assertNotNull(posted)
        assertTrue((posted!!.flags and Notification.FLAG_ONGOING_EVENT) != 0)

        val launchIntent = Shadows.shadowOf(posted.contentIntent).savedIntent
        assertEquals(MainActivity::class.java.name, launchIntent.component?.className)
        assertTrue(launchIntent.getBooleanExtra(FcmNotificationPreferences.EXTRA_OPEN_MAIN_SCREEN, false))

        // Background FCM tray tap (without NotificationHelper custom extras) also extracts banner cleanly
        val backgroundTrayTapIntent = Intent(FcmNotificationPreferences.ACTION_OPEN_MAIN_BANNER).apply {
            putExtra("google.message_id", "bg_99")
            putExtra("category", "SUPERUSER_DIRECT")
            putExtra("title", "Objetivo Diario")
            putExtra("body", "Revisar agenda confirmada")
            putExtra("sender", "Admin")
        }
        val extractedBg = FcmPayloadRouter.extractBannerFromTapIntent(
            intent = backgroundTrayTapIntent,
            catalogEnabled = true,
            assignedContactsEnabled = true
        )
        assertNotNull(extractedBg)
        assertEquals("bg_99", extractedBg!!.id)
        assertEquals(TeamNotificationType.SUPERUSER_DIRECT, extractedBg.parsedType)
        assertEquals("Objetivo Diario", extractedBg.title)
    }

    @Test
    fun `GeneralSettingsAndHelpDialog displays separate FCM switches and toggles them independently`() {
        val catalogEnabled = mutableStateOf(true)
        val contactsEnabled = mutableStateOf(true)

        composeTestRule.setContent {
            MyApplicationTheme {
                GeneralSettingsAndHelpDialog(
                    context = context,
                    currentFontLevel = FontScaleLevel.NORMAL,
                    activeCountry = PsaCountryRegistry.defaultCountry(),
                    hasDismissedSetupGuide = true,
                    notifyCatalogUpdates = catalogEnabled.value,
                    notifyAssignedContacts = contactsEnabled.value,
                    onSelectFontLevel = {},
                    onSelectCountry = {},
                    onToggleCatalogNotifications = { catalogEnabled.value = it },
                    onToggleAssignedContactsNotifications = { contactsEnabled.value = it },
                    onShowSetupGuideAgain = {},
                    onOpenNotificationSettings = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_switch_fcm_catalog").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_switch_fcm_catalog").assertIsOn()
        composeTestRule.onNodeWithTag("settings_switch_fcm_contacts").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_switch_fcm_contacts").assertIsOn()
        composeTestRule.onNodeWithText("Notificaciones Push (FCM)").assertIsDisplayed()

        composeTestRule.onNodeWithTag("settings_switch_fcm_catalog").performClick()
        composeTestRule.waitForIdle()
        assertFalse(catalogEnabled.value)
        assertTrue(contactsEnabled.value)
        composeTestRule.onNodeWithTag("settings_switch_fcm_catalog").assertIsOff()
        composeTestRule.onNodeWithTag("settings_switch_fcm_contacts").assertIsOn()

        composeTestRule.onNodeWithTag("settings_switch_fcm_contacts").performClick()
        composeTestRule.waitForIdle()
        assertFalse(contactsEnabled.value)
        composeTestRule.onNodeWithTag("settings_switch_fcm_contacts").assertIsOff()
    }
}
