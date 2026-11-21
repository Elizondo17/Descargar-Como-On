package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_profile")
data class BusinessProfile(
    @PrimaryKey
    val id: String = "primary_profile",
    val businessName: String = "Mi Negocio",
    val businessType: String = "Venta General",
    val offeringDescription: String = "Ofrecemos atención personalizada, productos y servicios de alta calidad, precios competitivos y facilidades de pago para nuestros clientes.",
    val toneAndStyle: String = "Persuasivo, cercano, empático y orientado a generar confianza y cerrar ventas",
    val salesObjective: String = "Responder dudas, calificar el interés y coordinar el siguiente paso para concretar la venta o servicio",
    val frequentQuestionsAndObjections: String = "¿Cuáles son los precios? ¿Qué medios de pago aceptan? ¿Hacen envíos o turnos?",
    val isAiEnabledForOperators: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
