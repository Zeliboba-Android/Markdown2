package com.example.markdown2.data

import android.content.Context
import android.net.Uri

class FileDataSource {
    fun load(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use {
                it.bufferedReader().readText()
            }
        } catch (e: Exception) {
            null
        }
    }
}