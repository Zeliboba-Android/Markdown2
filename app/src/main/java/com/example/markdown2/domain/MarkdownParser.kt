package com.example.markdown2.domain

class MarkdownParser {
    private val headerIdRegex = Regex("[^\\w\\d-]", RegexOption.MULTILINE)
    private val headerMap = mutableMapOf<String, Int>()
    fun parse(content: String): List<MarkdownElement> {
        val elements = mutableListOf<MarkdownElement>()
        val lines = content.lines()
        var i = 0
        lines.filter { it.startsWith("#") }.forEach { line ->
            parseHeader(line)?.let { header ->
                val id = generateHeaderId(header.text)
                headerMap[id] = header.level
            }
        }
        while (i < lines.size) {
            val line = lines[i].trim()

            when {
                line.startsWith("```") -> {
                    // Определяем язык кода (если указан)
                    val language = line.substring(3).trim().takeIf { it.isNotBlank() }
                    val codeLines = mutableListOf<String>()

                    i++ // Переходим к первой строке кода
                    while (i < lines.size && !lines[i].trim().startsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }

                    if (i < lines.size) i++

                    elements.add(MarkdownElement.CodeBlock(
                        code = codeLines.joinToString("\n"),
                        language = language
                    ))
                    continue
                }
                line.startsWith("#") -> parseHeader(line)?.let(elements::add)
                line.startsWith("|") -> {
                    // Проверяем, что это действительно таблица (есть строка разделителя)
                    if (i + 1 < lines.size && lines[i + 1].startsWith("|") && lines[i + 1].contains("-")) {
                        val tableData = parseTable(lines, i)
                        elements.add(MarkdownElement.Table(tableData))

                        // Правильное смещение: заголовок + разделитель + строки данных
                        i += tableData.rows.size + 2  // Исправлено здесь (+2 вместо +1)
                        continue
                    } else {
                        // Если нет разделителя - обрабатываем как обычный текст
                        elements.add(parseText(line))
                    }
                }
                line.startsWith("![") -> parseImage(line)?.let(elements::add)
                line.isNotBlank() -> parseText(line)?.let(elements::add)
            }
            i++
        }
        return elements
    }

    private fun parseHeader(line: String): MarkdownElement.Header? {
        val regex = """^(#{1,6})\s(.+)$""".toRegex()
        return regex.find(line)?.let {
            MarkdownElement.Header(it.groupValues[1].length, it.groupValues[2])
        }
    }

    private fun parseText(line: String): MarkdownElement.Text {
        val styles = mutableListOf<Style>()
        var processed = line
        val links = mutableListOf<MarkdownElement.Link>()

        // Apply styles
        val boldRegex = """\*\*(.*?)\*\*""".toRegex()
        val italicRegex = """\*(.*?)\*""".toRegex()
        val strikeRegex = """~~(.*?)~~""".toRegex()
        val linkRegex = """\[([^\]]+)]\(([^)]+)\)""".toRegex()
        processed = linkRegex.replace(processed) {
            val linkText = it.groupValues[1]
            val url = it.groupValues[2]

            if (url.startsWith("#")) {
                // Ссылка на заголовок
                links.add(MarkdownElement.Link(linkText, url.substring(1), true))
                linkText
            } else {
                // Обычная ссылка
                links.add(MarkdownElement.Link(linkText, url, false))
                linkText
            }
        }
        processed = boldRegex.replace(processed) {
            styles.add(Style.BOLD)
            it.groupValues[1]
        }

        processed = italicRegex.replace(processed) {
            styles.add(Style.ITALIC)
            it.groupValues[1]
        }

        processed = strikeRegex.replace(processed) {
            styles.add(Style.STRIKETHROUGH)
            it.groupValues[1]
        }

        return MarkdownElement.Text(processed, styles.distinct(), links)
    }

    private fun parseImage(line: String): MarkdownElement.Image? {
        val regex = """!\[(.*?)]\((.*?)\)""".toRegex()
        return regex.find(line)?.let {
            MarkdownElement.Image(it.groupValues[2], it.groupValues[1])
        }
    }

    private fun parseTable(lines: List<String>, start: Int): TableData {
        val headers = lines[start]
            .split("|")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // Проверяем строку разделителя
        if (start + 1 >= lines.size ||
            !lines[start + 1].startsWith("|") ||
            !lines[start + 1].contains("-")) {
            return TableData(headers, emptyList())
        }

        val rows = mutableListOf<List<String>>()
        var currentLine = start + 2  // Начинаем со строки после разделителя

        while (currentLine < lines.size &&
            lines[currentLine].startsWith("|") &&
            !lines[currentLine].contains("---")) {  // Игнорируем возможные разделители внутри
            rows.add(
                lines[currentLine]
                    .split("|")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
            )
            currentLine++
        }

        return TableData(headers, rows)
    }
    private fun generateHeaderId(text: String): String {
        return text.lowercase()
            .replace(headerIdRegex, "")
            .replace(" ", "-")
    }

}