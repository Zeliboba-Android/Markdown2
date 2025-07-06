package com.example.markdown2.presentation

import java.io.File

class MarkdownEditPresenter(
    private val view: MarkdownEditContract.View,
    private val filePath: String
) : MarkdownEditContract.Presenter {

    override fun saveContent(content: String) {
        try {
            File(filePath).writeText(content)
            view.closeWithResult(content)
        } catch (e: Exception) {
            view.showError("Failed to save file: ${e.message}")
        }
    }
}