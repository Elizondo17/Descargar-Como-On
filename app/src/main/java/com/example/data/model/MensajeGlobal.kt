package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

enum class MensajeGlobalPrioridad(val label: String) {
    INFO("Aviso"),
    IMPORTANTE("Importante"),
    URGENTE("Urgente")
}

@Keep
@IgnoreExtraProperties
data class MensajeGlobal(
    @DocumentId
    val id: String = "",
    val titulo: String = "",
    val mensaje: String = "",
    val prioridad: String = MensajeGlobalPrioridad.IMPORTANTE.name,
    val activo: Boolean = true,
    val autorEmail: String = "",
    val autorNombre: String = "",
    val autorUid: String = "",
    val creadoEnMs: Long = 0L
) {
    val parsedPrioridad: MensajeGlobalPrioridad
        get() = try {
            MensajeGlobalPrioridad.valueOf(prioridad.trim().uppercase())
        } catch (_: Exception) {
            when (prioridad.trim().uppercase()) {
                "IMPORTANT" -> MensajeGlobalPrioridad.IMPORTANTE
                "URGENT" -> MensajeGlobalPrioridad.URGENTE
                else -> MensajeGlobalPrioridad.IMPORTANTE
            }
        }
}
