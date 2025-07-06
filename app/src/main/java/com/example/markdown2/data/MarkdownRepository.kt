package com.example.markdown2.data

import android.content.Context
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MarkdownRepository(
    private val fileDataSource: FileDataSource,
    private val context: Context

) {
    fun loadFromFile(context: Context, uri: Uri): String? {
        return fileDataSource.load(context, uri)
    }

    fun downloadToDownloadsAndRead(
        url: String,
        callback: (String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uri = FileDownloader(context).downloadToDownloads(url)
                val content = uri?.let { fileDataSource.load(context, it) }

                CoroutineScope(Dispatchers.Main).launch {
                    if (content != null) {
                        Toast.makeText(
                            context,
                            "File saved to Downloads",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    callback(content)
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    callback(null)
                }
            }
        }
    }
}