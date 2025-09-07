package com.techsetu.geminichat.net

data class GeminiTextPart(val text: String)
data class GeminiContent(val role: String? = null, val parts: List<GeminiTextPart>)
data class GeminiRequest(val contents: List<GeminiContent>)

data class GeminiCandidate(val content: GeminiContent? = null)
data class GeminiResponse(val candidates: List<GeminiCandidate>? = null)
