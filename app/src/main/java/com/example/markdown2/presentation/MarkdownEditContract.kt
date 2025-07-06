package com.example.markdown2.presentation

interface MarkdownEditContract {
    interface View {
        fun showContent(content: String)
        fun closeWithResult(content: String)
        fun showError(message: String)
    }

    interface Presenter {
        fun saveContent(content: String)
    }
}