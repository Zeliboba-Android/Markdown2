package com.example.markdown2.presentation

import com.example.markdown2.domain.MarkdownElement

interface MarkdownViewContract {
    interface View {
        fun displayContent(elements: List<MarkdownElement>)
        fun showError(message: String)
    }

    interface Presenter {
        fun parseContent(content: String)
    }
}