package com.example.util

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

/**
 * Lightweight PDF text extractor for digital PSA price-list PDFs (uncompressed and FlateDecode streams).
 */
object PsaPdfTextExtractor {

    private val STREAM_REGEX = Regex("""stream\r?\n(.*?)\r?\nendstream""", setOf(RegexOption.DOT_MATCHES_ALL))
    private val TEXT_OBJECT_REGEX = Regex("""BT\b(.*?)\bET""", setOf(RegexOption.DOT_MATCHES_ALL))
    private val STRING_OP_REGEX = Regex("""\((?:\\.|[^\\()])*\)\s*Tj|\[(.*?)\]\s*TJ""", setOf(RegexOption.DOT_MATCHES_ALL))
    private val LITERAL_STRING_REGEX = Regex("""\(((?:\\.|[^\\()])*)\)""")

    fun extractTextFromUri(context: Context, uri: Uri): String {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return ""
        if (rawBytes.isEmpty()) return ""

        val pdfLatin1 = String(rawBytes, Charsets.ISO_8859_1)
        val extractedLines = mutableListOf<String>()

        // Collect direct uncompressed streams plus Flate-decompressed content streams
        for (match in STREAM_REGEX.findAll(pdfLatin1)) {
            val bodyStart = match.groups[1]?.range?.first ?: continue
            val bodyEnd = (match.groups[1]?.range?.last ?: continue) + 1
            if (bodyStart >= bodyEnd || bodyEnd > rawBytes.size) continue

            val headerStart = (match.range.first - 220).coerceAtLeast(0)
            val dictWindow = pdfLatin1.substring(headerStart, match.range.first)
            val rawStreamBytes = rawBytes.copyOfRange(bodyStart, bodyEnd)

            val contentText = if (dictWindow.contains("/FlateDecode", ignoreCase = true)) {
                inflateBytes(rawStreamBytes) ?: String(rawStreamBytes, Charsets.ISO_8859_1)
            } else {
                String(rawStreamBytes, Charsets.ISO_8859_1)
            }

            if (contentText.contains("BT") && (contentText.contains("Tj") || contentText.contains("TJ"))) {
                extractTextOperators(contentText, extractedLines)
            }
        }

        // Fallback if PDF stores inline BT..ET outside standard stream delimiters
        if (extractedLines.isEmpty() && pdfLatin1.contains("BT")) {
            extractTextOperators(pdfLatin1, extractedLines)
        }

        return extractedLines.joinToString("\n")
    }

    private fun inflateBytes(data: ByteArray): String? {
        return try {
            val inflater = Inflater()
            inflater.setInput(data)
            val buffer = ByteArray(4096)
            val out = ByteArrayOutputStream()
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) break
                }
                out.write(buffer, 0, count)
            }
            inflater.end()
            String(out.toByteArray(), Charsets.ISO_8859_1)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractTextOperators(streamText: String, outputLines: MutableList<String>) {
        for (btMatch in TEXT_OBJECT_REGEX.findAll(streamText)) {
            val block = btMatch.groupValues[1]
            val lineBuilder = StringBuilder()

            for (opMatch in STRING_OP_REGEX.findAll(block)) {
                val op = opMatch.value
                if (op.endsWith("TJ")) {
                    val arrayContent = opMatch.groupValues[1]
                    for (lit in LITERAL_STRING_REGEX.findAll(arrayContent)) {
                        lineBuilder.append(decodePdfLiteral(lit.groupValues[1]))
                    }
                    lineBuilder.append(' ')
                } else {
                    LITERAL_STRING_REGEX.find(op)?.let { lit ->
                        lineBuilder.append(decodePdfLiteral(lit.groupValues[1])).append(' ')
                    }
                }
            }

            val cleaned = lineBuilder.toString().replace(Regex("""\s+"""), " ").trim()
            if (cleaned.isNotBlank()) {
                outputLines.add(cleaned)
            }
        }
    }

    private fun decodePdfLiteral(raw: String): String {
        val sb = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            val ch = raw[i]
            if (ch == '\\' && i + 1 < raw.length) {
                when (val next = raw[i + 1]) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    '(', ')', '\\' -> sb.append(next)
                    in '0'..'7' -> {
                        val octEnd = (i + 4).coerceAtMost(raw.length)
                        val octStr = raw.substring(i + 1, octEnd).takeWhile { it in '0'..'7' }
                        val code = octStr.toIntOrNull(8)
                        if (code != null) {
                            sb.append(code.toChar())
                            i += octStr.length
                        }
                    }
                    else -> sb.append(next)
                }
                i += 2
            } else {
                sb.append(ch)
                i++
            }
        }
        return sb.toString()
    }
}
