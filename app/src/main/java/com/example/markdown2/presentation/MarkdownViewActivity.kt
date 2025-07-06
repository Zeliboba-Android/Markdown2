package com.example.markdown2.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.example.markdown2.R
import com.example.markdown2.domain.MarkdownElement
import com.example.markdown2.domain.MarkdownParser
import com.example.markdown2.domain.Style
import com.example.markdown2.domain.TableData
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MarkdownViewActivity : AppCompatActivity(), MarkdownViewContract.View {

    private lateinit var presenter: MarkdownViewContract.Presenter
    private lateinit var container: LinearLayout
    private lateinit var btnEdit: Button
    private var filePath: String = ""
    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_markdown_view)

        container = findViewById(R.id.container)
        btnEdit = findViewById(R.id.btn_edit)

        presenter = MarkdownViewPresenter(this, MarkdownParser())

        try {
            filePath = when {
                intent.data != null -> ({
                    getFilePathFromUri(intent.data!!)
                }).toString()
                intent.hasExtra(EXTRA_FILE_PATH) -> {
                    intent.getStringExtra(EXTRA_FILE_PATH) ?: run {
                        showError("No file path provided")
                        finish()
                        return
                    }
                }
                else -> {
                    showError("No file path or content provided")
                    finish()
                    return
                }
            }

            // Чтение файла
            val content = readFileContent(filePath ?: run {
                showError("Invalid file path")
                finish()
                return
            })

            // Настройка кнопки редактирования
            btnEdit.setOnClickListener {
                startActivityForResult(
                    MarkdownEditActivity.getIntent(this, filePath),
                    REQUEST_EDIT
                )
            }

            // Парсинг и отображение контента
            presenter.parseContent(content)

        } catch (e: SecurityException) {
            showError("Permission denied: ${e.message}")
            finish()
        } catch (e: IOException) {
            showError("File read error: ${e.message}")
            finish()
        } catch (e: Exception) {
            showError("Unexpected error: ${e.message}")
            finish()
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EDIT && resultCode == RESULT_OK) {
            // Перезагружаем содержимое после редактирования
            try {
                val newContent = readFileContent(filePath)
                presenter.parseContent(newContent)
            } catch (e: Exception) {
                showError("Reload error: ${e.message}")
            }
        }
    }

    private fun getFilePathFromUri(uri: Uri): String? {
        return when (uri.scheme) {
            "file" -> uri.path // Прямой путь к файлу
            "content" -> { // ContentProvider
                contentResolver.openInputStream(uri)?.use { input ->
                    val tempFile = File.createTempFile("markdown_", ".md", cacheDir)
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                    tempFile.absolutePath
                }
            }
            else -> null
        }
    }

    private fun readFileContent(filePath: String): String {
        return File(filePath).takeIf { it.exists() }?.readText()
            ?: throw IOException("File not found or inaccessible")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun displayContent(elements: List<MarkdownElement>) {
        container.removeAllViews()

        runOnUiThread {
            elements.forEach { element ->
                when (element) {
                    is MarkdownElement.Header -> addHeader(element)
                    is MarkdownElement.Text -> addText(element)
                    is MarkdownElement.Image -> addImage(element)
                    is MarkdownElement.Table -> addTable(element.data)
                    is MarkdownElement.CodeBlock -> addCodeBlock(element)

                }
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun addCodeBlock(codeBlock: MarkdownElement.CodeBlock) {
        val codeView = CodeBlockView(this)
        codeView.setCode(codeBlock.code, codeBlock.language)

        val layoutParams = LinearLayout.LayoutParams(
            MATCH_PARENT,
            WRAP_CONTENT
        ).apply {
            setMargins(0, 16.dp, 0, 16.dp)
        }

        container.addView(codeView, layoutParams)
    }

    override fun showError(message: String) {
        TODO("Not yet implemented")
    }

    private fun addHeader(header: MarkdownElement.Header) {
        TextView(this).apply {
            text = header.text
            tag = header.anchor ?: header.text.lowercase()
                .replace(Regex("[^\\w\\d-]"), "")
                .replace(" ", "-")
            textSize = when (header.level) {
                1 -> 24f
                2 -> 22f
                3 -> 20f
                4 -> 18f
                5 -> 16f
                6 -> 14f
                else -> 12f
            }
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                MATCH_PARENT,
                WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 8)
            }
            container.addView(this)
        }
    }

    private fun addText(text: MarkdownElement.Text) {
        if (text.links.isEmpty()) {
            TextView(this).apply {
                this.text = text.content
                applyStyles(text.styles)
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    setMargins(0, 8, 0, 8)
                }

                // Для длинных текстов
                movementMethod = ScrollingMovementMethod()
                container.addView(this)
            }
        }else {
            // Текст со ссылками
            val linearLayout = LinearLayout(this).apply {
                orientation = HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            }

            var currentIndex = 0
            text.links.sortedBy { text.content.indexOf(it.text) }.forEach { link ->
                // Текст до ссылки
                if (link.text.isNotEmpty()) {
                    val beforeText = text.content.substring(currentIndex, text.content.indexOf(link.text))
                    if (beforeText.isNotEmpty()) {
                        linearLayout.addView(createTextView(beforeText, text.styles))
                    }

                    // Сама ссылка
                    val linkView = createTextView(link.text, text.styles).apply {
                        setTextColor(ContextCompat.getColor(context, R.color.link_color))
                        setOnClickListener {
                            if (link.isAnchor) {
                                // Прокрутка к заголовку внутри документа
                                scrollToHeader(link.target)
                            } else {
                                // Открытие внешней ссылки в браузере
                                openUrlInBrowser(link.target)
                            }
                        }
                    }
                    linearLayout.addView(linkView)

                    currentIndex = text.content.indexOf(link.text) + link.text.length
                }
            }

            // Текст после последней ссылки
            if (currentIndex < text.content.length) {
                val afterText = text.content.substring(currentIndex)
                linearLayout.addView(createTextView(afterText, text.styles))
            }

            container.addView(linearLayout)
        }
    }

    private fun openUrlInBrowser(url: String) {
        try {
            // Создаем Intent с явным флагом
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Пытаемся открыть ссылку
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this@MarkdownViewActivity,
                "No app can handle this link",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this@MarkdownViewActivity,
                "Invalid URL: $url",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    private fun createTextView(text: String, styles: List<Style>): TextView {
        return TextView(this).apply {
            this.text = text
            applyStyles(styles)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }
    }

    private fun scrollToHeader(anchor: String) {
        // Поиск заголовка по идентификатору и прокрутка к нему
        container.children.find { view ->
            (view as? TextView)?.text?.toString()?.equals(anchor, ignoreCase = true) == true
        }?.let { headerView ->
            container.scrollTo(0, headerView.top)
        }
    }
    private fun addImage(image: MarkdownElement.Image) {
        val basePath = File(filePath).parent ?: ""
        val resolvedUrl = resolveImagePath(image.url, basePath)

        ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                setMargins(0, 8.dp, 0, 8.dp)
                height = 300
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = image.altText

            ImageLoader.load(resolvedUrl, this@MarkdownViewActivity) { bitmap ->
                if (bitmap != null) {
                    setImageBitmap(bitmap)
                } else {
                    // Устанавливаем ресурс вместо Bitmap
                    setImageResource(R.drawable.ic_launcher_background)
                }
            }

            container.addView(this)
        }
    }
    private fun resolveImagePath(imagePath: String, basePath: String = ""): String {
        return when {
            imagePath.startsWith("http://") || imagePath.startsWith("https://") -> imagePath
            imagePath.startsWith("content://") -> imagePath
            imagePath.startsWith("asset://") -> imagePath

            // Обработка относительных путей
            imagePath.startsWith("./") -> {
                val relativePath = imagePath.substringAfter("./")
                "file://${File(basePath, relativePath).absolutePath}"
            }

            imagePath.startsWith("/") -> "file://$imagePath"

            else -> {
                // Проверяем все возможные места
                val localFile = File(basePath, imagePath)
                when {
                    localFile.exists() -> "file://${localFile.absolutePath}"
                    assets.list("")?.contains(imagePath) == true -> "asset://$imagePath"
                    else -> imagePath // Возвращаем как есть (может быть raw GitHub ссылкой)
                }
            }
        }
    }
    private fun addTable(table: TableData) {
        // Создаем горизонтальную прокрутку
        val horizontalScrollView = HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                MATCH_PARENT,
                WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            isFillViewport = true
        }

        // Создаем таблицу внутри прокрутки
        val tableLayout = TableLayout(this).apply {
            layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )
            isStretchAllColumns = true
            background = ContextCompat.getDrawable(context, R.drawable.table_cell_border)

            // Создаем строку для заголовков
            val headerRow = TableRow(context).apply {
                layoutParams = TableLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                table.headers.forEach { header ->
                    addView(TextView(context).apply {
                        text = header
                        setTypeface(typeface, Typeface.BOLD)
                        gravity = Gravity.CENTER
                        setPadding(8, 8, 8, 8)
                        background = ContextCompat.getDrawable(context, R.drawable.table_cell_border)
                    })
                }
            }
            addView(headerRow)

            // Создаем строки с данными
            table.rows.forEach { row ->
                val dataRow = TableRow(context).apply {
                    layoutParams = TableLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                    row.forEach { cell ->
                        addView(TextView(context).apply {
                            text = cell
                            gravity = Gravity.CENTER
                            setPadding(8, 8, 8, 8)
                            background = ContextCompat.getDrawable(context, R.drawable.table_cell_border)
                        })
                    }
                }
                addView(dataRow)
            }
        }

        horizontalScrollView.addView(tableLayout)
        container.addView(horizontalScrollView)
    }

    companion object {
        const val EXTRA_FILE_PATH = "file_path"
        const val REQUEST_EDIT = 1

        fun getIntent(context: Context, filePath: String): Intent {
            return Intent(context, MarkdownViewActivity::class.java).apply {
                putExtra(EXTRA_FILE_PATH, filePath)
            }
        }

    }
}