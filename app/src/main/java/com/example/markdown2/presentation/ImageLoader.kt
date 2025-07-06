package com.example.markdown2.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object ImageLoader {
    private val memoryCache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }
    }

    fun load(url: String, context: Context, callback: (Bitmap?) -> Unit) {

        memoryCache.get(url)?.let { cachedBitmap ->
            callback(cachedBitmap)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = when {
                    url.startsWith("http://") || url.startsWith("https://") -> {
                        loadFromNetwork(url)
                    }
                    url.startsWith("file://") -> {
                        val filePath = url.substringAfter("file://")
                        BitmapFactory.decodeFile(filePath)
                    }
                    url.startsWith("content://") -> {
                        context.contentResolver.openInputStream(Uri.parse(url))?.use {
                            BitmapFactory.decodeStream(it)
                        }
                    }
                    url.startsWith("asset://") -> {
                        val assetPath = url.substringAfter("asset://")
                        context.assets.open(assetPath).use {
                            BitmapFactory.decodeStream(it)
                        }
                    }
                    else -> null
                }

                bitmap?.let { memoryCache.put(url, it) }

                withContext(Dispatchers.Main) {
                    callback(bitmap)
                }
            } catch (e: Exception) {
                Log.e("ImageLoader", "Error loading image: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }

    private fun loadFromNetwork(url: String): Bitmap? {
        val connection = URL(url).openConnection() as HttpURLConnection
        return connection.run {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            doInput = true
            setRequestProperty("User-Agent", "MarkdownApp")
            connect()

            if (responseCode in 200..299) {
                inputStream.use { BitmapFactory.decodeStream(it) }
            } else null
        }
    }
}