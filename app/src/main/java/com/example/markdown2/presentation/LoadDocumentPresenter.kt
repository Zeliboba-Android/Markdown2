package com.example.markdown2.presentation

import android.content.Context
import android.net.Uri
import com.example.markdown2.data.MarkdownRepository

class LoadDocumentPresenter(
    private val view: LoadDocumentContract.View,
    private val repository: MarkdownRepository
) : LoadDocumentContract.Presenter {
    private fun cleanHtmlAnchors(content: String): String {
        // Удаление конструкций <a name="..."></a>
        var cleaned = content.replace(Regex("""<a\s+name\s*=\s*"[^"]*"\s*>\s*</a>"""), "")
        // Преобразование ссылок на якоря (#...) в текст
        cleaned = cleaned.replace(Regex("""\[([^\]]+)]\(#[^)]+\)"""), "$1")
        return cleaned
    }
    override fun loadFromFile(context: Context, uri: Uri) {
        repository.loadFromFile(context, uri)?.let { content ->
            view.showContent(cleanHtmlAnchors(content))
        } ?: view.showError("File load error")
    }

    override fun loadFromUrl(url: String) {
        repository.downloadToDownloadsAndRead(url) { content ->
            if (content != null) {
                view.showContent(cleanHtmlAnchors(content))
            } else {
                view.showError("Download failed")
            }
        }
    }

    override fun onFileSelected(uri: Uri) {
        view.openFilePicker()
    }
}