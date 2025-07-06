package com.example.markdown2.presentation

import android.content.Context
import android.net.Uri

interface LoadDocumentContract {
    interface View {
        fun showContent(content: String)
        fun showError(message: String)
        fun openFilePicker()
        fun hideProgress()
    }

    interface Presenter {
        fun loadFromFile(context: Context, uri: Uri)
        fun loadFromUrl(url: String)
        fun onFileSelected(uri: Uri)
    }
}