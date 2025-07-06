package com.example.markdown2.test

import com.example.markdown2.domain.MarkdownElement
import com.example.markdown2.domain.MarkdownParser
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {
    private val parser = MarkdownParser()

    @Test
    fun parseHeaders() {
        val content = "# H1\n## H2\n### H3"
        val elements = parser.parse(content)

        assertTrue(elements[0] is MarkdownElement.Header)
        assertEquals(1, (elements[0] as MarkdownElement.Header).level)
        assertEquals("H1", (elements[0] as MarkdownElement.Header).text)
    }

    @Test
    fun parseTables() {
        val content = "| Header |\n| --- |\n| Cell |"
        val elements = parser.parse(content)

        assertTrue(elements[0] is MarkdownElement.Table)
        assertEquals(1, (elements[0] as MarkdownElement.Table).data.headers.size)
    }
}