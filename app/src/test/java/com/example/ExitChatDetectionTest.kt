package com.example

import android.view.accessibility.AccessibilityNodeInfo
import com.example.service.EzoAccessibilityService
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExitChatDetectionTest {

    @Test
    fun testFindChatListNodeRecognizesConversationRows() {
        val service = Robolectric.buildService(EzoAccessibilityService::class.java).create().get()

        val chatRowNode = AccessibilityNodeInfo.obtain().apply {
            viewIdResourceName = "com.whatsapp:id/conversations_row"
            className = "android.widget.RelativeLayout"
        }

        val found = service.findChatListNode(chatRowNode)
        assertNotNull("Debe identificar las filas de conversación en la lista", found)
    }

    @Test
    fun testNonEditablePreviewTextNotTreatedAsBackButton() {
        val service = Robolectric.buildService(EzoAccessibilityService::class.java).create().get()

        val previewNode = AccessibilityNodeInfo.obtain().apply {
            viewIdResourceName = "com.whatsapp:id/last_message_tv"
            text = "Tu último mensaje fue entregado con éxito..."
            isEditable = false
            className = "android.widget.TextView"
        }

        val backBtn = service.findTopBackButton(previewNode)
        assertNull("Un nodo de texto de vista previa no debe ser confundido con botón de volver atrás", backBtn)
    }
}
