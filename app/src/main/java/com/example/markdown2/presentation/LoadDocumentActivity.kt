package com.example.markdown2.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.markdown2.R
import com.example.markdown2.data.FileDataSource
import com.example.markdown2.data.MarkdownRepository
import java.io.File

class LoadDocumentActivity : AppCompatActivity(), LoadDocumentContract.View {
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startDownload()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private var pendingUrl: String? = null
    private lateinit var presenter: LoadDocumentContract.Presenter
    private val filePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { presenter.loadFromFile(this, it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_load_document)

        presenter = LoadDocumentPresenter(
            this,
            MarkdownRepository(FileDataSource(), this)
        )
        val btnLocal: Button = findViewById(R.id.btn_local)
        val btnUrl: Button = findViewById(R.id.btn_url)
        val etUrl: EditText = findViewById(R.id.et_url)
        btnLocal.setOnClickListener { presenter.onFileSelected(Uri.EMPTY) }
        btnUrl.setOnClickListener {
            var url = etUrl.text.toString().trim()
            if (url.contains("drive.google.com/file/d/")) {
                val fileId = url.substringAfter("/d/").substringBefore("/")
                url = "https://drive.google.com/uc?export=download&id=$fileId"
                etUrl.setText(url) // Показываем преобразованную ссылку
            }
            when {
                url.isEmpty() -> {
                    Toast.makeText(this, "Please enter URL", Toast.LENGTH_SHORT).show()
                }
                !url.startsWith("http://") && !url.startsWith("https://") -> {
                    Toast.makeText(this, "URL must start with http:// or https://", Toast.LENGTH_SHORT).show()
                }
                !isNetworkAvailable(this) -> {
                    Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    pendingUrl = url
                    checkStoragePermission()
                }
            }
        }
    }
    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startDownload()
        } else {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun startDownload() {
        pendingUrl?.let { url ->
            presenter.loadFromUrl(url)
        }
    }
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }
    override fun showContent(content: String) {
        val tempFile = File(cacheDir, "current_markdown.md").apply {
            writeText(content)
        }

        startActivityForResult(
            MarkdownViewActivity.getIntent(
                this,
                tempFile.absolutePath
            ),
            REQUEST_VIEW
        )
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun openFilePicker() {
        filePicker.launch("text/*")
    }

    override fun hideProgress() {
        Toast.makeText(this, "File loaded", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val REQUEST_VIEW = 1001
    }
}
