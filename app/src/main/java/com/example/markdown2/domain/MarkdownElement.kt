package com.example.markdown2.domain

sealed class MarkdownElement {
    data class Header(
        val level: Int,
        val text: String,
        val anchor: String? = null
    ) : MarkdownElement()

    data class Text(
        val content: String,
        val styles: List<Style> = emptyList(),
        val links: List<Link> = emptyList() // Добавлено поле для ссылок
    ) : MarkdownElement()

    data class Image(
        val url: String,
        val altText: String
    ) : MarkdownElement()

    data class Table(
        val data: TableData
    ) : MarkdownElement()

    data class CodeBlock(
        val code: String,
        val language: String? = null
    ) : MarkdownElement()

    data class Link(
        val text: String,
        val target: String,
        val isAnchor: Boolean
    )
}

enum class Style { BOLD, ITALIC, STRIKETHROUGH }

data class TableData(
    val headers: List<String>,
    val rows: List<List<String>>
)