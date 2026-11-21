package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sales_knowledge")
data class SalesKnowledgeItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessProfileId: String = "primary_profile",
    val category: String = "GENERAL_WINNING", // "QUESTION_DISCOVERY", "OBJECTION_PRICE", "OBJECTION_TRUST", "CLOSING_TECHNIQUE", "GENERAL_WINNING"
    val triggerContext: String = "", // Situación o duda del cliente (ej. "Objeción de precio", "Indagación de necesidades")
    val winningContent: String, // La pregunta persuasiva o la respuesta que generó conversión
    val successScore: Int = 1, // Calificaciones positivas acumuladas
    val usageCount: Int = 1, // Veces aplicada
    val isQuestion: Boolean = false, // True si es una pregunta inteligente, False si es respuesta
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)
