package com.spendwise.domain

data class ActiveTagToken(
    val query: String,
    val startIndex: Int,
    val endIndex: Int
)

object TagParser {
    private val tagRegex = Regex("""(^|[^\p{L}\p{N}_-])#([\p{L}\p{N}_-]+)""")
    private val allowedTagChar = Regex("""[\p{L}\p{N}_-]""")

    fun parse(note: String): List<String> {
        return tagRegex.findAll(note)
            .map { it.groupValues[2] }
            .map(::normalize)
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    fun normalize(tag: String): String =
        tag.trim().removePrefix("#").trim().lowercase()

    fun activeToken(text: String, cursorIndex: Int): ActiveTagToken? {
        val safeCursor = cursorIndex.coerceIn(0, text.length)
        val hashIndex = text.lastIndexOf('#', startIndex = (safeCursor - 1).coerceAtLeast(0))
        if (hashIndex < 0) return null
        if (hashIndex > 0 && !text[hashIndex - 1].isWhitespace()) return null

        val query = text.substring(hashIndex + 1, safeCursor)
        if (query.any { !allowedTagChar.matches(it.toString()) }) return null

        val tokenEnd = generateSequence(safeCursor) { index ->
            if (index < text.length && allowedTagChar.matches(text[index].toString())) index + 1 else null
        }.last()

        return ActiveTagToken(query = normalize(query), startIndex = hashIndex, endIndex = tokenEnd)
    }

    fun replaceActiveToken(text: String, token: ActiveTagToken, tag: String): String {
        val replacement = "#${normalize(tag)}"
        return buildString {
            append(text.substring(0, token.startIndex))
            append(replacement)
            append(" ")
            append(text.substring(token.endIndex))
        }.replace(Regex("""\s{2,}"""), " ")
    }
}

