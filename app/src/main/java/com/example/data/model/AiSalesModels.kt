package com.example.data.model

data class SmartResponseOption(
    val title: String,
    val text: String,
    val strategy: String,
    val tone: String
)

data class SalesCoachingResult(
    val score: Int, // 0 - 100
    val summary: String,
    val strengths: List<String>,
    val opportunities: List<String>,
    val coachingAdvice: List<String>,
    val idealResponse: String
)

data class ChatLearningInsight(
    val overallSummary: String,
    val commonQuestions: List<String>,
    val detectedObjections: List<String>,
    val recommendedActions: List<String>,
    val processedChatsCount: Int
)

data class MemoryEnhancedSalesAdvice(
    val persuasiveQuestions: List<String>,
    val highConversionResponses: List<SmartResponseOption>,
    val recommendedClosing: String,
    val learningInsight: String
)
