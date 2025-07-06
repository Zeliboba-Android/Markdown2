package com.example.markdown2.presentation

import android.graphics.Paint
import android.graphics.Typeface
import android.widget.TextView
import com.example.markdown2.domain.Style

fun TextView.applyStyles(styles: List<Style>) {
    styles.forEach { style ->
        when (style) {
            Style.BOLD -> setTypeface(typeface, Typeface.BOLD)
            Style.ITALIC -> setTypeface(typeface, Typeface.ITALIC)
            Style.STRIKETHROUGH -> paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }
    }
}