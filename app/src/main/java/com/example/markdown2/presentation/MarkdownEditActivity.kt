package com.example.markdown2.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.markdown2.R
import java.io.File

class MarkdownEditActivity : AppCompatActivity(), MarkdownEditContract.View {

    private lateinit var presenter: MarkdownEditContract.Presenter
    private lateinit var etContent: EditText
    private lateinit var btnBold: Button
    private lateinit var btnItalic: Button
    private lateinit var btnStrike: Button
    private lateinit var btnSave: Button
    private lateinit var filePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_markdown_edit)

        filePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: run {
            finish()
            return
        }

        etContent = findViewById(R.id.et_content)
        btnBold = findViewById(R.id.btn_bold)
        btnItalic = findViewById(R.id.btn_italic)
        btnStrike = findViewById(R.id.btn_strike)
        btnSave = findViewById(R.id.btn_save)

        presenter = MarkdownEditPresenter(this, this, filePath)

        // Загрузка содержимого файла
        try {
            val content = File(filePath).readText()
            etContent.setText(content)
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading file", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnBold.setOnClickListener { insertAroundSelection("**", "**") }
        btnItalic.setOnClickListener { insertAroundSelection("*", "*") }
        btnStrike.setOnClickListener { insertAroundSelection("~~", "~~") }

        btnSave.setOnClickListener {
            presenter.saveContent(etContent.text.toString())
        }
    }
    private fun insertAroundSelection(prefix: String, suffix: String) {
        val start = etContent.selectionStart
        val end = etContent.selectionEnd
        val text = etContent.text.insert(end, suffix).insert(start, prefix)
        etContent.text = text
        etContent.setSelection(end + prefix.length + suffix.length)
    }

    override fun showContent(content: String) {
        TODO("Not yet implemented")
    }

    override fun closeWithResult(content: String) {
        setResult(RESULT_OK, Intent().putExtra(EXTRA_CONTENT, content))
        finish()
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_CONTENT = "content"
        const val EXTRA_FILE_PATH = "file_path"
        fun getIntent(context: Context, filePath: String) =
            Intent(context, MarkdownEditActivity::class.java)
                .putExtra(EXTRA_FILE_PATH, filePath)
    }
}