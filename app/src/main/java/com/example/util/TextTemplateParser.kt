package com.example.util

import java.util.Calendar
import kotlin.random.Random

object TextTemplateParser {

    private val affectionateGreetings = listOf(
        "Mi cielo",
        "Mi corazón",
        "Alma hermosa",
        "Cariño",
        "Cielo",
        "Querida",
        "Querido"
    )

    private val variedGreetings = listOf(
        "¡Hola! ¿Cómo estás?",
        "¡Hola! Un placer saludarte.",
        "¡Qué tal! Espero que estés muy bien.",
        "¡Hola! Buenas tardes.",
        "¡Hola! ¿Todo bien?",
        "¡Buen día! Un gusto contactarte.",
        "¡Hola! Qué bueno comunicarme contigo."
    )

    fun getRandomAffectionateGreeting(): String {
        return affectionateGreetings.random()
    }

    fun getRandomVariedGreeting(name: String = ""): String {
        val base = variedGreetings.random()
        return if (name.isNotBlank()) {
            when (Random.nextInt(4)) {
                0 -> "¡Hola $name! ¿Cómo estás?"
                1 -> "¡Qué tal $name! Un placer saludarte."
                2 -> "¡Hola $name! Espero que te encuentres muy bien."
                else -> base
            }
        } else {
            base
        }
    }

    fun getTimeBasedGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Buenos días"
            in 12..19 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }

    /**
     * Resolves Spintax: {opción1|opción2|opción3}
     * Resolves variables: {afecto}, {nombre}, {name}, {saludo}, {saludo_variado}, {saludo_horario}, {telefono}, {phone}
     */
    fun process(template: String, contactName: String = "", contactPhone: String = ""): String {
        var result = template
        val cleanName = if (contactName.isNotBlank() && contactName != contactPhone) contactName.trim() else ""

        // 1. Resolve {afecto} or {cariño} with random affectionate greeting
        val affectionRegex = Regex("\\{(?:afecto|cariño|afectuoso)\\}", RegexOption.IGNORE_CASE)
        result = affectionRegex.replace(result) {
            getRandomAffectionateGreeting()
        }

        // 2. Resolve {saludo_variado} or {saludo} with natural dynamic greeting
        val variedGreetingRegex = Regex("\\{(?:saludo_variado)\\}", RegexOption.IGNORE_CASE)
        result = variedGreetingRegex.replace(result) {
            getRandomVariedGreeting(cleanName)
        }

        // 3. Resolve {saludo} or {saludo_horario}
        val timeGreetingRegex = Regex("\\{(?:saludo|saludo_horario)\\}", RegexOption.IGNORE_CASE)
        result = timeGreetingRegex.replace(result) {
            // If template has {saludo}, provide varied greetings half the time, or time-based greeting
            if (Random.nextBoolean()) getRandomVariedGreeting(cleanName) else getTimeBasedGreeting()
        }

        // 4. Resolve {nombre} or {name}
        val nameRegex = Regex("\\{(?:nombre|name)\\}", RegexOption.IGNORE_CASE)
        result = nameRegex.replace(result) {
            cleanName
        }

        // 5. Resolve {telefono} or {phone}
        val phoneRegex = Regex("\\{(?:telefono|teléfono|phone)\\}", RegexOption.IGNORE_CASE)
        result = phoneRegex.replace(result) {
            contactPhone
        }

        // 6. Resolve Spintax: {A|B|C}
        val spintaxRegex = Regex("\\{([^{}]+)\\}")
        var match = spintaxRegex.find(result)
        var safetyCounter = 0
        while (match != null && safetyCounter < 50) {
            safetyCounter++
            val fullMatch = match.value
            val content = match.groupValues[1]
            if (content.contains("|")) {
                val options = content.split("|")
                val chosen = options.random().trim()
                result = result.replaceFirst(fullMatch, chosen)
            } else {
                break
            }
            match = spintaxRegex.find(result)
        }

        return result.replace("  ", " ").trim()
    }
}
