package com.example.markdown2.presentation

import com.example.markdown2.domain.MarkdownParser

class MarkdownViewPresenter(
    private val view: MarkdownViewContract.View,
    private val parser: MarkdownParser
) : MarkdownViewContract.Presenter {

    override fun parseContent(content: String) {
        try {
            view.displayContent(parser.parse(content))
        } catch (e: Exception) {
            view.showError("Parsing error")
        }
    }
}