package com.example.markdown2.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.graphics.text.LineBreaker
import android.os.Build
import android.text.Layout
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.markdown2.R

@RequiresApi(Build.VERSION_CODES.Q)
class CodeBlockView(context: Context) : LinearLayout(context) {

    private lateinit var codeTextView: TextView
    private lateinit var copyButton: ImageButton
    private lateinit var languageLabel: TextView
    private lateinit var horizontalScrollView: HorizontalScrollView

    init {
        orientation = VERTICAL
        initViews()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun initViews() {
        // Верхняя панель
        val topBar = LinearLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16.dp, 8.dp, 8.dp, 8.dp)
            background = ContextCompat.getDrawable(context, R.color.code_block_header)
        }

        languageLabel = TextView(context).apply {
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                weight = 1f
            }
            setTextAppearance(context, R.style.CodeLanguageText)
        }

        copyButton = ImageButton(context).apply {
            layoutParams = LayoutParams(24.dp, 24.dp)
            setImageResource(R.drawable.ic_copy)
            background = null
            contentDescription = "Copy code"
            setOnClickListener { copyToClipboard() }
        }

        topBar.addView(languageLabel)
        topBar.addView(copyButton)

        // Область с кодом с горизонтальным скроллингом
        horizontalScrollView = HorizontalScrollView(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            isFillViewport = true
            overScrollMode = OVER_SCROLL_ALWAYS
            setBackgroundResource(R.drawable.code_block_background)
        }

        codeTextView = TextView(context).apply {
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            setTextAppearance(context, R.style.CodeText)
            setPadding(16.dp, 16.dp, 16.dp, 16.dp)
            typeface = Typeface.MONOSPACE
            movementMethod = ScrollingMovementMethod()
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = true
            // Сохраняем переносы строк
            setLineSpacing(0f, 1.1f)
            // Разрешаем перенос по словам (но не ломаем строки кода)
            breakStrategy = LineBreaker.BREAK_STRATEGY_BALANCED
            hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
        }

        horizontalScrollView.addView(codeTextView)

        addView(topBar)
        addView(horizontalScrollView)
    }

    fun setCode(code: String, language: String?) {
        codeTextView.text = code
        languageLabel.text = language ?: "Code"
        languageLabel.visibility = if (language.isNullOrEmpty()) GONE else VISIBLE

        // Рассчитываем ширину для длинных строк
        post {
            val maxLineWidth = calculateMaxLineWidth(code)
            val screenWidth = resources.displayMetrics.widthPixels

            if (maxLineWidth > screenWidth) {
                // Для очень длинных строк включаем горизонтальный скроллинг
                codeTextView.layoutParams.width = maxLineWidth + 32.dp // Добавляем padding
                codeTextView.isSingleLine = false
                codeTextView.ellipsize = null
            } else {
                // Для обычного текста оставляем как есть
                codeTextView.layoutParams.width = MATCH_PARENT
            }
            requestLayout()
        }
    }

    private fun calculateMaxLineWidth(code: String): Int {
        val paint = codeTextView.paint
        return code.lines()
            .maxOfOrNull { line -> paint.measureText(line) }?.toInt() ?: 0
    }

    private fun copyToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Code snippet", codeTextView.text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}